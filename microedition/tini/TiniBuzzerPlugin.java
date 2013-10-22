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
import java.text.*;
import com.systronix.sbx2.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.plugin.PluginAdapter;
import org.cougaar.microedition.asset.FlashlightResource;

/**
 * Plugin to control a flashlight.
 */
public class TiniBuzzerPlugin extends PluginAdapter
{

  private Subscription timeAlloc;
  private Subscription buzzTask;

  private boolean launchset = false;
  private boolean timeofarrivalset = false;
  private boolean targetrangeset = false;
  private long launchtime = 0;
  private long timeofarrival = 0;
  private long currenttimehack = 0;
  private int buzztime = 1000;
  private double targetrange = 0.0; //meters
  private double weaponspeed = 1.0; //meters per second

  UnaryPredicate getTaskPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroTask) {
          MicroTask mt = (MicroTask)o;
          return mt.getVerb().equals(Constants.Robot.verbs[Constants.Robot.ENGAGEWEAPON]);
        }
        return false;
      }
    };
    return myPred;
  }

  UnaryPredicate getTimeAllocation()
  {
    UnaryPredicate myPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof MicroAllocation)
	{
          MicroTask mt = ((MicroAllocation)o).getTask();
          return mt.getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTPOSITION]);
        }
        return false;
      }
    };
    return myPred;
  }

  public void setupSubscriptions()
  {
    System.out.println("TiniBuzzerPlugin: setupSubscriptions");
    if (getParameters() != null)
    {
      Hashtable t = getParameters();
      if (t.containsKey("buzztime"))
        buzztime = Integer.parseInt((String)t.get("buzztime"));
      if (t.get("weaponspeed") != null)
      {
	String pstr = (String)t.get("weaponspeed");
        Double temp = new Double(pstr);
        weaponspeed = temp.doubleValue();
      }
    }

    buzzTask = subscribe(getTaskPred());
    timeAlloc = subscribe(getTimeAllocation());
  }

  public void execute()
  {
    Enumeration enm = buzzTask.getAddedList().elements();
    while (enm.hasMoreElements())
    {
      MicroTask mt = (MicroTask)enm.nextElement();
      //System.out.println("TiniBuzzerPlugin: got added task " +mt);
      if (mt.getPrepositionalPhrases() != null)
      {
	System.out.println("TiniBuzzerPlugin: examining prepositional phrases");
	for (Enumeration enmprep = mt.getPrepositionalPhrases().elements();
	      enmprep.hasMoreElements(); )
	{
	  MicroPrepositionalPhrase preps=(MicroPrepositionalPhrase)enmprep.nextElement();

	  if (preps!=null)
	  {
	    if(preps.getPreposition().equals(Constants.Robot.prepositions[Constants.Robot.TIMEPREP]))
	    {
	      String strlaunchtime = preps.getIndirectObject();
	      Long temp = new Long(strlaunchtime);
              timeofarrival = temp.longValue();
	      timeofarrivalset = true;
	      System.out.println("Weapon Arrival Time: " +timeofarrival);
	    }
	    if(preps.getPreposition().equals(Constants.Robot.prepositions[Constants.Robot.RANGEPREP]))
	    {
	      String strrange = preps.getIndirectObject();
	      Double temp = new Double(strrange);
              targetrange = temp.doubleValue();
	      targetrangeset = true;
	      System.out.println("Target Range (meters): " +targetrange);
	    }
	  }
	  else
	  {
	    System.err.println("No prepositions found with task.");
	  }
        } // end-for
      }

      if(targetrangeset == true && timeofarrivalset == true)
      {
	launchtime = timeofarrival - (long)(targetrange/weaponspeed) * 1000;
        launchset = true;
	System.out.println("Launch time : " +launchtime);
	System.out.println("Current time: " +currenttimehack);
	System.out.println("Delay       :"  +((launchtime - currenttimehack)/1000));
      }
    }

    enm = timeAlloc.getChangedList().elements();
    while (enm.hasMoreElements())
    {
      MicroAllocation ma = (MicroAllocation)enm.nextElement();
      MicroAllocationResult mar = ma.getReportedResult();
      if (mar == null)
        continue;

      int [] aspects = mar.getAspects();
      long [] values = mar.getValues();

      for (int i=0; i< aspects.length; i++)
      {
	if(aspects[i] == Constants.Aspects.TIME)
	{
	  currenttimehack = values[i];
          //System.out.println("TiniBuzzer: time alloc " +currenttimehack);
	  if(launchset == true)
	  {
	    if(currenttimehack >= launchtime)
	    {
	      soundBuzzer();
	      launchset = false;
	    }
	  }
	}
      }
    }
  }

  private void soundBuzzer()
  {
    System.out.println("Buzzer on");

    Misc.setIntBuzzer(true);

    try
    {
      Thread.sleep(buzztime);
    }
    catch (Exception ex){}

    Misc.setIntBuzzer(false);

    System.out.println("Buzzer off");
  }
}
