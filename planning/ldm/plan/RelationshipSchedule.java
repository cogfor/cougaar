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

import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/** 
 * A RelationshipSchedule is a representation of an object (must implement
 * HasRelationships) relationships 
 **/

public interface RelationshipSchedule extends Schedule {

  /**
   * @return HasRelationships The object whose relationships are contained in
   * the schedule
   */
  HasRelationships getHasRelationships();

  /** getMatchingRelationships - return all Relationships which pass the
   * specified UnaryPredicate.
   * 
   * @param predicate UnaryPredicate to use in screening Relationships
   * @return a sorted Collection containing all Relationships which
   * which pass the specified UnaryPredicate
   **/
  Collection getMatchingRelationships(UnaryPredicate predicate);

  /** getMatchingRelationships - return all Relationships where the other 
   * has the specified role. getMatchingRelationships(SUBORDINATE) returns 
   * relationships with my subordinates
   * 
   * @param role Role to look for
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role
   **/
  Collection getMatchingRelationships(Role role);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and intersect the time.
   * 
   * @param role Role to look for
   * @param time long specifying the time
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and intersect the specified time 
   **/
  Collection getMatchingRelationships(Role role, long time);

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
  Collection getMatchingRelationships(Role role, long startTime, long endTime);


  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  Collection getMatchingRelationships(Role role, TimeSpan timeSpan);


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
  Collection getMatchingRelationships(Role role, HasRelationships otherObject, long time);

  /** getMatchingRelationships - return all Relationships which contain the 
   * specified other object, match the specified role, and overlap the 
   * specified time span.
   * 
   * @param role Role to look for
   * @param otherObject HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  Collection getMatchingRelationships(Role role,
                                      HasRelationships otherObject,
                                      TimeSpan timeSpan);

  /** getMatchingRelationships - return all Relationships which contain the 
   * specified other object, match the specified role, and overlap the 
   * the time span specified by the start and end time arguments
   *
   * @param role Role to look for
   * @param otherObject HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which contain 
   * the specified other object, match the specified role and direct object 
   * flag, and overlap the specified time span.
   * @deprecated Use getMatchingRelationships(Role role, HasRelationships otherObject, TimeSpan timeSpan) or
   * getMatchingRelationships(Role role, HasRelationships otherObject, long time)
   **/
  Collection getMatchingRelationships(Role role, 
                                      HasRelationships otherObject,
                                      long startTime, long endTime);


  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and intersect the specified time.
   * 
   * @param otherObject HasRelationships 
   * @param time long 
   * @return a sorted Collection containing all Relationships which
   * which contain the specified other HasRelationships and intersect the 
   * specified time span
   **/
  Collection getMatchingRelationships(HasRelationships otherObject,
                                      long time);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the specified time span.
   * 
   * @param otherObject HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain the specified other HasRelationships and overlap the 
   * specified time span
   **/
  Collection getMatchingRelationships(HasRelationships otherObject,
                                      TimeSpan timeSpan);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the time span specified by the start and end
   * time arguments.
   * 
   * @param otherObject HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   * @deprecated Use getMatchingRelationships(HasRelationships otherObject, TimeSpan timeSpan) or
   * getMatchingRelationships(HasRelationships otherObject, long time)
   **/
  Collection getMatchingRelationships(HasRelationships otherObject,
                                      long startTime,
                                      long endTime);


  /** getMatchingRelationships - return all Relationships which contain the
   * specified role suffix and overlap the specified time span.
   * getMatchingRelationships("Provider", timeSpan) will return
   * relationships with providers.
   * 
   * @param roleSuffix String specifying the role suffix to match
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain the specified role suffix and overlap the 
   * specified time span
   **/
  Collection getMatchingRelationships(String roleSuffix,
                                      TimeSpan timeSpan);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role suffix and intersect the specified time.
   * getMatchingRelationships("Provider", time) will return
   * relationships with providers.
   * 
   * @param roleSuffix String specifying the role suffix to match
   * @param time long
   * @return a sorted Collection containing all Relationships which
   * which contain the specified role suffix and intersect the 
   * specified time
   **/
  Collection getMatchingRelationships(String roleSuffix,
                                      long time);


  /** getMatchingRelationships - return all Relationships which contain the
   * specified role suffix and overlap the time span specified by the start and end
   * time arguments.
   * getMatchingRelationships("Provider", startTime, endTime) will return
   * relationships with providers.
   * 
   * @param roleSuffix String specifying the role suffix to match
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role suffix and overlap the specified time span
   * @deprecated Use getMatchingRelationships(String roleSuffix, TimeSpan timeSpan) or
   * getMatchingRelationships(String roleSuffix, long time)
   **/
  Collection getMatchingRelationships(String roleSuffix,
                                      long startTime,
                                      long endTime);

  /** getMatchingRelationships - return all Relationships which overlap the 
   * specified time span.
   * 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain overlap the specified time span
   **/
  Collection getMatchingRelationships(TimeSpan timeSpan);

  /** getMatchingRelationships - return all Relationships which intersect the 
   * specified time.
   * 
   * @param time long
   * @return a sorted Collection containing all Relationships which
   * which intersect the specified time
   **/
  Collection getMatchingRelationships(long time);

  /** getMatchingRelationships - return all Relationships which overlap the 
   * time span specified by the start and end time arguments. 
   * 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match overlap the specified time span
   * @deprecated Use getMatchingRelationships(TimeSpan timeSpan) or
   * getMatchingRelationships(long time)
   **/
  Collection getMatchingRelationships(long startTime,
                                      long endTime);

  /** getMyRole - return role for schedule's HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  Role getMyRole(Relationship relationship);

  /** getOtherRole - return role for other HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  Role getOtherRole(Relationship relationship);

  /** getOther  - return other (i.e. not schedule's) HasRelationships in the
   * specified relationship.
   *
   * @param relationship Relationship
   * @return HasRelationships
   */
  HasRelationships getOther(Relationship relationship);

  class RelationshipScheduleChangeReport 
    implements ChangeReport 
  {
    public RelationshipScheduleChangeReport() {
    }

    public String toString() {
      return "RelationshipScheduleChangeReport";
    }
  }

}














