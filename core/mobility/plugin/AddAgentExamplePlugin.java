/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
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

import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.util.UnaryPredicate;

/**
 * This component is an example plugin demonstrating how to use an
 * {@link AgentControl} to add an agent to the local node.
 * <p>
 * First create an AddTicket and use the MobilitySupport to create an
 * AgentControl request, the object through which all
 * adds/moves/removes are recognized by the mobility API and handled
 * correctly and securely.
 * <p>
 * This plugin also shows how one would subscribe to the changed
 * AgentControl objects and their status. 
 */
public class AddAgentExamplePlugin extends ComponentPlugin {
  
  protected UnaryPredicate AGENT_CONTROL_PRED =
    new UnaryPredicate() {
	/**
       * 
       */
      private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
	  return (o instanceof AgentControl);
	}
      };

  protected MobilityFactory mobilityFactory;
  DomainService domain;

  IncrementalSubscription sub;

  public void setDomainService(DomainService domain) {
    this.domain = domain;
    mobilityFactory = 
      (MobilityFactory) domain.getFactory("mobility");
  }
  
  @Override
protected void setupSubscriptions() {
    if (mobilityFactory == null) {
      throw new RuntimeException(
				 "Mobility factory (and domain) not enabled");
    }

    Iterator iter = getParameters().iterator();
    String newAgent = (String) iter.next();
    String destNode = (String) iter.next();

    // add the AgentControl request
    addAgent(newAgent, destNode);

    sub = blackboard.subscribe(AGENT_CONTROL_PRED);
  }
  
  @Override
public void execute() {
    if (sub.hasChanged()) {
      for (Enumeration en = sub.getAddedList(); en.hasMoreElements(); ) {
	AgentControl ac = (AgentControl) en.nextElement();
        System.out.println("ADDED "+ac);
      }
      for (Enumeration en = sub.getChangedList(); en.hasMoreElements(); ) {
	AgentControl ac = (AgentControl) en.nextElement();
        System.out.println("CHANGED "+ac);
      }
      for (Enumeration en = sub.getRemovedList(); en.hasMoreElements(); ) {
	AgentControl ac = (AgentControl) en.nextElement();
        System.out.println("REMOVED "+ac);
      }
    }
  }
  
  protected void addAgent(
			  String newAgent,
			  String destNode) {

    MessageAddress newAgentAddr = null;
    MessageAddress destNodeAddr = null;
    if (newAgent != null) {
      newAgentAddr = MessageAddress.getMessageAddress(newAgent);
    }
    if (destNode != null) {
      destNodeAddr = MessageAddress.getMessageAddress(destNode);
    }
    Object ticketId =
      mobilityFactory.createTicketIdentifier();
    AddTicket addTicket = 
      new AddTicket(
		    ticketId,
		    newAgentAddr,
		    destNodeAddr);
  
    AgentControl ac = mobilityFactory.createAgentControl(null, destNodeAddr, addTicket);

    System.out.println("CREATED "+ac);
    blackboard.publishAdd(ac);
  }

} 
