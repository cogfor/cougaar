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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

/** Composition Interface
 * An Composition represents the aggregation of multiple tasks
 * into a single task. Compositions are referenced by Aggregation PlanElements.
 *
 *
 **/

public class CompositionImpl 
  implements Composition, NewComposition, Cloneable, Serializable
{
  
  private List aggregations = new ArrayList();
  private MPTask compTask;
  private transient AllocationResultDistributor ard;
  private transient AllocationResultDistributor DEFAULT_DA = AllocationResultDistributor.DEFAULT;
  
  
  /*Constructor for factory for now */
  public CompositionImpl() {
    super();
    this.setDistributor(DEFAULT_DA);
  }
    
  /* Simple Constructor uses a specified Distributor.
   * @param aggregationpes  The Aggregations of the tasks that are being combined.
   * @param newtask  The resulting task.
   * @param distributor The Distributor to use in propogating notifications/results
   * @return Composition.
   */
  public CompositionImpl(AllocationResultDistributor distributor, Collection aggregationpes, MPTask newtask) {
    super();
    this.setAggregations(aggregationpes);
    this.setCombinedTask(newtask);
    this.setDistributor(distributor);
  }
  
  // Composition interface implementations
  
  /** Convenienve method that calculates the Tasks that are 
   * being aggregated by looking at all of the Aggregations.
   * (Aggregation.getTask())
   * @return List
   * @see org.cougaar.planning.ldm.plan.Task
   **/
  public synchronized List getParentTasks() {
    ListIterator lit = aggregations.listIterator();
    List parents = new ArrayList();
    while (lit.hasNext()) {
      Aggregation anagg = (Aggregation) lit.next();
      parents.add(anagg.getTask());
    }
    return parents;
  }
  
  /** Returns the Aggregation PlanElements of the Tasks that
    * are being combined
    * @return List
    */
  public synchronized List getAggregations() {
    return new ArrayList(aggregations);
  }
  
  /** Returns the newly created task that represents all 'parent' tasks.
   * @return Task
   * @see org.cougaar.planning.ldm.plan.Task
   */
  public MPTask getCombinedTask() {
    return compTask;
  }
  
  /** Allows the AllocationResult to be properly dispersed among the 
   * original (or parent) tasks.
   * @return AllocationResultDistributor
   * @see org.cougaar.planning.ldm.plan.AllocationResultDistributor
   */
  public AllocationResultDistributor getDistributor() {
    return ard;
  }
  
  /**Calculate seperate AllocationResults for each parent task of the Composition.
   * @return distributedresults
   * @see org.cougaar.planning.ldm.plan.Composition
   * @see org.cougaar.planning.ldm.plan.TaskScoreTable
   * @see org.cougaar.planning.ldm.plan.AllocationResult
   */
  public TaskScoreTable calculateDistribution() {
    Task task = getCombinedTask();
    PlanElement pe = task.getPlanElement();
    AllocationResult ar = null;
    if (pe != null) {
      ar = pe.getEstimatedResult();
    }
    // this should be cleaned up later - but for now copy the
    // list into a vector for the distributor that expects a vector
    Vector parentsv = new Vector(getParentTasks());
    return getDistributor().calculate(parentsv, ar);
  }
  
  
  // NewComposition interface implementations
  
  /** Set the Aggregation PlanElements of the tasks being combined
    * @param aggs  The Aggregations
    * @see org.cougaar.planning.ldm.plan.Aggregation
    */
  public synchronized void setAggregations(Collection aggs) {
    aggregations.clear();
    Iterator aggit = aggs.iterator();
    while (aggit.hasNext()) {
      Aggregation anagg = (Aggregation) aggit.next();
      if (anagg instanceof Aggregation) {
        aggregations.add(anagg);
      } else {
        throw new IllegalArgumentException("Composition.setAggregations(Collection aggs) expects that all " 
                                           + "members of the Collection are of type Aggregation");
      }
    }
  }
  
  public synchronized List clearAggregations() {
    List l = aggregations;
    aggregations = new ArrayList(5);
    return l;
  }

  /** Add a single Aggregation to the existing collection
   */
  public synchronized void addAggregation(Aggregation singleagg) {
    aggregations.add(singleagg);
  }
  
  /** for infrastructure */
  protected synchronized void removeAggregation( Aggregation removeagg) {
    aggregations.remove(removeagg);
  }
      
  
  /** Set the newly created task that represents all 'parent' tasks.
   * @param newTask
   * @see org.cougaar.planning.ldm.plan.Task
   */
  public void setCombinedTask(MPTask newTask) {
    compTask = newTask;
  }
  
  /** Allows the AllocationResult to be properly dispersed among the 
   * original (or parent) tasks.
   * @param distributor
   * @see org.cougaar.planning.ldm.plan.AllocationResultDistributor
   */
  public void setDistributor(AllocationResultDistributor distributor) {
    ard = distributor;
  }
  
  private boolean _propagateP = true;
  public boolean isPropagating() { return _propagateP; }
  public void setIsPropagating(boolean isProp) { _propagateP = isProp; }
  /** @deprecated  Use setIsPropagating(boolean isProp) -defaults to true*/
  public void setIsPropagating() { _propagateP = true; }
  
  private boolean shouldCleanUp = true;
  public synchronized boolean shouldDoMassCleanUp() { return shouldCleanUp; }
  public synchronized void cleanedUp() { shouldCleanUp = false; }
  public synchronized boolean doingCleanUp() {
    boolean old = shouldCleanUp;
    shouldCleanUp = false;
    return old;
  }
    

  /* for user interface */
  public Task[] getParentTasksAsArray() {
    Collection ptasks = getParentTasks();
    Task[] t = (Task[])ptasks.toArray(new Task[ptasks.size()]);
    return t;
  }

  public Task getParentTaskFromArray(int i) {
    Task t[] = getParentTasksAsArray();
    if (i < t.length)
      return t[i];
    else
      return null;
  }
  
  public Aggregation[] getAggregationsAsArray() {
    synchronized(aggregations) {
      Aggregation[] a = (Aggregation[])aggregations.toArray(new Aggregation[aggregations.size()]);
      return a;
    }
  }
  
  public Aggregation getAggregationFromArray(int i) {
    Aggregation a[] = getAggregationsAsArray();
      if (i < a.length)
        return a[i];
      else
        return null;
  }

  private void writeObject(ObjectOutputStream os) throws IOException {
    os.defaultWriteObject();
    if (ard == DEFAULT_DA) {
      os.writeObject(null);
    } else {
      os.writeObject(ard);
    }
  }
  
  private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
    is.defaultReadObject();
    DEFAULT_DA = AllocationResultDistributor.DEFAULT;
    ard = (AllocationResultDistributor) is.readObject();
    if (ard == null) {
      ard = DEFAULT_DA;
    }
  }
}
