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

package org.cougaar.core.blackboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.persist.BlackboardPersistence;
import org.cougaar.core.persist.Persistence;
import org.cougaar.core.persist.PersistenceObject;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DomainForBlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.community.CommunityChangeAdapter;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A {@link Subscriber} created by the {@link StandardBlackboard}
 * that maintains a view of all published objects, invokes
 * {@link org.cougaar.core.domain.Domain} {@link
 * org.cougaar.core.domain.LogicProvider}s, and monitors
 * community membership changes.
 *
 * @property org.cougaar.core.agent.savePriorPublisher
 * When set to <em>true</em>, will collect extra 
 * information on each publish to detect problems with multiple adds,
 * deletes, etc by complaining
 * about unexpected state changes.  This adds significant runtime overhead.
 * @property org.cougaar.core.agent.enablePublishException
 * When set to <em>true</em>, collects stack frames
 * for each published object in order to pinpoint both sides of
 * publish conflicts.  This is <em>extremely</em> 
 * expensive.
 * @property org.cougaar.core.persistence.enable
 * When set to <em>true</em> will enable blackboard persistence.
 * @property org.cougaar.core.blackboard.waitForNewCommChangeNotifications Time in 
 * milliseconds to wait for more community changes before asking the community 
 * service for them. Default is 1,000.
 */
