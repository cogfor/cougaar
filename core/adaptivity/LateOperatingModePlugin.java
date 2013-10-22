/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.OperatingModeService;

public class LateOperatingModePlugin extends ServiceUserPlugin {
  public static final String OM_NAME = "LateOperatingModePlugin.LATE";

  private static final OMCRange[] RANGES = {
    new OMCRange(0.0, 1.0)
  };

  private static final OMCRangeList VALUES = new OMCRangeList(RANGES);

  private static final Class[] requiredServices = {
    OperatingModeService.class
  };

  private OperatingModeService omService;
  private OperatingMode om;
  private int step = 0;

  public LateOperatingModePlugin() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
    if (haveServices()) check();
  }

  private boolean haveServices() {
    if (omService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      omService = sb.getService(this, OperatingModeService.class, null);
      return true;
    }
    return false;
  }

  @Override
public void execute() {
    if (timerExpired()) {
      if (haveServices()) check();
    }
  }

  private void check() {
    cancelTimer();
    if (++step >= 4) {
      if (om == null) {
        om = new OperatingModeImpl(OM_NAME, VALUES, new Double(0.0));
        blackboard.publishAdd(om);
        logger.info("Publishing " + om);
      } else {
        logger.info("Checking " + om);
      }
    } else {
      logger.info("Waiting step " + step);
    }
    resetTimer(5000);
  }
}
