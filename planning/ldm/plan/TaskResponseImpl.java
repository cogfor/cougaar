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

import org.cougaar.core.util.UID;


/** An implementation of org.cougaar.TaskResponse
 */
public class TaskResponseImpl extends PlanningDirectiveImpl
  implements TaskResponse, NewTaskResponse
{
                
  private UID taskUID; 
  private UID childUID;
                
  //no-arg constructor
  public TaskResponseImpl () {
    super();
  }

  //constructor that takes the Task and Plan
  public TaskResponseImpl(Task t, Plan plan) {
    taskUID = t.getUID();
    setPlan(plan);
  }
                
  public TaskResponseImpl(UID tuid, Plan plan) {
    taskUID = tuid;
    setPlan(plan);
  }

  /** implementation of the TaskResponse interface */
                
  /** 
   * Returns the task UID the notification is in reference to.
   * @return Task 
   **/
                
  public UID getTaskUID() {
    return taskUID;
  }
  
  /** Get the child task's UID that was disposed.  It's parent task is getTask();
    * Useful for keeping track of which subtask of an Expansion caused
    * the re-aggregation of the Expansion's reported allocationresult.
    * @return UID
    */
  public UID getChildTaskUID() {
    return childUID;
  }
  
  // implementation methods for the NewTaskResponse interface

  /** 
   * Sets the task the notification is in reference to.
   * @param t 
   **/
                
  public void setTask(Task t) {
    taskUID = t.getUID();
  }

  public void setTaskUID(UID tuid) {
    taskUID = tuid;
  }
  
  /** Sets the child task's UID that was disposed.  It's parent task is getTask();
    * Useful for keeping track of which subtask of an Expansion caused
    * the re-aggregation of the Expansion's reported allocationresult.
    * @param thechildUID
    */
  public void setChildTaskUID(UID thechildUID) {
    childUID = thechildUID;
  }
                    
  /** Always serialize Notifications with TaskProxy
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }
}
