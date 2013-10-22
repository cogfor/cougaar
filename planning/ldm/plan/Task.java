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

import java.util.Date;
import java.util.Enumeration;

import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.asset.Asset;

/** Task Interface
  * Task is the essential "execute" directive,
  * instructing a subordinate or service provider
  * to plan and eventually accomplish a task.
  * A the general form of a task is:
  * Verb <DirectObject> {PrepositionalPhrase} per <Schedule> per <Constraints>
  **/	
public interface Task
  extends PlanningDirective, UniqueObject, Priority, Annotatable, Publishable
{
		
  /** 
   * Returns the UID of the base or parent task of
   * a given task, where the given task is
   * an expansion of the base task. The
   * parent task could be "move vehicles
   * from point a to point b ...". An
   * expanded task could be "fuel vehicles ...".
   * </PRE> UID basetask = fueltask.getParentTaskUID(); </PRE>
   * @return UID of the Task that is the "parenttask"
   **/
  UID getParentTaskUID();
		
  /** 
   * All Tasks are members of
   * a Workflow. The tasks that are expansions
   * of a basetask are placed in one workflow. 
   * For example, the fueltask will be a member
   * of a workflow that contains all of the tasks
   * and constraints needed to complete the basetask.
   * <PRE> Workflow myworkflow = fueltask.getWorkflow(); </PRE>
   * @return Workflow  Returns the Workflow that the task is a member of. 
   **/
  Workflow getWorkflow();
		
  /** 
   * Returns the prepositional phrase(s) of the Task.  
   * A PrepositionalPhrase object contains a String
   * representation of the preposition (from, to, with, etc.) 
   * and an object representing the indirect object. The indirect
   * object will be an Asset which can represent an Asset, AssetGroup or Location.
   * For example, in the task
   * "UnitA requisitions commodityB from UnitC"...
   * the PrepositionalPhrase is "from UnitC".
   * @return An enumeration of PrepositionalPhrases
   * @see Preposition
   **/
  Enumeration getPrepositionalPhrases();

		
  /**
   * Return the first PrepositionalPhrase found with the
   * specified Preposition.  Returns null if not found.
   * @param preposition One of the strings named in 
   * org.cougaar.planning.ldm.plan.Preposition.
   **/
  PrepositionalPhrase getPrepositionalPhrase(String preposition);

  /**
   * The getVerb method returns the verb of the Task.
   * For example, in the Task "fuel vehicles...", the
   * Verb is the object represented by "fuel".
   * <PRE> Verb mytaskverb = fueltask.getVerb(); </PRE>
   * @return the Verb of the Task.
   **/
  Verb getVerb();
  
  /**
   * Returns the Asset (or AssetGroup) that is being acted upon
   * by the Task.  For example, in the task "fuel
   * vehicle 14 ..." the direct object is "vehicle 14".
   * @return the Direct Object of the task.
   **/
  Asset getDirectObject();
		
  /** 
   * @return Plan.RealityPlan -- this slot is unused / deprecated
   **/
  Plan getPlan();
		
  /**
   * Returns PlanElement that this Task is associated with.  
   * Can be used to discern between expandable and non-expandable
   * Tasks.  If Task has no PlanElement associated with it, will 
   * return null.
   */
  PlanElement getPlanElement();
  
  /** get the preferences on this task.
   * @return Enumeration{Preference}
   */
  Enumeration getPreferences();
  
  /** return the preference for the given aspect type
   * will return null if there is not a preference defined for this aspect type
   * @param aspect_type The Aspect referenced by the preference
   */
  Preference getPreference(int aspect_type);
  
  /** return the preferred value for a given aspect type
   * from the defined preference (and scoring function)
   * will return Double.NaN if there is not a preference defined for this aspect type
   * @param aspect_type The Aspect referenced by the preference
   * @note Reminder that you must use Double.isNaN to test for NaN, since NaN == NaN is always false.
   */
  double getPreferredValue(int aspect_type);
  
  /** Get the priority of this task.
   * Note that this should only be used when there are competing tasks
   * from the SAME customer.
   * @return  The priority of this task
   * @see org.cougaar.planning.ldm.plan.Priority
   */
  byte getPriority();
  
  /** WARNING: This method may return null if the commitment date is undefined.
    * The task commitment date of a task represents the date past which the planning
    * module will warn if the task is changed or removed.  Commitment dates in the planning
    * domain are used to note the last possible date that a task could be changed before
    * the supplier has committed resources to fill or commit the task. E.g. If a supplier
    * has an order and ship lead time of 3 days, then the task's commitment date should be
    * atleast 3 days before the desired delivery date (usually represented with an end
    * date preference).
    * @return Date The Commitment date of this task.
    */
  Date getCommitmentDate();

  /**
   * Get the deleted status of this task.
   **/
  boolean isDeleted();

  Enumeration getObservableAspects();

  /** 
   * Check to see if the current time is before the Commitment date.
   * Will return true if we have not reached the commitment date.
   * Will return true if the commitment date is undefined.
   * Will return false if we have passed the commitment date.
   * @param currentdate  The current date.
   */
  boolean beforeCommitment(Date currentdate);
  
  /** Get a collection of the requested AuxiliaryQueryTypes (int).
   * Note:  if there are no types set, this will return an
   * array with one element = -1
   * @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
   */
  int[] getAuxiliaryQueryTypes();
    
  /**
   * Get the problem Context (if any) for this task.
   * @see Context
   **/
  Context getContext();

  interface TaskChangeReport extends ChangeReport {}

  class PreferenceChangeReport implements TaskChangeReport {
    private int type;
    public final static int UNDEFINED_TYPE = AspectType.UNDEFINED;
    private Preference old = null;

    public PreferenceChangeReport() {
      type = UNDEFINED_TYPE;
    }
    public PreferenceChangeReport(int t) {
      type=t;
    }
    public PreferenceChangeReport(int t, Preference o) {
      type=t;
      old = o;
    }
    public PreferenceChangeReport(Preference o) {
      type = o.getAspectType();
      old = o;
    }
    /** May return AspectType.UNDEFINED if the aspect type id is unknown **/
    public int getAspectType() { return type; }
    public int hashCode() { return getClass().hashCode()+type; }
    public boolean equals(Object o) {
      if (o == null) return false;

      return (this == o) ||
        (o.getClass() == getClass() &&
         ((PreferenceChangeReport)o).type == type);
    }
    public String toString() {
      if (type == UNDEFINED_TYPE) {
        return "PreferenceChangeReport (?)";
      } else {
        return "PreferenceChangeReport ("+type+")";
      }
    }
  }

  class PrepositionChangeReport implements TaskChangeReport {
    private String prep;

    public PrepositionChangeReport() {
      prep = null;
    }
    public PrepositionChangeReport(String p) {
      prep = p;
    }

    /** May return null if unknown **/
    public String getPreposition() { return prep; }

    public int hashCode() { 
      int hc = getClass().hashCode();
      if (prep != null) hc +=prep.hashCode();
      return hc;
    }

    public boolean equals(Object o) {
      if (o == null) return false;

      return (this == o) ||
        (o.getClass() == getClass() &&
         prep != null &&
         prep.equals(((PrepositionChangeReport)o).prep));
    }
    public String toString() {
      if (prep == null) {
        return "PrepositionChangeReport (?)";
      } else {
        return "PrepositionChangeReport ("+prep+")";
      }
    }
  }


}
