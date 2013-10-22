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
package org.cougaar.microedition.node;

import java.util.*;
import java.lang.*;
import java.io.*;

import org.cougaar.microedition.plugin.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.io.*;
import org.cougaar.microedition.util.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.shared.*;

/**
 * This is the VM entry point for the cougaarME agent.  Arguments are
 * [myName][nameServerHostname][nameServerPort]   OR
 * [myConfigFileName]
 */
public class Node {

  /**
   * argv[0] ==> my name
   * argv[1] ==> name/ipAddress of the nameServer
   * argv[2] ==> port number the nameServer listens on
   *
   * OR
   *
   * argv[0] ==> my config file name
   */
  public static void main(String[] args) {
    Node node = new Node(args);
  }

  private String nodeName;
  private String mom = "";

  // These are only used if the configuration is being read from a config server.
  private OutputStream nameServerOutputStream = null;
  private InputStream nameServerInputStream = null;
  private SocketME nameServerSocket = null;
  private MessageTransport mt;


  public String getNodeName() {
    return nodeName;
  }

  private Node(String[] args) {

    System.out.println("I am java "+System.getProperty("java.version") + " or " +
                                    System.getProperty("microedition.configuration") + ", running on "+
                                    System.getProperty("os.name"));

    String nameServerName = "";
    short nameServerPort = 0;
    
    System.out.println("args = "+args);

    if ((args == null) || (args.length == 0)) { // Take args from system properties
        String nodeName = System.getProperty("org.cougaar.microedition.nodeName");
        if (nodeName == null)
            System.out.println("Error: No way to get node name.  No args or properties");
        if (System.getProperty("org.cougaar.microedition.configServer") != null) {
            args = new String[3];
            args[0] = nodeName;
            args[1] = System.getProperty("org.cougaar.microedition.configServer");
            args[2] = System.getProperty("org.cougaar.microedition.configServerPort");
        } else {
            args = new String[1];
            args[0] = nodeName;
        }
    }
    
    if (args.length == 1) { // Read config from a file
      this.nodeName = args[0];
      xtl = readConfig(args[0]);
    }
    else if (args.length == 3) {
      this.nodeName = args[0];
      xtl = readSocketConfig(args[0], args[1], args[2]);
      nameServerName = args[1];
      nameServerPort = Short.parseShort(args[2]);
    } else {
      System.err.println("Usage: Node <config file>      OR");
      System.err.println("       Node <config name> <config server> <config port>");
      System.exit(0);
    }

    System.out.println("My description: " + ((NameTablePair)xtl.getTokenVect("description").firstElement()).name);

    // get plugin list
    Enumeration plugInList = xtl.getTokenVect("plugin").elements();

    plugIns = new Vector();
    // instantiate plugin objects
    while (plugInList.hasMoreElements()) {
      NameTablePair ntp = (NameTablePair)plugInList.nextElement();
      String classname = ntp.name.trim();
      try {
        Class clazz = Class.forName(classname);
        Plugin p = (Plugin)clazz.newInstance();
        plugIns.addElement(p);
        ((PluginAdapter)p).setParameters(ntp.table);
      } catch (ClassNotFoundException cnfe) {
        System.err.println("Plugin class not found: "+classname);
      } catch (IllegalAccessException iae) {
        System.err.println("Exception initializing Plugin: "+classname);
      } catch (InstantiationException ie) {
        System.err.println("Exception instantiating Plugin: "+classname);
      }
    }

    // Make the Distributor
    Distributor d = new Distributor(this);

    // Configure the messsage transport
    // If the port is specified as "0" I will re-use the socket that the client connected on
    // for messaging.   Otherwise, I assume he will open a server socket.
    int port = 7000;
    if (!xtl.getTokenVect("port").isEmpty()) {
      NameTablePair portPair = (NameTablePair)xtl.getTokenVect("port").firstElement();
      port = Integer.parseInt(portPair.name);
    }
    if (port == 0) { // conserve sockets
      mt = new ConservativeMessageTransport(nameServerInputStream, nameServerOutputStream, nodeName);
    } else {         // use a server socket
      mt = new ServerSocketMessageTransport(port, nodeName);
      try {
        if (nameServerSocket != null)
          nameServerSocket.close();  // don't need this one anymore if the client will use a server socket
      } catch (IOException ioe) {
        System.err.println("Error closing registration socket");
      }
    }

    d.setMessageTransport(mt);

    //
    // Add a MicroAgent for the name server
    //
    addMicroAgent(d, mom, nameServerName, nameServerPort, "Name Server");

    // get resource list
    Enumeration resourceList = xtl.getTokenVect("resource").elements();
    // instantiate, prime and execute resource objects
    while (resourceList.hasMoreElements()) {
      NameTablePair ntp = (NameTablePair)resourceList.nextElement();
      String classname = ntp.name;
      try {
        Class clazz = Class.forName(classname);
        Resource r = (Resource)clazz.newInstance();
        r.setParameters(ntp.table);
        r.setDistributor(d);
        d.openTransaction(Thread.currentThread());
        d.publishAdd(r);
        d.closeTransaction(Thread.currentThread());
      } catch (ClassNotFoundException cnfe) {
        System.err.println("Resource class not found: "+classname);
      } catch (IllegalAccessException iae) {
        System.err.println("Exception initializing Resource: "+classname);
      } catch (InstantiationException ie) {
        System.err.println("Exception instantiating Resource: "+classname);
      }
    }

    // initialize all of the Plugins
    plugInList = plugIns.elements();
    while (plugInList.hasMoreElements()) {
      PluginAdapter p = (PluginAdapter)plugInList.nextElement();
      p.setDistributor(d);
      d.openTransaction(Thread.currentThread());
      p.setupSubscriptions();
      d.closeTransaction(Thread.currentThread(), p);
    }

    // Start processing subscriptions
    d.cycle();
  }

