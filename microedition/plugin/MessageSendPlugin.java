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
package org.cougaar.microedition.plugin;

import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.io.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.shared.tinyxml.*;

import java.util.*;
import java.io.*;

/**
 * Plugin that looks for tasks allocated to microagents and sends them there.
 */
public class MessageSendPlugin extends PluginAdapter {

  private boolean debugging = false;

  /**
   * Need to subscribe to allocations to microagents
   */
  private static UnaryPredicate myPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      boolean ret = false;
      if (o instanceof MicroAllocation) {
        MicroAllocation ma = (MicroAllocation)o;
        ret = ma.getAsset() instanceof MicroAgent;
      }
      return ret;
    }
  };
  private Subscription allocs;

  public void setupSubscriptions() {
    debugging = isDebugging();
    allocs = subscribe(myPred);
  }
  public void execute() {
    Enumeration alloc_enum = allocs.getAddedList().elements();
    while (alloc_enum.hasMoreElements()) {
      processAllocation((MicroAllocation)alloc_enum.nextElement(), "add");
    }

    alloc_enum = allocs.getChangedList().elements();
    while (alloc_enum.hasMoreElements()) {
      processAllocation((MicroAllocation)alloc_enum.nextElement(), "change");
    }

    alloc_enum = allocs.getRemovedList().elements();
    while (alloc_enum.hasMoreElements()) {
      processAllocation((MicroAllocation)alloc_enum.nextElement(), "remove");
    }

  }

  private void processAllocation(MicroAllocation ma, String operation) {
    MicroAgent asset = (MicroAgent)ma.getAsset();
    if (debugging) System.out.println("MessageSendPlugin: Sending "+ma.getTask().getVerb()+
                                      " to "+asset.getAgentId().getName()+" op="+operation);
		try {
      getDistributor().getMessageTransport().sendMessage(ma.getTask(), asset, operation);
		} catch (IOException ioe) {
      System.err.println("MessageSendPlugIn.processAllocation: couldn't send task to remote agent: " + asset);
		}
  }
}