public class Blackboard extends Subscriber
  implements
  BlackboardServesDomain,
  BlackboardClient,
  PrivilegedClaimant
{
  protected CollectionSubscription everything;
  protected MessageAddress self;
  private Distributor myDistributor;
  protected ServiceBroker myServiceBroker;
  protected AlarmService alarmService;
  protected DomainForBlackboardService myDomainService;
  protected ThreadService threadS;
  protected LoggingService logger;

  public static final String INSERTION_POINT = Agent.INSERTION_POINT + ".Blackboard";
  public MessageAddress getCID() { return self; }

  public static final boolean isSavePriorPublisher =
    SystemProperties.getBoolean("org.cougaar.core.agent.savePriorPublisher");
  public static final boolean enablePublishException =
    SystemProperties.getBoolean("org.cougaar.core.agent.enablePublishException");

  /** 
   * @property org.cougaar.core.blackboard.pedantic When true (the default) enables a variety
   * of extra checks for suspicious blackboard activity.  None of these checks are especially
   * expensive, so it is generally recommended that pedantic be left enabled.
   *
   */
  public static final boolean PEDANTIC = 
    SystemProperties.getBoolean("org.cougaar.core.blackboard.pedantic", true);


  /** the queue of messages to send */
  private List sendQueue = new ArrayList();

  // mark the envelopes which we emit so that we can detect them later.
  @Override
protected Envelope createEnvelope() {
    if (isTimestamped()) {
      return new TimestampedPlanEnvelopeImpl();
    } else {
      return new PlanEnvelopeImpl();
    }
  }

  /**
   * Marked Envelope <i>interface</i> so that we can detect envelopes which we've
   * emitted.
   *
   * This isn't an Envelope, since Envelope is a class, but this interface
   * will only be applied to the two Envelope subclasses listed below.
   */
  interface PlanEnvelope {
  }

  private static final class PlanEnvelopeImpl 
    extends Envelope implements PlanEnvelope {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
    }

  private static final class TimestampedPlanEnvelopeImpl
    extends TimestampedEnvelope implements PlanEnvelope {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;

      @Override
      public boolean isBlackboard() { return true; }
    }

  /** override to immediately publish deltas rather than delay until transaction close */
  @Override
protected EnvelopeTuple clientAddedObject(Object o) {
    EnvelopeTuple tup = super.clientAddedObject(o);
    consumeTuple(tup);
    return tup;
  }

  /** override to immediately publish deltas rather than delay until transaction close */
  @Override
protected EnvelopeTuple clientRemovedObject(Object o) {
    EnvelopeTuple tup = super.clientRemovedObject(o);
    consumeTuple(tup);
    return tup;
  }

  /** override to immediately publish deltas rather than delay until transaction close */
  @Override
protected EnvelopeTuple clientChangedObject(Object o, List changes) {
    EnvelopeTuple tup = super.clientChangedObject(o, changes);
    consumeTuple(tup);
    return tup;
  }

  /** invoked via client*Object while executing an LP */
  private final boolean consumeTuple(EnvelopeTuple tup) {
    boolean somethingFired = false;
    synchronized (subscriptions) {
      for (int i = 0, n = subscriptions.size(); i < n; i++) {
        Subscription subscription = (Subscription) subscriptions.get(i);
        somethingFired |= tup.applyToSubscription(subscription, true);
      }
    }
    // recurses
    callLogicProviders(tup, false);
    return somethingFired;
  }

  /** is the object non-null? */
  private static final UnaryPredicate anythingP = new AnythingPredicate();
  private static final class AnythingPredicate implements UnaryPredicate {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return (o != null);
    }
  }

  public Blackboard(
      MessageSwitchService msgSwitch, ServiceBroker sb, Object state) {
    myServiceBroker = sb;
    self = msgSwitch.getMessageAddress();
    myDistributor = createDistributor(msgSwitch, state);
    setClientDistributor(this, myDistributor);
    setName("<blackboard>");
    logger = sb.getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(logger, self+": ");
    myDomainService = sb.getService(this, DomainForBlackboardService.class, null);
    if (myDomainService == null) {
      RuntimeException re = 
        new RuntimeException("Couldn't get DomainForBlackboardService!");
      re.printStackTrace();
      throw re;
    }
    alarmService = sb.getService(this, AlarmService.class, null);
    threadS = sb.getService(this, ThreadService.class, null);
  }

  public void stop() {
    // FIXME: Stop the cacheClearer thread
    // This for bug 3704
    myDistributor = null;
  }

  private static class AllObjectsSet extends HashSet {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   Map stacks = createStackMap();
    protected Map createStackMap() {
      if (isSavePriorPublisher) {
        return new HashMap();
      } else {
        return null;              // Don't keep prior publishing info
      }
    }

    public AllObjectsSet(int size) {
      super(size);
    }
    @Override
   public boolean add(Object o) {
      boolean result = super.add(o);
      if (!result) {
        PublishStack priorStack = null;
        if (stacks != null) {
          priorStack = (PublishStack) stacks.get(o);
        }
        throw new PublishException(
            "Blackboard.everything.add object already published: " +
            o.toString(),
            priorStack,
            stacks != null);
      } else if (stacks != null) {
        stacks.put(o, new PublishStack("Prior publisher: "));
      }
      return result;
    }
    @Override
   public boolean remove(Object o) {
      boolean result = super.remove(o);
      if (!result) {
        PublishStack priorStack = null;
        if (stacks != null) {
          priorStack = (PublishStack) stacks.get(o);
        }
        throw new PublishException(
            "Blackboard.everything.remove object not published: " + o.toString(),
                                   priorStack, stacks != null);
      } else if (stacks != null) {
        stacks.put(o, new PublishStack("Prior remover: "));
      }
      return result;
    }
  }

  public final void init() {
    everything =
      new CollectionSubscription(anythingP,
                                 enablePublishException
                                 ? new AllObjectsSet(111)
                                 : new HashSet(111));
    subscribe(everything);
    setReadyToPersist();
  }

  // Subscription Client interface
  public String getBlackboardClientName() {
    return getClass().getName();
  }

  /**
   * Provide a new subscription with its initial fill. Called under
   * the protection of the distributor lock so we are guaranteed that
   * the allPlanObjects won't change.
   */
  public void fillSubscription(Subscription subscription) {
    if (subscription == everything) {
      return; // Don't fill ourselves
    }
    Envelope envelope = createQueryEnvelope(subscription);
    envelope.bulkAddObject(everything.getCollection());
    subscription.fill(envelope);
  }

  public void fillQuery(Subscription subscription) {
    Envelope envelope = createQueryEnvelope(subscription);
    envelope.bulkAddObject(everything.getCollection());
    subscription.fill(envelope);
  }

  private Envelope createQueryEnvelope(Subscription subscription) {
    if (isTimestamped()) {
      TimestampedEnvelope te = new TimestampedEnvelope();
      Subscriber subscriber = subscription.getSubscriber();
      if (subscriber != null) {
        te.setName(subscriber.getName());
      }
      long nowTime = System.currentTimeMillis();
      te.setTransactionOpenTime(nowTime);
      // should we wait until after the query to set the close time?
      te.setTransactionCloseTime(nowTime);
      return te;
    } else {
      return new Envelope();
    }
  }

  /** Alias for sendDirective(aDirective, null); */
  public void sendDirective(Directive aDirective) {
    if (aDirective == null) {
      throw new IllegalArgumentException("directive must not be null.");
    } else {
      sendQueue.add(aDirective);
    }
  }

  /**
   * Submit a directive with attached ChangeReports for transmission 
   * from this agent. We fill in the ContentsId with the next available number.
   */
  public void sendDirective(Directive aDirective, Collection c) {
    if (aDirective == null) {
      throw new IllegalArgumentException("directive must not be null.");
    } else {
      if (c != null && c.size()>0) {
        DirectiveMessage.DirectiveWithChangeReports dd =
          new DirectiveMessage.DirectiveWithChangeReports(aDirective,c);
        aDirective = dd;
      }
      sendQueue.add(aDirective);
    }
  }

  public long currentTimeMillis() {
    return alarmService.currentTimeMillis();
  }

  /**
   * Add Object to the Blackboard Collection
   */
  public void add(Object o) {
    publishAdd(o);
  }

  /** Removed Object to the Blackboard Collection */
  public void remove(Object o) {
    publishRemove(o);
  }

  /** Change Object to the Blackboard Collection */
  public void change(Object o) {
    publishChange(o,null);
  }
  public void change(Object o, Collection changes) {
    publishChange(o, changes);
  }

  public Enumeration searchBlackboard(UnaryPredicate predicate) {
    Vector vec = new Vector();

    for (Iterator i = everything.getCollection().iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (predicate.execute(o)) {
        vec.addElement(o);
      }
    }
    return vec.elements();
  }

  public int countBlackboard(Class cl) {
    // could optimize by maintaining an LRU table
    int c = 0;
    for (Iterator i = everything.getCollection().iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (o != null && cl.isAssignableFrom(o.getClass())) {
        c++;
      }
    }
    return c;
  }

  public int countBlackboard(UnaryPredicate predicate) {
    int c = 0;
    for (Iterator i = everything.getCollection().iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (predicate.execute(o)) {
        c++;
      }
    }
    return c;
  }

  public int getBlackboardSize() {
    return everything.size();
  }

  /**
   * Process incoming directive messages. All messages have been
   * blessed by the message manager. The messages are implicitly
   * acknowledged by this method. The envelope of published events
   * resulting from handling the messages is returned.
   */
  public final Envelope receiveMessages(List msgs) {
    //try {
    //  startTransaction();
    for (Iterator iter = msgs.iterator(); iter.hasNext(); ) {
      DirectiveMessage msg = (DirectiveMessage) iter.next();
      applyMessageAgainstLogicProviders(msg);
    }

    checkUnpostedChangeReports();
    // There really should not be any change tracking subscriptions, at
    // least not in the base classes!!!  MT
    resetSubscriptionChanges(); // clear change tracking subscriptions
    
    return privateGetPublishedChanges();
    //} finally {
    //  stopTransaction();
    //}
  }

  private final List oneEnvelope = new ArrayList(1);

  /**
   * called by distributor to prepare for "receiveEnvelope(..)" calls.
   */
  public final void prepareForEnvelopes() {
    setTransactionOpenTime();
  }

  /** 
   * Called by transaction close within the thread of Plugins.  
   * Also called at the end of an LP pseudo-transaction, but
   * most of the logic here is disabled in that case.
   */
  public final Envelope receiveEnvelope(Envelope envelope) {
    oneEnvelope.add(envelope);
    super.receiveEnvelopes(oneEnvelope, false); // Move to our inbox
    oneEnvelope.clear();

    if (! (envelope instanceof PlanEnvelope)) {
      // although we aways consume envelopes, we only act on them
      // when we didn't generate 'em
      privateUpdateSubscriptions();

      try {
        boolean isPersistenceEnvelope = envelope instanceof PersistenceEnvelope;
        List tuples = envelope.getRawDeltas();
        int l = tuples.size();
        for (int i = 0; i<l; i++) {
          try {
            callLogicProviders((EnvelopeTuple) tuples.get(i), isPersistenceEnvelope);
          } catch (Exception e) {
            System.err.println("Caught " + e + " while running logic providers.");
            e.printStackTrace();
          }
        }
      } finally {
        // clear subscriptions deltas, just in case.
        resetSubscriptionChanges();
      }
    }

    return privateGetPublishedChanges();
  }

  private static class DestinationKey {
    public MessageAddress cid;
    public MessageAttributes attrs;
    private int hc;
    public DestinationKey(MessageAddress cid, MessageAttributes attrs) {
      this.cid = cid;
      this.attrs = attrs;
      hc = cid.hashCode() + attrs.hashCode();
    }
    @Override
   public int hashCode() {
      return hc;
    }
    @Override
   public boolean equals(Object o) {
      if (o instanceof DestinationKey) {
        DestinationKey that = (DestinationKey) o;
        return this.cid.equals(that.cid) && this.attrs.equals(that.attrs);
      }
      return false;
    }
  }

  private MessageAddress getDirectiveDestinationOfKey(Object key) {
    if (key instanceof MessageAddress) {
      return (MessageAddress) key;
    } else {
      DestinationKey dkey = (DestinationKey) key;
      return dkey.cid;
    }
  }

  private Object getDirectiveKeyOfDestination(MessageAddress dest) {
    MessageAttributes attrs = dest.getMessageAttributes();
    if (attrs == null) return dest;
    return new DestinationKey(dest, attrs);
  }

  /*
   * Builds up hashmap of arrays of directives for each agent, <code>MessageAddress</code>.
   * Modified to handle destinations of <code>AttributeBasedAddress</code>es, so that these are 
   * sent properly as well. 
   */
  public void appendMessagesToSend(List messages) {
    HashMap directivesByDestination = new HashMap(89);

    // FIXME - prefill cache of aba roles to addresses here, instead of building up a cache
    // fillCache();
    
    for (Iterator iter = sendQueue.iterator(); iter.hasNext(); ) {
      Directive dir = (Directive) iter.next();  
      MessageAddress dest = dir.getDestination();
      
      // get all destinations
      
      /**
       * If dest is an ABA, get all agent_names from cache or 
       * nameserver and fills in the hashmap of directives
       * Short and easy way to handle ABA destinations
       */
      ArrayList dirs;

      if (dest instanceof AttributeBasedAddress) {
        //System.out.println("-------BLACKBOARD ENCOUNTERED ABA-----");
        MessageAttributes qosAttributes = dest.getMessageAttributes();
	Collection agents = getABAAddresses((AttributeBasedAddress) dest);   // List of CIs
	// for all destinations, add a new directive array and insert a new directive, or add to 
	// an existing array in the destinations hashmap
	for (Iterator i = agents.iterator(); i.hasNext(); ) {
          MessageAddress agentAddress = (MessageAddress) i.next();
          if (qosAttributes != null) {
            agentAddress =
              MessageAddress.getMessageAddress(
                  agentAddress, qosAttributes);
          }
          Object key = getDirectiveKeyOfDestination(agentAddress);
	  dirs = (ArrayList)directivesByDestination.get(key);
	  if (dirs == null) {
	    dirs = new ArrayList(1);
	    directivesByDestination.put(key, dirs); 
	  }
	  dirs.add(dir);
	}
      } // done with aba handling
      
      /**
       * dest is regular address so proceed as before 
       */
      else {
        Object key = getDirectiveKeyOfDestination(dest);
	dirs = (ArrayList) directivesByDestination.get(key);
	if (dirs == null) {
	  dirs = new ArrayList(1);
	  directivesByDestination.put(key, dirs);
	}
	dirs.add(dir);	
      }
    }
    /**
     * By now directivesByDestination only has ArrayLists of MessageAddresss,
     * so we can set their directives as before. 
     */
    for (Iterator iter = directivesByDestination.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry entry = (Map.Entry) iter.next();
      MessageAddress tmpci = getDirectiveDestinationOfKey(entry.getKey());
      ArrayList dirs = (ArrayList) entry.getValue();
      int size = dirs.size();
      if (size > 0) {
        Directive[] directives = (Directive[]) dirs.toArray(new Directive[size]);
        DirectiveMessage ndm = new DirectiveMessage(directives);
	ndm.setDestination(tmpci);
        ndm.setSource(self);
        messages.add(ndm);
        dirs.clear();
      }
    }
    directivesByDestination.clear(); // maybe help gc a bit.
    sendQueue.clear();
  }

  public void restart(MessageAddress cid) {
    myDomainService.invokeRestartLogicProviders(cid);
  }

  private void applyMessageAgainstLogicProviders(DirectiveMessage m) {
    myDomainService.invokeMessageLogicProviders(m);
  }

  /**
   * called by receiveEnvelope (on behalf of a plugin) and
   * consumeTuple (on behalf of an LP).
   */
  private void callLogicProviders(EnvelopeTuple obj, boolean isPersistenceEnvelope) {
    if (!isPersistenceEnvelope) {
      handleActiveSubscriptionObjects(obj);
    }
    myDomainService.invokeEnvelopeLogicProviders(obj, isPersistenceEnvelope);
  }

  private void handleActiveSubscriptionObjects(EnvelopeTuple tup) {
    if (ActiveSubscriptionObject.deferCommit) {
      Object o = tup.getObject();
      if (o instanceof ActiveSubscriptionObject) {
        ActiveSubscriptionObject aso = (ActiveSubscriptionObject) o;
        try {
          if (tup.isAdd()) {
            aso.addingToBlackboard(this, true);
          } else if (tup.isChange()) {
            aso.changingInBlackboard(this, true);
          } else if (tup.isRemove()) {
            aso.removingFromBlackboard(this, true);
          } // else ignore: bulk and event are uneffected by ASOs
        } catch (BlackboardException be) {
          logger.error(
              "Deferred ActiveSubscriptionObject action could"+
             " not be vetoed", be);
        }
      }
    }
  }
  
  /**
   * If {@link ActiveSubscrptionObject#deferCommit} is enabled, this
   * class tracks objects that would be modified by an {@link
   * ActiveSubscriptionObject} at publish time and logs a warning
   * if they are accessed before the transaction close time.
   * <p>
   * For details, see bug 3663.
   */
  private final static ObjectTracker tracker = new ObjectTracker();
  public static final ObjectTracker getTracker() { return tracker; }
  public static class ObjectTracker {
    private final static Logger log = Logging.getLogger(ObjectTracker.class);
    private static final Set globalSet = new HashSet(11);
    private final ThreadLocal localSet = new ThreadLocal() {
        @Override
      protected synchronized Object initialValue() { return new HashSet(11); }
      };
    
    public Set getLocalSet() {
      return (Set) (localSet.get());
    }

    public void checkpoint(boolean commit, Object ob, Object a) {
      if (ActiveSubscriptionObject.deferCommit) {
        // short circuit if we aren't actually tracking ASO gaps
        if (commit) {
          resolve(ob, a);
        } else {
          track(ob, a);
        }
      }
    }
    private void track(Object ob, Object a) {
      Object o = new Traversal(ob,a);
      if (log.isDebugEnabled()) log.debug("Tracking "+o);
      synchronized (globalSet) {
        globalSet.add(o);
      }
      getLocalSet().add(o);
    }

    private void resolve(Object ob, Object a) {
      Object o = new Traversal(ob,a);
      if (log.isDebugEnabled()) log.debug("Resolving "+o);
      synchronized (globalSet) {
        globalSet.remove(o);
      }
      getLocalSet().remove(o);
    }

    public void clearLocalSet() {
      if (ActiveSubscriptionObject.deferCommit) {
        // short circuit if we aren't actually tracking ASO gaps
        getLocalSet().clear();
      }
    }

    public void checkAccess(Object ob, Object a) {
      if (ActiveSubscriptionObject.deferCommit) {
        // short circuit if we aren't actually tracking ASO gaps
        Object o = new Traversal(ob,a);
        if (log.isDebugEnabled()) log.debug("Checking "+o);
        boolean locP = getLocalSet().contains(o);
        if (locP) {
          log.warn(
              "Local access of uncommitted ActiveSubscriptionObject"+
             " data "+o, new Throwable());
        } else {
          if (log.isDebugEnabled()) {
            boolean gloP;
            synchronized (globalSet) {
              gloP = globalSet.contains(o);
            }
            if (gloP) {
              log.debug(
                  "Global access of uncommitted ActiveSubscriptionObject"+
                 " data "+o, new Throwable());
            }
          }
        }
      }
    }
  }

  private static class Traversal {
    private final Object o;
    private final Object a;
    public Traversal(Object o, Object a) { this.o=o; this.a=a;}
    @Override
   public boolean equals(Object thing) {
      if (thing instanceof Traversal) {
        return o.equals(((Traversal)thing).o) && a.equals(((Traversal)thing).a);
      } else {
        return false;
      }
    }
    @Override
   public int hashCode() { return o.hashCode(); } /*don't bother to spread 'em out*/
    @Override
   public String toString() { return "Traversal("+a+") "+o; }
  }

  public PublishHistory getHistory() {
    return myDistributor.history;
  }

  protected Envelope executeDelayedLPActions() {
    myDomainService.invokeDelayedLPActions();
    return privateGetPublishedChanges();
  }

  public PersistenceObject getPersistenceObject() {
    return myDistributor.getPersistenceObject();
  }

  /** Ensure that all the domains know that this is THE blackboard */
  protected void connectDomains() {
    myDomainService.setBlackboard(this);
    setReadyToPersist();
  }
  

  //
  // Distributor
  //
  private Distributor createDistributor(
      MessageSwitchService msgSwitch,
      Object state) {
    Distributor d = new Distributor(this, myServiceBroker, self.getAddress());
    Persistence persistence = createPersistence();
    boolean lazyPersistence = 
      SystemProperties.getBoolean("org.cougaar.core.persistence.lazy", true);
    d.setPersistence(persistence, lazyPersistence);
    d.start(msgSwitch, state);       // msgSwitch, state

    return d;
  }

  public Distributor getDistributor() {
    return myDistributor;
  }

  protected Persistence createPersistence() {
     return BlackboardPersistence.find(myServiceBroker);
  }
 
  
  // -------- Methods for ABA Handling Below --------  needs work //
  
  // AttributeBasedAddress to ABATranslation cache
  private Map cache = new HashMap(89);

  private CacheClearer cacheClearer = new CacheClearer();
  private Object cacheClearerLock = new Object();

  private static class ABATranslationImpl implements ABATranslation {
    Collection old, current;
    ABATranslationImpl(Collection current) {
      this.current = current;
    }
    public Collection getOldTranslation() {
      return old;
    }
    public Collection getCurrentTranslation() {
      return current;
    }
    void setCurrentTranslation(Collection newCurrentTranslation) {
      current = newCurrentTranslation;
    }
    void setOldTranslation(Collection newOldTranslation) {
      old = newOldTranslation;
    }
    boolean isEmpty() {
      return old == null && current == null;
    }
  }

  private class MyCommunityChangeListener extends CommunityChangeAdapter {
    @Override
   public void communityChanged(CommunityChangeEvent e) {
      if (logger.isDebugEnabled()) logger.debug(e.toString());
      clearCache(e.getCommunityName());
    }
  }

  /*
   * Loops through the cache of ABAs and returns MessageAddresss, 
   * else it querries the nameserver for all agents with the ABA's role attribute, 
   * and builds the cache.
   * @return list (copy) of the addresses of the agents matching the ABA
   */
  public Collection getABAAddresses(AttributeBasedAddress aba) {
    // first look in cache
    Collection matches = null;
    synchronized (cache) {
      ABATranslation abaTranslation = (ABATranslation) cache.get(aba);
      if (abaTranslation != null){
        matches = abaTranslation.getCurrentTranslation();
      }
    }
    if (matches == null) {
      // Not in cache. Get it the hard way from community service
      matches = lookupABA(aba);
      if (logger.isDebugEnabled()) {
        logger.debug("lookupABA: " + aba + "->" + matches);
      }
      matches=Collections.unmodifiableCollection(matches);
      synchronized (cache) {
        ABATranslationImpl abaTranslation = (ABATranslationImpl) cache.get(aba);
        if (abaTranslation == null) {
          abaTranslation = new ABATranslationImpl(matches);
          cache.put(aba, abaTranslation);
        } else {
          abaTranslation.setCurrentTranslation(matches);
        }
      }
    }
    return matches;             // matches is unmodifiable - no need to copy it
  }

  // get the CommunityService when possible 
  private CommunityService _myCommunityService = null;

  private CommunityService getCommunityService() {
    if (_myCommunityService != null) {
      return _myCommunityService;
    } else {
      _myCommunityService = myServiceBroker.getService(this, CommunityService.class, null);
      if (_myCommunityService == null) {
        logger.warn(
            "Warning: Blackboard had no CommunityService -"+
            " will fall back to dynamic service lookup."+
            "  Risk of Deadlock!", new Throwable());
      }
      _myCommunityService.addListener(new MyCommunityChangeListener());
      return _myCommunityService;
    }
  }

  /*
   * Queries NameServer and gets a collection MessageAddresss of
   * all agents having the attribute type and value specified by the
   * ABA and stores the collection in the ABA cache. Returns a List
   * (copy) of the addresses found.
   * @return list (copy) of the addresses of the agents matching the ABA
   */
  private Collection lookupABA(AttributeBasedAddress aba) {
    CommunityService cs = getCommunityService();
    String communitySpec = getCommunitySpec(aba);
    String roleValue = aba.getAttributeValue();
    String roleName = aba.getAttributeType();
    String filter = "(" + roleName + "=" + roleValue + ")";
    
    if (cs == null) {
      return Collections.EMPTY_SET;
    }
    Collection matches = cs.search(communitySpec, filter);
    // MIK - do we really need to copy this?.  No, but we're also filtering for MAs
    List cis = new ArrayList(matches.size()); 
    for (Iterator i = matches.iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (o instanceof MessageAddress) {
        cis.add(o);
      }
    }
    return cis;
  }

  private void clearCache(String communityName) {
    synchronized (cacheClearerLock) {
      if (cacheClearer == null) {
        cacheClearer = new CacheClearer();
      }
    }
    cacheClearer.add(communityName);
  }

  private class CacheClearer implements Runnable {
    private Set changedCommunities = new HashSet();
    private Schedulable thread;

    // The delay time spent waiting for additional community
    // change notifications to arrive before processing
    // all such notifications. A trade-off between
    // time for ABA translations to be updated
    // and time for the local Comm Service to update
    // its cache from the NameService
    // This is in milliseconds
    private long waitForNewCommChangeNotifications =
      SystemProperties.getLong(
          "org.cougaar.core.blackboard.waitForNewCommChangeNotifications",
          1000L);
    
    private void reschedule() {
      if (thread != null) {
        thread.schedule(waitForNewCommChangeNotifications);
      }
    }

    public synchronized void add(String communityName) {
      changedCommunities.add(communityName);
      if (thread == null) {
        thread = threadS.getThread(this, this, "ABA Cache Clearer");
      }
      reschedule();
    }

    public void run() {
      Set changes = new HashSet(11);
      synchronized (this) {
        if (changedCommunities.size() == 0) {
          // exit without rescheduling
          return;
        } else {
          changes.addAll(changedCommunities);
          changedCommunities.clear();
        } // end of synch block
      }
	
      if (myDistributor == null) {
        // Blackboard was stopped?
        if (logger != null && logger.isInfoEnabled())
          logger.info(
                      "ABA Cache clearer dropping received changes cause"+
                      " Distributor is null -- assuming Blackboard is stopping");
        thread = null;
        return;
      }
      
      // Process the community changes
      myDistributor.invokeABAChangeLPs(changes);
      changes.clear();
      reschedule();             // take another pass in a bit in case something else comes in
    }
  }

  /**
   * Tell all the ABA interested LPs about the new 
   * community memberships, using the local cache of ABA translations.
   */
  public void invokeABAChangeLPs(Set communities) {
    synchronized (cache) {
      for (Iterator i = cache.values().iterator(); i.hasNext(); ) {
        ABATranslationImpl at = (ABATranslationImpl) i.next();
        at.setOldTranslation(at.getCurrentTranslation());
        at.setCurrentTranslation(null); // Filled in when needed
      }
      myDomainService.invokeABAChangeLogicProviders(communities);
      for (Iterator i = cache.values().iterator(); i.hasNext(); ) {
        ABATranslationImpl at = (ABATranslationImpl) i.next();
        at.setOldTranslation(null);
        if (at.isEmpty()) i.remove();
      }
    }
  }

  public ABATranslation getABATranslation(AttributeBasedAddress aba) {
    synchronized (cache) {
      ABATranslationImpl ret = (ABATranslationImpl) cache.get(aba);
      if (ret == null) return null;
      if (ret.getOldTranslation() == null) return null;
      if (ret.getCurrentTranslation() == null) {
        ret.setCurrentTranslation(lookupABA(aba));
      }
      return ret;
    }
  }

  // Stub - should be replaced when we figure out semantics for
  // community name spec in the aba.d
  protected String getCommunitySpec(AttributeBasedAddress aba) {
    String abaComm = aba.getCommunityName();

    if ((abaComm == null) ||
        (abaComm.equals("")) ||
        (abaComm.equals("*"))) {
      return "";
    } else {
      return abaComm;
    }
  }
}
