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

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.cougaar.util.Collectors;
import org.cougaar.util.Enumerator;
import org.cougaar.util.Filters;
import org.cougaar.util.SynchronizedTimeSpanSet;
import org.cougaar.util.Thunk;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * A Schedule is an encapsulation of spatio-temporal relationships.
 * It has a collection of ScheduleElements.
 */

public class ScheduleImpl 
  extends SynchronizedTimeSpanSet
  implements Schedule, NewSchedule
{
  protected String scheduleType = ScheduleType.OTHER;
  protected Class scheduleElementType = ScheduleElement.class;
                
  /** Construct an empty schedule **/
  public ScheduleImpl () {
    // default scheduleType to Other since quantity and quantityrange schedule
    // elements are probably the only schedules using meaningful scheuduletypes
    // for 1999
    scheduleType = ScheduleType.OTHER;
  }
        
  /** Construct a schedule which has the same elements as the specified
   * collection.  If the specified collection needs to be sorted, it will
   * be.
   **/
  public ScheduleImpl(Collection c) {
    super(c.size());
    
    if (c instanceof Schedule) {
      Schedule s = (Schedule)c;
      scheduleType = s.getScheduleType();
      scheduleElementType = s.getScheduleElementType();
      unsafeUpdate(c);
    } else {
      scheduleType = ScheduleType.OTHER;
      scheduleElementType = ScheduleElementType.MIXED;
      addAll(c);
    }

  }

  public String getScheduleType() {
    return scheduleType;
  }
  
  public Class getScheduleElementType() {
    return scheduleElementType;
  }
        
  public synchronized Date getStartDate() {
    TimeSpan ts = (TimeSpan) first();
    if (ts == null) {
      throw new IndexOutOfBoundsException("Called getStartDate on an empty schedule");
    }
    return new Date(ts.getStartTime());
  }

  public synchronized long getStartTime() {
    TimeSpan ts = (TimeSpan) first();
    if (ts == null) {
      throw new IndexOutOfBoundsException("Called getStartTime on an empty schedule");
    }
    return ts.getStartTime();
  }

  public synchronized Date getEndDate() {
    if (isEmpty()) {
      throw new IndexOutOfBoundsException("Called getEndDate on an empty schedule");
    }
    return new Date(getEndTime());
  }

  public synchronized long getEndTime() {
    if (isEmpty()) {
      throw new IndexOutOfBoundsException("Called getEndTime on an empty schedule");
    }
    long max = MIN_VALUE;
    for (int i = 0; i < size; i++) {
      ScheduleElement se = (ScheduleElement) elementData[i];
      long end = se.getEndTime();
      if (end > max) max = end;
    }
    return max;
  }

  /** get an enumeration over a copy of all of the schedule elements of this 
   * schedule.
   * Note that this is a copy, changes to the underlying schedule will not be 
   * reflected in the Enumeration.
   * @return Enumeration{ScheduleElement}
   */
  public synchronized Enumeration getAllScheduleElements() {
    ArrayList copy = new ArrayList(this);
    return new Enumerator(copy);
  }
   
  public synchronized Collection filter(UnaryPredicate predicate) {
    return Filters.filter(this, predicate);
  }

  
  /** get a colleciton of schedule elements that include this date.
   * Note that the schedule element can have a start or end date
   * that equals the given date or the date may fall in the time span
   * of a schedule element.
   * @return OrderedSet
   */
  public synchronized Collection getScheduleElementsWithDate(Date aDate) {
    return getScheduleElementsWithTime(aDate.getTime());
  }

  public synchronized Collection getScheduleElementsWithTime(final long aTime) {
    return intersectingSet(aTime);
  }
        
  public synchronized void applyThunkToScheduleElements(Thunk t) {
    Collectors.apply(t, this);
  }


  /** get a sorted Collection of schedule elements that have dates in the
   * given range of dates.  Note that these schedule elements may
   * or may not be fully bound by the date range - they may overlap.
   * @return OrderedSet
   */
  public synchronized Collection getOverlappingScheduleElements(Date startDate, Date endDate){
    return getOverlappingScheduleElements(startDate.getTime(), endDate.getTime());
  }

  public synchronized Collection getOverlappingScheduleElements(final long startTime, 
                                                                final long endTime)
  {
    return intersectingSet(startTime, endTime);
  }

  /** get a Collection of schedule elements that are fully bound
   * or encapsulated by a date range.
   * @return OrderedSet
   */
  public synchronized Collection getEncapsulatedScheduleElements(Date startDate, Date endDate) {
    return getEncapsulatedScheduleElements(startDate.getTime(), endDate.getTime());
  } 
        
  public synchronized Collection getEncapsulatedScheduleElements(final long startTime,
                                                                 final long endTime)
  {
    return encapsulatedSet(startTime, endTime);
  }

  /** add a single schedule element to the already existing Schedule.
   * @param aScheduleElement
   */
  public synchronized void addScheduleElement(ScheduleElement aScheduleElement) {
    add(aScheduleElement);
  }
        
  public synchronized void removeScheduleElement(ScheduleElement aScheduleElement) {
    remove(aScheduleElement);
  }

  public synchronized void clearScheduleElements() {
    clear();
  }

  /** set a single schedule element - used for a simple schedule
   * container will be cleared before it is added to ensure that 
   * there is only one schedule element.
   * @param aScheduleElement
   **/
  public synchronized void setScheduleElement(ScheduleElement aScheduleElement) {
    clear();
    add(aScheduleElement);
  }
  
  /** Return the Collection.   
   * This is now a noop.
   **/
  public Collection UnderlyingContainer() {
    return this;
  }


  public boolean isAppropriateScheduleElement(Object o) {
    return (scheduleElementType.isAssignableFrom(o.getClass()));
  }
        
  /** Set the schedule elements for this schedule.
   * Note this method assumes that you are adding things to 
   * an empty container, hence it clears the container of old
   * schedule elements before setting the new ones.
   */
  public synchronized void setScheduleElements(Collection collection) {
    clear();
    if (collection == null) return;    // setting it to null clears it

    addAll(collection);
  }

  // Over write ArrayList methods

  /** add object to Schedule. Verifies that object matches specifed
   * ScheduleElement type.
   * @param o Object to add to Schedule
   * @return boolean true if successful, else false
   */
  public synchronized boolean add(Object o) {
    if (!isAppropriateScheduleElement(o)) {
      ClassCastException cce = new ClassCastException("ScheduleImpl.add(Object o): o - " + o + " does not match schedule element type - " + getScheduleElementType());
      cce.printStackTrace();
      return false;
    }
    
    return super.add(o);
  }
      
  /** returns Iterator over a copy of the Schedule. Prints a warning and
   *  dumps a stack trace.
   *  Use filter() to get an copy which can be iterated over without 
   *  the warning.
   *  @return Iterator over a copy
   *  @deprecated Get a copy of the Schedule before iterating
   */
  public synchronized Iterator iterator() {
    Throwable throwable = 
      new Throwable("Returning an iterator over a copy of this Schedule." + 
                    " Stack trace is included so that calling code can be modified");
    throwable.printStackTrace();
    
    ArrayList copy = new ArrayList(this);
    return copy.iterator();
  }

  /** listIterator - Returns an iterator of the elements in this list 
   * (in proper sequence).
   * Iterator does not support add(Object o)/set(Object o)
   * @return ListIterator
   */
  public synchronized ListIterator listIterator() {
    return listIterator(0);
  }

  /** listIterator - Returns a list iterator of the elements in this list (in
   * proper sequence), starting at the specified position in the list.
   * Iterator does not support add(Object o)/set(Object o)
   * @return ListIterator
   */
  public ListIterator listIterator(final int index) {
    if (index<0 || index>size)
      throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);

//      System.out.println("ScheduleImpl.ListIterator()");
    return new ListItr(index);
  }

  /** returns a subset from a copy of the Schedule.  Prints a warning and
   *  dumps a stack trace. Subset made from a copy of the Schedule so that 
   *  the Schedule continues to Synchronization safe.
   *  Use filter() to get an copy which can be iterated over without 
   *  the warning.
   *  @return Iterator over a copy
   *  @deprecated Get a copy of the Schedule before calling subList
   */
  public synchronized List subList(int fromIndex, int toIndex) {
    Throwable throwable = 
      new Throwable("Returning an subList over a copy of this Schedule." +
                    " Stack trace is included so that calling code can be modified");
    throwable.printStackTrace();

    ArrayList copy = new ArrayList(this);
    return copy.subList(fromIndex, toIndex);
  }

  public synchronized void setScheduleElements(Enumeration someScheduleElements) {
    //clear the container
    clear();
    //unpack the enumeration
    while (someScheduleElements.hasMoreElements()) {
      Object o = someScheduleElements.nextElement();
      if (o instanceof ScheduleElement) {
        add((ScheduleElement)o);
      } else {
        throw new IllegalArgumentException("Schedule elements must be instanceof ScheduleElement");
      }
    }
  }
        
  /* setScheduleType - type can only be set for empty schedule.
   */
  public synchronized void setScheduleType(String type) {
    if (!isEmpty()) {
      throw new ClassCastException("Can not set ScheduleType for non-empty schedule");
    }
    scheduleType = type;
  }
  
  public synchronized void setScheduleElementType(Class setype) {
    if (!ScheduleElement.class.isAssignableFrom(setype)) {
      throw new ClassCastException(setype + " is  not a ScheduleElement");
    } else if (!isEmpty() &&
               !setype.isAssignableFrom(scheduleElementType)) {
      throw new ClassCastException(setype + 
                                   " is not assignable from current ScheduleElement type " + 
                                   scheduleElementType);
    }
    scheduleElementType = setype;
  }

  protected static final String EOL = System.getProperty("line.separator");

  public String toString() {
    String tstring = "?";
    String setstring = "?";
    if (scheduleType!=null) 
      tstring = scheduleType;
    if (scheduleElementType != null) 
      setstring = scheduleElementType.toString();
    return EOL+"<Schedule "+tstring+"/"+setstring+" "+super.toString()+">";
  }

        
  /* methods returned by ScheduleImplBeanInfo */

  public synchronized Date getStartDate_quiet() {
    return (isEmpty() ? (new Date(-1)) : getStartDate());
  }

  public synchronized Date getEndDate_quiet() {
    return (isEmpty() ? (new Date(-1)) : getEndDate());
  }

  public synchronized ScheduleElement[] getScheduleElements() {
    ScheduleElement s[] = new ScheduleElement[size()];
    return (ScheduleElement[])toArray(s);
  }

  public synchronized ScheduleElement getScheduleElement(int i) {
    return (ScheduleElement)elementData[i];
  }

  // Offered as an aid to extenders who want to get their hands on an
  // iterator.
  protected Iterator protectedIterator() {
    return super.iterator();
  }

  // Accessor for AbstractList.modCount - used by ListItr class
  protected int getModCount() {
    return modCount;
  }

  // ScheduleImpl specific. Code from java.util.AbstractList but
  // does not support add/set - don't want to mess up the sort order.
  private class ListItr implements ListIterator {

    /**
     * Index of element to be returned by subsequent call to next.
     */
    int cursor = 0;
    
    /**
     * Index of element returned by most recent call to next or
     * previous.  Reset to -1 if this element is deleted by a call
     * to remove.
     */
    int lastRet = -1;
    
    /**
     * The modCount value that the iterator believes that the backing
     * List should have.  If this expectation is violated, the iterator
     * has detected concurrent modification.
     */
    int expectedModCount = getModCount();
    
    ListItr(int index) {
      cursor = index;
    }

    public void set(Object o) {
      throw new UnsupportedOperationException("ScheduleImpl.ListIterator.set(Object o) is not supported.");
    }
    
    public void add(Object o) {
      throw new UnsupportedOperationException("ScheduleImpl.ListIterator.add(Object o) is not supported.");
    }
    
    public boolean hasNext() {
      return cursor != size();
    }
    
    public Object next() {
      try {
        Object next = get(cursor);
        checkForComodification();
        lastRet = cursor++;
        return next;
      } catch(IndexOutOfBoundsException e) {
        checkForComodification();
        throw new NoSuchElementException();
      }
    }
    
    public boolean hasPrevious() {
      return cursor != 0;
    }
    
    public Object previous() {
      try {
        Object previous = get(--cursor);
        checkForComodification();
        lastRet = cursor;
        return previous;
      } catch(IndexOutOfBoundsException e) {
        checkForComodification();
        throw new NoSuchElementException();
      }
    }
    
    public int nextIndex() {
      return cursor;
    }
    
    public int previousIndex() {
      return cursor-1;
    }
    
    
    public void remove() {
      if (lastRet == -1)
        throw new IllegalStateException();
      
      try {
        ScheduleImpl.this.remove(lastRet);
        if (lastRet < cursor)
          cursor--;
        lastRet = -1;
        
        int newModCount = getModCount();
        if (newModCount - expectedModCount > 1)
          throw new ConcurrentModificationException();
        expectedModCount = newModCount;
      } catch(IndexOutOfBoundsException e) {
        throw new ConcurrentModificationException();
      }
    }
    
    final void checkForComodification() {
      if (getModCount() != expectedModCount)
        throw new ConcurrentModificationException();
    }

  }
  
  private static class TestScheduleElementImpl extends LocationScheduleElementImpl
    implements ScheduleElementWithValue {

    private double myValue;

    public TestScheduleElementImpl(double value) {
      super();
      myValue = value;
    }

    public double getValue() {
      return myValue;
    }

    public ScheduleElementWithValue newElement(long start, long end, 
                                               double value) {
      TestScheduleElementImpl newElement = new TestScheduleElementImpl(value);
      newElement.stime = start;
      newElement.etime = end;
      return newElement;
    }

    public String toString() {
      return "<value:" + myValue + " " + stime + "-" + etime + ">";
    }
  }   

  public static void main(String []args) {
    Vector vector = new Vector();

    TestScheduleElementImpl lsei = new TestScheduleElementImpl(2.0);
    lsei.setEndTime(10);
    vector.add(lsei);
    
    lsei = new TestScheduleElementImpl(3.0);
    lsei.setEndTime(100000);
    vector.add(lsei);

    ScheduleImpl lsSchedule = new ScheduleImpl();
    lsei = new TestScheduleElementImpl(4.0);
    lsei.setEndTime(200);
    lsei.setStartTime(5);
    lsSchedule.add(lsei);

    ScheduleImpl schedule1 = new ScheduleImpl(vector);
    System.out.println("Schedule1: " + schedule1);

    ScheduleImpl schedule2 = new ScheduleImpl(lsSchedule);
    System.out.println("Schedule2: " + schedule2);

    schedule2.addAll(schedule1);
    System.out.println("Schedule2 after adding Schedule1: " + schedule2);

    ScheduleImpl schedule3 = 
      (ScheduleImpl )ScheduleUtilities.subtractSchedules(schedule2, schedule2);
    System.out.println("Schedule3 (Schedule2 - Schedule2): "+ schedule3);

    schedule2.setScheduleElements(vector);
    schedule2.addAll(1, vector);
  }
}






