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

package org.cougaar.core.persist;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OperatingModeImpl;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.PersistenceControlService;
import org.cougaar.util.UnaryPredicate;

/**
 * This component creates blackboard {@link
 * org.cougaar.core.adaptivity.OperatingMode}s that control
 * persistence settings.
 */ 
public class PersistenceControlPlugin extends ServiceUserPlugin {
  private static class MyOperatingMode
    extends OperatingModeImpl
    implements NotPersistable
  {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   String mediaName;
    String controlName;

    MyOperatingMode(String mediaName, String controlName, OMCRangeList values) {
      super("Persistence." + mediaName + "." + controlName, values);
      this.mediaName = mediaName;
      this.controlName = controlName;
    }

    MyOperatingMode(String controlName, OMCRangeList values) {
      super("Persistence." + controlName, values);
      this.mediaName = null;
      this.controlName = controlName;
    }
  }

  private static UnaryPredicate myOperatingModePredicate =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return o instanceof MyOperatingMode;
      }
    };

  private static Class[] requiredServices = {
    PersistenceControlService.class
  };

  private boolean createdOperatingModes = false;

  private IncrementalSubscription myOperatingModes;

  private PersistenceControlService persistenceControlService;

  public PersistenceControlPlugin() {
    super(requiredServices);
  }

  protected boolean haveServices() {
    if (persistenceControlService != null) return true;
    if (acquireServices()) {
      persistenceControlService = getServiceBroker().getService(this, PersistenceControlService.class, null);
      return true;
    }
    return false;
  }


  @Override
public void setupSubscriptions() {
    myOperatingModes = blackboard.subscribe(myOperatingModePredicate);
  }

  @Override
public void execute() {
    if (haveServices()) {
      if (!createdOperatingModes ) {
        createOperatingModes();
        createdOperatingModes = true;
      } else if (myOperatingModes.hasChanged()) {
        updateOperatingModes(myOperatingModes.getChangedCollection());
      }
    }
  }

  private void createOperatingModes() {
    String[] controlNames = persistenceControlService.getControlNames();
    for (int i = 0; i < controlNames.length; i++) {
      String controlName = controlNames[i];
      OMCRangeList controlValues = persistenceControlService.getControlValues(controlName);
      MyOperatingMode om = new MyOperatingMode(controlName, controlValues);
      blackboard.publishAdd(om);
      if (logger.isDebugEnabled()) logger.debug("Added " + om);
    }
    String[] mediaNames = persistenceControlService.getMediaNames();
    for (int j = 0; j < mediaNames.length; j++) {
      String mediaName = mediaNames[j];
      controlNames = persistenceControlService.getMediaControlNames(mediaName);
      for (int i = 0; i < controlNames.length; i++) {
        String controlName = controlNames[i];
        OMCRangeList controlValues = persistenceControlService.getMediaControlValues(mediaName, controlName);
        MyOperatingMode om = new MyOperatingMode(mediaName, controlName, controlValues);
        blackboard.publishAdd(om);
      if (logger.isDebugEnabled()) logger.debug("Added " + om);
      }
    }
  }

  private void updateOperatingModes(Collection changedOperatingModes) {
    for (Iterator i = changedOperatingModes.iterator(); i.hasNext(); ) {
      MyOperatingMode om = (MyOperatingMode) i.next();
      try {
        if (om.mediaName == null) {
          persistenceControlService.setControlValue(om.controlName, om.getValue());
        } else {
          persistenceControlService.setMediaControlValue(om.mediaName, om.controlName, om.getValue());
        }
      } catch (RuntimeException re) {
        logger.error("set persistence control failed", re);
      }
    }
  }
}
