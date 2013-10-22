/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.mobility.arch;

import org.cougaar.util.GenericStateModel;

/**
 * Handle a failed move response from the target node.
 */
public class NackHandler extends AbstractHandler {

  private GenericStateModel model;
  private Throwable throwable;

  public NackHandler(
      MobilitySupport support,
      GenericStateModel model,
      Throwable throwable) {
    super(support);
    this.model = model;
    this.throwable = throwable;
  }

  @Override
public void run() {
    nack();
  }

  private void nack() {

    // FIXME race condition between move & agent-add!

    if (log.isInfoEnabled()) {
      log.info(
          "Handling failed move of agent "+id+
          " from "+nodeId+
          " to node "+moveTicket.getDestinationNode());
    }

    // agent is suspended -- let's resume it.

    try {
      resumeAgent();
    } catch (Exception e) {
      // did we lose an agent?!
      // should we kill it and reclaim the memory?
      if (log.isErrorEnabled()) {
        log.error("Unable to resume agent "+id, e);
      }
      return;
    }

    try {
      onFailure(throwable);
    } catch (Exception e) {
      // too late now -- the dispatch failed.
      if (log.isErrorEnabled()) {
        log.error(
            "Notification for \"onFailure\" of agent "+
            id+" failed (ignored)", e);
      }
      return;
    }

    if (log.isInfoEnabled()) {
      log.info("Completed failed move of agent "+id);
    }

  }

  private void resumeAgent() {
    if (log.isInfoEnabled()) {
      log.info("Resume  agent "+id);
    }
    model.resume();
    if (log.isInfoEnabled()) {
      log.info("Resumed agent "+id);
    }
  }

  @Override
public String toString() {
    return "Move (nack) of agent "+id;
  }
}
