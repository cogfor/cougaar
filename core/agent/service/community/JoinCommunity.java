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

package org.cougaar.core.agent.service.community;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.util.Arguments;

/**
 * This component makes it easy for an agent to join one or more
 * communities at startup and leave those communities on shutdown.
 * <p>
 * Usage is:<pre>
 *   &lt;component 
 *     class="org.cougaar.core.agent.service.community.JoinCommunity"&gt;
 *     &lt;argument name="community" value="X, Y, X"&gt;
 *   &lt;/componnent&gt;
 * </pre>
 */
public class JoinCommunity extends ComponentSupport {

  private Arguments args = Arguments.EMPTY_INSTANCE;
  private List communities = Collections.EMPTY_LIST;

  private LoggingService log;
  private String localAgent;
  private CommunityService cs;

  public void setParameter(Object o) {
    args = new Arguments(o);
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }
  public void setAgentIdentificationService(AgentIdentificationService ais) {
    MessageAddress addr = (ais == null ? null : ais.getMessageAddress());
    this.localAgent = (addr == null ? null : addr.getAddress());
  }
  public void setCommunityService(CommunityService cs) {
    this.cs = cs;
  }

  @Override
public void load() {
    super.load();

    // parse our comma-separated "community" argument, e.g.
    //   <argument name="community" value="a, b"/>
    //
    // TODO use
    //   getParameterValues("community")
    // which is line-separated, e.g.
    //   <argument name="community" value="a"/>
    //   <argument name="community" value="b"/>
    // this would require the update of our Arguments class to match
    //   org.cougaar.core.qos.metrics.ParameterizedPlugin
    String s = args.getString("community");
    if (s == null) return;
    String[] sa = s.split("\\s*,\\s*");
    communities = new ArrayList(sa.length);
    for (int i = 0; i < sa.length; i++) {
      String si = sa[i].trim();
      if (si.length() <= 0) continue;
      if (communities.contains(si)) continue;
      communities.add(si);
    }
  }

  @Override
public void start() {
    super.start();

    joinAll(communities);
  }

  @Override
public void stop() {
    super.stop();

    // it's not clear if we want to remove our entries if we're moving.
    //
    // The downside is that it'll look like a leave-then-join, which might
    // cause unnecessary ABA relaying due to the (false) transient changes
    // to the membership lists.
    //
    // However, if we keep our entries, then they'll never be removed from the
    // membership list, even if our moved agent dies or is cleanly unloaded.
    //
    // So, we'll do the safe thing and remove our entries.
    //
    // Odds are that the caching built into the community service will hide
    // the fact that we're doing this, so this issue is arguably moot.
    leaveAll(communities);
  }

  private void joinAll(List l) {
    for (int i = 0, n = l.size(); i < n; i++) {
      String ci = (String) l.get(i);
      join(ci);
    }
  }

  private void join(String community) {
    if (log.isInfoEnabled()) {
      log.info("Joining community "+community);
    }
    cs.joinCommunity(
        community,
        localAgent,
        CommunityService.AGENT,
        null,
        true,
        null,
        new ListenerImpl("Join", community));
  }

  private void leaveAll(List l) {
    for (int i = 0, n = l.size(); i < n; i++) {
      String ci = (String) l.get(i);
      leave(ci);
    }
  }

  private void leave(String community) {
    if (log.isInfoEnabled()) {
      log.info("Leaving"+community);
    }
    cs.leaveCommunity(
        community,
        localAgent,
        new ListenerImpl("Leave", community));
  }

  private class ListenerImpl implements CommunityResponseListener {
    private final String action;
    private final String community;
    public ListenerImpl(String action, String community) {
      this.action = action;
      this.community = community;
    }
    public void getResponse(CommunityResponse res) {
      if (log.isInfoEnabled()) {
        String s = (res == null ? null : res.getStatusAsString());
        log.info(action+" response for community "+community+" is "+s);
      }
    }
  }
}
