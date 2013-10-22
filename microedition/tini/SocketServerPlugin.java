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
package org.cougaar.microedition.tini;

import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.plugin.*;


/**
 */
public class SocketServerPlugin extends PluginAdapter
{

  private int port = 1230;
  private boolean debugging = false;

  public void setupSubscriptions()
  {
    System.out.println("SocketServerPlugin::setupSubscriptions");
    if (getParameters() != null)
    {
      Hashtable t = getParameters();
      if (t.containsKey("debug"))
        debugging = true;
      if (t.containsKey("port"))
        port = Integer.parseInt((String)t.get("port"));
    }
    else
    {
      System.out.println("SocketServerPlugin: setupSubscriptions No Parameters specified");
    }

    try
    {
      ServerSocket socket = new ServerSocket(port);

      if(socket != null)
      {
	Thread ls = new Thread(new ListenOnSocket(socket));
	ls.start();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void execute()
  {
  }

  public void ConnectionEstablished(Socket s)
  {
    System.out.println("SocketServerPlugin::ConnectionEstablished");
  }

  class ListenOnSocket implements Runnable
  {

    private ServerSocket server = null;

    public ListenOnSocket(ServerSocket ss)
    {
      server = ss;
    }

    public void run()
    {
      System.out.println("ListenOnSocket: spawned");

      while(true)
      {
	System.out.println("ListenOnSocket: waiting for accept");

	try
	{
	  Socket socket = server.accept();

	  System.out.println("ListenOnSocket: socket connection made");

	  ConnectionEstablished(socket);
	}
	catch (Exception e)
	{
	  e.printStackTrace();
	}
      }
    }
  }
}
