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
package org.cougaar.microedition.se.domain;

import java.util.*;

import org.cougaar.planning.ldm.plan.*;

import org.cougaar.microedition.shared.*;

/**
 * This service facilitates communication between CougaarME and big Cougaar.
 * It is initizlized by the MicroAgentMessagePlugin.
 */
public class MEMessageService implements org.cougaar.core.component.Service {

  MessageTransport mt = null;

  
  public MEMessageService(String agentName) {
    mt = new MessageTransport(agentName);
    String port = System.getProperty("org.cougaar.microedition.ServerPort");
    // TODO: Is this the best way to get this port number?
    if (port == null)
      port = "1235";
    mt.setPort(Short.parseShort(port));
    mt.startListener();
  }

  /**
   * Create a microTask based on the contents of the ALP task.
   * @param t the ALP task to emulate
   * @return A MicroTask with the verb, UID, and prepositions filled in
   */
  public MicroTask newMicroTask(Task t) {
    // Copy task fields to MicroTask
    MicroTask ret = new MicroTask();
    ret.setVerb(t.getVerb().toString());
    ret.setUniqueID(t.getUID().toString());

    Enumeration preps = t.getPrepositionalPhrases();
    while (preps.hasMoreElements()) {
      PrepositionalPhrase pp = (PrepositionalPhrase)preps.nextElement();
      ret.addPrepositionalPhrase(
          new MicroPrepositionalPhrase(
              pp.getPreposition(),
              pp.getIndirectObject().toString()));
    }

    return ret;
  }

  /**
   * Get the MessageTransport used to communicate with the MicroAgents
   * @return the MessageTransport
   */
  public MessageTransport getMessageTransport() {
    return mt;
  }
}
