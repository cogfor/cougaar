/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.persist;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.RegisterContext;
import org.cougaar.core.agent.service.uid.UIDServiceComponent;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BindingUtility;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceBrokerSupport;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/*
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.glm.ldm.asset.*;
*/

/**
 * Test class to rehydrate an agent snapshot and print the contents.
 * <p>
 * Usage is:<pre> 
 *   java \
 *     -Djava.class.path=$COUGAAR_INSTALL_PATH/lib/bootstrap.jar \
 *     -Dorg.cougaar.install.path=$COUGAAR_INSTALL_PATH \
 *     -Dorg.cougaar.core.persistence.enable=true \
 *     org.cougaar.bootstrap.Bootstrapper \
 *     org.cougaar.core.persist.RehydrateTest \
 *     <i>THE_AGENT_NAME</i>
 * </pre>
 * Optionally enable persistence detail logging by setting:<pre>
 *     -Dorg.cougaar.core.logging.log4j.category.org.cougaar.core=DETAIL#org.cougaar.util.log.log4j.DetailPriority
 * </pre>
 *
 * @property org.cougaar.core.persist.rehydrateTest.agent
 * Default RehydrateTest agent name if not specified on the
 * command line
 */
public class RehydrateTest {

  private static final String DEFAULT_AGENT_PROP =
    "org.cougaar.core.persist.rehydrateTest.agent";

  private static final String DEFAULT_AGENT =
    SystemProperties.getProperty(DEFAULT_AGENT_PROP);

  public static void main(String[] args) throws Exception {
    (new RehydrateTest(args)).run();
  }

  private final String[] args;
  private ServiceBroker sb;

  public RehydrateTest(String[] args) {
    this.args = args;
  }

  public void run() throws Exception {
    String name = getAgent();
    System.out.println("Test rehydrate "+name);

    // create component model and minimal services
    createServiceBroker();
    loadAgentIdentificationService(name);
    loadLoggingService();
    loadUIDService();
    loadContext();
    loadPersistence();

    // rehydrate
    rehydrateAgent();

    // get blackboard contents
    System.out.println("# rehydrating blackboard for "+name);
    PersistenceEnvelope rehydrationEnvelope = rehydrateBlackboard();

    // print objects matching our filter
    UnaryPredicate pred = createPredicate(); 
    showContents(rehydrationEnvelope, pred);
  }

  protected String getAgent() {
    if (args != null && args.length > 0) {
      return args[0];
    }
    if (DEFAULT_AGENT != null) {
      return DEFAULT_AGENT;
    }
    throw new RuntimeException("Specify an agent name");
  }

  protected UnaryPredicate createPredicate() {
    UnaryPredicate pred = new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return true;
      }
    };
    /*
     final Verb GLS = Verb.getVerb("GetLogSupport");
     UnaryPredicate pred = new UnaryPredicate() {
       public boolean execute(Object o) {
         if (o instanceof Task) {
           Task t = (Task) o;
           Verb v = t.getVerb();
           return (v != null && v.equals(GLS));
         } else if (o instanceof AssetTransfer) {
           return true;
         } else if (o instanceof Organization) {
           return true;
         }
         return false;
       }
     };
     */
    return pred;
  }

  /**
   * Construct the service broker with minimal services.
   */
  private void createServiceBroker() {
    this.sb = new ServiceBrokerSupport() {};
  }
  private void loadAgentIdentificationService(String name) {
    sb.addService(
        AgentIdentificationService.class,
        new AgentIdentificationServiceProvider(name));
  }
  private void loadLoggingService() {
    sb.addService(
        LoggingService.class,
        new LoggingServiceProvider());
  }
  private void loadUIDService() {
    UIDServiceComponent uidC = new UIDServiceComponent();
    load(uidC);
  }
  private void loadContext() {
    RegisterContext rc = new RegisterContext();
    load(rc);
  }
  //DataProtectionService.class
  private void loadPersistence() {
    PersistenceServiceComponent psc = new PersistenceServiceComponent();
    load(psc);
  }
  private void load(Component c) {
    BindingSite bs = new BindingSite() {
      public ServiceBroker getServiceBroker() {
        return sb;
      }
      public void requestStop() {
      }
    };
    BindingUtility.activate(c, bs, sb);
  }

  private void rehydrateAgent() {
    rehydrateList("ignoreme");
  }

  private List rehydrateList(String clname) {
    // mobile state
    PersistenceObject persistenceObject = null;
    // identity
    final PersistenceIdentity persistenceIdentity =
      new PersistenceIdentity(clname);
    PersistenceClient persistenceClient =
      new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          return persistenceIdentity;
        }
        public List getPersistenceData() {
          throw new UnsupportedOperationException();
        }
      };
    // rehydrate list
    PersistenceServiceForAgent persistenceService = 
      sb.getService(
       persistenceClient, PersistenceServiceForAgent.class, null);
    persistenceService.rehydrate(persistenceObject);
    RehydrationData rd = persistenceService.getRehydrationData();
    List rehydrationList = (rd == null ? null : rd.getObjects());
    return rehydrationList;
  }

  private PersistenceEnvelope rehydrateBlackboard(
      ) {
    Object state = null;
    Persistence persistence = BlackboardPersistence.find(sb);
    PersistenceEnvelope rehydrationEnvelope = new PersistenceEnvelope();
    // ignore the returned "RehydrateResult", we only want the envelope
    persistence.rehydrate(rehydrationEnvelope, state);
    return rehydrationEnvelope;
  }

  private void showContents(
      PersistenceEnvelope rehydrationEnvelope,
      UnaryPredicate pred) {
    int size = rehydrationEnvelope.size();
    System.out.println("size: "+size);
    Set addedSet = new HashSet();
    Set removedSet = new HashSet();
    Iterator iter = rehydrationEnvelope.getAllTuples();
    while (iter.hasNext()) {
      EnvelopeTuple et = (EnvelopeTuple) iter.next();
      Object obj = et.getObject();
      if (et.isAdd()) {
        if (pred.execute(obj)) addedSet.add(obj);
      } else if (et.isBulk()) {
        Collection c = (Collection) obj;
        for (Iterator x = c.iterator(); x.hasNext(); ) {
          Object o2 = x.next();
          if (pred.execute(o2)) addedSet.add(o2);
        }
      } else if (et.isRemove()) {
        if (pred.execute(obj)) removedSet.add(obj);
      }
    }
    System.out.println("addedSet["+addedSet.size()+"]:");
    for (Iterator i2 = addedSet.iterator(); i2.hasNext(); ) {
      Object obj = i2.next();
      System.out.println(" "+obj);
    }
    System.out.println("removedSet["+removedSet.size()+"]:");
    for (Iterator i2 = removedSet.iterator(); i2.hasNext(); ) {
      Object obj = i2.next();
      System.out.println(" "+obj);
    }
  }

  private static class AgentIdentificationServiceProvider
    implements ServiceProvider {
      private final AgentIdentificationService AIS;
      public AgentIdentificationServiceProvider(
          final String name) {
        final MessageAddress addr =
          MessageAddress.getMessageAddress(name);
        this.AIS = new AgentIdentificationService() {
          public MessageAddress getMessageAddress() {
            return addr;
          }
          public String getName() {
            return name;
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (AgentIdentificationService.class.isAssignableFrom(serviceClass)) {
          return AIS;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service)  {
      }
    }
}
