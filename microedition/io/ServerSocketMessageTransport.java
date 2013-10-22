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

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;
import java.io.*;
import java.util.*;

/**
 * This class handles sending messages to another processes server socket,
 * it also kicks off a thread which establishes this processes listening/server socket.
 */
public class ServerSocketMessageTransport implements MessageTransport {

/**
 * This variable declares the reader class whose thread listens on a serversocket port.
 */
  private PacketReader reader = null;

  private String nodeName = null;
/**
 * This constructor starts a listening/reader thread as well as instantiates a sender object.
 *
 * @param   myListenPort    the port number the reader thread should listen on.
 */
  public ServerSocketMessageTransport (int myListenPort, String name) {
    reader = new PacketReader(myListenPort);
    reader.setMessageTransport(this);
    reader.start();
    nodeName = name;
  }

/**
   * This method sends a message to the desired server on a particular port.
   *
   * @param   server    a string that represents the host name or ip address of the recepient.
   * @param   port      an int representing the port number the recepient is listening on.
   * @param   message   a string representing the message to be sent.
   * @return  none
   */
protected void sendMessage(String server, int port, String message)
throws IOException {
	
  boolean done = false;
  int retryCount = 0;
  while (!done && retryCount < 3) {
      SocketME sock=null;
    try {
      sock = (SocketME)MicroEdition.getObjectME(Class.forName("org.cougaar.microedition.io.SocketME"));
      sock.open(server, port);
      OutputStream os = sock.getOutputStream();
      byte [] data = message.getBytes();
      os.write(data);
      os.flush();
      os.close();
      sock.close();
      done = true;
    } catch (ClassNotFoundException cfe) {
			System.err.println("SocketME class not found\n");
			break;
    } catch (IOException e) {
      System.err.println("Unable to sendMessage " + e);
      if (sock != null) sock.close();
      if (retryCount == 2) {
        org.cougaar.microedition.node.Node.reboot(); // reboot if possible
        throw e;                        // otherwise rethrow
			}
    }
    if (!done) {
      System.gc();
      if (retryCount++ < 3)
        System.err.println("ServerSocketMessageTransport.sendMessage: retrying");
      else
        System.err.println("ServerSocketMessageTransport.sendMessage: failed.");
      
      try {Thread.sleep(3000);} catch (InterruptedException ie){}
    }
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

  public void sendMessage(Encodable msg, MicroAgent dest, String op) throws IOException
    {
    StringBuffer buf = new StringBuffer();
    buf.append(nodeName + ":");
    buf.append(msg.xmlPreamble);
    buf.append("<message op=\""+op+"\">");
    msg.encode(buf);
    buf.append("</message>");
    buf.append('\0');
    String ipAddress = dest.getAgentId().getIpAddress();
    short port = dest.getAgentId().getPort();
//    System.out.println("Sending: "+buf.toString()+" to "+ipAddress);

    notifyListeners(buf.toString(), dest.getAgentId().getName());

    sendMessage(ipAddress, port, buf.toString());
  }

/**
   * This main is for testing
   *
   * @param   args    array of 3 strings, my port, remote host remote port
   * @return  none

  public static void main (String[] argv) {
    if (argv.length != 3)
      System.exit(0);

    int j = 1;
    int myListenPort = Integer.parseInt(argv[0]);
    String host = argv[1];
    int port = Integer.parseInt(argv[2]);
    MessageTransport m = new MessageTransport(myListenPort);

    for (;;) {
      try {Thread.sleep(5000);} catch (Exception e) {}
      String outgoing = new String("Message #" + j++);
      System.out.println("Sending: " + outgoing);
      m.sendMessage(host, port, outgoing);
    }
  }
 */
}
