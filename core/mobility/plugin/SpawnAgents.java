/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.mobility.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin adds agents to the local or remote nodes, as specified by
 * its plugin parameter list.
 * <p>
 * This is really just a souped-up version of the {@link
 * AddAgentExamplePlugin}.
 * <p>
 * Each plugin parameter is a "key=value" pair, where the default key
 * is "agent".  The keys are:<dl>
 *   <dt>loopback=true</dt><dd>
 *       If the target is the local node and this plugin is loaded in the
 *       node agent, then use the local node's "addAgent" method instead of
 *       a mobility blackboard relay.  This avoids the mobility domain if
 *       all agents are local.</dd>
 *   <dt>parallel</dt><dd>
 *       Spawn agents in parallel, instead of waiting for each add to
 *       complete before adding the next agent.</dd>
 *   <dt>agent</dt><dd>
 *       An add-agent command, see {@link #parseAgent}.  This can
 *       simply be a list of agent names, e.g.:<pre>
 *         &lt;argument&gt;Foo&lt;/argument&gt;
 *         &lt;argument&gt;Bar&lt;/argument&gt;
 *       </pre></dd>
 * </dl>
 */
public class SpawnAgents extends ComponentPlugin {

  // if an add is to the local node, and we're in the node-agent,
  // then use the local "addAgent" method instead of a relay.
  protected static final boolean DEFAULT_LOOPBACK = true;
  // spawn agents in parallel
  protected static final boolean DEFAULT_PARALLEL = false;

  protected String localNode;
  protected LoggingService log;
  protected UIDService uidService;

  // these services may not be available and are not always required,
  // so we'll only 
  private boolean setMobilityFactory = false;
  private MobilityFactory _mobilityFactory;
  private boolean setAgentContainer = false;
  private AgentContainer _agentContainer;
  private boolean setThreadService = false;
  private ThreadService _threadService;

  // only use if loopback is enabled:
  private final List completedLocalAdds = new ArrayList();

  protected IncrementalSubscription sub;
  protected UID pluginId;

  // only used if !parallel
  protected boolean addInProgress;

  protected boolean loopback = DEFAULT_LOOPBACK;
  protected boolean parallel = DEFAULT_PARALLEL;
  protected Map agents = Collections.EMPTY_MAP;

  public void setNodeIdentificationService(NodeIdentificationService nis) {
    localNode = (nis == null ? null : nis.getMessageAddress().getAddress());
  }
 
  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  protected AgentContainer getAgentContainer() {
    if (!setAgentContainer) {
      setAgentContainer = true;
      NodeControlService ncs = getServiceBroker().getService(
            this, NodeControlService.class, null);
      if (ncs != null) {
        _agentContainer = ncs.getRootContainer();
        getServiceBroker().releaseService(
            this, NodeControlService.class, ncs);
      }
    }
    return _agentContainer;
  }

  protected ThreadService getThreadService() {
    if (!setThreadService) {
      setThreadService = true;
      _threadService = getServiceBroker().getService(this, ThreadService.class, null);
    }
    return _threadService;
  }

  protected MobilityFactory getMobilityFactory() {
    if (!setMobilityFactory) {
      setMobilityFactory = true;
      DomainService ds = getServiceBroker().getService(this, DomainService.class, null);
      if (ds != null) {
        _mobilityFactory = (MobilityFactory) ds.getFactory("mobility");
        getServiceBroker().releaseService(
            this, DomainService.class, ds);
      }
    }
    return _mobilityFactory;
  }

  @Override
public void setupSubscriptions() {
    parseParameters(getParameters());

    // listen for responses to our add requests
    // 
    // if all the agents are local then we'll never publish a relay, but
    // we won't know that until we've processed the agents list
    pluginId = uidService.nextUID();
    sub = blackboard.subscribe(new AgentControlPredicate(pluginId));

    // TODO handle rehydration?

    if (parallel) {
      // request all our agent adds in parallel
      addAllAgents();
    } else {
      // add the first agent
      Object o = getNextAgent();
      if (o != null) {
        addInProgress = true;
        addAgent(o);
      }
    }
  }
  
  @Override
public void execute() {
    // check for completed local (loopback) adds
    if (loopback) {
      List l = takeCompletedLocalAdds();
      for (int i = 0, n = l.size(); i < n; i++) {
        CompletedLocalAdd cla = (CompletedLocalAdd) l.get(i);
        String agent = cla.getAgent();
        Throwable ex = cla.getException();

        completedAdd(agent, ex);
      }
    }

    // check for completed remote adds, which may include local adss if
    // loopback is disabled or unavailable
    for (Enumeration en = sub.getChangedList(); en.hasMoreElements(); ) {
      AgentControl ac = (AgentControl) en.nextElement();
      int status = ac.getStatusCode();
      if (status == AgentControl.NONE) {
        // keep waiting, not sure why we were notified
        continue;
      }
      AddTicket t = (AddTicket) ac.getAbstractTicket();
      String agent = t.getMobileAgent().getAddress();
      Throwable ex;
      if (status == AgentControl.CREATED) {
        ex = null;
      } else {
        ex = ac.getFailureStackTrace();
        if (ex == null) {
          ex = new RuntimeException("Unknown failure: "+ac);
        }
      }
      // cleanup completed relay
      blackboard.publishRemove(ac);

      completedAdd(agent, ex);
    }
  }

  protected void completedAdd(String agent, Throwable ex) {
    // report status
    if (ex == null) {
      if (log.isInfoEnabled()) {
        log.info("Added agent "+agent);
      }
    } else {
      if (log.isErrorEnabled()) {
        log.error("Unable to add agent "+agent, ex);
      }
    }

    if (parallel) {
      // we've already submitted all our adds, so we don't expect any more
      // agents.  However, a subclass may override that method in some
      // odd/clever way, so we'll check again anyways.
      addAllAgents();
      return;
    }

    // record completed add
    addInProgress = false;

    Object o = getNextAgent();
    if (o == null) {
      // no more agents to add
      return;
    }

    // add the next agent
    addInProgress = true;
    addAgent(o);
  }

  /** Submit all adds in parallel. */
  protected void addAllAgents() {
    while (true) {
      Object o = getNextAgent();
      if (o == null) break;
      addAgent(o);
    }
  }

  /**
   * Get the next add-agent command.
   * <p>
   * Note that a subclass can override this method for greater control
   * over the iteration order and values.
   * @see #parseAgents
   */
  protected Object getNextAgent() {
    if (agents.isEmpty()) {
      return null;
    }
    Iterator iter = agents.entrySet().iterator();
    Map.Entry me = (Map.Entry) iter.next();
    Object o = me.getValue();
    if (o == null) {
      o = me.getKey();
    }
    iter.remove();
    return o;
  }

  /** Request a non-blocking agent add */
  protected void addAgent(Object o) {
    // parse command
    String agent;
    String node = localNode;
    Object options = null;
    if (o instanceof String) {
      agent = (String) o;
    } else if (o instanceof AddCommand) {
      AddCommand ac = (AddCommand) o;
      agent = ac.getAgent();
      node = ac.getNode();
      options = ac.getOptions();
    } else {
      log.error("Invalid object type: "+o);
      return;
    }

    addAgent(agent, options, node);
  }

  /** Request a non-blocking agent add */
  protected void addAgent(String agent, Object options, String node) {
    if (loopback && localNode.equals(node)) {
      // attempt to add this local agent without using the mobility factory.
      // This should only return false if this plugin can't obtain the
      // agentContainer (e.g. we're not in the node agent).
      if (addLocalAgent(agent, options)) {
        return;
      }
    }

    // use the mobility factory to publish a blackboard add request
    addRemoteAgent(agent, options, node);
  }

  /** @return false if the caller must use #addRemoteAgent */
  protected boolean addLocalAgent(final String agent, final Object options) {
    // get the node's container
    final AgentContainer agentContainer = getAgentContainer();
    if (agentContainer == null) {
      // not in the node-agent, must use a mobility relay
      return false;
    }
    // get the thread service
    //
    // We need to spawn a thread, since we're in a blackboard transaction.
    // If we attempted to call "addLocalAgent" from our thread then we'd get
    // "nested transaction" exceptions.
    ThreadService threadService = getThreadService();
    if (threadService == null) {
      // unable to obtain thread service?  must use a mobility relay
      return false;
    }
    Runnable runner = new Runnable() {
      public void run() {
        // add the agent
        Throwable ex = null;
        try {
          // TODO support options
          agentContainer.addAgent(
              MessageAddress.getMessageAddress(agent));
        } catch (Throwable t) {
          ex = t;
        }

        // request an "execute()" cycle to process the response
        synchronized (completedLocalAdds) {
          completedLocalAdds.add(new CompletedLocalAdd(agent, ex));
        }
        blackboard.signalClientActivity();
      }
    };
    threadService.getThread(this, runner, "Add agent "+agent).start();

    // "add" is running in a background thread
    return true;
  }

  protected List takeCompletedLocalAdds() {
    List ret = Collections.EMPTY_LIST;
    synchronized (completedLocalAdds) {
      if (!completedLocalAdds.isEmpty()) {
        ret = new ArrayList(completedLocalAdds);
        completedLocalAdds.clear();
      }
    }
    return ret;
  }

  /** Add an agent using a mobility relay */
  protected void addRemoteAgent(String agent, Object options, String node) {
    // we need to use mobility relays
    MobilityFactory mobilityFactory = getMobilityFactory();
    if (mobilityFactory == null) {
      throw new RuntimeException("Missing \"mobility\" domain");
    }

    // create an add-ticket
    // TODO support options
    AddTicket addTicket = 
      new AddTicket(
          uidService.nextUID(),
          MessageAddress.getMessageAddress(agent),
          MessageAddress.getMessageAddress(node));

    // create the blackboard object
    AgentControl ac =
      mobilityFactory.createAgentControl(
          pluginId,
          MessageAddress.getMessageAddress(node),
          addTicket);

    if (log.isInfoEnabled()) {
      log.info(
          "Adding agent "+agent+" to "+
          (localNode.equals(node) ? "local" : "remote")+
          " node "+node);
    }

    // send it to the node
    blackboard.publishAdd(ac);
  }


  /**
   * Parse the plugin parameters.
   */
  protected void parseParameters(Collection parameters) {
    if (parameters.isEmpty()) {
      return;
    }
    // extract agent strings, set local options (e.g. loopback)
    List l = new ArrayList();
    for (Iterator iter = parameters.iterator(); iter.hasNext();) {
      String s = (String) iter.next();
      int sep = s.indexOf('=');
      String key;
      String value;
      if (sep < 0) {
        key = "agent";
        value = s;
      } else {
        key = s.substring(0, sep).trim();
        value = s.substring(sep+1).trim();
      }
      if ("agent".equals(key)) {
        l.add(value);
      } else {
        parseParameter(key, value);
      }
    }
    // parse agents map
    parseAgents(l);
  }

  /**
   * Parse a non-agent parameter.
   */
  protected void parseParameter(String key, String value) {
    if ("loopback".equals(key)) {
      loopback = "true".equals(value);
    } else if ("parallel".equals(key)) {
      parallel = "true".equals(value);
    } else {
      throw new IllegalArgumentException("Invalid parameter: "+key+"="+value);
    }
  }

  /**
   * A subclass can override this method to specify a dynamically generated
   * parameter list, e.g.:<pre>
   *   protected void parseAgents(List dummy) {
   *      List l = new ArrayList();
   *      if (localNode.equals("A")) {
   *        l.add("foo");
   *      } else {
   *        l.add("bar");
   *        l.add("qux");
   *      }
   *      super.parseAgents(l);
   *   }
   * </pre>
   * <p>
   * Also see {@link #getNextAgent}, which accesses the "agents" table
   * created by this method.
   */
  protected void parseAgents(List l) {
    agents = new LinkedHashMap();
    for (int i = 0; i < l.size(); i++) {
      Object o = parseAgent((String) l.get(i));
      String agent;
      if (o instanceof String) {
        agent = (String) o;
      } else if (o instanceof AddCommand) {
        agent = ((AddCommand) o).getAgent();
      } else if (o == null) {
        continue;
      } else {
        throw new IllegalArgumentException("Invalid agent["+i+"]: "+o);
      }
      agents.put(agent, o);
    }
    if (log.isDebugEnabled()) {
      log.debug("parsed agents["+agents.size()+"]: "+agents);
    }
  }

  /**
   * Parse an "agent=<i>STRING</i>" command specified in the plugin
   * parameters.
   * <p>
   * An agent string is either an agent name, a name followed by an "@" 
   * and a node name, or either of the prior options followed by an " if "
   * and a node name.  This allows the client to specific all of the
   * following simple commands:
   * <pre>
   *    &lt;!-- add agent A to the local node --&gt;
   *    &lt;argument&gt;A&lt;/argument&gt;
   *
   *    &lt;!-- add agent B to node X, which may be remote --&gt;
   *    &lt;argument&gt;B @ X&lt;/argument&gt;
   *
   *    &lt;!-- if this is node Y, then add agent C to the local node --&gt;
   *    &lt;argument&gt;C if Y&lt;/argument&gt;
   *
   *    &lt;!-- if this is node Z, then add agent D to node Q --&gt;
   *    &lt;argument&gt;D @ Q if Z&lt;/argument&gt;
   * </pre>
   *
   * @return an agent name, an AddCommand, or null
   */
  protected Object parseAgent(String s) {
    String value = s;
    int sep = value.indexOf(" if ");
    if (sep > 0) {
      String test = value.substring(sep+" if ".length()).trim();
      if (!localNode.equals(test)) {
        if (log.isDebugEnabled()) {
          log.debug("skipping "+value);
        }
        return null;
      }
      value = value.substring(0, sep).trim();
    }
    String agent = value;
    String node = localNode;
    sep = value.indexOf('@');
    if (sep >= 0) {
      agent = value.substring(0, sep).trim();
      node = value.substring(sep+1).trim();
    }
    if (node.equals(localNode) || node.length() == 0) {
      return agent;
    } else {
      return new AddCommand(agent, node);
    }
  }

  protected static class AddCommand {
    private final String agent;
    private final String node;
    private final Object options;
    public AddCommand(String agent, String node) {
      this(agent, node, null);
    }
    public AddCommand(String agent, String node, Object options) {
      this.agent = agent; this.node = node; this.options = options;
    }
    public String getAgent() { return agent; }
    public String getNode() { return node; }
    public Object getOptions() { return options; }
    @Override
   public String toString() {
      return agent+" @ "+node;
    }
  }

  protected static final class AgentControlPredicate implements UnaryPredicate {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final UID pluginId;
    public AgentControlPredicate(UID pluginId) {
      this.pluginId = pluginId;
    }
    public boolean execute(Object o) {
      return
        (o instanceof AgentControl &&
         pluginId.equals(((AgentControl) o).getOwnerUID()));
    }
  }

  protected static class CompletedLocalAdd {
    private final String agent;
    private final Throwable ex;
    public CompletedLocalAdd(String agent, Throwable ex) {
      this.agent = agent;
      this.ex = ex;
    }
    public String getAgent() { return agent; }
    public Throwable getException() { return ex; }
    @Override
   public String toString() {
      return "(completed-local-add agent="+agent+" success="+(ex == null)+")";
    }
  }
}
