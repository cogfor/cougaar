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
package org.cougaar.microedition.plugin.test;

import java.io.*;
import java.util.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.plugin.*;

/**
 *
 */
public class TestAllocatorPlugin extends PluginAdapter {

  UnaryPredicate getPred(String verb) {
   return new VerbPred(verb);
  }

  
  UnaryPredicate getResourcePred(String name) {
    final String myTargetName = name;
    UnaryPredicate resourcePred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroAgent) {
          MicroAgent mc = (MicroAgent)o;
          return mc.getAgentId().getName().equals(myTargetName);
        }
        return false;
      }
    };
    return resourcePred;
  }

  UnaryPredicate getQueryPred(String name) {
    final String myTargetName = name;
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof AgentQuery) {
          AgentQuery cq = (AgentQuery)o;
          return cq.getName().equals(myTargetName);
        }
        return false;
      }
    };
    return pred;
  }


  Subscription taskSub;
  Subscription resourceSub;
  Subscription querySub;

  String target = "unset";
  String verb = "Measure";

  public void setupSubscriptions() {
    if (getParameters() != null) {
      Hashtable t = getParameters();
      if (t.containsKey("target"))
        target = (String)t.get("target");
      if (t.containsKey("verb"))
        verb = (String)t.get("verb");

      System.out.println("CommsTestPlugin: setupSubscriptions " + t);
    }
    else {
      System.out.println("commsTestPlugin: setupSubscriptions No Params");
    }
    taskSub = subscribe(getPred(verb));
    resourceSub = subscribe(getResourcePred(target));
    querySub = subscribe(getQueryPred(target));

    makeAgentQuery();

    taskSub.setSeeOwnChanges(false);
  }

  /**
   * Assumes that it is called within a transaction.
   */
  private AgentQuery cq = null;

  private void makeAgentQuery()
  {
    if(cq != null) publishRemove(cq);

    cq = new AgentQuery();
    cq.setName(target);
    publishAdd(cq);
  }


  void allocate(MicroTask mt) {
    Enumeration agents = resourceSub.getMemberList().elements();
    if (agents.hasMoreElements()) {
      MicroAgent mc = (MicroAgent) agents.nextElement();
      MicroAllocation ma = new MicroAllocation(mc, mt);
      publishAdd(ma);
      publishChange(mt);
      System.out.println("commsTestPlugin: Made allccation to "+mc.getAgentId().getName());
    }
  }

  private void updateReportedResult(MicroTask mt) {
    System.out.println("commsTestPlugin: updateReportedResult");
    publishChange(mt);
  }

  public void execute() {
  // Find unallocated tasks and allocate them
  //System.out.println("testalloc: execute "+resourceSub.getMemberList().size());
    Enumeration tasks = taskSub.getMemberList().elements();
    while (tasks.hasMoreElements()) {
      MicroTask mt = (MicroTask)tasks.nextElement();
      if (mt.getAllocation() == null)
        allocate(mt);
    }
    // Update allocation results of changed tasks
    tasks = taskSub.getChangedList().elements();
    while (tasks.hasMoreElements()) {
      MicroTask mt = (MicroTask)tasks.nextElement();
      if ((mt.getAllocation() != null) &&
          (mt.getAllocation().getReportedResult() != null))
        updateReportedResult(mt);
    }

    Enumeration queries = querySub.getChangedList().elements();
    while (queries.hasMoreElements()) {
      AgentQuery cq = (AgentQuery)queries.nextElement();
      if (resourceSub.getMemberList().size() == 0) {
        System.out.println("Re-trying agent query");
        makeAgentQuery();
      }
    }
  }

}


class VerbPred implements UnaryPredicate {
  private String verb;

  public VerbPred(String verb) {
    this.verb = verb;
  }

  public boolean execute(Object o) {
    if (o instanceof MicroTask) {
      MicroTask mt = (MicroTask)o;
      return mt.getVerb().equals(verb);
    }
    return false;
  }
}
