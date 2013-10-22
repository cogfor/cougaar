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

package org.cougaar.planning.plugin.legacy;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.StateModelException;

/** SimplePlugin is a replacement for SimplifiedPlugin and
 * SimplifiedFatPlugin.
 * Call setThreadingChoice(SINGLE_THREAD) in constructor or before this
 * load is invoked to get the equivalent of SimplifiedFatPlugin.
 **/

public abstract class SimplePlugin extends PluginAdapter {

  private long minDelay = 0L;
  private long maxDelay = 0L;
  private Alarm minTimer = null;
  private Alarm maxTimer = null;

  public void load(Object object) throws StateModelException {
    if (getThreadingChoice() == UNSPECIFIED_THREAD)
      setThreadingChoice(SHARED_THREAD);

    super.load(object);
  }

  public final void initialize() throws StateModelException {
    super.initialize();
  }
  public final void start() throws StateModelException {
    super.start();
  }
  public final void suspend() throws StateModelException {
    super.suspend();
  }
  public final void resume() throws StateModelException {
    super.resume();
  }
  public final void stop() throws StateModelException {
    super.stop();
  }

  protected void setExecutionDelay(long minDelay, long maxDelay) {
    this.minDelay = minDelay;
    this.maxDelay = Math.max(minDelay, maxDelay);
  }

  /** call initialize within an open transaction. **/
  protected final void prerun() {
    try {
      openTransaction();
      setupSubscriptions();
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println(getMessageAddress().toString()+"/"+this+" caught "+e);
        e.printStackTrace();
      }
    } finally {
      closeTransactionDontReset();
    }
  }

  /** Called during initialization to set up subscriptions.
   * More precisely, called in the plugin's Thread of execution
   * inside of a transaction before execute will ever be called.
   **/
  protected abstract void setupSubscriptions();

  /** Call execute in the right context.
   * Note that this transaction boundary does NOT reset
   * any subscription changes.
   * @see #execute() documentation for details
   **/
  protected final void cycle() {
    boolean doExecute = false; // Synonymous with resetTransaction
    try {
      openTransaction();
      if (wasAwakened() || (getBlackboardService().haveCollectionsChanged())) {
        if (minTimer != null) {
          if (minTimer.hasExpired() || maxTimer.hasExpired()) {
            minTimer.cancel();
            maxTimer.cancel();
            minTimer = null;
            maxTimer = null;
            doExecute = true;
          } else {
            minTimer.cancel();
            minTimer = wakeAfterRealTime(minDelay);
          }
        } else if (minDelay > 0) {
          minTimer = wakeAfterRealTime(minDelay);
          maxTimer = wakeAfterRealTime(maxDelay);
        } else {
          doExecute = true;
        }
      }
      if (doExecute) {
        execute();
      }
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println(getMessageAddress().toString()+"/"+this+" caught "+e);
        e.printStackTrace();
      }
      doExecute = true;
    } finally {
      closeTransaction(doExecute);
    }
  }


  /**
   * Called inside of an open transaction whenever the plugin was
   * explicitly told to run or when there are changes to any of
   * our subscriptions.
   **/
  protected abstract void execute();

  //
  // Utility methods go here
  //

  /**
   * Returns an AllocationResult with specified <confidenceRating> and <success>
   * based on the preferences of Task <t>. If <t> has no preferences, returns
   * a null AllocationResult.
   */
  public AllocationResult createEstimatedAllocationResult(
      Task t, double confidenceRating, boolean success) {
    return PluginHelper.createEstimatedAllocationResult(t, getFactory(), confidenceRating, success);
  }

  /**
   * Creates a subtask from <parent> with same Verb, PrepositionalPhrases,
   * DirectObject, Plan, Preferences, and Context as <parent>. Creates
   * an expansion containing the subtask with null estimated allocation
   * result. Publishes both subtask and expansion.
   */
  public void createPublishExpansion(Task parent) {
    NewTask subtask = PluginHelper.makeSubtask(parent, getFactory());
    //at this point, could change some of the prepositional phrases or other
    //properties of the subtask
    //could also make a vector of subtasks instead of just one
    Expansion expansion = PluginHelper.wireExpansion(parent, subtask, getFactory());
    //alternatively, use a different wireExpansion method to pass in an estimated
    //allocation result or to use vectors of subtasks
    // publish the Expansion and the workflow's subtasks.
    PluginHelper.publishAddExpansion(getBlackboardService(), expansion);
  }

}
