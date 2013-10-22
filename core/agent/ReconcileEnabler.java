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

package org.cougaar.core.agent;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component uses the {@link ReconcileEnablerService}
 * provided by the {@link Reconcile} component to enable and
 * disable blackboard reconciliation.
 * <p>
 * Reconcile can not start/stop itself, since the message transport
 * (which is loaded after Reconcile) must be started/stopped before
 * Reconcile.
 */
public final class ReconcileEnabler
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private ReconcileEnablerService res;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    res = sb.getService(this, ReconcileEnablerService.class, null);
    if (res == null) {
      throw new RuntimeException(
          "Unable to obtain ReconcileEnablerService");
    }
  }
  @Override
public void start() {
    super.start();
    res.startTimer();
  }
  @Override
public void suspend() {
    super.suspend();
    res.stopTimer();
  }
  @Override
public void resume() {
    super.resume();
    res.startTimer();
  }
  @Override
public void stop() {
    super.stop();
    res.stopTimer();
  }
  @Override
public void unload() {
    super.unload();
    if (res != null) {
      sb.releaseService(this, ReconcileEnablerService.class, res);
      res = null;
    }
  }
}
