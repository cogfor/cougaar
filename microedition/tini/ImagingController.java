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

import java.util.*;
import java.io.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.plugin.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.shared.Constants;

public class ImagingController extends ControllerResource
{

  private String commandstring = "/root/bin/cam -s 1 -Q 3 -z 0 -t /root/bin/photo.jpg";

  public void modifyControl(String controlparameter, String controlparametervalue)
  {

  }

  public void setUnits(String u) {}
  public void setChan(int c) {}

  private boolean isStarted = false;

  public boolean isUnderControl()
  {
    return isStarted;
  }

  public void startControl()
  {
    //take picture
    if(jruntime != null)
    {
      try
      {
	System.out.println("ImagingController: start Control...");
        Process proc = jruntime.exec(commandstring);
        String line = null;

	DataInputStream ls_in = new DataInputStream(proc.getErrorStream());
        while ((line = ls_in.readLine()) != null)
	{
            System.out.println(line);
	}

        ls_in = new DataInputStream(proc.getInputStream());
        while ((line = ls_in.readLine()) != null)
	{
            System.out.println(line);
	}

	int exitVal = proc.waitFor();
        System.out.println("Process exitValue: " + exitVal);
      }
      catch (Exception e)
      {
	System.err.println("ImagingController: Unable to execute runtime environment!!!");
	e.printStackTrace();
      }
    }
    else
    {
      System.err.println("ImagingController: jruntime is null!");
    }

    isStarted = true;
  }

  public void stopControl()
  {
    isStarted = false;
  }

  public void getValues(long [] values)
  {

  }

  public void getValueAspects(int [] aspects)
  {

  }

  public int getNumberAspects()
  {
    return 0;
  }

  public boolean getSuccess()
  {
    return true;
  }

  public boolean conditionChanged()
  {
    return false;
  }

  /**
   * Constructor.  Sets name default.
   */
  public ImagingController() {}

  /**
   * Set parameters with values from my node and initialize resource.
   */
  private Runtime jruntime = null;

  public void setParameters(java.util.Hashtable t)
  {
    setName("ImagingController");
    jruntime = Runtime.getRuntime();
    if(jruntime != null)
    {
      try
      {
	System.out.println("ImagingController: runtime exec...");
        Process proc = jruntime.exec("/bin/echo Runtime object testing...");
        String line = null;

	DataInputStream ls_in = new DataInputStream(proc.getErrorStream());
        while ((line = ls_in.readLine()) != null)
	{
            System.out.println(line);
	}

        ls_in = new DataInputStream(proc.getInputStream());
        while ((line = ls_in.readLine()) != null)
	{
            System.out.println(line);
	}

	int exitVal = proc.waitFor();

      }
      catch (Exception e)
      {
	e.printStackTrace();
	System.err.println("ImagingController: Unable to execute runtime environment!!!");
      }
    }
    else
    {
      System.err.println("ImagingController: Unable to retrieve runtime environment!!!");
    }
  }
}
