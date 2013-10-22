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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import org.cougaar.core.persist.ActivePersistenceObject;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.Enumerator;
import org.cougaar.util.log.Logger;


/** MPTask implementation - MPTasks should only be created or used if they are
 * attached to MPWorkflows.  MPTasks always have multiple ParentTasks.
 **/
        
public final class MPTaskImpl 
  extends TaskImpl 
  implements MPTask, NewMPTask, ActivePersistenceObject
{
        
  //private transient  Vector parenttasks = new Vector();
  private transient List parenttasks = new ArrayList();
  private Composition comp;
        
  /** Constructor that takes no args */
  public MPTaskImpl(UID uid) {
    super(uid);
  }
  
  /** 
   * Returns the base or parent tasks of
   * a given task, where the given task is
   * an aggregation expansion of the base tasks. The
   * These tasks are members of MPWorkflows.
   * @return Enumeration{Task} that are the "parenttasks"
   **/
                
  public Enumeration getParentTasks() {
    synchronized(parenttasks) {
      List copy = new ArrayList(parenttasks);
      return new Enumerator(copy);
    }
    //return parenttasks.elements();
  }
  
  /**
   * Override getParentTask in Task.java and return null.
   * Dump stack as this shouldn't be called for MPTask, because
   * MPTasks have more than one parent.
   **/
  public Task getParentTask() {
    Thread.dumpStack();
    return null;
  }
  
  /**
    * The Composition object that created this task.
    * @return Composition
    * @see org.cougaar.planning.ldm.plan.Composition
    */
  public Composition getComposition() {
    if (comp == null) {
      System.err.println("MPTask is MALFORMED - The composition is null!");
      Thread.dumpStack();
    }
    return comp;
  }
        
  /**
   * Do not allow this to be called on an MPTask, since there are by 
   * definition more than one Parent Task of an MPTask.
   **/
  public void setParentTask(Task parentTask) {
    throw new RuntimeException("This method is NOT to be called for an MPTask. MPTasks have more than one Parent Task, use: setParentTasks(Enum)");
  }

  public void setParentTaskUID(UID parentTaskuid) {
    throw new RuntimeException("This method is NOT to be called for an MPTask. MPTasks have more than one Parent Task, use: setParentTasks(Enum)");
  }

                
  /** 
   * Sets the base or parent tasks of
   * a given task, where the given task is
   * an aggregation expansion of the base tasks. 
   * Note that an MPTask contains multiple ParentTasks.
   * If the Context of the MPTask hasn't been set, the
   * Context will be set the be the union of Contexts of the parenttasks
   * @param pt - Enumeration of Tasks which are the "parenttasks"
   **/
                
  public void setParentTasks(Enumeration pt) {
    synchronized(parenttasks) {
      parenttasks.clear();
      while (pt.hasMoreElements()) {
        Task t = (Task) pt.nextElement();
        if ( t instanceof Task ) {
          parenttasks.add(t);
        } else {
          // buzzz... wrong answer - tryed to pass in a null!
          throw new IllegalArgumentException("MPTask.setParentTask(Enumeration pt): all elements of pt must be Tasks");
        }
      }
    }
    if (getContext() == null) {
      setMPContext();
    }
  }
  
  /**
   * Set the Composition that created this task.
   * @param aComposition
   */
  public void setComposition(Composition aComposition) {
    comp = aComposition;
  }
  
  /* for infrastructure only */
  protected void removeParentTask(Task ptask) {
    synchronized(parenttasks) {
      parenttasks.remove(ptask);
    }
  }
  
  //
  // serialization
  //

  /** serialize tasks making certain that references to other tasks and
   * workflows are appropriately proxied.
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(parenttasks);
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
    parenttasks = (ArrayList) stream.readObject();
  }

  // ActivePersistenceObject
  public void postRehydration(Logger logger) {
    // If the composition points to any Aggregations that were not rehydrated,
    // then collapse the list of aggregations
    NewComposition comp = (NewComposition) getComposition();
    List aggregations = comp.getAggregations();
    List newAggregations = null;
    for (int i = 0, n = aggregations.size(); i < n; i++) {
      Aggregation agg = (Aggregation) aggregations.get(i);
      if (agg == null) {
        logger.warn("Removing null aggregation from composition of " + this);
        if (newAggregations == null) {
          newAggregations = new ArrayList(n - 1);
          newAggregations.addAll(aggregations.subList(0, i));
        }
      } else if (newAggregations != null) {
        newAggregations.add(agg);
      }
    }
    if (newAggregations != null) {
      comp.setAggregations(newAggregations);
    }
  }
  public void checkRehydration(Logger logger) {
    // okay
  }
  public boolean skipUnpublishedPersist(Logger logger) {
    // persist me as usual
    return false;
  }

  // for UI

  public String[] getParentTaskIDs() {
    synchronized(parenttasks) {
      String UUIDs[] = new String[parenttasks.size()];
      for (int i = 0; i < parenttasks.size(); i++)
        UUIDs[i] = ((UniqueObject)(parenttasks.get(i))).getUID().toString();
      return UUIDs;
    }
  }

  public String getParentTaskID(int i) {
    String UUIDs[] = getParentTaskIDs();
    if (i < UUIDs.length)
      return UUIDs[i];
    else
      return null;
  }

  /**
   * Set the Context of the MPTask to be the union of the Contexts in the parenttasks
   * This works only if the Contexts are instances of ContextOfUIDs
   * Any Context that is not a ContextOfUIDs will not be included 
   */
  private void setMPContext() {
    synchronized(parenttasks) {
      HashSet union = new HashSet(5);
      for (int i = 0; i < parenttasks.size(); i++) {
	Context c = ((Task)parenttasks.get(i)).getContext();
	if (c instanceof ContextOfUIDs) {
	  UID[] uids = (UID[])((ContextOfUIDs)c).toArray();
	  for (int j=0; j<uids.length; j++) {
            union.add(uids[j]);
	  }
	}
	ContextOfUIDs newContext = new ContextOfUIDs(union);
	if (newContext.size() > 0 )
	  setContext(newContext);
      }
    }
  }
}
