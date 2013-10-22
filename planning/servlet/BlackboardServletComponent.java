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

package org.cougaar.planning.servlet;

import javax.servlet.Servlet;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.servlet.SimpleServletComponent;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.LDMService;
import org.cougaar.util.ConfigFinder;

/**
 * Basic servlet base class for servlets that need blackboard access.
 **/
public class BlackboardServletComponent 
extends SimpleServletComponent 
implements BlackboardClient
{

  protected BlackboardService blackboard;
  private LDMService ldmService = null;
  private SchedulerService scheduler;

  public final void setBlackboardService(BlackboardService s) {
    blackboard = s;
  }
  protected final BlackboardService getBlackboardService() {
    return blackboard;
  }
  public final void setLDMService(LDMService s) {
    ldmService = s;
  }
  protected final LDMService getLDMService() {
    return ldmService;
  }
  protected ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }


  // rely upon load-time introspection to set these services - 
  //   don't worry about revokation.
  public final void setSchedulerService(SchedulerService ss) {
    scheduler = ss;
  }

  public SchedulerService getSchedulerService () { return scheduler; }

  /** just like the core, except I can create a servlet support subclass */
  protected SimpleServletSupport createSimpleServletSupport(Servlet servlet) {
    super.createSimpleServletSupport(servlet);
    
    // throw original support object away
    // create a new "SimpleServletSupport" instance
    return makeServletSupport ();
  }

  /**
   * so a subclass can create a different servlet support just by overriding this method
   * perhaps the core should work like this?
   */
  protected SimpleServletSupport makeServletSupport () {
    if (log.isInfoEnabled())
      log.info ("Creating BlackboardServletSupport");

    // create a new "SimpleServletSupport" instance
    // FIXME - deprecation: Should replace getLDMService().getFactory() with
    // getDomainService().getFactory()
    return 
      new BlackboardServletSupport (
        path,
        agentId,
        blackboardQuery,
	log,
        blackboard,
	getConfigFinder(),
	((PlanningFactory) getLDMService().getFactory("planning")),
	getLDMService().getLDM(),
	scheduler);
  }

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    //
    // see "ComponentPlugin" for details.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }
}
