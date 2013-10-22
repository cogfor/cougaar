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

import org.cougaar.core.component.Component;
import org.cougaar.util.StateModelException;

/**
 * implement the standard state model
 *
 **/
public abstract class StateModelAdapter
implements PluginStateModel,  Component {
  
  /** current reflection of Plugin run state **/
  private int runState = UNINITIALIZED;

  /** Plugin State model accessor.
   **/
  public final int getModelState() {
    return runState; 
  }

  /** simple initialize method. 
   * Transits the state to UNLOADED.
   *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
   **/
  public synchronized void initialize() throws StateModelException {
    transitState("initialize()", UNINITIALIZED, UNLOADED);
  }


  /** Notice which Agent we are.
   * also transit to LOADED.
   *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
   **/
  public synchronized void load(Object obj) throws StateModelException {
    transitState("load()", UNLOADED, LOADED);
  }

  /** This version of start just transits to ACTIVE.
   * Daemon subclasses may want to start threads here.
   *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
   **/
  public synchronized void start() throws StateModelException {
    transitState("start()", LOADED, ACTIVE);
  }

  /** 
  *Just change the state to IDLE.
  **  @exception org.cougaar.util.StateModelException Cannot transition to new state.  
  **/
  public synchronized void suspend() throws StateModelException {
    transitState("suspend()", ACTIVE, IDLE);
  }

  /**
  *		Transit from IDLE to ACTIVE .
  *  @exception org.cougaar.util.StateModelException If Cannot transition to new state.   
  **/
  public synchronized void resume() throws StateModelException {
    transitState("resume()", IDLE, ACTIVE);
  }

  /** 
  *	  Transit from IDLE to LOADED. 
  *	  @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
  **/
  public synchronized void stop() throws StateModelException {
    transitState("stop()", IDLE, LOADED);
  }

  /** Transit from ACTIVE to LOADED. 
  *   @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
  **/
  public synchronized void halt() throws StateModelException {
    transitState("halt()", ACTIVE, LOADED);
  }

  /** Transit from LOADED to UNLOADED.
  *   @exception org.cougaar.util.StateModelException If Cannot transition to new state.  
  **/
  public synchronized void unload() throws StateModelException {
    transitState("unload()", LOADED, UNLOADED);
  }

  /** Accomplish the state transition.
  *   Be careful and complain if we are in an inappropriate starting state.
  *   @exception org.cougaar.util.StateModelException If Cannot transition to new state.   
  **/
  private synchronized void transitState(String op, int expectedState, int endState) throws StateModelException {
    if (runState != expectedState) {
      throw new StateModelException(""+this+"."+op+" called in inappropriate state ("+runState+")");
    } else {
      runState = endState;
    }
  }


}  

  
  
