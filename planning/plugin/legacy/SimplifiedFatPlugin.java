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

import org.cougaar.util.StateModelException;

/**
 * Just like SimplifiedPlugin except that it extends 
 * SingleThreadedPlugin instead of ThinPlugin.  
 * @deprecated Use SimplePlugin and call chooseThreadingModel(SINGLE_THREAD) 
 * from your constructor.
 **/

// Perfect use for multiple inheritence, sigh.
public abstract class SimplifiedFatPlugin extends SingleThreadedPlugin 
{
  /** */
  public SimplifiedFatPlugin() {}

  //
  // final all the important state model functions.
  //

  public final void initialize() throws StateModelException {
    super.initialize();
  }
  public void load(Object object) throws StateModelException {
    super.load(object);
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

  /** call initialize within an open transaction. **/
  protected final void prerun() {
    try {
      openTransaction();
      setupSubscriptions();
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println("Caught "+e);
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
    try {
      openTransaction();
      if (wasAwakened() || (getBlackboardService().haveCollectionsChanged())) {
        execute();
      }
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println("Caught "+e);
        e.printStackTrace();
      }
    } finally {
      closeTransaction();
    }
  }

  
  /**
   * Called inside of an open transaction whenever the plugin was
   * explicitly told to run or when there are changes to any of
   * our subscriptions.
   **/
  protected abstract void execute();

}

