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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

/** TaskRescind implementation
 * TaskRescind allows a task to be rescinded from the Plan. 
 *
 **/

public class TaskRescindImpl 
  extends PlanningDirectiveImpl
  implements TaskRescind, NewTaskRescind
{

  private UID taskUID = null;
  private boolean deleted;      // Signifies that the task being rescinded is deleted.
        
  /** @param src
   * @param dest
   * @param t
   **/
  public TaskRescindImpl(MessageAddress src, MessageAddress dest, Plan plan, Task t) {
    super.setSource(src);
    super.setDestination(dest);
    super.setPlan(plan);
    taskUID = t.getUID();
    deleted = t.isDeleted();
  }

  public TaskRescindImpl(MessageAddress src, MessageAddress dest, Plan plan,
                         UID tuid, boolean deleted)
  {
    super.setSource(src);
    super.setDestination(dest);
    super.setPlan(plan);
    this.taskUID = tuid;
    this.deleted = deleted;
  }

  /**
   * Returns the task to be rescinded
   */

  public UID getTaskUID() {
    return taskUID;
  }
     
  /**
   * Sets the task to be rescinded
   * @deprecated
   */

  public void setTask(Task atask) {
    taskUID = atask.getUID();
  }

  /**
   * Sets the task UID to be rescinded
   * @deprecated
   */
  public void setTaskUID(UID tuid) {
    taskUID = tuid;
  }

  public void setDeleted(boolean newDeleted) {
    deleted = newDeleted;
  }

  public boolean isDeleted() {
    return deleted;
  }
   
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }

  public String toString() {
    return "<TaskRescind for " + taskUID + ">";
  }

}
