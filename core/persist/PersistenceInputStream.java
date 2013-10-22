/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.persist;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.log.Logger;

/**
 * Read persisted objects from a stream. Detects objects that have
 * been wrapped in a PersistenceAssociation and resolves those to the
 * current definition of the object. The first occurence of any object
 * must be inside a PersistenceAssociation. This defining instance is
 * stored in the identityTable. Thereafter, its values are updated
 * from later versions of the same object.
 */
public class PersistenceInputStream extends ObjectInputStream implements PersistenceStream {
  private Logger logger;

  public MessageAddress getOriginator() { return null; }
  public MessageAddress getTarget() { return null; }

  @Override
public void close() throws IOException {
    super.close();
  }

  /**
   * The array of object references that are expected during the
   * decoding of an object.
   */
  private PersistenceReference[] references;

  /**
   * Steps through the references during decoding.
   */
  private int nextReadIndex;

  /**
   * InputStream implementation that extracts a segment of bytes from
   * an ObjectInputStream. This is the input counterpart of the
   * PersistenceOutputStream.writeBytes
   */
  private static class Substream extends FilterInputStream {
    private int bytes;
    public Substream(ObjectInputStream ois) throws IOException {
      super(ois);
      bytes = ois.readInt();
    }
    @Override
   public int read(byte[] buf) throws IOException {
      return read(buf, 0, buf.length);
    }
    @Override
   public int read() throws IOException {
      if (bytes == 0) return -1;
      bytes--;
      return super.read();
    }
    @Override
   public int read(byte[] buf, int offset, int nb) throws IOException {
      if (nb > bytes) nb = bytes;
      nb = super.read(buf, offset, nb);
      if (nb >= 0) bytes -= nb;
      return nb;
    }
  }

  /**
   * Construct from the object stream
   * @param ois ObjectInputStream
   * @param logger Logger to use for progress
   */
  public PersistenceInputStream(ObjectInputStream ois, Logger logger) throws IOException {
    super(new Substream(ois));
    enableResolveObject(true);
    this.logger = logger;
  }

  /**
   * Read the association for one object. This is the inverse of
   * PersistenceOutputStream.writeAssociation. The active state of the
   * PersistenceAssociation is set according to whether it was active
   * when the persistence delta was generated.
   * @param references the array of references for objects that were
   * written when this association was written. This allows us to know
   * in advance the identity of each object as it is read.
   */
  public PersistenceAssociation readAssociation(PersistenceReference[] references)
    throws IOException, ClassNotFoundException
  {
    this.references = references;
    nextReadIndex = 0;
    int active = readInt();
    PersistenceIdentity clientId = (PersistenceIdentity) readObject();
    Object object = readObject();
    if (object == null) {
      String msg =
        "Rehydrated object is null. nextReadIndex is "
        + nextReadIndex + "/" + references.length;
      logger.error(msg);
      return null;
    }
    if (object instanceof ActivePersistenceObject) {
      ((ActivePersistenceObject) object).checkRehydration(logger);
    }
    this.references = null;
    PersistenceAssociation pAssoc = identityTable.find(object);
    if (pAssoc == null) {
      logger.error("Null PersistenceAssociation found for " + object.getClass().getName() + ": " + object);
    } else {
      pAssoc.setActive(active);
      pAssoc.setClientId(clientId);
    }
    if (logger.isDetailEnabled()) logger.detail("read association " + pAssoc);
    return pAssoc;
  }

  // Use reflection to avoid calling super.newInstanceFromDesc. Don't want to
  // force installation of javaiopatch.jar for compilation if persistence not 
  // involved.
  private static Method _ano = null;
  private static Object _anoLock = new Object();
  private static Object callNewInstanceFromDesc(ObjectInputStream stream, ObjectStreamClass desc) 
    throws InstantiationException, IllegalAccessException
  {
    Method m;
    synchronized (_anoLock) {
      if ((m = _ano) == null) {
        try {
          Class c = ObjectInputStream.class;
          Class[] argp = new Class[] {ObjectStreamClass.class};
          m = c.getDeclaredMethod("real_newInstanceFromDesc", argp);
          _ano = m;
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("javaiopatch is not installed properly!");
          throw new RuntimeException("javaiopatch not installed");
        }
      }
    }
    try {
      Object[] args= new Object[]{desc};
      return m.invoke(stream, args);
    } catch (Exception e) {
      e.printStackTrace();
      if (e instanceof InvocationTargetException) {
        Throwable t = ((InvocationTargetException)e).getTargetException();
        if (t instanceof RuntimeException) {
          throw (RuntimeException)t;
        } else if (t instanceof InstantiationException) {
          throw (InstantiationException) t;
        } else if (t instanceof IllegalAccessException) {
          throw (IllegalAccessException) t;
        }
      }
      throw new RuntimeException("javaiopatch not installed");
    }
  }


