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
import org.cougaar.microedition.shared.tinyxml.*;

import java.util.*;
import java.io.*;

/**
 * Handles queries for agents based on capabilities.  It subscribes to AgentQuery
 * objects, uses the capabilitiesSubstring to query the name server for matching
 * agents, and publishes the new agents to the blackboard.
 */
public class AgentQueryPlugin extends PluginAdapter {
  private boolean debugging = false;
  /**
   * Need to subscribe to new agent queries
   */
  private static UnaryPredicate myPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof AgentQuery;
    }
  };
  private Subscription queries;

  /**
   * Need to subscribe to the name server agent
   */
  private static UnaryPredicate myNameServerPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      boolean ret = false;
      if (o instanceof MicroAgent) {
        MicroAgent mc = (MicroAgent)o;
        ret = (mc.getAgentId().getCapabilities().indexOf("Name Server") >= 0);
      }
      return ret;
    }
  };
  private Subscription nameServerSub;

  public void setupSubscriptions() {
    queries = subscribe(myPred);
    nameServerSub = subscribe(myNameServerPred);
  }

  private MicroAgent nameServer = null;

  public void execute() {
    //
    // Update name server if necessary
    //
    if (nameServerSub.hasChanged()) {
      synchronized (nameServerSub) {
        nameServer = null;
        if (nameServerSub.getMemberList().size() > 0)
          nameServer = (MicroAgent)nameServerSub.getMemberList().firstElement();
      }
    }
    //
    // Create any new queries
    //
    Enumeration en = queries.getAddedList().elements();
    while (en.hasMoreElements()) {
      AgentQuery mq = (AgentQuery)en.nextElement();
      startQuery(mq);
    }
  }

  private Vector waitingQueries = null;
  private void startQuery(AgentQuery mq) {
    if (waitingQueries == null) {
      waitingQueries = new Vector();
      // Why won't the KVM let me name a thread?
      // Thread t = new Thread(new Querier(), "AgentQueryThread");
      Thread t = new Thread(new Querier());
      t.start();
    }
    synchronized (waitingQueries) {
      waitingQueries.addElement(mq);
      waitingQueries.notify();
    }
  }

  /**
   * A worker thread class to make the name server requests and parse the answers
   */
  private class Querier implements Runnable {
    public void run() {
      Vector workingQueries = new Vector();
      Vector microAgents = new Vector();
      for (;;) {
        workingQueries.removeAllElements();
        microAgents.removeAllElements();
        synchronized (waitingQueries) {
          try {
            if (waitingQueries.size() == 0)
              waitingQueries.wait();
            // copy into a working vector
            while (waitingQueries.size() > 0) {
              workingQueries.addElement(waitingQueries.elementAt(0));
              waitingQueries.removeElementAt(0);
            }
          } catch (InterruptedException ie) { /* Nothing to do */ }
        } // end "synchronized"
        //
        // Make the queries
        //
        Enumeration query_enum = workingQueries.elements();
        while (query_enum.hasMoreElements()) {
          AgentQuery mq = (AgentQuery)query_enum.nextElement();
          mq.setAgents(makeQuery(mq));
          copyInto(microAgents, mq.getAgents());
        }
        //
        // publish the resulting MicroAgents
        //
        openTransaction();
        Enumeration en = microAgents.elements();
        while (en.hasMoreElements()) {
          publishAdd(en.nextElement());
        }
        //
        // The queries have changed
        //
        query_enum = workingQueries.elements();
        while (query_enum.hasMoreElements()) {
          publishChange(query_enum.nextElement());
        }
        closeTransaction();
      } // end "for(;;)"
    } // end run()

    /**
     *  Add elements of "outof" into "into" as long as their UIDs are unique
     */
    private void copyInto(Vector into, Vector outof) {
      Enumeration src_enum = outof.elements();
      next_src:
      while (src_enum.hasMoreElements()) {
        MicroAgent mc = (MicroAgent)src_enum.nextElement();
        Enumeration dst_enum = into.elements();
        while (dst_enum.hasMoreElements()) {
          MicroAgent test = (MicroAgent)dst_enum.nextElement();
          if (test.getAgentId().getName().equals(mc.getAgentId().getName()))
            continue next_src;  // already got that one
        }
        // not found, let's add it
        into.addElement(mc);
      }
    } // end copyInto()

    /**
     * Query the name server for a agent my capability
     */
     private Vector makeQuery(AgentQuery mq) {
       Vector ret = new Vector();
       StringBuffer str = new StringBuffer();
       str.append(getDistributor().getNodeName() + ":");
       str.append(mq.xmlPreamble);
       mq.encode(str);
       // get the name server
       MicroAgent myNameServer;
       synchronized(nameServerSub) {
         myNameServer = nameServer;
       }
       if (myNameServer == null) {
         throw new RuntimeException("ERROR: No name server available");
       }

       String response = xmit(str.toString(), myNameServer);

       try {
         XMLInputStream aStream = new XMLInputStream(response);
         XMLParser aParser = new XMLParser();
         aParser.setDocumentHandler(new Parser(ret));
         aParser.setInputStream(aStream);
         aParser.parse();
       } catch (ParseException e) {
         System.out.println("Error parsing XML agent query response:"+e.toString());
       }

       return ret;
     }

     private String xmit(String message, MicroAgent nameServer) {
       SocketME sock = null;
       StringBuffer response = new StringBuffer();
       String msg = null;
       try {
         sock = (SocketME)MicroEdition.getObjectME(Class.forName("org.cougaar.microedition.io.SocketME"));
	 sock.open(nameServer.getAgentId().getIpAddress(),
                   nameServer.getAgentId().getPort());
         OutputStream os = sock.getOutputStream();
         os.write(message.getBytes());
         os.write(0);
         os.flush();

         InputStream is = sock.getInputStream();
         while (true) {
           int bite = is.read();
           if (bite <= 0)
             break;
           response.append((char)bite);
         }
         sock.close();
         String responseStr = response.toString();
         int idx = responseStr.indexOf(":");
         if (idx < 0) {
           System.err.println("ERROR: Malformed reg resp message (no source)" + response);
         }
         String source = responseStr.substring(0, idx);
         msg = responseStr.substring(idx+1);
         if (debugging) System.out.println("Got query response from '" + source + "':\n" + msg);
       } catch (Exception ex) {
         System.err.println("Error in AgentQueryPlugin xmit: "+ex);
       }
      return msg;
     } // end xmit

    private class Parser extends HandlerBase {
      private String lastKey = "";
      private String name;
      private short port;
      private String ipAddress;
      private String capabilities;

      private Vector agents;

      public Parser(Vector agents) {
        this.agents = agents;
      }

      public void elementStart(String name, Hashtable attr) throws ParseException {
        lastKey = name;
      }

      public void elementEnd(String elementName) throws ParseException {
        if (elementName.equals("nodelookupresult")) {
          // got a complete one
          MicroAgent mc = new MicroAgent();
          mc.setAgentId(new AgentId(name, ipAddress, port, capabilities));
          agents.addElement(mc);
          //reset
          port = 0; name = ipAddress = capabilities = null;
        }
      }

      public void charData(String charData) {
        if (lastKey.equals("node_name"))
          name = charData;
        else if (lastKey.equals("node_ip_address"))
          ipAddress = charData;
        else if (lastKey.equals("node_server_port"))
          port = Short.parseShort(charData);
        else if (lastKey.equals("node_description"))
          capabilities = charData;
      }
    }

  } // end class Querier
}
