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

import java.io.Serializable;

import org.cougaar.planning.plugin.legacy.PluginDelegate;

/**
 * A Predictor is an object intended to be available on an 
 * Entity Asset which provides a prediction of how the 
 * associated remote agent WOULD respond if it were allocated a given
 * task. The predictor should be self-contained, meaning that it should
 * not require any resources other than those of the provided task and
 * its own internal resources to provide the allocation response.
 *
 * It should be noted that a Predictor is not required for every 
 * Entity Asset : some agents will not provide Predictors.
 *
 * It is anticipated that a predictor class will be optionally specified in 
 * a agent's initialization file (<agentname>.ini) which will allow
 * the agent to pass an instance of the predictor embedded in the
 * Entity Asset copy of itself when it hooks up with other agents.
 */  
public interface Predictor extends Serializable {
    
  /** 
   * @return AllocationResult A predictive result for the given task.
   * @see org.cougaar.planning.ldm.plan.AllocationResult
   **/
  AllocationResult Predict(Task for_task, PluginDelegate plugin);
    
}
