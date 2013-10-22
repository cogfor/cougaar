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

package org.cougaar.planning.ldm.plan;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.cougaar.core.util.UID;

/** ContextOfUIDs is an implementation of Context. It is simply a collection of UIDs. 
 * It can be used when the "problem" or "problems" that a task is related to can be
 * be referenced by the "problem's" UID.
 * @see UID
 * @see Context
 */
public class ContextOfUIDs 
  extends AbstractCollection  implements Context, Collection
{

  UID[] uids;
  
  /** Copies all UID objects from uids into the ContextOfUIDs 
   * If there are objects in uids that are not UID objects, they will not be added.
   * @see org.cougaar.core.util.UID
   */
  public ContextOfUIDs(Collection uids){
    int UIDcount=0;
    // Count the number of elements in the collection that are actually UIDs
    for (Iterator iterator = uids.iterator(); iterator.hasNext();) {
      Object o = iterator.next();
      if (o instanceof UID) 
	UIDcount++;
    }
    // Now go through again and add the UIDs to the array
    this.uids = new UID[UIDcount];
    int i = 0;
    for (Iterator iterator = uids.iterator(); iterator.hasNext(); ) {
      Object o = iterator.next();
      if (o instanceof UID) 
	this.uids[i] = (UID)o;
    }
  }

  /** Creates empty ContextOfUIDs  
   *  This is completely useless as this object is immutable.
   */
  public ContextOfUIDs(){
  }

  /** 
   * Constructor that creates a collection with one and only one UID
   */
  public ContextOfUIDs(UID oneUID){
    uids = new UID[1];
    uids[0] = oneUID;
  }

  /**
   * A constructor that copies the elements of the passed in array into the collection
   */
  public ContextOfUIDs(UID[] arrayOfUIDS) {
    uids = new UID[arrayOfUIDS.length];
    for (int i=0; i<arrayOfUIDS.length; i++) {
      uids[i] = arrayOfUIDS[i];
    }
  }

  public Iterator iterator() {
    return new UIDIterator(uids);
  }

  /** @return the number of elements in the collection */
  public int size() {
    return uids.length;
  }

  public boolean contains(Object o) {
    if (o instanceof UID) {
      UID inthere = (UID)o;
      for (int i = 0; i < uids.length; i++) {
	if (uids[i].equals(inthere))
	  return true;
      }
    }
    return false;
  }

  /** 
   * @return true if this collection contains all of the elements in other
   */
  public boolean containsAll(Collection other) {
    boolean found = false;
    for (Iterator otherIterator = other.iterator(); otherIterator.hasNext();) {
      Object o=otherIterator.next();
      if (!(o instanceof UID))
	return false;
      UID otherUID = (UID)o;
      found = false;
      for (int i=0; i<uids.length; i++) {
	if (otherUID.equals(uids[i])) {
	  found = true;
	  break;
	}
      }
      if (!found) 
	return false;
    }
    return true;
  }

  /**
   * @return an array of Object containing UIDs in the collection 
   * @see org.cougaar.core.util.UID
   **/
  public Object[] toArray() {
    return toArray(new UID[uids.length]);
  }

  /**
   * @return an array of UIDs with all the UIDs in the collection
   * @param array an array of UID. A new array will be allocated if array size is incorrect.
   * 
   * @exception Throws ArrayStoreException if array is not an array of UID
   **/
  public Object[] toArray(Object[] array) {
    if (!(array instanceof UID[]))
      throw new ArrayStoreException("array must be an array of UID");

    if (array.length != uids.length) {
      array = new UID[uids.length];
    }
    System.arraycopy(uids, 0, array, 0, uids.length);
    return array;
  }

  /**
   * Simple accessor for n-th uid. Avoids consing iterators or arrays.
   **/
  public UID get(int i) {
    return uids[i];
  }

  public static class UIDIterator implements Iterator {
    UID[] uidArray;
    int place = 0;
    UIDIterator(UID[] uids) {
      uidArray = uids;
    }
    public boolean hasNext() {
      if (place < uidArray.length)
	return true;
      return false;
    }
    public Object next() {
      return uidArray[place++];
    }

    /**
     * @exception always throws UnsupportedOperationException
     */
    public void remove() {
      throw new UnsupportedOperationException("ContextOfUIDs is immutable collection");
    }
  }

  public String toString() {
    String output = "[ContextOfUIDs ";
    for (int i=0; i <uids.length; i++) {
      output += uids[i].toString();
      output += " ";
    }
    output += "]";
    return output;
  }

  /**
   * Convenience method that creates a new ContextOfUIDs that is the union of
   * the of all of the members of the Collection passed in. Items in the Collection
   * that are not instances of ContextOfUIDs are ignored.
   * @param contexts A Collection containing ContextOfUIDs
   * @return the union of the members of parameter
   */
  public static ContextOfUIDs merge(Collection contexts) {
    
    // Add the UIDs to a HashMap. The HashMap will prevent duplicates
    HashSet union = new HashSet(5);

    for (Iterator contextsIt = contexts.iterator(); contextsIt.hasNext();) {
      Object o = contextsIt.next();
      if (o instanceof ContextOfUIDs) {
	ContextOfUIDs couid = (ContextOfUIDs) o;
	for (Iterator it = couid.iterator(); it.hasNext();) {
	  UID oneUID = (UID)it.next();
          union.add(oneUID);
	}
      }
    }
    // create a new ContextOfUIDs from the Collection of the HashMap
    return new ContextOfUIDs(union);
  }
}
