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
import java.io.*;

/**
 * This class handles listening on a server socket port in a second thread.
 * It should probably be refactored into one for server sockets and one for
 * input/output streams.
 */
public class PacketReader {

/**
 * This variable declares the InputStream to read from.
 */
  private InputStream bufr = null;

/**
 * This variable declares the Thread.
 */
  private Thread runner = null;

/**
 * This variable declares the private object that provides the run method.
 */
  private Retriever retriever = new Retriever();

/**
 * This variable stores the sender object, what to do with the stuff once we read it.
 */
  private MessageTransport deliverer = null;

/**
 * This variable holds the value of the server socket port on which I am to listen.
 */
  private int myListenPort;

/**
 * This private class provides the run method.
 */
  private class Retriever implements Runnable {

    public void run () {

      StringBuffer msg = null;
      int bite;

      try {
        ServerSocketME ss = null;
        if (bufr == null) {
          ss = (ServerSocketME) MicroEdition.getObjectME(Class.forName("org.cougaar.microedition.io.ServerSocketME"));
          ss.open(myListenPort);
          System.out.println("Listening on " + myListenPort);
        } else {
          try {Thread.sleep(3000);}catch (InterruptedException ie){}
          System.out.println("Using registration message stream");
        }

        while (true) {
          try {
            SocketME socketme = null;
            if (ss != null) {
              //System.err.println("PacketReader --> enter accept  ***");
	      socketme = ss.accept();
              //System.err.println("PacketReader --> finish accept ---");
              bufr = socketme.getInputStream();
            }
            msg = readMessage(bufr);
            if (ss != null) {
              bufr.close();
              socketme.close();
            }
            deliverMessage(msg);
          } catch (Exception ex) {
            System.err.println("SocketException:"+ex);
            if (ss == null) {
              System.out.println("Shutting down connection");
	      org.cougaar.microedition.node.Node.reboot();
              return;
            }
          }
        }
      } catch (ClassNotFoundException cnfe) {
        System.err.println("Error configuring message recv: ClassNotFoundException");
        cnfe.printStackTrace();
      } catch (IllegalAccessException iae) {
        System.err.println("Error configuring message recv: IllegalAccessException");
        iae.printStackTrace();
      } catch (IOException ioe) {
        System.err.println("Error configuring message recv: IOException");
        ioe.printStackTrace();
      }
    }
  }

  private String getSource(String msg) {
    return msg.substring(0, msg.indexOf(":"));
  }

  private String getMessage(String msg) {
    return msg.substring(msg.indexOf(":")+1);
  }

  /**
   * This constructor takes the server socket port and saves it in an object variable.
   *
   * @param   port    server socket port on which I am to listen
   * @return  none
   */
  public PacketReader (int port) {
    myListenPort = port;
  }

  /**
   * This constructor takes the InputStream object and saves it in an object variable.
   *
   * @param   port    server socket port on which I am to listen
   * @return  none
   */
  public PacketReader (InputStream in) {
    this.bufr = in;
  }

  /**
   * This method saves the packet handler object.
   *
   * @param   ps    the MessageTransport object that will do something with the incoming message.
   * @return  none
   */
  public void setMessageTransport (MessageTransport ps) {
    deliverer = ps;
  }

  /**
   * This method startes the runner thread.
   *
   * @param   none
   * @return  none
   */
  public void start () {
    if (runner == null || !runner.isAlive()) {
      runner = new Thread(retriever);
      runner.start();
    }
  }

  private StringBuffer readMessage(InputStream in) throws IOException {
    StringBuffer msg = new StringBuffer();
    int bite;
    while (true) {
      bite = bufr.read();
      if (bite <= 0)
        break;
      msg.append((char)bite);
    }
    return msg;
  }

  private void deliverMessage(StringBuffer msg) {
    if (msg.length() > 0) {
      String message = msg.toString();
      String source = getSource(message);
      deliverer.takePacket(getMessage(message), source);
    }
  }
}
