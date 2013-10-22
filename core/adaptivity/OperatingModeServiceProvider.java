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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.util.Collectors;
import org.cougaar.util.KeyedSet;
import org.cougaar.util.Thunk;
import org.cougaar.util.UnaryPredicate;

/**
 * This Plugin serves as an OperatingModeServiceProvider. It
 * subscribes to {@link OperatingMode}s using a subscription allowing
 * immediate access to individual OperatingModes by name. Provides
 * OperatingModeService to components needing name-based access to
 * OperatingModes.
 **/
public class OperatingModeServiceProvider
    extends ComponentPlugin
    implements ServiceProvider
{
  /**
   * A {@link KeyedSet} for sets of OperatingModes keyed by name.
   **/
  private static class OperatingModeSet extends KeyedSet {
    public OperatingModeSet() {
      super();
      makeSynchronized();
    }

    @Override
   protected Object getKey(Object o) {
      OperatingMode om = (OperatingMode) o;
      return om.getName();
    }

    public OperatingMode getOperatingMode(String name) {
      return (OperatingMode) inner.get(name);
    }
  }

  /**
   * An implementation of OperatingModeService in terms of the
   * information in the OperatingModeSet.
   **/
  private class OperatingModeServiceImpl implements OperatingModeService {
    public void addListener(OperatingModeService.Listener l) {
      synchronized (listeners) {
        listeners.add(l);
      }
    }
    public void removeListener(OperatingModeService.Listener l) {
      synchronized (listeners) {
        listeners.remove(l);
      }
    }
    public OperatingMode getOperatingModeByName(String knobName) {
      synchronized (omSet) {
        return omSet.getOperatingMode(knobName);
      }
    }
    public Set getAllOperatingModeNames() {
      synchronized (omSet) {
        return new HashSet(omSet.keySet());
      }
    }
  }

  private static UnaryPredicate operatingModePredicate = new UnaryPredicate() {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return o instanceof OperatingMode;
    }
  };

  LoggingService logger;
  private OperatingModeSet omSet = new OperatingModeSet();
  private IncrementalSubscription operatingModes;
  private List listeners = new ArrayList(2);
  private Thunk listenerAddThunk =
    new Thunk() {
      public void apply(Object o) {
        OperatingModeService.Listener l =
          (OperatingModeService.Listener) o;
        if (l.wantAdds()) blackboard.publishChange(o);
      }
    };
  private Thunk listenerChangeThunk =
    new Thunk() {
      public void apply(Object o) {
        OperatingModeService.Listener l =
          (OperatingModeService.Listener) o;
        if (l.wantChanges()) blackboard.publishChange(o);
      }
    };
  private Thunk listenerRemoveThunk =
    new Thunk() {
      public void apply(Object o) {
        OperatingModeService.Listener l =
          (OperatingModeService.Listener) o;
        if (l.wantRemoves()) blackboard.publishChange(o);
      }
    };

  /**
   * Override base class method to register our service with the
   * service broker.
   **/
  @Override
public void load() {
    super.load();
    getServiceBroker().addService(OperatingModeService.class, this);
    logger = getServiceBroker().getService(this, LoggingService.class, null);
  }

  /**
   * Override base class method to unregister our service with the
   * service broker.
   **/
  @Override
public void unload() {
    getServiceBroker().revokeService(OperatingModeService.class, this);
    super.unload();
  }

  /**
   * Standard setupSubscriptions subscribes to all OperatingModes.
   **/
  @Override
public void setupSubscriptions() {
    synchronized (omSet) {
      operatingModes = getBlackboardService().subscribe(operatingModePredicate);
      omSet.addAll(operatingModes);
    }
  }

  /**
   * Standard execute method does nothing. Our subscription
   * automatically maintains the information of interest in omSet
   * where it is referenced directly by the service API.
   **/
  @Override
public void execute() {
    synchronized (omSet) {
      if (operatingModes.hasChanged()) {
        Collection c;
        c = operatingModes.getAddedCollection();
        if (c.size() > 0) {
          omSet.addAll(c);
          if (logger.isDebugEnabled()) logger.debug("OM Added");
          Collectors.apply(listenerAddThunk, listeners);
        }
        c = operatingModes.getChangedCollection();
        if (c.size() > 0) {
          if (logger.isDebugEnabled()) logger.debug("OM Changed");
          Collectors.apply(listenerChangeThunk, listeners);
        }
        c = operatingModes.getRemovedCollection();
        if (c.size() > 0) {
          omSet.removeAll(c);
          if (logger.isDebugEnabled()) logger.debug("OM Removed");
          Collectors.apply(listenerRemoveThunk, listeners);
        }
      }
    }
  }

  /**
   * Gets (creates) an implementation of the OperatingModeService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == OperatingModeService.class) {
      return new OperatingModeServiceImpl();
    }
    throw new IllegalArgumentException(getClass() + " does not furnish "
                                       + serviceClass);
  }

  /**
   * Releases an implementation of the OperatingModeService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object svc)
  {
    if (serviceClass != OperatingModeService.class
        || svc.getClass() != OperatingModeServiceImpl.class) {
      throw new IllegalArgumentException(getClass() + " did not furnish " + svc);
    }
  }
}
