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

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.util.GC;
import org.cougaar.util.log.Logger;

/**
 * Identifies all objects that have been (or are about to be) written
 * to persistence media. This purpose of this table is to remember
 * objects that have persisted in previous persistence snapshots so
 * that references to such objects in subsequent persistence deltas
 * can be replaced with references to the previously persisted
 * objects. This is similar to the wire handle of the Java
 * serialization process (ObjectOutputStream), but applies across
 * persistence snapshots whereas the wire handle applies within a
 * single snapshot.
 * <p>
 * This class is essentially a hash table implementation. It differs
 * from the java.util versions of hash tables in that the values are
 * {@link WeakReference}s so that values to which there are no longer
 * any references get removed from the table. WeakHashMap has weak
 * keys, not weak values.
 */
class IdentityTable {
  static class MyArrayList extends ArrayList {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   /**
     * Strangely enough, ArrayLists cannot be resized; this extension
     * adds setSize(). 
     * @param newSize the new size.  
     */
    public void setSize(int newSize) {
      while (this.size() < newSize) {
	this.add(null);
      }
      while (this.size() > newSize) {
	this.remove(this.size() - 1);
      }
    }
  }

  private int nextId = 0;

  private PersistenceAssociation[] table = new PersistenceAssociation[123];

  private int count = 0;

  private ReferenceQueue referenceQueue = new ReferenceQueue();

  private Collection rehydrationCollection = null;

  private Logger logger;

  /**
   * This list keeps all the PersistenceAssociation objects indexed
   * by their refId. Only the first encountered (last written) version
   * is kept.
   */
  private MyArrayList persistentObjects = new MyArrayList();

  IdentityTable(Logger logger) {
    this.logger = logger;
  }

  private void processQueue() {
    PersistenceAssociation pAssoc;
    while ((pAssoc = (PersistenceAssociation) referenceQueue.poll()) != null) {
      if (logger.isDetailEnabled()) logger.detail("processQueue removing " + pAssoc);
      int hashIndex = (pAssoc.hash & 0x7fffffff) % table.length;
      for (PersistenceAssociation x = table[hashIndex], prev = null; ; prev = x, x = x.next) {
        if (x == null) {
          break;  // Not found due to "clear()"
        }
        if (x == pAssoc) {
          if (prev == null) {
            table[hashIndex] = pAssoc.next;
          } else {
            prev.next = pAssoc.next;
          }
          count--;
          break;
        }
      }
      persistentObjects.set(pAssoc.getReferenceId().intValue(), null);
    }
  }

  public void setRehydrationCollection(Collection list) {
    rehydrationCollection = list;
    if (list == null) {
      GC.gc();
    }
  }

  public int getNextId() {
    return nextId;
  }

  public void setNextId(int newNextId) {
    nextId = newNextId;
  }

  /**
   * Find the PersistenceAssociation for an object.
   * @param object the object
   * @return the PersistenceAssociation for the object
   * @return null if the object has no current association
   */
  public PersistenceAssociation find(Object object) {
    processQueue();
    int hash = System.identityHashCode(object);
    int hashIndex = (hash & 0x7fffffff) % table.length;
    PersistenceAssociation pAssoc;
    for (pAssoc = table[hashIndex]; pAssoc != null; pAssoc = pAssoc.next) {
      if (pAssoc.get() == object) {
        return pAssoc;
      }
    }
    return null;
  }

  private void rehash() {
    PersistenceAssociation[] oldTable = table;
    int newLength = table.length * 2 + 1;
    table = new PersistenceAssociation[newLength];
    for (int i = 0; i < oldTable.length; i++) {
      PersistenceAssociation p = oldTable[i];
      while (p != null) {
        PersistenceAssociation n = p.next;
        int hashIndex = (p.hash & 0x7fffffff) % newLength;
        p.next = table[hashIndex];
        table[hashIndex] = p;
        p = n;
      }
    }
  }

  /**
   * Create a new PersistenceAssociation and enter it into the table.
   * @param o the object of the PersistenceAssociation
   * @param ref The PersistenceReference of the object.
   * @return the new PersistenceAssociation
   */
  public PersistenceAssociation create(Object o, PersistenceReference ref) {
    if (count > table.length * 3 / 4) {
      rehash();
    }
    PersistenceAssociation pAssoc = new PersistenceAssociation(o, ref, referenceQueue);
    if (rehydrationCollection != null) {
      rehydrationCollection.add(o);	// Make sure there is a reference to the object
    }
    int hashIndex = (pAssoc.hash & 0x7fffffff) % table.length;
    pAssoc.next = table[hashIndex];
    table[hashIndex] = pAssoc;
    int ix = ref.intValue();
    if (persistentObjects.size() <= ix) persistentObjects.setSize(ix + 1);
    if (persistentObjects.get(ix) != null) {
      throw new IllegalArgumentException("Slot full: " +
					 persistentObjects.get(ix) +
					 "<-" +
					 pAssoc);
    }
    persistentObjects.set(ix, pAssoc);
    count++;
    return pAssoc;
  }

  /**
   * Assign an identity to the given object. Makes an entry in the
   * identityTable if not already there and marks the entry as
   * "needing to be written".  The next time the
   * ObjectOutputStream.replaceObject method is called for the object
   * it will be written. Thereafter, a reference object will be
   * written.
   */
  public PersistenceAssociation findOrCreate(Object o) {
    PersistenceAssociation pAssoc = find(o);
    if (pAssoc != null) {
      return pAssoc;
    }
    return create(o, new PersistenceReference(nextId++));
  }

  /**
   * Get the PersistenceAssociation corresponding to a PersistenceReference.
   * @param ref the PersistenceReference
   * @return the corresponding PersistenceAssociation
   * @return null if the is no corresponding PersistenceAssociation
   */
  public PersistenceAssociation get(PersistenceReference ref) {
    return get(ref.intValue());
  }

  /**
   * Get the PersistenceAssociation at a particular index.
   * @param ix the index
   * @return the corresponding PersistenceAssociation
   * @return null if the is no corresponding PersistenceAssociation
   */
  public PersistenceAssociation get(int ix) {
    processQueue();
    if (ix < 0 || ix >= persistentObjects.size()) return null;
    return (PersistenceAssociation) persistentObjects.get(ix);
  }

  public int size() {
    return persistentObjects.size();
  }

  public Iterator iterator() {
    processQueue();
    return new Iterator() {
      Iterator iter = persistentObjects.iterator();
      Object nextObject = null;
      public boolean hasNext() {
        while (nextObject == null) {
          if (!iter.hasNext()) {
            return false;
          }
          nextObject = iter.next();
        }
        return true;
      }
      public Object next() {
        if (nextObject == null) {
          return iter.next();
        }
        Object result = nextObject;
        nextObject = null;
        return result;
      }
      public void remove() {
        throw new UnsupportedOperationException("remove not supported");
      }
    };
  }

  public void clear() {
    table = new PersistenceAssociation[123];
    count = 0;
    setRehydrationCollection(null);
  }
}
