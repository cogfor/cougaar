/*
 * <copyright>
 * 
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.asset;

import org.cougaar.microedition.shared.Encodable;
/**
 * AgentQuery objects are picked up by the AgentQueryPlugin and interpreted as
 * requests to look up other agent by capability.  The capabilities substring is
 * text-matched against the capabilities in the other agents.  Those that match
 * are added to the blackboard.
 */
public class AgentQuery implements Encodable{

  public AgentQuery() {
  }
  private String capabilitiesSubstring = null;
  private java.util.Vector agents = null;
  private String name = null;

  /**
   * Fetch the string used to match against the capabilities of other agents.
   */
  public String getCapabilitiesSubstring() {
    return capabilitiesSubstring;
  }
  /**
   * Set the string used to match against the capabilities of other agents.
   */
  public void setCapabilitiesSubstring(String newCapabilitiesSubstring) {
    capabilitiesSubstring = newCapabilitiesSubstring;
  }

  /**
   * Fetch the string used to match against the name of other agents.
   */
  public String getName() {
    return name;
  }
  /**
   * Set the string used to match against the name of other agents.
   */
  public void setName(String newName) {
    name = newName;
  }
  /**
   * Set the list of agents with capabilities matching the capabilities substring.
   * This method is used by AgentQueryPlugin.
   */
  public void setAgents(java.util.Vector newAgents) {
    agents = newAgents;
  }
  /**
   * Fetch the list of agents with capabilities matching the capabilities substring.
   * This list is not filled in until the query is complete.
   */
  public java.util.Vector getAgents() {
    return agents;
  }

  /**
   * From Encodable.  Convert this object to XML and append it to the StringBuffer.
   */
  public void encode(StringBuffer str) {
    str.append("<agentQuery>");
    str.append("<capabilitiesSubstring>");
    if (getCapabilitiesSubstring() != null) {
      str.append(getCapabilitiesSubstring());
    }
    str.append("</capabilitiesSubstring>");
    str.append("<name>");
    if (getName() != null) {
      str.append(getName());
    }
    str.append("</name>");
    str.append("</agentQuery>");
  }
}
