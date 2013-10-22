/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.community;

import javax.naming.directory.Attributes;

import org.cougaar.core.service.community.Agent;

/**
 * Implementation of org.cougaar.core.service.community.Agent interface used
 * to define community member agents.
 */
public class AgentImpl extends EntityImpl implements Agent, java.io.Serializable {

  /**
   * Constructor
   * @param name Agent name
   */
  public AgentImpl(String name) {
    super(name);
  }

  /**
   * Constructor
   * @param name Agent name
   * @param attrs Initial attributes
   */
  public AgentImpl(String name, Attributes attrs) {
    super(name, attrs);
  }

  public Object clone() {
    return super.clone();
  }

  /**
   * Returns an XML representation of agent.
   * @return XML representation of agent
   */
  public String toXml() {
    return toXml("");
  }

  /**
   * Returns an XML representation of agent.
   * @param indent Blank string used to pad beginning of entry to control
   *               indentation formatting
   * @return XML representation of agent
   */
  public String toXml(String indent) {
    StringBuffer sb = new StringBuffer(indent + "<Agent name=\"" + getName() + "\" >\n");
    Attributes attrs = getAttributes();
    if (attrs != null && attrs.size() > 0)
      sb.append(attrsToString(getAttributes(), indent + "  "));
    sb.append(indent + "</Agent>\n");
    return sb.toString();
  }

}
