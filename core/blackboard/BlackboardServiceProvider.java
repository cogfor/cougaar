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

package org.cougaar.core.blackboard;

import java.util.Collection;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.BlackboardMetricsService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;

/**
 * The service provider for the {@link BlackboardService},
 * {@link BlackboardQueryService}, and {@link
 * BlackboardMetricsService}.
 * <p>
 * All operations are backed by the
 * {@link org.cougaar.core.blackboard.Distributor}. 
 */
public class BlackboardServiceProvider implements ServiceProvider {
  
  private Distributor distributor;
  
  public BlackboardServiceProvider(Distributor distributor) {
    super();
    this.distributor = distributor;
  }
  
  public Object getService(BlackboardClient bbclient) {
    return new BlackboardServiceImpl(bbclient);
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == BlackboardService.class) {
      return new BlackboardServiceImpl( (BlackboardClient)requestor);
    } else if (serviceClass == BlackboardMetricsService.class) {
      return getBlackboardMetricsService();
    } else if (serviceClass == BlackboardQueryService.class) {
      return new BlackboardQueryServiceImpl(requestor);
    } else {
      throw new IllegalArgumentException(
          "BlackboardServiceProvider does not provide a service for: "+
                                         serviceClass);
    }
  }

  public void releaseService(
      ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
    // ?? each client will get its own subscriber - how can we clean them up??
  }

  // only need one instance of this service.
  private BlackboardMetricsService bbmetrics = null;
  private BlackboardMetricsService getBlackboardMetricsService() {
    if (bbmetrics == null) {
      bbmetrics = new BlackboardMetricsServiceImpl();
    }
    return bbmetrics;
  }
  
  /** BlackboardService is a Subscriber */
  private final class BlackboardServiceImpl
    extends Subscriber
    implements BlackboardService
  {
    BlackboardServiceImpl(BlackboardClient client) {
      super(client, distributor);
    }
  }

  /** The implementation of BlackboardMetricsService */
  private final class BlackboardMetricsServiceImpl
    implements BlackboardMetricsService
  {
    public int getBlackboardCount() {
      return distributor.getBlackboardSize();
    }
    public int getBlackboardCount(Class cl) {
      return distributor.getBlackboardCount(cl);
    }
    public int getBlackboardCount(UnaryPredicate predicate) {
      return distributor.getBlackboardCount(predicate);
    }
  }

  /** The implementation of BlackboardQueryService */
  private final class BlackboardQueryServiceImpl 
    implements BlackboardQueryService 
  {
    private BlackboardQueryServiceImpl(Object requestor) {
      // ignore the requestor (for now).
    }
    public final <T> Collection query(UnaryPredicate<T> isMember) {
      QuerySubscription<T> qs = new QuerySubscription<T>(isMember);
      //qs.setSubscriber(null);  // ignored
      distributor.fillQuery(qs);
      return qs.getCollection();
    }
  }
}