  /**
   * Allocate an object to be filled in from the serialized
   * stream. This is a hook provided by the ObjectInputStream class
   * for obtaining an object whose fields can be filled in. Normally,
   * this returns a brand new object, but during rehydration we need
   * to update the values of objects that already exist so we override
   * this method and return existing objects corresponding to the
   * reference ids we expect to encounter.
   * @param desc description of class to create
   * @return the object to be filled in.
   */
  protected Object newInstanceFromDesc(ObjectStreamClass desc) 
    throws InstantiationException, IllegalAccessException  {
    Class clazz = desc.forClass();
    if (references != null &&
	clazz != PersistenceReference.class &&
	!clazz.isArray() &&
	clazz != String.class) {
      PersistenceReference reference = references[nextReadIndex++];
      if (reference != null) {
	PersistenceAssociation pAssoc = identityTable.get(reference);
	if (pAssoc == null) {
	  Object object = callNewInstanceFromDesc(this, desc);
	  pAssoc = identityTable.create(object, reference);
	  if (logger.isDetailEnabled()) logger.detail("Allocating " + (nextReadIndex-1) + " " + PersistenceServiceComponent.getObjectName(object) + " @ " + reference);
	  return object;
	}
	Object result = pAssoc.getObject();
	if (result == null) throw new InstantiationException("no object @ " + reference);
	if (result.getClass() != clazz) throw new InstantiationException("wrong object @ " + reference);
	if (logger.isDetailEnabled()) logger.detail("Overwriting " + (nextReadIndex-1) + " " + PersistenceServiceComponent.getObjectName(result) + " @ " + reference);
	return result;
      } else {
        Object result = callNewInstanceFromDesc(this, desc);
        if (logger.isDetailEnabled()) logger.detail("Allocating " + (nextReadIndex-1) + " " +
              PersistenceServiceComponent.getObjectName(result));
        return result;
      }
    }
    Object result = callNewInstanceFromDesc(this, desc);
    if (logger.isDetailEnabled()) logger.detail("Allocating " + PersistenceServiceComponent.getObjectName(result));
    return result;
  }

  /**
   * Resolve an object just read from the stream into the actual
   * result object. We replace PersistenceReference objects with the
   * object to which they refer.
   * @param o the object to resolve.
   * @return the replacement.
   */
  @Override
protected Object resolveObject(Object o) throws IOException {
    if (o instanceof PersistenceReference) {
      PersistenceReference pRef = (PersistenceReference) o;
      PersistenceAssociation pAssoc = identityTable.get(pRef);
      if (pAssoc == null) {
	logger.error("Reference to non-existent object id = " + pRef);
	for (int i = 0; i < identityTable.size(); i++) {
	  logger.error(i + ": " + identityTable.get(i));
	}
	throw new IOException("Reference to non-existent object id = " + pRef);
//  	return null;
      }
      Object result = pAssoc.getObject();
      if (logger.isDetailEnabled()) logger.detail("Resolving " + PersistenceServiceComponent.getObjectName(result) + " @ " + pRef);
      return result;
    } else {
      if (logger.isDetailEnabled()) logger.detail("Passing " + PersistenceServiceComponent.getObjectName(o));
      return o;
    }
  }

  /**
   * Object identity table. This is supplied by the creator of this stream.
   */
  private IdentityTable identityTable;

  /**
   * Get the IdentityTable being used by this stream. This is not
   * normally used since the IdentityTable is usually maintained by
   * the creator of this stream.
   * @return the IdentityTable being used by this stream.
   */
  public IdentityTable getIdentityTable() {
    return identityTable;
  }

  /**
   * Set the IdentityTable to be used by this stream. The
   * IdentityTable contains assocations of objects to earlier
   * persistence deltas. References to these earlier objects are
   * replaced with reference objects to save space.
   * @param identityTable the new IdentityTable to use.
   */
  public void setIdentityTable(IdentityTable identityTable) {
    this.identityTable = identityTable;
  }
}
