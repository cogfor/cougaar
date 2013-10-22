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

package org.cougaar.core.relay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.blackboard.ABATranslation;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.ABAChangeLogicProvider;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;
import org.cougaar.core.domain.RestartLogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

/**
 * A {@link LogicProvider} to transmit and update {@link Relay}
 * objects.
 *
 * @see Relay
 */
public class RelayLP
implements EnvelopeLogicProvider, MessageLogicProvider, RestartLogicProvider, ABAChangeLogicProvider
{
  private final RootPlan rootplan;
  private final MessageAddress self;
  private final Relay.Token token;

  private final Logger logger = LoggerFactory.getInstance().createLogger(getClass());

  public RelayLP(
      RootPlan rootplan, 
      MessageAddress self) {
    this.rootplan = rootplan;
    this.self = self;
    token = TokenImpl.getToken(self);
  }

  public void init() {
  }

  // EnvelopeLogicProvider implementation
  /**
   * Sends the Content of Relay sources to the their targets and sends
   * target responses back to the source.
   * @param o an EnvelopeTuple where the tuple.object is
   *    a Relay.Source or Relay.Target
   */
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (obj instanceof Relay) { // Quick test for Target or Source
      if (changes != null && changes.contains(MarkerReport.INSTANCE)) {
	// Ignore changes containing our MarkerReport
	// This avoids looping
        return;                 
      }
      if (obj instanceof Relay.Target) {
        Relay.Target rt = (Relay.Target) obj;
	// Only changes are significant at a Target
	// The target is sending a response back to the source
        if (o.isChange()) {
          localResponse(rt, changes);
        }
      }

      // Note no else -- so something both a Target and a Relay
      // will run through all of these

      if (obj instanceof Relay.Source) {
        Relay.Source rs = (Relay.Source) obj;
        if (o.isAdd()) {
	  // New relay to be sent to targets.
	  // Note that a Relay.Target just published at Dest that's also
	  // a Relay.Source would get in here -- so must have the MarkerReport
          localAdd(rs);
        } else if (o.isChange()) {
	  // New relay content or targets list
	  // Note that a Relay.Target just changed at Dest that's also
	  // a Relay.Source would get in here -- so must have the MarkerReport
          localChange(rs, changes);
        } else if (o.isRemove()) {
	  // Remove the relay from the Targets
	  // Note that a Relay.Target just changed at Dest that's also
	  // a Relay.Source would get in here -- so must have the MarkerReport
          localRemove(rs);
        }
      }
    }
  }

  // New Relay.Source added. Only called from LP.execute()
  private void localAdd(Relay.Source rs) {
    Set targets = rs.getTargets();
    if (targets == null) return;
    if (targets.isEmpty()) return; // No targets
    localAdd(rs, targets);
  }

  // Propogate the new Relay to each listed target
  // Called from above localAdd and from abaChange when an aba expands
  // to new targets.
  private void localAdd(Relay.Source rs, Set targets) {
    // If this were also a target, we could check that this agent
    // is the source. That might help break looping
    boolean gotContent = false;
    Object content = null;
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.getPrimary().equals(self)) {
        // Never send to self.  Likely an error.
      } else {
        if (!gotContent) {
          gotContent = true;
          content = rs.getContent();
        }
        sendAdd(rs, target, content);
      }
    }
  }

  /**
   * Handle a change to this source. We need to send the new content
   * to the targets.
   */
  private void localChange(Relay.Source rs, Collection changes) {
    // called from changeTarget, receiveResponse, and LP.execute
    Set targets = rs.getTargets();
    Collection oldTargets = null;
    // Get the oldtargets mentioned in the _first_ RelayChangeReport 
    // (if there are many, later ones are ignored)
    if (changes != null) {
      for (Iterator i = changes.iterator(); i.hasNext(); ) {
        Object o = i.next();
        if (o instanceof RelayChangeReport) {
          if (oldTargets == null) {
            RelayChangeReport rcr = (RelayChangeReport) o;
            oldTargets = rcr.getOldTargets();
          }
          i.remove();
        }
      }
    }

    // If we got targets from a ChangeReport above, winnow that
    // down to targets no longer in the targets list.
    // Tell each such agent to remove this Relay
    if (oldTargets != null) {
      if (targets != null) oldTargets.removeAll(targets);
      UID uid = rs.getUID();
      for (Iterator i = oldTargets.iterator(); i.hasNext(); ) {
        MessageAddress target = (MessageAddress) i.next();
        sendRemove(uid, target);
      }
    }
    if (targets == null || targets.isEmpty()) {
      return; // No targets
    }

    // FIXME check for targets-change-report:
    //   calculate set differences
    //   for added targets: sendAdd
    //   for removed targets: sendRemove
    // add ContentReport to changes
    boolean gotContent = false;
    Object content = null;
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.getPrimary().equals(self)) {
        // Never send to self.  Likely an error.
      } else {
        if (!gotContent) {
          gotContent = true;
          content = rs.getContent();
        }
	// This target could be an ABA that includes this agent, right?
        sendChange(rs, target, content, changes);
      }
    }
  }

  // Local Relay.Source was publishRemoved
  // Called from lp.execute
  private void localRemove(Relay.Source rs) {
    Set targets = rs.getTargets();
    if (targets == null) return;
    if (targets.isEmpty()) return; // No targets
    // Again, if this is also a Relay.Target, could check that this is 
    // really the source
    localRemove(rs.getUID(), targets);
  }

  // Propogate removal of relay to each target
  // called from above, ie lp.execute, and from abaChange
  private void localRemove(UID uid, Set targets) {
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.getPrimary().equals(self)) {
        // Never send to self.  Likely an error.
      } else {
	// Again, what if the target is an ABA that includes this agent?
        sendRemove(uid, target);
      }
    }
  }

  /**
   * Handle a change to this target. We need to send the new response
   * to the source
   */
  private void localResponse(Relay.Target rt, Collection changes) {
  // called from changeTarget, receiveResponse, LP.execute
    MessageAddress source = rt.getSource();
    if (source == null) return; // No source
    if (self.equals(source.getPrimary())) return; // BOGUS source must be elsewhere. Ignore.

    Object resp = rt.getResponse();
    // cancel if response is null
    if (resp == null) return;

    sendResponse(rt, source, resp, changes);
  }

  // Send directive to given target Agent to add this Relay
  // called from localAdd and resend
  private void sendAdd(Relay.Source rs, MessageAddress target, Object content) {
    RelayDirective.Add dir = 
      new RelayDirective.Add(rs.getUID(), content, rs.getTargetFactory());
    dir.setSource(self);
    dir.setDestination(target);
    rootplan.sendDirective(dir);
  }

  // Send directive to given target Agent of change to this Relay
  // called from localChange
  private void sendChange(
      Relay.Source rs, MessageAddress target, Object content, Collection c) {
    RelayDirective.Change dir =
      new RelayDirective.Change(rs.getUID(), content, rs.getTargetFactory());
    dir.setSource(self);
    dir.setDestination(target);
    rootplan.sendDirective(dir, c);
  }

  // Send directive to given target agent to remove this Relay
  // called from localChange, localRemove, receiveResponse 
  private void sendRemove(UID uid, MessageAddress target) {
    RelayDirective.Remove dir = new RelayDirective.Remove(uid);
    dir.setSource(self);
    dir.setDestination(target);
    rootplan.sendDirective(dir);
  }

  // Send directive back to the Source of Response from this Target
  // called from sendVerification, addTarget, localResponse
  private void sendResponse(
      Relay.Target rt, MessageAddress source, Object resp, Collection c) {
    RelayDirective.Response dir = new RelayDirective.Response(rt.getUID(), resp);
    dir.setSource(self);
    dir.setDestination(source);
    rootplan.sendDirective(dir, c);
  }

  // Resend latest (possibly null) response from this target to the source
  // called from verify
  private void sendVerification(Relay.Target rt, MessageAddress source) {
    Object resp = rt.getResponse();
    // Send even if null response
    sendResponse(rt, source, resp, Collections.EMPTY_SET);
  }

  // MessageLogicProvider implementation
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof RelayDirective) { 
      // Quick test for one of ours
      if (self.equals(dir.getSource().getPrimary())) return;

      if (dir instanceof RelayDirective.Change) {
        receiveChange((RelayDirective.Change) dir, changes);
        return;
      }
      if (dir instanceof RelayDirective.Add) {
        receiveAdd((RelayDirective.Add) dir);
        return;
      }
      if (dir instanceof RelayDirective.Remove) {
        receiveRemove((RelayDirective.Remove) dir);
        return;
      }
      if (dir instanceof RelayDirective.Response) {
        receiveResponse((RelayDirective.Response) dir, changes);
        return;
      }
    }
  }

  // called from receiveAdd and receiveChange
  // In the target agent, add the Relay.Target (which may also implement Relay.Source)
  private void addTarget(Relay.TargetFactory tf, Object cont, RelayDirective dir) {
    Relay.Target rt;
    if (tf != null) {
      rt = tf.create(dir.getUID(), dir.getSource(), cont, token);
    } else if (cont instanceof Relay.Target) {
      rt = (Relay.Target) cont;
    } else {
      // ERROR cannot create target
      return;
    }
    if (rt == null) return;     // Target should not exist here
    // Add the target. Note that if it is also a source,
    // this LP will wake up again, and try to send the relay
    // to all the targets.
    /// FIXME: This is a place to block relaying. Add a Marker report?
    rootplan.add(rt);
    // Check for immediate response due to arrival
    Object resp = rt.getResponse();
    if (resp != null) {
      sendResponse(rt, dir.getSource(), resp, Collections.EMPTY_SET);
    }
  }

  // called from receiveAdd and receiveChange
  private void changeTarget(Relay.Target rt, Object cont, Collection changes) {
    // Branch on the change type flag.
    // If the content changed, then mark the taret as changed,
    // but in such a way that this LP won't run again
    int flags = rt.updateContent(cont, token);
    if ((flags & Relay.CONTENT_CHANGE) != 0) {
      Collection c;
      if (changes == null) {
        c = Collections.singleton(MarkerReport.INSTANCE);
      } else {
        c = new ArrayList(changes);
        c.add(MarkerReport.INSTANCE);
      }
      // Note the MarkerReport is on this change,
      // so the LP will not think the Response changed
      // and needs to flow back
      rootplan.change(rt, c);
      // FIXME: What is this for?!!
      // Note I made localChange bail if this is not the Source
      // Presumably this is for chaining. It means that if a content
      // change comes in to this agent, and the local Target is also
      // a source, we can let this LP pretend the change
      // was local, and propogate it to the listed targets
      // FIXME!!
      if (rt instanceof Relay.Source) localChange((Relay.Source) rt, changes);
    }

    // If we (also) changed the response on the relay,
    // send that reponse to the source if possible
    // -- but I don't see how or why an incoming directive would say that
    if ((flags & Relay.RESPONSE_CHANGE) != 0) {
      // Note localResponse does nothing if this is the source (correctly)
      localResponse(rt, Collections.EMPTY_SET);
    }
  }

  // called from lp.execute when get an incoming add directive
  private void receiveAdd(RelayDirective.Add dir) {
    UniqueObject uo = rootplan.findUniqueObject(dir.getUID());
    if (! (uo instanceof Relay.Target) && uo != null) {
      logger.error(self + ".receiveAdd RelayDirective.Add expected to find a Target on the BBoard, found: " + uo + " for Directive " + dir + ", source " + dir.getSource());
      return;
    }
    Relay.Target rt = (Relay.Target)uo;
    if (rt == null) {
      addTarget(dir.getTargetFactory(), dir.getContent(), dir);
    } else {
      // Unusual. Treat as change
      changeTarget(rt, dir.getContent(), Collections.EMPTY_SET);
    }
  }

  // Receive a change from remote Source at this Target
  // called only from incoming directive to lp.execute
  private void receiveChange(RelayDirective.Change dir, Collection changes) {
    Relay.Target rt = (Relay.Target) rootplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      // Unusual. Treat as add.
      addTarget(dir.getTargetFactory(), dir.getContent(), dir);
    } else {
      // What if this is the source?
      changeTarget(rt, dir.getContent(), changes);
    }
  }

  // called only from lp.execute when get a directive to remove this relay
  private void receiveRemove(RelayDirective.Remove dir) {
    Relay.Target rt = (Relay.Target) rootplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      // Unusual. Ignore.
    } else {
      rootplan.remove(rt);
    }
  }

  // called only from lp.execute
  private void receiveResponse(RelayDirective.Response dir, Collection changes) {
    UniqueObject uo = rootplan.findUniqueObject(dir.getUID());
    MessageAddress target = dir.getSource();
    if (! (uo instanceof Relay.Source) && uo != null) {
      // This is not legitimate. We'll get a ClassCastException below
      // if we're not careful
      logger.error(self + ": receiveResponse got non Relay.Source (Bug 3202?). Got: " + uo + " from the Response[" + dir.getUID() + "] with source " + target + " and dest " + dir.getDestination() + ", response " + dir.getResponse(), new Throwable()); 
      return;
    }
    Relay.Source rs = (Relay.Source) uo;
    //    Relay.Source rs = (Relay.Source) rootplan.findUniqueObject(dir.getUID());
    if (rs == null) {
      // No longer part of our blackboard. Rescind it.
      if (logger.isInfoEnabled())
	logger.info(self + ": receiveResponse got NULL Relay.Source from the Response[" + dir.getUID() + "] with source " + target + " and dest " + dir.getDestination() + ", response " + dir.getResponse()); 
      
      sendRemove(dir.getUID(), target);
    } else {
      Object resp = dir.getResponse();
      if (resp != null) {
	// Have a response. If the response changed, must locally 
	// publishChange the relay, but don't loop and resend the relay.
        int flags = rs.updateResponse(target, resp);
        if ((flags & Relay.RESPONSE_CHANGE) != 0) {
          Collection c;
          if (changes == null) {
            c = new ArrayList(1);
          } else {
            c = new ArrayList(changes);
          }
          c.add(MarkerReport.INSTANCE);
	  // FIXME: Must I require that this Relay.Source in fact originated
	  // on the local agent before doing a publishChange?
	  // FIXME: Should dir.getDestination().getPrimary().equals(self)?
	  // And if this is also a target, should its source be this
	  // agent, or not necessarily? If I was trying to chain Relays,
	  // then The source field on a relay.target need not be the place
	  // that originated the relay.source implementation - which
	  // will be local... I'm confused
          rootplan.change(rs, c);

	  // Note that localResponse will do nothing
	  // if this is the Source for this target. 
	  // This says that a downstream target
	  // told us they changed their response. If this is downstream
	  // of someone else, then send the response further upstream
          if (rs instanceof Relay.Target) localResponse((Relay.Target) rs, changes);
        }

	// If (also) the content of the relay is different (from the source)
	// then this lets us send the changes downstream maybe? But
	// we just got the info from downstream?
	// Or is this to check the targets list?
        if ((flags & Relay.CONTENT_CHANGE) != 0) {
	  // localChange requires that this is the Source as well
          localChange(rs, Collections.EMPTY_SET);
        }
      }
    }
  }

  // RestartLogicProvider implementation

  /**
   * Agent restart handler. Resend all our Relay.Source again and
   * send verification directives for all our Relay.Targets.
   */
  public void restart(final MessageAddress cid) {
    if (logger.isInfoEnabled()) {
      logger.info(
        self+": Reconcile with "+
        (cid==null?"all agents":cid.toString()));
    }
    UnaryPredicate pred = new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return o instanceof Relay;
      }
    };

    // Loop over all Relays on the Blackboard
    Enumeration en = rootplan.searchBlackboard(pred);
    while (en.hasMoreElements()) {
      Relay r = (Relay) en.nextElement();
      // Resend all Relay.Sources
      if (r instanceof Relay.Source) {
        Relay.Source rs = (Relay.Source) r;
	// What if it's also a target?
        resend(rs, cid);
      }

      // And for Targets, send back a verify to the source
      if (r instanceof Relay.Target) {
        Relay.Target rt = (Relay.Target) r;
        verify(rt, cid);
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info(self+": Reconciled");
    }
  }

  // When someone is restarting, resend all Relays
  private void resend(Relay.Source rs, MessageAddress t) {
    Set targets = rs.getTargets();
    if (targets == null) return; // Not really a source
    if (targets.isEmpty()) return;

    boolean gotContent = false; // Only grab the content once
    Object content = null;

    // For each target
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.getPrimary().equals(self)) {
        // Don't send to ourself.  Likely an error.
      } else if (t != null && !target.getPrimary().equals(t.getPrimary())) { 
        // Only resend to the specified address.
      } else {
        if (!gotContent) {
          gotContent = true;
          content = rs.getContent();
        }
	if (logger.isInfoEnabled()) {
          logger.info(
            self+": Resend"+(t==null?"*":"")+
            " to "+target+": "+rs.getUID());
        }
	
	// FIXME: Check that we're not sending to an ABA that includes this agent?

	// Caller ensures that Relay.Sources here
	// really originated here
	// Re-send that Relay as though it were new
        sendAdd(rs, target, content);
      }
    }
  }

  // Given address is restarting (or null). If it's the source
  // of the given relay or null and the relay didn't start here,
  // then send a verification
  private void verify(Relay.Target rt, MessageAddress s) {
    MessageAddress source = rt.getSource();
    if (source == null) return;
    if (source.getPrimary().equals(self)) {
      // Don't send to ourself.  Likely an error.
      return;
    } else {

      // Sends a verification back to the source
      // if the given address is null or the source address,
      // ie if the source restarted or we did
      if (s == null || source.getPrimary().equals(s.getPrimary())) {
	if (logger.isInfoEnabled()) {
          logger.info(
            self+": Verify"+(s==null?"*":"")+
            " to "+source+": "+rt.getUID());
        }
        sendVerification(rt, source);
      }
    }
  }

  // ABAChange implementation
  private static final UnaryPredicate relaySourcePred =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
	// FIXME: Somehow require it really is a source from here?
        return o instanceof Relay.Source;
      }
    };

  // Implement ABAChangeLogicProvider.

  // Can get called from DomainAdapter.invokeABAChangeLogicProviders and from RootDomain.invokeABAChangeLogicProviders
  // Distributor/Blackboard does one call - on the DomainService (ie DomainManager)
  // DomainManager loops over the domains (so RootDomain)
  // RootDomain is just an alternate implementation to DomainAdapter
  // So really this is all happening from the cacheClearer thread in the
  // Blackboard, inside some locks in the Distributor

  // Basically, this means that some ABA memberships (may have?) changed
  // So we need to go through all Relay sources, look at the
  // target lists, and if one is an ABA, get the translation,
  // figure out what additions or removals there are. Send
  // those adds/removes as necessary.

  // Note that when the Relay is initially published,
  // no effort is made to translate the ABA

  // If we didn't change that the source really started here,
  // then we'd get some relays that didn't start here and all targets
  // of the relay would each try to send a remove/add to the
  // changed members - duplicative at least. The only other
  // way around this is if the targets had an empty targets list (via
  // making it transient or a clever target factory).
  public void abaChange(Set communities) {
    if (logger.isDebugEnabled()) logger.debug(self+": abaChange");
    Enumeration en = rootplan.searchBlackboard(relaySourcePred);
    while (en.hasMoreElements()) {
      Relay.Source rs = (Relay.Source) en.nextElement();
      Set targets = rs.getTargets();
      if (targets != null && !targets.isEmpty()) {
        Set oldTranslation = Collections.EMPTY_SET;
        Set newTranslation = Collections.EMPTY_SET;
        for (Iterator i = targets.iterator(); i.hasNext(); ) {
          Object o = i.next();
          if (o instanceof AttributeBasedAddress) {
            AttributeBasedAddress aba = (AttributeBasedAddress) o;
            if (communities.contains(aba.getCommunityName())) {
              ABATranslation abaTranslation = rootplan.getABATranslation(aba);
              if (abaTranslation != null) {
                Collection oldC = abaTranslation.getOldTranslation();
                if (oldC != null && !oldC.isEmpty()) {
                  if (oldTranslation.isEmpty()) {
                    oldTranslation = new HashSet();
                  }
                  oldTranslation.addAll(oldC);
                }
                Collection newC = abaTranslation.getCurrentTranslation();
                if (newC != null && !newC.isEmpty()) {
                  if (newTranslation.isEmpty()) {
                    newTranslation = new HashSet();
                  }
                  newTranslation.addAll(newC);
                }
              }
            }
          }
        }
        if (!newTranslation.equals(oldTranslation)) {
          Set adds = new HashSet(newTranslation);
          Set removes = new HashSet(oldTranslation);
          adds.removeAll(oldTranslation);
          removes.removeAll(newTranslation);
          boolean isNOP = adds.isEmpty() && removes.isEmpty();
          if (isNOP && logger.isDebugEnabled()) {
            logger.debug("old " + oldTranslation);
            logger.debug("new " + newTranslation);
            logger.debug("Rmv " + removes + " from " + rs);
            logger.debug("Add " + adds + " to " + rs);
          }
          if (!isNOP && logger.isInfoEnabled()) {
            logger.info("old " + oldTranslation);
            logger.info("new " + newTranslation);
            logger.info("Rmv " + removes + " from " + rs);
            logger.info("Add " + adds + " to " + rs);
          }
          if (!removes.isEmpty()) {
            localRemove(rs.getUID(), removes);
          }
          if (!adds.isEmpty()) {
            localAdd(rs, adds);
          }
        }
      }
    }
  }

  /** 
   * ChangeReport for this LP to identify its own changes.
   */
  private static final class MarkerReport implements ChangeReport {
    public static final MarkerReport INSTANCE = new MarkerReport();
    private MarkerReport() { }
    private Object readResolve() { return INSTANCE; }
    @Override
   public String toString() { return "relay-marker-report"; }
    static final long serialVersionUID = 9091843781928322223L;
  }

  /** 
   * Token implementation, private to RelayLP.
   * <p>
   * Keeps a map of (agent-&gt;token), which allows rehydrated
   * relay objects to use "==" token matching.
   */
  private static final class TokenImpl extends Relay.Token {
    private static final Map tokens = new HashMap(13);
    private final MessageAddress addr;
    public static TokenImpl getToken(MessageAddress addr) {
      synchronized (tokens) {
        TokenImpl t = (TokenImpl) tokens.get(addr);
        if (t == null) {
          t = new TokenImpl(addr);
          tokens.put(addr, t);
        }
        return t;
      }
    }
    private TokenImpl(MessageAddress addr) { this.addr = addr; }
    private Object readResolve() { return getToken(addr); }
    @Override
   public String toString() { return "<token "+addr+">"; }
    static final long serialVersionUID = 3878912876728718092L;
  }
}
