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

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.Claimable;
import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.SubscriptionWatcher;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.service.UIDService;
import org.cougaar.planning.ldm.ClusterServesPlugin;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.LatePropertyProvider;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.PropertyProvider;
import org.cougaar.planning.ldm.PrototypeProvider;
import org.cougaar.planning.service.LDMService;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.StateModelException;
import org.cougaar.util.SyncTriggerModelImpl;
import org.cougaar.util.Trigger;
import org.cougaar.util.TriggerModel;
import org.cougaar.util.UnaryPredicate;

public abstract class PluginAdapter
  extends GenericStateModelAdapter
  implements PluginStateModel,  Component,
  BlackboardClient, PluginBase
{
  /** minimum time (in millis) which wake functions like to pause for **/
  public final static long WAKE_LATENCY = 1000L;

  /** keep this around for compatability with old plugins **/
  protected PlanningFactory theLDMF = null;

  protected LDMServesPlugin theLDM = null;

  // 
  // constructor
  //

  public PluginAdapter() {
  }

  private PluginBindingSite bindingSite = null;

  public final void setBindingSite(final BindingSite bs) {
    this.bindingSite = new PluginBindingSite() {
      public MessageAddress getAgentIdentifier() {
        return PluginAdapter.this.getAgentIdentifier();
      }
      public ConfigFinder getConfigFinder() {
        return PluginAdapter.this.getConfigFinder();
      }
      public ServiceBroker getServiceBroker() {
        return bs.getServiceBroker();
      }
      public void requestStop() {
        bs.requestStop();
      }
    };
  }

  protected final PluginBindingSite getBindingSite() {
    return bindingSite;
  }

  //
  // extra services
  //

  private MessageAddress agentId = null;

  public final void setAgentIdentificationService(
      AgentIdentificationService ais) {
    if (ais == null) {
      // Revocation
    } else {
      this.agentId = ais.getMessageAddress();
    }
  }

  private LDMService ldmService = null;
  public final void setLDMService(LDMService s) {
    ldmService = s;
  }
  protected final LDMService getLDMService() {
    return ldmService;
  }

  // alarm service
  private AlarmService alarmService = null;
  public final void setAlarmService(AlarmService s) {
    alarmService = s;
  }
  protected final AlarmService getAlarmService() {
    return alarmService;
  }

  // demo control service
  private DemoControlService demoControlService = null;
  public final void setDemoControlService(DemoControlService dcs) {
    demoControlService = dcs;
  }
  protected final DemoControlService getDemoControlService() {
    return demoControlService;
  }

  private SchedulerService schedulerService = null;
  public final void setSchedulerService(SchedulerService ss) {
    schedulerService = ss;
  }
  protected final SchedulerService getSchedulerService() {
    return schedulerService;
  }

  //UID service
  private UIDService theUIDService = null;
  public void setUIDService(UIDService us) {
    theUIDService = us;
  }
  public final UIDService getUIDService() {
    return theUIDService;
  }

  //Domain service (factory service piece of old LDM)
  private DomainService theDomainService = null;
  public void setDomainService(DomainService ds) {
    theDomainService = ds;
  }
  public final DomainService getDomainService() {
    return theDomainService;
  }

  //PrototypeRegistryService (prototype/property piece of old LDM)
  private PrototypeRegistryService thePrototypeRegistryService = null;
  public void setPrototypeRegistryService(PrototypeRegistryService prs) {
    thePrototypeRegistryService = prs;
  }
  public final PrototypeRegistryService getPrototypeRegistryService() {
    return thePrototypeRegistryService;
  }

  //
  // Implement (some of) BlackboardClient
  //
  protected String blackboardClientName = null;

  public String getBlackboardClientName() {
    if (blackboardClientName == null) {
      StringBuffer buf = new StringBuffer();
      buf.append(getClass().getName());
      if (parameters != null) {
	buf.append("[");
	String sep = "";
	for (Enumeration params = parameters.elements(); params.hasMoreElements(); ) {
	  buf.append(sep);
	  buf.append(params.nextElement().toString());
	  sep = ",";
	}
	buf.append("]");
      }
      blackboardClientName = buf.substring(0);
    }
    return blackboardClientName;
  }

  public String toString() {
    return getBlackboardClientName();
  }

  //
  // implement ParameterizedPlugin
  //


    /**
     * Support "interval parameters" which are long values that can be
     * expressed with time period units (e.g. seconds)
     **/
    private static class Interval {
        String name;
        long factor;
        public Interval(String name, long factor) {
            this.name = name;
            this.factor = factor;
        }
    }

    /**
     * The known unit names
     **/
    private static Interval[] intervals = {
        new Interval("seconds", 1000L),
        new Interval("minutes", 1000L * 60L),
        new Interval("hours",   1000L * 60L * 60L),
        new Interval("days",    1000L * 60L * 60L * 24L),
        new Interval("weeks",   1000L * 60L * 60L * 24L * 7L),
    };

    /**
     * Make this utility trivially accessible to plugins
     **/
    public long parseIntervalParameter(int paramIndex) {
        return parseInterval((String) getParameters().get(paramIndex));
    }

    public long parseInterval(String param) {
        param = param.trim();
        int spacePos = param.indexOf(' ');
        long factor = 1L;
        if (spacePos >= 0) {
            String units = param.substring(spacePos + 1).toLowerCase();
            param = param.substring(0, spacePos);
            for (int i = 0; i < intervals.length; i++) {
                if (intervals[i].name.startsWith(units)) {
                    factor = intervals[i].factor;
                    break;
                }
            }
        }
        return Long.parseLong(param) * factor;
    }

  
  // Many plugins expect a non-null value
  private Vector parameters = new Vector(0);

  public void setParameter(Object param) {
    if (param != null) {
      if (param instanceof List) {
        parameters = new Vector((List) param);
      } else {
        System.err.println("Warning: "+this+" initialized with non-vector parameter "+param);
      }
    }
  }


  /** get any Plugin parameters passed by the plugin instantiator.
   * If they haven't been set, will return null.
   * Should be set between plugin construction and initialization.
   **/
  public Vector getParameters() {
    return parameters;
  }


  //
  // StateModel extensions
  //

  /** Component Model <em>AND</em> GenericStateModel initialization **/
  public void initialize() {
    super.initialize();         // uninitialized->unloaded (defined in GSMAdapter)
  }

  public void load() throws StateModelException {
    if (getBlackboardService() == null)
      System.err.println("Warning: Could not get Blackboard service "+this);
    super.load();
    load(null);
  }

  /** Load the plugin.  No longer pays any attention to the passed object,
   * as it will now always be null.
   **/
  public void load(Object object) {
    setThreadingChoice(getThreadingChoice()); // choose the threading model
    theLDM = getLDMService().getLDM();
    theLDMF = (PlanningFactory) getDomainService().getFactory("planning");

    if (this instanceof PrototypeProvider) {
      getPrototypeRegistryService().addPrototypeProvider((PrototypeProvider)this);
    }
    if (this instanceof PropertyProvider) {
      getPrototypeRegistryService().addPropertyProvider((PropertyProvider)this);
    }
    if (this instanceof LatePropertyProvider) {
      getPrototypeRegistryService().addLatePropertyProvider((LatePropertyProvider)this);
    }
    
    //ServiceBroker sb = getBindingSite().getServiceBroker();

    // fire up the threading model
    setThreadingModel(createThreadingModel());
  }

  /** */
  public void start() throws StateModelException {
    super.start();
    startThreadingModel();
  }


  public void suspend() throws StateModelException {
    super.suspend();
    threadingModel.suspend();
  }

  public void resume() throws StateModelException {
    super.resume();
    threadingModel.resume();
  }

  //
  // Customization of PluginAdapter
  //

  public int getSubscriptionCount() {
    return getBlackboardService().getSubscriptionCount();
  }
  
  public int getSubscriptionSize() {
    return getBlackboardService().getSubscriptionSize();
  }

  public int getPublishAddedCount() {
    return getBlackboardService().getPublishAddedCount();
  }

  public int getPublishChangedCount() {
    return getBlackboardService().getPublishChangedCount();
  }

  public int getPublishRemovedCount() {
    return getBlackboardService().getPublishRemovedCount();
  }

  //
  // Ivars and accessor methods
  //

  //Blackboard service
  private BlackboardService theBlackboard = null;

  public void setBlackboardService(BlackboardService s) {
    theBlackboard = s;
  }

  /** Safely return our BlackboardService 
   * Plugin.load() must have completed in order 
   * for the value to be defined.
   **/
  public final BlackboardService getBlackboardService() {
    return theBlackboard;
  }

  /** let subclasses get ahold of the cluster without having to catch it at
   * load time.  May throw a runtime exception if the plugin hasn't been 
   * loaded yet.
   * @deprecated This method no longer allows direct access to the Cluster (Agent): instead
   * it will always return null.
   **/
  protected final ClusterServesPlugin getCluster() {
    return dummyCluster;
  }

  private ClusterServesPlugin dummyCluster = new ClusterServesPlugin() {
      // real ones
      public ConfigFinder getConfigFinder() { return PluginAdapter.this.getConfigFinder(); }
      public MessageAddress getMessageAddress() { return PluginAdapter.this.getMessageAddress();}
      public UIDServer getUIDServer() { return PluginAdapter.this.getUIDServer(); }
      public LDMServesPlugin getLDM() { return PluginAdapter.this.getLDM(); }
      
      // DemoControl service
      public void setTime(long time) { getDemoControlService().setSocietyTime(time);}
      public void setTime(long time, boolean foo) { getDemoControlService().setSocietyTime(time,foo);}
      public void setTimeRate(double rate) { getDemoControlService().setSocietyTimeRate(rate); }
      public void advanceTime(long period) { getDemoControlService().advanceSocietyTime(period); }
      public void advanceTime(long period, boolean foo) { getDemoControlService().advanceSocietyTime(period, foo); }
      public void advanceTime(long period, double rate) { getDemoControlService().advanceSocietyTime(period, rate); }
      public void advanceTime(ExecutionTimer.Change[] changes) { getDemoControlService().advanceSocietyTime(changes); }
      public double getExecutionRate() { return getDemoControlService().getExecutionRate(); }

      // alarm service
      public long currentTimeMillis() { return getAlarmService().currentTimeMillis(); }
      public void addAlarm(Alarm alarm) {getAlarmService().addAlarm(alarm);}
      public void addRealTimeAlarm(Alarm a) {getAlarmService().addRealTimeAlarm(a);}

      // ??
      public java.sql.Connection getDatabaseConnection(Object locker) {throw new RuntimeException("Should not be called");}
      public void releaseDatabaseConnection(Object locker) {throw new RuntimeException("Should not be called");}
    };

  protected ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }

  // 
  // aliases for Transaction handling 
  //

  protected final void openTransaction() {
    getBlackboardService().openTransaction();
  }

  protected final boolean tryOpenTransaction() {
    return getBlackboardService().tryOpenTransaction();
  }

  protected final void closeTransaction() throws SubscriberException {
    getBlackboardService().closeTransaction();
  }
  
  protected final void closeTransactionDontReset() throws SubscriberException {
    getBlackboardService().closeTransactionDontReset();
  }
  
  protected final void closeTransaction(boolean resetp) throws SubscriberException {
    getBlackboardService().closeTransaction(resetp);
  }


  //
  // aliases for kicking watchers
  //

  /** storage for wasAwakened. Set/reset by run() **/
  private boolean explicitlyAwakened = false;

  /** true IFF were we awakened explicitly (i.e. we were asked to run
   * even if no subscription activity has happened).
   * The value is valid only while running in the main plugin thread.
   */
  protected boolean wasAwakened() { return explicitlyAwakened; }

  /** For adapter use only **/
  public final void setAwakened(boolean value) { explicitlyAwakened = value; }

  /** 
   * Hook which allows a plugin thread to request that the
   * primary plugin thread (the execute() method) be called.
   * Generally used when you want the plugin to be stimulated
   * by some non-internal state change ( e.g. when a timer goes off,
   * database activity, offline server activity, etc.)
   *
   * For plugin use only; No longer called by the infrastructure.
   **/
  public final void wake() {
    getBlackboardService().signalClientActivity();
  }


  /** Convenience method to specify given time to stimulate plugin.
   * (based on COUGAAR scenario time). 
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   * @param wakeTime actual scenario time to wake in milliseconds.
   **/ 	
  public Alarm wakeAt(long wakeTime) { 
    long cur = getAlarmService().currentTimeMillis()+WAKE_LATENCY;
    if (wakeTime < cur) {
      System.err.println("Warning: wakeAt("+(new Date(wakeTime))+") is less than "+WAKE_LATENCY+"ms in the future!");
      Thread.dumpStack();
      wakeTime = cur;
    }
      
    PluginAlarm pa = new PluginAlarm(wakeTime);
    getAlarmService().addAlarm(pa);
    return pa;
  }

  /** Convenience method to specify period of time to wait before
   * stimulating plugin (based on COUGAAR scenario time).
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   * @param delayTime (Scenario) milliseconds to wait before waking.
   **/
  public Alarm wakeAfter(long delayTime) { 
    if (delayTime<WAKE_LATENCY) {
      System.err.println("Warning: wakeAfter("+delayTime+"ms) is less than "+WAKE_LATENCY+"ms in the future!");
      Thread.dumpStack();
      delayTime=WAKE_LATENCY;
    }
      
    long absTime = getAlarmService().currentTimeMillis()+delayTime;
    PluginAlarm pa = new PluginAlarm(absTime);
    getAlarmService().addAlarm(pa);
    return pa;
  }

  /** like wakeAt() except always in real (wallclock) time.
   **/ 	
  public Alarm wakeAtRealTime(long wakeTime) { 
    long cur = System.currentTimeMillis()+WAKE_LATENCY;
    if (wakeTime < cur) {
      System.err.println("Warning: wakeAtRealTime("+(new Date(wakeTime))+") is less than "+WAKE_LATENCY+"ms in the future!");
      Thread.dumpStack();
      wakeTime = cur;
    }

    PluginAlarm pa = new PluginAlarm(wakeTime);
    getAlarmService().addRealTimeAlarm(pa);
    return pa;
  }

  /** like wakeAfter() except always in real (wallclock) time.
   **/
  public Alarm wakeAfterRealTime(long delayTime) { 
    if (delayTime<WAKE_LATENCY) {
      System.err.println("Warning: wakeAfterRealTime("+delayTime+"ms) is less than "+WAKE_LATENCY+"ms in the future!");
      Thread.dumpStack();
      delayTime=WAKE_LATENCY;
    }

    long absTime = System.currentTimeMillis()+delayTime;
    PluginAlarm pa = new PluginAlarm(absTime);
    getAlarmService().addRealTimeAlarm(pa);
    return pa;
  }


  /** What is the current Scenario time? 
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   **/
  public long currentTimeMillis() {
    return getAlarmService().currentTimeMillis();
  }

  /** what is the current (COUGAAR) time as a Date object?
   * Note: currentTimeMillis() is preferred, as it gives
   * more control over timezone, calendar, etc.
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   **/
  public Date getDate() {
    return new Date(currentTimeMillis());
  }

  //
  // aliases for subscriptions
  //


  /** Request a subscription to all objects for which 
   * isMember.execute(object) is true.  The returned Collection
   * is a transactionally-safe set of these objects which is
   * guaranteed not to change out from under you during run()
   * execution.
   * 
   * subscribe() may be called any time after 
   * load() completes.
   *
   * NOTE: we'll probably want a "new things" sort of collection 
   * for expanders.
   * Alias for getBlackboardService().subscribe(UnaryPredicate);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember) {
    return getBlackboardService().subscribe(isMember);
  }

  /** like subscribe(UnaryPredicate), but allows specification of
   * some other type of Collection object as the internal representation
   * of the collection.
   * Alias for getBlackboardService().subscribe(UnaryPredicate, Collection);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, Collection realCollection){
    return getBlackboardService().subscribe(isMember, realCollection);
  }

  /**
   * Alias for getBlackboardService().subscribe(UnaryPredicate, boolean);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
    return getBlackboardService().subscribe(isMember, isIncremental);
  }
  /**
   * Alias for <code>getBlackboardService().subscribe(UnaryPredicate, Collection, boolean);</code>
   * @param isMember a predicate to execute to ascertain
   * membership in the collection of the subscription.
   * @param realCollection a container to hold the contents of the subscription.
   * @param isIncremental should be true if an incremental subscription is desired.
   * An incremental subscription provides access to the incremental changes to the subscription.
   * @return the Subsciption.
   * @see org.cougaar.core.blackboard.Subscriber#subscribe
   * @see org.cougaar.core.blackboard.Subscription
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental) {
    return getBlackboardService().subscribe(isMember, realCollection, isIncremental);
  }

  /** Issue a query against the logplan.  Similar in function to
   * opening a new subscription, getting the results and immediately
   * closing the subscription, but can be implemented much more efficiently.
   * Note: the initial implementation actually does exactly this.
   **/
  protected final Collection query(UnaryPredicate isMember) {
    return getBlackboardService().query(isMember);
  }

  /**
   * Cancels the given Subscription which must have been returned by a
   * previous invocation of subscribe().  Alias for
   * <code> getBlackboardService().unsubscribe(Subscription)</code>.
   * @param subscription the subscription to cancel
   * @see org.cougaar.core.blackboard.Subscriber#unsubscribe
   **/
  protected final void unsubscribe(Subscription subscription) {
    getBlackboardService().unsubscribe(subscription);
  }


  // 
  // LDM access
  //

  protected final LDMServesPlugin getLDM() {
    return theLDM;
  }

  protected final PlanningFactory getFactory() {
    return theLDMF;
  }

  protected final Factory getFactory(String s) {
    return getDomainService().getFactory(s);
  }
  
  
  //
  // agent
  // 

  protected final MessageAddress getAgentIdentifier() {
    return agentId;
  }
  protected final MessageAddress getMessageAddress() {
    return getAgentIdentifier();
  }

  protected final UIDServer getUIDServer() {
    return getUIDService();
  }

  //
  // Blackboard changes publishing
  //

  protected final void publishAdd(Object o) {
    getBlackboardService().publishAdd(o);
  }
  protected final void publishRemove(Object o) {
    getBlackboardService().publishRemove(o);
  }
  protected final void publishChange(Object o) {
    getBlackboardService().publishChange(o, null);
  }
  /** mark an element of the Plan as changed.
   * Behavior is not defined if the object is not a member of the plan.
   * There is no need to call this if the object was added or removed,
   * only if the contents of the object itself has been changed.
   * The changes parameter describes a set of changes made to the
   * object beyond those tracked automatically by the object class
   * (see the object class documentation for a description of which
   * types of changes are tracked).  Any additional changes are
   * merged in <em>after</em> automatically collected reports.
   * @param changes a set of ChangeReport instances or null.
   **/
  protected final void publishChange(Object o, Collection changes) {
    getBlackboardService().publishChange(o, changes);
  }
    

  private PluginDelegate delegate = null;

  /** @return an object that exposes the protected plugin methods
   * as publics.
   **/
  protected final PluginDelegate getDelegate() {
    if (delegate == null) 
      delegate = createDelegate();
    return delegate;
  }
    
  protected PluginDelegate createDelegate() {
    return new Delegate();
  }

  //
  // implement PluginDelegate
  //
  protected class Delegate implements PluginDelegate {
    public ServiceBroker getServiceBroker() {
      return PluginAdapter.this.getBindingSite().getServiceBroker();
    }
    public BlackboardService getBlackboardService() { 
      return theBlackboard;
    }
    public BlackboardService getSubscriber() { 
      return theBlackboard;
    }
    public ClusterServesPlugin getCluster() {
      return PluginAdapter.this.getCluster();
    }
    public LDMServesPlugin getLDM() {
      return getLDMService().getLDM();
    }
    public PlanningFactory getFactory() {
      return PluginAdapter.this.getFactory();
    }
    public Factory getFactory(String s) {
      return PluginAdapter.this.getFactory(s);
    }
    public MessageAddress getMessageAddress() {
      return PluginAdapter.this.getAgentIdentifier();
    }
    public void openTransaction() {
      getBlackboardService().openTransaction();
    }
    public boolean tryOpenTransaction() {
      return getBlackboardService().tryOpenTransaction();
    }
    public void closeTransaction() throws SubscriberException {
      getBlackboardService().closeTransaction();
    }
    public void closeTransactionDontReset() throws SubscriberException {
      getBlackboardService().closeTransactionDontReset();
    }
    public void closeTransaction(boolean resetp) throws SubscriberException {
      getBlackboardService().closeTransaction(resetp);
    }

    public boolean wasAwakened() { return PluginAdapter.this.wasAwakened(); }

    public void wake() {
      PluginAdapter.this.wake();
    }
    public Alarm wakeAt(long n) {
      return PluginAdapter.this.wakeAt(n);
    }
    public Alarm wakeAfter(long n) {
      return PluginAdapter.this.wakeAfter(n);
    }
    public Alarm wakeAtRealTime(long n) {
      return PluginAdapter.this.wakeAtRealTime(n);
    }
    public Alarm wakeAfterRealTime(long n) {
      return PluginAdapter.this.wakeAfterRealTime(n);
    }
    public long currentTimeMillis() {
      return getAlarmService().currentTimeMillis();
    }
    public Date getDate() {
      return new Date(currentTimeMillis());
    }

    public Subscription subscribe(UnaryPredicate isMember) {
      return getBlackboardService().subscribe(isMember);
    }
    public Subscription subscribe(UnaryPredicate isMember, Collection realCollection) {
      return getBlackboardService().subscribe(isMember, realCollection);
    }
    public Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
      return getBlackboardService().subscribe(isMember,isIncremental);
    }
    public Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental) {
      return getBlackboardService().subscribe(isMember, realCollection, isIncremental);
    }
    public void unsubscribe(Subscription collection) {
      getBlackboardService().unsubscribe(collection);
    }
    public Collection query(UnaryPredicate isMember) {
      return PluginAdapter.this.query(isMember);
    }

    public void publishAdd(Object o) {
      getBlackboardService().publishAdd(o);
    }
    public void publishRemove(Object o) {
      getBlackboardService().publishRemove(o);
    }
    public void publishChange(Object o) {
      getBlackboardService().publishChange(o, null);
    }
    public void publishChange(Object o, Collection changes) {
      getBlackboardService().publishChange(o, changes);
    }
    public Collection getParameters() {
      return parameters;
    }
    public boolean didRehydrate() {
      return getBlackboardService().didRehydrate();
    }
    public boolean didRehydrate(BlackboardService subscriber) {
      return subscriber.didRehydrate();
    }

    public boolean claim(Object o) {
      return PluginAdapter.this.claim(o);
    }
    public void unclaim(Object o) {
      PluginAdapter.this.unclaim(o);
    }
  }

  public boolean didRehydrate() {
    return getBlackboardService().didRehydrate();
  }

  public boolean didRehydrate(BlackboardService subscriber) {
    return subscriber.didRehydrate();
  }

  /** Attempt to stake a claim on a logplan object, essentially telling 
   * everyone else that you and only you will be disposing, modifying, etc.
   * it.
   * Calls Claimable.tryClaim if the object is Claimable.
   * @return true IFF success.
   **/
  protected boolean claim(Object o) {
    if (o instanceof Claimable) {
      return ((Claimable)o).tryClaim(getBlackboardService());
    } else {
      return false;
    }
  }
      
  /** Release an existing claim on a logplan object.  This is likely to
   * thow an exception if the object had not previously been (successfully) 
   * claimed by this plugin.
   **/
  protected void unclaim(Object o) {
    ((Claimable) o).resetClaim(getBlackboardService());
  }

  // 
  // threading model
  //

  /** called from PluginBinder **/
  public void plugin_prerun() {
    try {
      //start(); // just in case..  ACK! NO!
      BlackboardClient.current.set(this);
      prerun();
    } finally {
      BlackboardClient.current.set(null);
    }
  }

  /** override to define prerun behavior **/
  protected void prerun() { }

  /** called from PluginBinder **/
  public void plugin_cycle() {
    try {
      BlackboardClient.current.set(this);
      cycle();
    } finally {
      BlackboardClient.current.set(null);
    }
  }

  /** override to define cycle behavior **/
  protected void cycle() { }


  // 
  // compatability methods
  //
  
  /** alias for getBlackboardService **/
  protected BlackboardService getSubscriber() {
    return getBlackboardService();
  }

  public class PluginAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public PluginAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        getBlackboardService().signalClientActivity();
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }
    public String toString() {
      return "<PluginAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for "+PluginAdapter.this.toString()+">";
    }
  }


  //
  // threading model support
  // 

  private Threading threadingModel = null;
  
  protected final void setThreadingModel(Threading t) {
    threadingModel = t;
  }

  protected final Threading getThreadingModel() { 
    return threadingModel;
  }
  
  public final static int UNSPECIFIED_THREAD = -1;
  public final static int NO_THREAD = 0;
  public final static int SHARED_THREAD = 1;
  public final static int SINGLE_THREAD = 2;
  public final static int ONESHOT_THREAD = 3;

  private int threadingChoice = UNSPECIFIED_THREAD;

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   **/
  protected final void setThreadingChoice(int m) {
    if (threadingModel != null) 
      throw new IllegalArgumentException("Too late to select threading model for "+this);
    threadingChoice = m;
  }

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   **/
  protected final void chooseThreadingModel(int m) {
    setThreadingChoice(m);
  }

  /** @return the current choice of threading model.  **/
  protected final int getThreadingChoice() {
    return threadingChoice;
  }

  /** create a Threading model object as specified by the plugin.
   * The default implementation creates a Threading object
   * based on the value of threadingChoice.
   * The default choice is to use a SharedThreading model, which
   * shares thread of execution with others of the same sort in
   * the agent.
   * Most plugins can ignore this altogether.  Most that
   * want to select different behavior should
   * call chooseThreadingModel() in their constructer.
   * Plugins which implement their own threading model
   * will need to override createThreadingModel.
   * createThreadingModel is called late in PluginBinder.load(). 
   * if an extending plugin class wishes to examine or alter
   * the threading model object, it will be available only when 
   * PluginBinder.load() returns, which is usually called by
   * the extending plugin classes overriding load() method.
   * The constructed Threading object is initialized by
   * PluginBinder.start().
   **/
  protected Threading createThreadingModel() {
    Threading t;
    switch (getThreadingChoice()) {
    case NO_THREAD:
      t = new NoThreading();
      break;
    case SHARED_THREAD: 
      t = new SharedThreading();
      break;
    case SINGLE_THREAD:
      t = new SingleThreading();
      break;
    case ONESHOT_THREAD:
      t = new OneShotThreading();
      break;
    default:
      throw new RuntimeException("Invalid Threading model "+getThreadingChoice());
    }
    return t;
  }

  public void startThreadingModel() {
    try {
      threadingModel.initialize();
      threadingModel.load();
      threadingModel.start();
    } catch (RuntimeException e) {
      System.err.println("Caught exception during threadingModel initialization: "+e);
      e.printStackTrace();
    }
  }


  protected abstract class Threading implements GenericStateModel {
    public void initialize() {}
    /** the argument passed to load is a ClusterServesPlugin **/
    public void load() {}
    public void start() {}
    public void suspend() {}
    public void resume() {}
    public void stop() {}
    public void halt() {}
    public void unload() {}
    public int getModelState() { 
      return UNINITIALIZED; 
    }
    public String toString() {
      return getAgentIdentifier()+"/"+PluginAdapter.this;
    }
  }

  /** up to the class to implement what it needs **/
  protected class NoThreading extends Threading {
  }
    
  /** prerun only: cycle will never be called. **/
  protected class OneShotThreading extends Threading {
    public OneShotThreading() {}
    public void start() {
      plugin_prerun();
    }
  }

  /** 
   * Shares a Thread with other SharedThreading plugins in the same agent.
   * <p>
   * There are two callbacks:<ul>
   *   <li>the subscription watcher, to track blackboard activity</li>
   *   <li>the scheduler trigger, to request plugin_cycle()</li>
   * </ul><br>
   * The order is:<ol>
   *   <li>the blackboard calls "subscriptionWatcher.signalNotify(..)"</li>
   *   <li>the subscription watcher calls "schedTrigger.trigger()"</li>
   *   <li>the scheduler calls "this.trigger()"</li>
   *   <li>"this.trigger()" calls "plugin_cycle()"</li>
   * </li>.
   */
  protected class SharedThreading extends Threading {

    // callback for subscription activity
    private SubscriptionWatcher sw = null; 
    private TriggerModel tm;

    private boolean didPrerun = false;

    public void load() {
      sw = new ThinWatcher();
      Trigger piTrig = new PluginTrigger();
      tm = new SyncTriggerModelImpl(getSchedulerService(), piTrig);
      getBlackboardService().registerInterest(sw);
    }

    public void start() {
      if (!(didPrerun)) {
        didPrerun = true;
        plugin_prerun();
      }
      tm.start();
    }

    public void suspend() {
      tm.suspend();
    }

    public void resume() {
      tm.resume();
    }
    
    public void stop() {
      tm.stop();
    }

    public void unload() {
      tm.unload();
      getBlackboardService().unregisterInterest(sw);
      sw = null;
    }

    // implement Trigger

    private class PluginTrigger implements Trigger {
      // no need to "sync" when using "SyncTriggerModel"
      public void trigger() {
        // get wake() right
        setAwakened(sw.clearSignal()); 
        plugin_cycle();
      }
    }

    private class ThinWatcher extends SubscriptionWatcher {
      public void signalNotify(int event) {
        super.signalNotify(event);
        tm.trigger();
      }
      public String toString() {
        return "ThinWatcher("+PluginAdapter.this.toString()+")";
      }
    }

    public String toString() {
      return this.getClass().getName()+"("+PluginAdapter.this.toString()+")";
    }
  }

  /** has its own Thread **/
  protected class SingleThreading extends Threading implements Runnable {
    /** a reference to personal Thread which each Plugin runs in **/
    private Thread myThread = null;
    /** our subscription watcher **/
    private SubscriptionWatcher waker = null;
    private static final int STOPPED = 0;
    private static final int SUSPENDED = 1;
    private static final int RUNNING = 2;
    private int state = STOPPED;
    private boolean firstRun = true;
    
    public SingleThreading() {}

    private int priority = Thread.NORM_PRIORITY;

    /** plugins and subclasses may set the Thread priority to 
     * a value lower than standard.  Requests to raise the priority
     * are ignored as are all requests after start()
     * Note that the default priority is one level below the
     * usual java priority - that is one level below where the
     * infrastructure runs.
     **/
    public void setPriority(int newPriority) {
      if (newPriority<priority) {
        priority = newPriority;
      }
    }
    
    private boolean isYielding = true;

    /** If isYielding is true, the plugin will force a thread yield
     * after each call to cycle().  This is on by default since plugins
     * generally need reaction from infrastructure and other plugins
     * to progress.
     * This may be set at any time, even though the effect is only periodic.
     * Most plugins would want to (re)set this value at initialization.
     **/
    public void setIsYielding(boolean v) {
      isYielding = v;
    }

    public void load() {
      setWaker(getBlackboardService().registerInterest());
    }

    public void unload() {
      getBlackboardService().unregisterInterest(getWaker());
    }

    public synchronized void start() {
      if (state != STOPPED) throw new RuntimeException("Not stopped");
      state = RUNNING;
      firstRun = true;
      startThread();
    }

    public synchronized void suspend() {
      if (state != RUNNING) throw new RuntimeException("Not running");
      state = SUSPENDED;
      stopThread();
    }

    public synchronized void resume() {
      if (state != SUSPENDED) throw new RuntimeException("Not suspended");
      state = RUNNING;
      startThread();
    }

    public synchronized void stop() {
      if (state != SUSPENDED) throw new RuntimeException("Not suspended");
      state = RUNNING;
      startThread();
      suspend();
    }

    private void startThread() {
      myThread =
        new Thread(this, "Plugin/"+getAgentIdentifier()+"/"+PluginAdapter.this);
      myThread.setPriority(priority);
      myThread.start();
    }

    private void stopThread() {
      signalStateChange();
      try {
        myThread.join(60000);
      } catch (InterruptedException ie) {
      }
      myThread = null;
    }

    private void signalStateChange() {
      if (waker != null) {
        waker.signalNotify(waker.INTERNAL);
      }
    }

    public final void run() {
      if (firstRun) {
        plugin_prerun();                 // plugin first time through
        firstRun = false;
      }
      while (state == RUNNING) {
	boolean xwakep = waker.waitForSignal();
	setAwakened(xwakep);
        plugin_cycle();                // do work
        if (isYielding)
          Thread.yield();
      }
    }
    public void setWaker(SubscriptionWatcher sw) {
      waker = sw;
    }

    public SubscriptionWatcher getWaker() {
      return waker;
    }
  }
}
