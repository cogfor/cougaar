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

import java.io.*;
import java.util.*;

import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;

/**
 * This message transport strategy uses the same socket that the client originally connected
 * on to talk to it.
 */
public class ConservativeMessageTransport implements MessageTransport {

/**
 * This variable declares the reader class whose thread listens on a serversocket port.
 */
  private PacketReader reader = null;

  private InputStream in;
  private OutputStream out;

  private String nodeName = null;
/**
 * This constructor starts a listening/reader thread as well as instantiates a sender object.
 *
 * @param   myListenPort    the port number the reader thread should listen on.
 */
  public ConservativeMessageTransport (InputStream in, OutputStream out, String name) {
    this.in = in;
    this.out = out;
    reader = new PacketReader(in);
    reader.setMessageTransport(this);
    reader.start();
    nodeName = name;
  }

/**
   * This method sends a message to the desired server on a particular port.
   *
   * @param   message   a string representing the message to be sent.
   * @return  none
   */
  protected void sendMessage(String message) {

    try {
      byte [] data = message.getBytes();
      out.write(data);
      out.flush();
    } catch (Exception e) {
      System.err.println("Unable to sendMessage " + e + " Try to reboot...");
      org.cougaar.microedition.node.Node.reboot();
    }
  }

  Vector listeners = new Vector();

  public void takePacket(String data, String source) {
    Enumeration en = listeners.elements();
    while (en.hasMoreElements()) {
      MessageListener ml = (MessageListener)en.nextElement();
      ml.deliverMessage(data, source);
    }

  }

  public void notifyListeners(String data, String dest)
  {
    Enumeration en = listeners.elements();
    while (en.hasMoreElements())
    {
      Object obj = en.nextElement();
      if(obj instanceof OutgoingMessageListener)
      {
	OutgoingMessageListener oml = (OutgoingMessageListener)obj;
        oml.outgoingMessage(data, dest);
      }
    }
  }

  public void addMessageListener(MessageListener ml) {
    if (!listeners.contains(ml))  {
      listeners.addElement(ml);
    }
  }

  public void removeMessageListener(MessageListener ml) {
    if (listeners.contains(ml))  {
      listeners.removeElement(ml);
    }
  }

  public void sendMessage(Encodable msg, MicroAgent dest, String op) throws IOException {
    StringBuffer buf = new StringBuffer();
    buf.append(nodeName + ":");
    buf.append(msg.xmlPreamble);
    buf.append("<message op=\""+op+"\">");
    msg.encode(buf);
    buf.append("</message>");
    buf.append('\0');

//    System.out.println("Sending: "+buf.toString()+" to "+ipAddress);

    notifyListeners(buf.toString(), dest.getAgentId().getName());

    sendMessage(buf.toString());
  }



}
