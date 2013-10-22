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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.LinkedByteOutputStream;
import org.cougaar.util.log.Logger;

/**
 * Write persistable objects to a stream.
 * @see PersistenceInputStream
 */
public class PersistenceOutputStream extends ObjectOutputStream implements PersistenceStream {

  private static final int DEFAULT_INITIAL_SIZE = 10000;

  private Logger logger;
  private boolean detail;

  public MessageAddress getOriginator() { return null; }
  public MessageAddress getTarget() { return null; }

  /**
   * Keeps track of the (identity of) objects written to the stream.
   */
  private List writeIndex = null;

  /**
   * The stream to which the objects are written
   */
  private LinkedByteOutputStream byteStream;

  /**
   * Public constructor
   */
  public PersistenceOutputStream(Logger logger) throws IOException {
    this(new LinkedByteOutputStream(DEFAULT_INITIAL_SIZE), logger);
  }

  /**
   * Private constructor needed to capture the byte buffer
   * we are using.
   * @param stream the byte buffer into which we store everything.
   */
  private PersistenceOutputStream(LinkedByteOutputStream stream, Logger logger) throws IOException {
    super(stream);
    byteStream = stream;
    enableReplaceObject(true);
    this.logger = logger;
    this.detail = logger.isDetailEnabled();
  }

//    /**
//     * Get the array of bytes encoding everything we stored.
//     * @return the array of bytes encoding everything we stored.
//     */
//    public byte[] getBytes() throws IOException {
//      flush();
//      if (history != null) {
//        history.close();
//        history = null;
//      }
//      return byteStream.toByteArray();
//    }

  /**
   * Write the bytes encoding everything we stored preceded by the
   * byte count.
   */
  public void writeBytes(ObjectOutputStream oos) throws IOException {
    oos.writeInt(byteStream.size());
    byteStream.writeTo(oos);
  }

  public byte[] getBytes() {
    return byteStream.toByteArray();
  }

  public int size() {
    return byteStream.size();
  }

  /**
   * Write a plan object from a PersistenceAssociation.
   * @param pAssoc the PersistenceAssocation having the object to save.
   * @return an array of the PersistenceReferences of the objects that
   * were actually saved. This typically includes the objects that are
   * referenced by the object being saved (and the objects they
   * reference, etc.).
   */
  public PersistenceReference[] writeAssociation(PersistenceAssociation pAssoc)
    throws IOException
  {
    Object obj = pAssoc.getObject();
    if (obj == null) {
      logger.error("pAssoc has null object: " + pAssoc);
    }
    writeIndex = new ArrayList();
    writeInt(pAssoc.getActive());
    writeObject(pAssoc.getClientId());
    try {
      writeObject(obj);
    } catch (java.io.NotSerializableException e) {
      logger.error(e + " for: " + obj.getClass().getName()
                   + ": " + obj);
    } catch (IllegalArgumentException e) {
      logger.error(e + " for: " + obj.getClass().getName());
    } catch (Exception e) {
      logger.error(e + " for: " + obj.getClass().getName()
                   + ": " + obj);
      if (e.getClass().getName().equals(java.io.NotSerializableException.class.getName())) {
        logger.error("Catch failed", e);
      } else {
        IOException ioe = new IOException();
        ioe.initCause(e);
        throw ioe;
      }
    }
    PersistenceReference[] result = new PersistenceReference[writeIndex.size()];
    result = (PersistenceReference[]) writeIndex.toArray(result);
    writeIndex = null;
    return result;
  }

  /**
   * Replace objects that are in the identityTable with reference
   * objects. This operation is suppressed for objects that have
   * changed and actually need to be written.
   * @param o the object to consider for replacement.
   * @return the replacement object.
   */
  @Override
protected Object replaceObject(Object o) {
    if (o instanceof PersistenceReference) {
      if (detail) print("Writing Ref ", o);
      return o;
    }
    if (o.getClass().isArray()) {
      if (detail) print("Writing array " + java.lang.reflect.Array.getLength(o));
      return o;
    }
    if (o instanceof String) {
      if (detail) print("Writing String ", o);
      return o;
    }
    PersistenceAssociation pAssoc = identityTable.find(o);
    String ix = null;
    if (detail) {
      if (writeIndex != null) {
        ix = writeIndex.size() + " ";
      } else {
        ix = "";
      }
    }
    if (pAssoc == null) {
      // This is (probably) _not_ something we care about
      if ((o instanceof ActivePersistenceObject) &&
          ((ActivePersistenceObject) o).skipUnpublishedPersist(logger)) {
        if (detail) print("Skipping " + ix + getShortClassName(o) + " " + o);
	return null;            // Not published yet
      }
      if (writeIndex != null) {
	writeIndex.add(null);   // No identityTable fixup needed
      }
      if (detail) print("Writing " + ix + getShortClassName(o) + " " + o);
      return o;
    }
    if (pAssoc.isMarked()) {
      if (writeIndex != null) {
        // Remember that we wrote it here
	writeIndex.add(pAssoc.getReferenceId());
      }
      if (detail) print("Writing " + ix, pAssoc, " as ", o);
      return o;
    }
    if (detail) print("Subst ", pAssoc, " for ", o);
    return pAssoc.getReferenceId();
  }

  private String getShortClassName(Object o) {
    String cn = o.getClass().getName();
    int dot = cn.lastIndexOf('.');
    if (dot >= 0) return cn.substring(dot + 1);
    return cn;
  }

  private void print(String intro, PersistenceAssociation pAssoc,
                     String prep, Object o) {
    print(intro + pAssoc.getReferenceId() + (pAssoc.isActive() ? " active" : " inactive") + prep + o);
  }

  private void print(String intro, Object o) {
    print(intro + o);
  }

  private void print(String message) {
    logger.detail(message);
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
