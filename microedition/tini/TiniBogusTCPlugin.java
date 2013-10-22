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
import java.util.Vector;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.plugin.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.shared.Constants;

/**
 */
public class TiniBogusTCPlugin extends PluginAdapter
{
  final private static String myVerb=Constants.Robot.verbs[Constants.Robot.TARGETINGCONTROLLER];

  UnaryPredicate getPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroTask) {
          MicroTask mt = (MicroTask)o;
          return mt.getVerb().equals(myVerb) ;
        }
        return false;
      }
    };
    return myPred;
  }

  private Subscription taskSub;
  private long reportdelay = 1000; //msecs
  private long reportbearing = 0;
  private Thread pushthread = null;

  public void setupSubscriptions()
  {
    System.out.println("TiniBogusTCPlugin::setupSubscriptions");
    Hashtable t = getParameters();
    if(t != null)
    {
      if (t.get("ReportDelay") != null)
      {
	String pstr = (String)t.get("ReportDelay");
        Integer temp = new Integer(pstr);
        reportdelay = temp.longValue();
	System.out.println("TiniBogusTCPlugin:: Report Delay " +reportdelay);
      }
      if (t.get("TargetBearing") != null)
      {
	String pstr = (String)t.get("TargetBearing");
        Integer temp = new Integer(pstr);
        reportbearing = temp.longValue();
	System.out.println("TiniBogusTCPlugin:: Report Bearing " +reportbearing);
      }
    }
    taskSub = subscribe(getPred());
  }

  private synchronized void allocate(MicroTask mt) {
    long [] values = { reportbearing*1000, 0};

    int [] aspects = { Constants.Aspects.BEARING,
		       Constants.Aspects.SCANDIR};


    MicroAllocation ma = new MicroAllocation(null, mt);
    MicroAllocationResult mar = new MicroAllocationResult();
    ma.setReportedResult(mar);
    mar.setAspects(aspects);
    mar.setValues(values);
    System.out.println("Sending bogus target report: " +reportbearing);
    openTransaction();
    mt.setAllocation(ma);
    publishChange(mt);
    closeTransaction();
  }

  public void execute()
  {
    Enumeration enm = taskSub.getRemovedList().elements();
    while (enm.hasMoreElements())
    {
      MicroTask mt = (MicroTask)enm.nextElement();
      System.out.println("Got removed "+mt.getVerb()+" task");
      if(pushthread != null)
      {
	if(pushthread.isAlive())
	{
	  pushthread.interrupt();
	  pushthread = null;
	}
      }
    }

    enm = taskSub.getAddedList().elements();
    while (enm.hasMoreElements()) {
      MicroTask mt = (MicroTask)enm.nextElement();
      System.out.println("Bogus Targeting: started thread");

      for (Enumeration eprep = mt.getPrepositionalPhrases().elements();
                eprep.hasMoreElements(); )
      {
	MicroPrepositionalPhrase preps=(MicroPrepositionalPhrase)eprep.nextElement();
	if (preps!=null)
	{
              String prepIO=preps.getIndirectObject();
              String prep=preps.getPreposition();
	      System.out.println("Targeting Prepositions: " +prep +" " +prepIO);
        }
      }

      if(pushthread != null)
      {
	if(pushthread.isAlive())
	{
	  pushthread.interrupt();
	  pushthread = null;
	}
      }

      Pushout p=new Pushout(mt);
      pushthread = new Thread(p);
      pushthread.start();

    }

    enm = taskSub.getChangedList().elements();
    while (enm.hasMoreElements())
    {
      MicroTask mt = (MicroTask)enm.nextElement();
      System.out.println("Got changed "+mt.getVerb()+" task");
    }
  }

  class Pushout implements Runnable
  {
    public Pushout(MicroTask m)
    {
      mt = m;
    }

    private MicroTask mt;

    public void run()
    {
       try
       {
	Thread.sleep(reportdelay);
	allocate(mt);
       }
       catch (Exception e)
       {
	  System.out.println("Pushout thread interrupted");
       }
    }
  }
}