  private void addMicroAgent(Distributor d, String name, String ipAddress, short port, String capabilities) {
    d.openTransaction(Thread.currentThread());
    MicroAgent mc = new MicroAgent();
    mc.setAgentId(new AgentId(name, ipAddress, port, capabilities));
    d.publishAdd(mc);
    d.closeTransaction(Thread.currentThread());
  }

  private static XMLTokenList xtl;
  private static Vector plugIns;

  private XMLTokenList readSocketConfig(String nodeName, String nameServerName, String port) {
    short nameServerPort = Short.parseShort(port);
    StringBuffer regRespMsg = new StringBuffer();
    int bite;

    System.out.println("me = " + nodeName +
                      ", mom = " + nameServerName +
                      ", herPort = "+nameServerPort);

    // register with the name server
    String regMsg = nodeName +
      ":<?xml version=\"1.0\"?><registration>\n"+
        "\t<name>"+nodeName+"</name>\n"+
      "</registration>\0";
    byte [] data = regMsg.getBytes();

    try {
      nameServerSocket = (SocketME)MicroEdition.getObjectME(Class.forName("org.cougaar.microedition.io.SocketME"));
    } catch (Exception e) {
      System.err.println("Unable to determine ME Socket type: " + e);
      System.exit(0);
    }

  boolean waiting = true;
  while (waiting) {
    try {
      nameServerSocket.open(nameServerName, nameServerPort);
      nameServerOutputStream = nameServerSocket.getOutputStream();
      nameServerOutputStream.write(data);
      nameServerOutputStream.flush();
    } catch (Exception e) {
      System.err.println("Unable to write register msg:\n" + regMsg + "\nto mom: " + nameServerName + ":" + nameServerPort + e);
      try{Thread.sleep(10000);}catch(Exception ex){}
      continue;
    }

    // get response from mom and parse it
    try {
      nameServerInputStream = nameServerSocket.getInputStream();
      while (true) {
        bite = nameServerInputStream.read();
        if (bite <= 0)
          break;
        regRespMsg.append((char)bite);
      }
    } catch (Exception e) {
      System.err.println("Unable to read regResp msg from mom: " + e);
      try{Thread.sleep(10000);}catch(Exception ex){}
      continue;
    }

    String regRespStr = regRespMsg.toString();
    int idx = regRespStr.indexOf(":");
    if (idx < 0) {
      System.err.println("ERROR: Malformed reg resp message (no source)" + regRespStr);
      try{Thread.sleep(10000);}catch(Exception ex){}
      continue;
    }
    mom = regRespStr.substring(0, idx);
    String msg = regRespStr.substring(idx+1);
    try {
      xtl = new XMLTokenList(msg);
      waiting = false;
    } catch (Exception e) {
      System.err.println("unable to parse regRespMsg:\n" + msg + "\nfrom '" + mom + "': " + nameServerName + ":" + nameServerPort + e);
      try{Thread.sleep(10000);}catch(Exception ex){}
      continue;
    }
    }

    // System.out.println("Got regRespMsg from '" + mom + "':\n" + msg);
    return xtl;
  }




  private XMLTokenList readConfig(String fileName) {

  boolean waiting = true;
  String msg = "";

  System.out.println("Reading config from local file: "+fileName);
  while (waiting) {
    try {
      FileLoader loader = (FileLoader)MicroEdition.getObjectME("org.cougaar.microedition.io.FileLoader");
      msg = loader.getFile(fileName);
      xtl = new XMLTokenList(msg);
      waiting = false;
    } catch (Exception e) {
      System.err.println("unable to parse regRespMsg:\n" + msg + "\nfrom '" + fileName + e);
      try{Thread.sleep(10000);}catch(Exception ex){}
      continue;
    }
    }

    return xtl;
  }

  private static Rebooter rebooter;
  public static void setRebooter(Rebooter reb) {
	  rebooter = reb;
  }
  public static void reboot() {
    if (rebooter == null) 
      System.out.println("ERROR: No rebooter configured");
    else {
      System.out.println("Reboot requested ... ");
      rebooter.reboot();
    }
  }
}
