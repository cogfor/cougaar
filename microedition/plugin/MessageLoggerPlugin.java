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

import java.io.*;
import java.util.*;

import org.cougaar.microedition.io.*;
import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;


/**
 */
public class MessageLoggerPlugin extends PluginAdapter implements OutgoingMessageListener
{

  String logfileName = "_MsgLog.xml";
  String protocol = "file";
  String hostname = "localhost";
  FileLoader filedumper = null;
  short port = 0;
  private boolean debugging = false;

  public void setupSubscriptions()
  {
    System.out.println("MessageLoggerPlugin::setupSubscriptions");
    if (getParameters() != null) {
      Hashtable t = getParameters();
      if (t.containsKey("debug"))
        debugging = true;

      if (t.containsKey("logfile"))
        logfileName = (String)t.get("logfile");
      if (t.containsKey("protocol"))
        protocol = (String)t.get("protocol");
      if (t.containsKey("hostname"))
        hostname = (String)t.get("hostname");
      if (t.containsKey("port"))
        port = Short.parseShort((String)t.get("port"));
    }
    else
    {
      System.out.println("ControlPlugin: setupSubscriptions No Parameters specified");
    }

    getDistributor().getMessageTransport().addMessageListener(this);

    filedumper = (FileLoader)MicroEdition.getObjectME("org.cougaar.microedition.io.FileLoader");
    filedumper.configure(protocol,hostname,port);
    System.out.println("MessageLoggerPlugin logfile name: " +filedumper.showConfig()+logfileName);

    // write header
    try
    {
      filedumper.sendFile(logfileName, "<?xml version=\"1.0\"?>\r\n" +
				       " <messageLog>\r\n");
    }
    catch(Exception e)
    {
       System.out.println("Exception caught: unable to write header to log");
    }
  }


  // problem with this call is that there's no place to call it from
  private void closeLog()
  {
    try
    {
        filedumper.sendFile(logfileName, " </messageLog>\r\n");
    }
    catch(Exception e)
    {
       System.out.println("Exception caught: unable to write trailer to log");
    }
  }

  public void execute()
  {

  }

  public void outgoingMessage(String data, String destination)
  {
    //System.out.println("outgoingMessage to " +destination+":");

    try
    {


      String dirstring = "send";
      Date currentdate = new Date();
      filedumper.sendFile(logfileName, "  <message time="+currentdate.getTime()+" direction="+dirstring+" peer="+destination+">\r\n" +
				       "    " + data + "\r\n" +
				       "  </message>\r\n");
    }
    catch(Exception e)
    {
      System.out.println("Exception caught: unable to write message to log");
    }
  }

  public void deliverMessage(String data, String source)
  {
    //System.out.println("Ignore delivered message");
  }

}
