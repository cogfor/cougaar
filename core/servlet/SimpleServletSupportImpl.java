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

package org.cougaar.core.servlet;

import java.net.URLEncoder;
import java.util.Collection;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/**
 * This is a generic API, provided by "SimpleServletComponent",
 * that allows a Servlet to access COUGAAR Services.
 * <p>
 * The SimpleServletComponent class passes an instance of 
 * SimpleServletSupport to a Servlet's constructor.  This API
 * abstracts away the Component and Service details from the
 * Servlet.
 */
public class SimpleServletSupportImpl
implements SimpleServletSupport 
{
  protected String path;
  protected MessageAddress agentId;
  protected BlackboardQueryService blackboardQuery;
  protected LoggingService log;

  protected String encAgentName;

  public SimpleServletSupportImpl(
      String path,
      MessageAddress agentId,
      BlackboardQueryService blackboardQuery) {
    this(path, agentId, blackboardQuery, null);
  }

  public SimpleServletSupportImpl(
      String path,
      MessageAddress agentId,
      BlackboardQueryService blackboardQuery,
      LoggingService log) {
    this.path = path;
    this.agentId = agentId;
    this.blackboardQuery = blackboardQuery;
    this.log = 
      ((log != null) ? log :  LoggingService.NULL);
    // cache:
    encAgentName = encodeAgentName(agentId.getAddress());
  }

  public String getPath() {
    return path;
  }

  public Collection queryBlackboard(UnaryPredicate pred) {
    return blackboardQuery.query(pred);
  }

  public String getEncodedAgentName() {
    return encAgentName;
  }

  public MessageAddress getAgentIdentifier() {
    return agentId;
  }

  public LoggingService getLog() {
    return log;
  }

  // maybe add a "getAllAgentIdentifiers()"

  public String encodeAgentName(String name) {
    try {
      return URLEncoder.encode(name, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to encode to UTF-8?");
    }
  }

  // etc to match "SimpleServletSupport"
}
