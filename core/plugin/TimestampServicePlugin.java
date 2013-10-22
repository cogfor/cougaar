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

package org.cougaar.core.plugin;

import org.cougaar.core.blackboard.TimestampEntry;
import org.cougaar.core.blackboard.TimestampSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.BlackboardTimestampService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * This component advertises the {@link BlackboardTimestampService}
 * for use by other plugins.
 * <p>
 * This classes provides access to a shared {@link
 * TimestampSubscription}'s contents.
 */
public final class TimestampServicePlugin
extends ComponentPlugin
{
  
  private static final UnaryPredicate PRED =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        // for now accept all unique objects
        //
        // could filter for just Tasks and PlanElements
        return (o instanceof UniqueObject);
      }
    };

  private final TimestampSubscription timeSub = 
    new TimestampSubscription(PRED);

  private BlackboardTimestampServiceProvider btSP;

  @Override
public void load() {
    super.load();
    if (btSP ==  null) {
      btSP = new BlackboardTimestampServiceProvider();
      getServiceBroker().addService(BlackboardTimestampService.class, btSP);
    }
  }

  @Override
public void unload() {
    if (btSP != null) {
      getServiceBroker().revokeService(BlackboardTimestampService.class, btSP);
      btSP = null;
    }
    super.unload();
  }

  @Override
protected void setupSubscriptions() {
    Object sub = blackboard.subscribe(timeSub);
    if (sub != timeSub) {
      throw new RuntimeException(
          "Subscribe returned a different subscription?");
    }
  }
  
  @Override
protected void execute() {
    // never, since we never register to watch the changes
  }

  private class BlackboardTimestampServiceProvider
  implements ServiceProvider {

    // for now we can share a single service instance
    private BlackboardTimestampServiceImpl INSTANCE =
      new BlackboardTimestampServiceImpl();

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      if (serviceClass == BlackboardTimestampService.class) {
        return INSTANCE;
      } else {
        throw new IllegalArgumentException(
            this+" does not provide a service for: "+serviceClass);
      }
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      // ignore
    }

    private class BlackboardTimestampServiceImpl
      implements BlackboardTimestampService {
        public long getCreationTime(UID uid) {
          return timeSub.getCreationTime(uid);
        }
        public long getModificationTime(UID uid) {
          return timeSub.getModificationTime(uid);
        }
        public TimestampEntry getTimestampEntry(UID uid) {
          return timeSub.getTimestampEntry(uid);
        }
      }
  }
  
}
