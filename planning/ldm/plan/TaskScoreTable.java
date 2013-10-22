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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import org.cougaar.core.util.UID;


/** Provides a table containing tasks (keys) and their AllocationResults
 **/

public final class TaskScoreTable {
  private Task tasks[];
  private AllocationResult results[];
  
  public TaskScoreTable(Hashtable table) {
    // initialize our arrays
    int l = table.size();
    tasks = new Task[l];
    results = new AllocationResult[l];

    int i = 0;
    for (Enumeration e = table.keys(); e.hasMoreElements(); i++) {
      Task t = (Task) e.nextElement();
      tasks[i] = t;
      results[i] = (AllocationResult) table.get(t);
    }
  }

  /**
   * Use this constructor when no allocation results have been computed
   **/
  public TaskScoreTable(Task tasks[]) {
    this(tasks, new AllocationResult[tasks.length]);
  }


  /**
   * Use this constructor when allocation results have already been computed
   **/
  public TaskScoreTable(Task tasks[], AllocationResult results[]) {
    this.tasks = tasks;
    this.results = results;
  }

  /** @return AllocationResult The current allocation result associated 
   * with the task
   * @param task The task to use for the key 
   **/

  public AllocationResult getAllocationResult(Task task) {
    int l = tasks.length;

    for (int i = 0; i<l; i++) {
      if (tasks[i] == task) return results[i];
    }
    return null;
  }

  public int size() {
    return tasks.length;
  }

  public Task getTask(int i) {
    return tasks[i];
  }
  public AllocationResult getAllocationResult(int i) {
    return results[i];
  }
  public void setAllocationResult(int i, AllocationResult newAR) {
    results[i] = newAR;
  }

  public AllocationResult getAllocationResult(UID uid) {
    int l = tasks.length;
    for (int i = 0; i<l; i++) {
      if (tasks[i].getUID().equals(uid)) return results[i];
    }
    return null;
  }
  public Task getTask(UID uid) {
    int l = tasks.length;
    for (int i = 0; i<l; i++) {
      Task t = tasks[i];
      if (t.getUID().equals(uid)) return t;
    }
    return null;
  }
  public int getTaskIndex(UID uid) {
    int l = tasks.length;

    for (int i = 0; i<l; i++) {
      Task t = tasks[i];
      if (t.getUID().equals(uid)) return i;
    }
    return -1;
  }

  // support for ExpansionImpl.
  public void fillSubTaskResults(HashMap staskinfo, UID changedUID) {
    // if the set is empty, just copy in the info in
    if (staskinfo.isEmpty()) {
      int l = size();
      for (int i = 0; i<l; i++) {
        Task t = getTask(i);
        AllocationResult ar = getAllocationResult(i);
        UID uid = t.getUID();
        boolean changedp = uid.equals(changedUID);
        // should the "t" below be searched for in the workflow??
        SubTaskResult sr = new SubTaskResult(t, changedp, ar);
        staskinfo.put(uid.toString(), sr);
      }
    } else {
      // we can cheat and just update the right one.
      int ti = getTaskIndex(changedUID);
      if (ti >= 0) {
        Task t = getTask(ti);
        AllocationResult ar = getAllocationResult(ti);
        SubTaskResult sr = new SubTaskResult(t, true, ar);
        staskinfo.put(changedUID.toString(), sr);
      } else {
        // Task not found (no longer in the expansion): drop it on the floor.
      }
    }
  }
}
