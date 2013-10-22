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
package org.cougaar.microedition.io;

/**
 * An identifer for another agent.  Includes name, capabilities, and contact information.
 */
public class AgentId {

  /**
   * Create a new agent identifier.  The properties are filled in with the
   * arguments given.
   */
  public AgentId(String name, String ipAddress, short port, String capabilities) {
    setName(name);
    setIpAddress(ipAddress);
    setPort(port);
    setCapabilities(capabilities);
  }
  private String name;
  private short port;
  private String ipAddress;
  private String capabilities;

  /**
   * Get the name of the agent.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the IP port number for sending messages to the agent.
   */
  public short getPort() {
    return port;
  }

  /**
   * Get the IP address for sending messages to the agent.  Could be a
   * host name on nodes with name resolution available.
   */
  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * Get a string describing the capabilities of the node.  This is free text, but
   * should be a list of things the agent can do for client agents.  It is used by
   * AgentQuery and AgentQueryPlugin.
   */
  public String getCapabilities() {
    return capabilities;
  }

  //
  // ----- private setter methods -----
  //
  private void setName(String newName) {
    name = newName;
  }

  private void setPort(short newPort) {
    port = newPort;
  }

  private void setIpAddress(String newIpAddress) {
    ipAddress = newIpAddress;
  }

  private void setCapabilities(String newCapabilities) {
    capabilities = newCapabilities;
  }
}
