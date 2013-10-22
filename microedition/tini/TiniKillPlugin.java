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

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.plugin.PluginAdapter;
import org.cougaar.microedition.asset.FlashlightResource;

/**
 * Plugin to control a flashlight.
 */
public class TiniKillPlugin extends PluginAdapter {

  public void setupSubscriptions()
  {
    System.out.println("TiniKillPlugin is ready (Ctrl-J to terminate)....");

    Thread killt = new Thread(new WaitForDeath());
    killt.start();

  }

  public void execute()
  {

    System.err.println("TiniFlashlightControlPlugin: execute");

  }

  class WaitForDeath implements Runnable
  {
    public WaitForDeath()
    {
      System.out.println("WaitForDeath class instance created");
    }

    public void run()
    {
      int val = 0;

      while (true)
      {
	try
	{
	  val = System.in.read();
	}
	catch (Exception ex)
	{
	  System.out.println("TiniKillPlugin: Unable to read in byte");
	}

	if(val == 10)
	{
	  System.out.println("Kill Signal Received...");
	  System.exit(0);
	}
      }
    }
  }
}
