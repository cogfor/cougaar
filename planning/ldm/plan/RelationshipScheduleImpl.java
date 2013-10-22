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

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.util.UID;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;
import org.cougaar.util.UnaryPredicate;

/** 
 * A RelationshipSchedule is a representation of an object (must implement
 * HasRelationships) relationships 
 **/

public class RelationshipScheduleImpl extends ScheduleImpl 
  implements NewRelationshipSchedule {
  private HasRelationships myHasRelationships;
  
  public RelationshipScheduleImpl() {
    super();
    setScheduleType(ScheduleType.RELATIONSHIP);
    setScheduleElementType(ScheduleElementType.RELATIONSHIP);
  }

  public RelationshipScheduleImpl(HasRelationships hasRelationships) {
    this();
    
    setHasRelationships(hasRelationships);
  }

  public RelationshipScheduleImpl(HasRelationships hasRelationships, 
                                  Collection relationships) {
    this(hasRelationships);
    
    addAll(relationships);
  }
                
  /** Construct a schedule which has the same elements as the specified
   * collection.  If the specified collection needs to be sorted, it will
   * be.
   **/
  public RelationshipScheduleImpl(RelationshipSchedule schedule) {
    this(schedule.getHasRelationships(), schedule);
  }

  
  public HasRelationships getHasRelationships() {
    return myHasRelationships;
  }


  public synchronized void setHasRelationships(HasRelationships hasRelationships) {
    if (!isEmpty()) {
      throw new IllegalArgumentException("RelationshipScheduleImpl.setHasRelationships() can only be called on an empty schedule"); 
    }
    
    myHasRelationships = hasRelationships;
  }
    
  public synchronized boolean isAppropriateScheduleElement(Object o) {
    if (!super.isAppropriateScheduleElement(o)) {
      return false;
    }

    Relationship relationship = (Relationship)o;

    if ((myHasRelationships == null) ||
        ((!relationship.getA().equals(myHasRelationships)) &&
         (!relationship.getB().equals(myHasRelationships)))) {
      return false;
    }

    return true;
  }

  /** getMatchingRelationships - return all Relationships which pass the
   * specified UnaryPredicate.
   * 
   * @param predicate UnaryPredicate to use in screening Relationships
   * @return a sorted Collection containing all Relationships which
   * which pass the specified UnaryPredicate
   **/
  public synchronized Collection getMatchingRelationships(UnaryPredicate predicate) {
    return filter(predicate);
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role.
   * 
   * @param role Role to look for
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role
   **/
  public synchronized Collection getMatchingRelationships(final Role role) {
    final RelationshipScheduleImpl schedule = this;
    return filter(new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return schedule.getOtherRole(relationship).equals(role);
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and intersect the time.
   * 
   * @param role Role to look for
   * @param time long specifying the time
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and intersect the specified time 
   **/
   public Collection getMatchingRelationships(final Role role, final long time) {
    final RelationshipScheduleImpl schedule = this;
   
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).equals(role)) &&
                (time >= relationship.getStartTime()) &&
                (time < relationship.getEndTime()));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final TimeSpan timeSpan) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).equals(role)) &&
                (relationship.getStartTime() < timeSpan.getEndTime()) &&
                (relationship.getEndTime() > timeSpan.getStartTime()));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and overlap the time span specified by the start and end
   * time arguments
   * 
   * @param role Role to look for
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   * @deprecated Use getMatchingRelationships(Role role, TimeSpan timeSpan) or
   * getMatchingRelationships(Role role, long time)
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final long startTime, 
                                                          final long endTime) {
    final TimeSpan timeSpan = TimeSpans.getSpan(startTime, endTime);

    return getMatchingRelationships(role, timeSpan);
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object, match the specified role and intersect the time.
   * 
   * @param role Role to look for
   * @param otherObject HasRelationships 
   * @param time long specifying the time
   * @return a sorted Collection containing all Relationships which contain 
   * the specified other object, match the specified role and direct object 
   * flag, and intersect the specified time.
   **/
    public Collection getMatchingRelationships(final Role role, 
					       final HasRelationships otherObject, 
					       final long time) {
    final RelationshipScheduleImpl schedule = this;
      return filter( new UnaryPredicate() {
	public boolean execute(Object obj) {
	  Relationship relationship = (Relationship)obj;
	  return ((schedule.getOtherRole(relationship).equals(role)) &&
		  (schedule.getOther(relationship).equals(otherObject)) &&
		  (time >= relationship.getStartTime()) &&
		  (time < relationship.getEndTime()));
	}
      });
    }
    

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object, match the specified role and and overlap the specified time 
   * span.
   * 
   * @param role Role to look for
   * @param other HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final HasRelationships other,
                                                          final TimeSpan timeSpan) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).equals(role)) &&
                (schedule.getOther(relationship).equals(other)) &&
                ((relationship.getStartTime() < timeSpan.getEndTime()) &&
                 (relationship.getEndTime() > timeSpan.getStartTime())));
      }
    });
  }


  /** getMatchingRelationships - return all Relationships which match the 
   * specified other object, match the specified role, and overlap the 
   * the time span specified by the start and end time arguments
   *
   * @param role Role to look for
   * @param other HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role, direct object flag and overlap the 
   * specified time span.
   * @deprecated Use getMatchingRelationships(Role role, HasRelationships otherObject, TimeSpan timeSpan) or
   * getMatchingRelationships(Role role, HasRelationships otherObject, long time)
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final HasRelationships other,
                                                          final long startTime, 
                                                          final long endTime) {
    final TimeSpan timeSpan = TimeSpans.getSpan(startTime, endTime);

    return getMatchingRelationships(role, other, timeSpan);
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and intersect the specified time.
   * 
   * @param otherObject HasRelationships 
   * @param time long 
   * @return a sorted Collection containing all Relationships which
   * which contain the specified other HasRelationships and intersect the 
   * specified time span
   **/
  public Collection getMatchingRelationships(final HasRelationships otherObject,
					     final long time) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOther(relationship).equals(otherObject)) &&
                (time >= relationship.getStartTime()) &&
                (time < relationship.getEndTime()));
      }
    });
  }


  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the specified time span.
   * 
   * @param other HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final HasRelationships other,
                                                          final TimeSpan timeSpan) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOther(relationship).equals(other)) &&
                ((relationship.getStartTime() < timeSpan.getEndTime()) &&
                 (relationship.getEndTime() > timeSpan.getStartTime())));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the time span specified by the start and end
   * time arguments.
   * 
   * @param other HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified direct object flag and overlap the 
   * specified time span
   * @deprecated Use getMatchingRelationships(HasRelationships otherObject, TimeSpan timeSpan) or
   * getMatchingRelationships(HasRelationships otherObject, long time)
   **/
  public synchronized Collection getMatchingRelationships(final HasRelationships other,
                                                          final long startTime, 
                                                          final long endTime) {
    final TimeSpan timeSpan = TimeSpans.getSpan(startTime, endTime);

    return getMatchingRelationships(other, timeSpan);
  }

  /** getMatchingRelationships - return all Relationships where the role
   * ends with the specifed suffix and intersects the specified time.
   * 
   * @param roleSuffix String
   * @param time long specifying the time
   * @return a sorted Collection containing all Relationships which
   * which match the specified role suffix and intersect the specified time
   **/
  public synchronized Collection getMatchingRelationships(final String roleSuffix,
                                                          final long time) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).getName().endsWith(roleSuffix)) &&
                (time >= relationship.getStartTime()) &&
                (time < relationship.getEndTime()));
      }
    });
  }


  /** getMatchingRelationships - return all Relationships where the role
   * ends with the specifed suffix and overlap the specified time span.
   * 
   * @param roleSuffix String
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified role suffix and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final String roleSuffix,
                                                          final TimeSpan timeSpan) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).getName().endsWith(roleSuffix)) &&
                ((relationship.getStartTime() < timeSpan.getEndTime()) &&
                 (relationship.getEndTime() > timeSpan.getStartTime())));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships where the role
   * ends with the specifed suffix and overlap the time span specified by
   * the start and end time arguments
   * 
   * @param roleSuffix String
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified role suffix and overlap the 
   * specified time span
   * @deprecated Use getMatchingRelationships(String roleSuffix, TimeSpan timeSpan) or
   * getMatchingRelationships(String roleSuffix, long time)
   **/
  public synchronized Collection getMatchingRelationships(final String roleSuffix,
                                                          final long startTime, 
                                                          final long endTime) {
    final TimeSpan timeSpan = TimeSpans.getSpan(startTime, endTime);

    return getMatchingRelationships(roleSuffix , timeSpan);
  }

  /** getMatchingRelationships - return all Relationships which intersect the 
   * specified time.
   * 
   * @param time long specifying the time
   * @return a sorted Collection containing all Relationships which
   * which intersect the specified time 
   **/
  public synchronized Collection getMatchingRelationships(final long time) {
    return filter(new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((time >= relationship.getStartTime()) &&
                (time < relationship.getEndTime()));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which overlap the 
   * specified time span.
   * 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final TimeSpan timeSpan) {
    return filter(new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((relationship.getStartTime() < timeSpan.getEndTime()) &&
                (relationship.getEndTime() > timeSpan.getStartTime()));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which overlap the 
   * time span specified by the start and end time arguments
   * 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match overlap the specified time span
   * @deprecated Use getMatchingRelationships(TimeSpan timeSpan) or
   * getMatchingRelationships(long time)
   **/
  public synchronized Collection getMatchingRelationships(final long startTime, 
                                                          final long endTime) {
    final TimeSpan timeSpan = TimeSpans.getSpan(startTime, endTime);

    return getMatchingRelationships(timeSpan);
  }

  /** getMyRole - return role for schedule's HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  public Role getMyRole(Relationship relationship) {
    if (relationship.getA().equals(getHasRelationships())) {
      return relationship.getRoleA();
    } else if (relationship.getB().equals(getHasRelationships())) {
      return relationship.getRoleB();
    } else {
      return null;
    }
  }

  /** getMyRole - return role for other HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  public Role getOtherRole(Relationship relationship) {
    if (relationship.getA().equals(getHasRelationships())) {
      return relationship.getRoleB();
    } else if (relationship.getB().equals(getHasRelationships())) {
      return relationship.getRoleA();
    } else {
      return null;
    }
  }

  /** getOther  - return other (i.e. not schedule's) HasRelationships in the
   * specified relationship.
   *
   * @param relationship Relationship
   * @return HasRelationships
   */
  public HasRelationships getOther(Relationship relationship) {
    if (relationship.getA().equals(getHasRelationships())) {
      return relationship.getB();
    } else if (relationship.getB().equals(getHasRelationships())) {
      return relationship.getA();
    } else {
      return null;
    }
  }

  private static class TestAsset implements org.cougaar.core.util.UniqueObject, HasRelationships {
    private RelationshipSchedule mySchedule;
    private org.cougaar.core.util.UID myUID;

    public TestAsset() {
      mySchedule = new RelationshipScheduleImpl(this);
    }
  

    public RelationshipSchedule getRelationshipSchedule() {
      return mySchedule;
    }

    public void setRelationshipSchedule(RelationshipSchedule newSchedule) {
      mySchedule = newSchedule;
    }

    public boolean isLocal() {
      return true;
    }

    public void setLocal(boolean flag) {
    }

    public boolean isSelf() {
      return isLocal();
    }

    public void setUID(org.cougaar.core.util.UID uid) {
      myUID = uid;
    }

    public org.cougaar.core.util.UID getUID() {
      return myUID;
    }
  }
  
  public static void main(String []args) {
    int uidNum = 0;

    TestAsset testAsset0 = new TestAsset();
    testAsset0.setUID(new UID("testAsset",uidNum));
    uidNum++;

    TestAsset testAsset1 = new TestAsset();
    testAsset1.setUID(new UID("testAsset", uidNum));
    uidNum++;

    TestAsset testAsset2 = new TestAsset();
    testAsset2.setUID(new UID("testAsset",uidNum));
    uidNum++;

    Role.create("ParentProvider", "ParentCustomer");
    Role parent = Role.getRole("ParentProvider");
    Role child = Role.getRole("ParentCustomer");
    
    Role.create("GarbageCustomer", "GarbageProvider");

    Relationship rel1 = new RelationshipImpl(0, 10, parent, 
                                             testAsset0, testAsset1);
    Relationship rel2 = new RelationshipImpl(5, 15, parent, 
                                             testAsset1, testAsset0);
    Relationship rel3 = new RelationshipImpl(2, 9, child,
                                             testAsset1, testAsset0);
    Relationship rel4 = new RelationshipImpl(0, 30, 
                                             Role.getRole("GarbageCustomer"),
                                             testAsset0, 
                                             testAsset1);

    Relationship testRel = new RelationshipImpl(0, 30, Role.getRole("GarbageCustomer"), 
                                                testAsset1, testAsset0);

    RelationshipSchedule schedule = new RelationshipScheduleImpl(testAsset0);
    schedule.add(rel1);
    schedule.add(rel2);
    schedule.add(rel3);
    schedule.add(rel4);

    
    System.out.println(schedule);
    System.out.println(schedule.iterator());
    Collection collection = schedule.getMatchingRelationships(parent);
    Iterator iterator = collection.iterator();
    System.out.println("Role -" + parent);
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };

    TimeSpan timeSpan = TimeSpans.getSpan(10, 17);
    collection = schedule.getMatchingRelationships(parent, timeSpan);
    iterator = collection.iterator();
    System.out.println("Role -" + parent + " time span 10 - 17");
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };

    timeSpan = TimeSpans.getSpan(0, 5);
    collection = schedule.getMatchingRelationships(child, timeSpan);
    iterator = collection.iterator();
    System.out.println("Role -" + child + " time span 0 - 5"); 
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };

    timeSpan = TimeSpans.getSpan(10, 17);
    collection = schedule.getMatchingRelationships("Provider", timeSpan);
    iterator = collection.iterator();
    System.out.println("Role Suffix -  'Provider',  time span 10 - 17");
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };
    
    schedule.add(testRel);

  }

}




