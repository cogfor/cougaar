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
public class FileServerPlugin extends SocketServerPlugin
{
  private String filename = null;

  public void setupSubscriptions()
  {
    super.setupSubscriptions();

    if (getParameters() != null)
    {
      Hashtable t = getParameters();
      if (t.containsKey("filename"))
      {
        filename = (String)t.get("filename");
	System.out.println("FileServerPlugin filename is " +filename);
      }
    }
  }

  public void ConnectionEstablished(Socket s)
  {
    System.out.println("FileServerPlugin::ConnectionEstablished");
    FileInputStream fimage;
    DataOutputStream dataout = null;

    try
    {
      dataout = new DataOutputStream(s.getOutputStream());
    }
    catch (Exception e)
    {
      System.out.println("FileServerPlugin:: Unable to create DataOutputStream");
      return;
    }

    int nbytes = 0;
    int nread = 0;
    int nwrite = 0;

    try
    {
      fimage = new FileInputStream(filename);
    }
    catch(FileNotFoundException fnfe)
    {
      System.out.println(filename+" not found");

      try
      {
	dataout.writeInt(nbytes);
	s.close();
      }
      catch (Exception e) {}
      return;
    }



    try
    {
      nbytes = fimage.available();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.err.println("Error reading available bytes.");
      nbytes = 0;
    }

    if(nbytes > 0)
    {

      System.out.println("FileServerPlugin  bytes in file = "+nbytes);

      byte [] image = new byte[nbytes];

      try
      {
        nread = fimage.read(image);
      }
      catch (Exception e)
      {
	e.printStackTrace();
        System.err.println("Error reading image data.");
	nread = 0;
      }

      if(nread == nbytes)
      {
	System.out.println("Sending image data over socket...");

	try
	{
	  dataout.writeInt(nbytes);
	  dataout.write(image);
	  dataout.flush();
	}
	catch (Exception e)
	{
	  System.out.println("Error in writing image data.");
	}
      }
      else
      {
	try { dataout.writeInt(0); } catch (Exception e) {}
	System.out.println("Error: Could not read nbytes = "+nbytes+" nread = "+nread);
      }
    }
    else
    {
      try { dataout.writeInt(0); } catch (Exception e) {}
      System.out.println("Zero bytes in file " +filename);
    }

    try
    {
      s.close();
    }

    catch (Exception e) {}

  }
}
