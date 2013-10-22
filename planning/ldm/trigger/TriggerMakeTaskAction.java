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


package org.cougaar.planning.ldm.trigger;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.legacy.PluginDelegate;

/**
 * Trigger action to generate a new task when fired - abstract method
 * for task generation
 */

public abstract class TriggerMakeTaskAction implements TriggerAction {

  // Provide TriggerAction method : publish generated task
  public void Perform(Object[] objects, PluginDelegate pid) {
    Task task = GenerateTask(objects, pid);
    if (task != null) {
      pid.publishAdd(task);
    }
  }

  /** Abstract method to generate a new task from the set of objects provided
    * @param objects  The objects to work from
    * @param pid  The PluginDelegate to use for things like getClusterObjectFactory.
    * @return Task  The new task.
    */
  public abstract Task GenerateTask(Object[] objects, PluginDelegate pid);
    
 

}

