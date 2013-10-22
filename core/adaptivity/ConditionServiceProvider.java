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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.KeyedSet;
import org.cougaar.util.UnaryPredicate;

/**
 * This Plugin serves as an ConditionServiceProvider. It
 * subscribes to {@link Condition}s using a subscription allowing
 * immediate access to individual Conditions by name. Provides
 * ConditionService to components needing name-based access to
 * Conditions.
 **/
public class ConditionServiceProvider
    extends ComponentPlugin
    implements ServiceProvider
{
  /**
   * A {@link KeyedSet} for sets of Conditions keyed by name.
   **/
  private static class ConditionSet extends KeyedSet {
    public ConditionSet() {
      super();
      makeSynchronized();
    }

    @Override
   protected Object getKey(Object o) {
      Condition sm = (Condition) o;
      return sm.getName();
    }

    public Condition getCondition(String name) {
      return (Condition) inner.get(name);
    }
  }

  /**
   * An implementation of ConditionService in terms of the
   * information in the ConditionSet.
   **/
  private class ConditionServiceImpl implements ConditionService {
    public Condition getConditionByName(String knobName) {
      synchronized (smSet) {
        return smSet.getCondition(knobName);
      }
    }

    public Set getAllConditionNames() {
      synchronized (smSet) {
        return Collections.unmodifiableSet(smSet.keySet());
      }
    }

    public void addListener(Listener l) {
      synchronized (listeners) {
        listeners.add(l);
      }
    }

    public void removeListener(Listener l) {
      synchronized (listeners) {
        listeners.remove(l);
      }
    }
  }

  private static UnaryPredicate ConditionPredicate = new UnaryPredicate() {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return o instanceof Condition;
    }
  };

  private ConditionSet smSet = new ConditionSet();
  private IncrementalSubscription conditions;

  private List listeners = new ArrayList(2);

  private LoggingService logger;

  /**
   * Override base class method to register our service with the
   * service broker.
   **/
  @Override
public void load() {
    super.load();
    logger = getServiceBroker().getService(this, LoggingService.class, null);
    getServiceBroker().addService(ConditionService.class, this);
  }

  @Override
public void unload() {
    getServiceBroker().releaseService(this, LoggingService.class, logger);
    getServiceBroker().revokeService(ConditionService.class, this);
    super.unload();
  }

  /**
   * Standard setupSubscriptions subscribes to all Conditions.
   **/
  @Override
public void setupSubscriptions() {
    synchronized (smSet) {
      conditions = getBlackboardService().subscribe(ConditionPredicate);
      smSet.addAll(conditions);
    }
  }

  /**
   * Standard execute method does nothing. Our subscription
   * automatically maintains the information of interest in smSet
   * where it is referenced directly by the service API.
   **/
  @Override
public void execute() {
    if (conditions.hasChanged()) {
      synchronized (smSet) {
        smSet.addAll(conditions.getAddedCollection());
        smSet.removeAll(conditions.getRemovedCollection());
      }
      fireListeners();
    }
  }

  private void fireListeners() {
    BlackboardService bb = getBlackboardService();
    if (logger.isDebugEnabled()) {
      for (Iterator i = conditions.getAddedCollection().iterator(); i.hasNext(); ) {
        logger.debug("Condition added: " + i.next());
      }
      for (Iterator i = conditions.getChangedCollection().iterator(); i.hasNext(); ) {
        logger.debug("Condition changed: " + i.next());
      }
      for (Iterator i = conditions.getRemovedCollection().iterator(); i.hasNext(); ) {
        logger.debug("Condition removed: " + i.next());
      }
      logger.debug("ConditionServiceProvider.fireListeners:");
    }
    for (Iterator i = listeners.iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (logger.isDebugEnabled()) logger.debug("    " + o);
      bb.publishChange(o);
    }
  }

  /**
   * Gets (creates) an implementation of the ConditionService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == ConditionService.class) {
      return new ConditionServiceImpl();
    }
    throw new IllegalArgumentException(getClass() + " does not furnish "
                                       + serviceClass);
  }

  /**
   * Releases an implementation of the ConditionService.
   * This is part of our implementation of the ServiceProvider API.
   **/
  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object svc)
  {
    if (serviceClass != ConditionService.class
        || svc.getClass() != ConditionServiceImpl.class) {
      throw new IllegalArgumentException(getClass() + " did not furnish " + svc);
    }
  }
}
