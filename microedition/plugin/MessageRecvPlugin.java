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

import java.util.*;
import java.io.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.io.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;

/**
 * Infrastructure plugin for receiving messages.
 */
public class MessageRecvPlugin  extends PluginAdapter implements MessageListener {

  private boolean debugging = false;
  /**
   * Need to subscribe to task changes to see allocations
   */
  private UnaryPredicate myTaskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof MicroTask;
    }
  };

  /**
   * Need to subscribe other agents to send messages
   */
  private UnaryPredicate myAgentsPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof MicroAgent;
    }
  };

  private Subscription taskSub = null;
  private Subscription agentSub = null;
  private Hashtable tasks = new Hashtable();


  private Object unwrapObject(String data) {
    TaskDecoder td = new TaskDecoder();
    MicroTask mt = td.decode(data);
    return mt;
  }

  public void deliverMessage(String data, String source) {
    if (debugging) System.out.println("MessageRecvPlugin: GOT : "+data+" from "+source);
    String op = null;
    int idx = data.indexOf("op=");
    // Snip out just the first three characters of the operation
    if (idx >= 0)
      op = data.substring(idx+4, idx+7);
    Object microTask = unwrapObject(data);

    openTransaction();
    if (op.equals("add")) {
      publishAdd(microTask);
      tasks.put(microTask, source);
    }

    else if (op.equals("cha")) {
      Enumeration changes = taskSub.getMemberList().elements();
      while (changes.hasMoreElements()) {
        MicroTask mt = (MicroTask)changes.nextElement();
        if (mt.getUniqueID().equals(((MicroTask)microTask).getUniqueID())) {
				  MicroAllocation ma = mt.getAllocation(); // keep old allocation
          mt.setMe((MicroTask)microTask);
          if (debugging) System.out.println("MessageRecvPlugin: Change to "+mt.getUniqueID());
          publishChange(mt);
          if (mt.getAllocation() != null) {
            MicroAllocation new_alloc = mt.getAllocation();
            MicroAllocationResult mar = new_alloc.getReportedResult();
            // append the source of this result
            if (mar != null)
              mar.setAuxData(source);
            ma.setReportedResult(mar);
            // detach new allocation
            new_alloc.setTask(null);
					  mt.setAllocation(ma);
            publishChange(ma);
					}
        }
      }
    }
    else if (op.equals("rem")) {
      Enumeration removes = taskSub.getMemberList().elements();
      while (removes.hasMoreElements()) {
        MicroTask mt = (MicroTask)removes.nextElement();
        if (mt.getUniqueID().equals(((MicroTask)microTask).getUniqueID()))
        {
          publishRemove(mt);
          if (mt.getAllocation()  != null) {
            publishRemove(mt.getAllocation());
            tasks.remove(mt);
          }
        }
      }
    }
//    tasks.put(microTask, source);

    closeTransaction();
  }

  public void setupSubscriptions() {
    debugging = isDebugging();
    if (debugging) System.out.println("MessageRecvPlugin: setupSubscriptions");
    getDistributor().getMessageTransport().addMessageListener(this);
    taskSub = subscribe(myTaskPred);
    // Use a special version of close transaction. I don't want to see these
    // changes; they are for other Plugins.
    taskSub.setSeeOwnChanges(false);
    agentSub = subscribe(myAgentsPred);
  }

  private Vector waitingChangeMessages = new Vector();
  public void execute() {
    if (debugging) System.out.println("MessageRecvPlugin: execute()");
    // deal with changed tasks
    Enumeration changes = taskSub.getChangedList().elements();
    while (changes.hasMoreElements()) {
      MicroTask mt = (MicroTask)changes.nextElement();
      String source = (String)tasks.get(mt);
      if (debugging) System.out.println("MessageRecvPlugin: Changed task from "+source);
      // ignore tasks from me.  Don't need to send them anywhere.
      if ((source == null) || (source.equals(getNodeName())))
        continue;
      MicroAgent mc = findMicroAgent(source);
      // If I don't know who this is from, look him up.
      if (mc == null) {
        if (debugging) System.out.println("MessageRecvPlugin: make agent query for "+source);
        AgentQuery cq = new AgentQuery();
        cq.setName(source);
        publishAdd(cq);
        waitingChangeMessages.addElement(mt);
      } else {
        try {
          if (debugging) System.out.println("MessageRecvPlugin: Sending messsage to "+source);
          getDistributor().getMessageTransport().sendMessage(mt, mc, "change");
				} catch (IOException ioe) {
          System.err.println("MessageRecvPlugin.execute: couldn't deliver task change message to " + mc);
				}
      }
    }
    // try to clear waiting messsages
    if (waitingChangeMessages.size() > 0) {
      MicroTask [] agents = new MicroTask[waitingChangeMessages.size()];
      waitingChangeMessages.copyInto(agents);
      for (int i=0; i<agents.length; i++) {
        MicroTask mt = agents[i];
        String source = (String)tasks.get(mt);
        MicroAgent mc = findMicroAgent(source);
        if (mc != null) {
				  try {
          if (debugging) System.out.println("MessageRecvPlugin: Clearing messsage to "+source);
          getDistributor().getMessageTransport().sendMessage(mt, mc, "change");
          waitingChangeMessages.removeElement(mt);
				  } catch (IOException ioe) {
            System.err.println("MessageRecvPlugin.execute: couldn't deliver task change message to " + mc);
				  }
        }
      }
    }
  }
  private MicroAgent findMicroAgent(String name) {
    Enumeration en = agentSub.getMemberList().elements();
    MicroAgent ret = null;
    while (en.hasMoreElements()) {
      MicroAgent mc = (MicroAgent)en.nextElement();
      if (name.equals(mc.getAgentId().getName())) {
        ret = mc;
        break;
      }
    }
    return ret;
  }
}

