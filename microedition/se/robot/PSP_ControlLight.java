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
package org.cougaar.microedition.se.robot;

import org.cougaar.util.UnaryPredicate;
import java.io.*;
import java.util.*;
import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;
import org.cougaar.lib.planserver.RuntimePSPException;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.microedition.shared.Constants;

import org.cougaar.core.blackboard.IncrementalSubscription;

/**
 * This PSP is meant for the PC 104 board which controls only a single TiniFlashlight
 */
public class PSP_ControlLight extends PSP_BaseAdapter
  implements PlanServiceProvider, UISubscriber
{
  /** A zero-argument constructor is required for dynamically loaded PSPs,
   *         required by Class.newInstance()
   **/
  public PSP_ControlLight()
  {
    super();
  }

  /**
   * This constructor includes the URL path as arguments
   */
  public PSP_ControlLight( String pkg, String id ) throws RuntimePSPException
  {
    setResourceLocation(pkg, id);
  }

  /**
   * Some PSPs can respond to queries -- URLs that start with "?"
   * I don't respond to queries
   */
  public boolean test(HttpInput query_parameters, PlanServiceContext sc)
  {
    super.initializeTest(); // IF subclass off of PSP_BaseAdapter.java
    return false;  // This PSP is only accessed by direct reference.
  }

  static int count=0;

  /**
   * Called when a HTTP request is made of this PSP.
   * @param out data stream back to the caller.
   * @param query_parameters tell me what to do.
   * @param psc information about the caller.
   * @param psu unused.
   */
  public void execute( PrintStream out,
                       HttpInput query_parameters,
                       PlanServiceContext psc,
                       PlanServiceUtilities psu ) throws Exception
  {
    boolean wantOn=false;
    try {
      String onParam="on";
      String onText="false";
      String verbText="ControlFlashlight";

      System.out.println("PSP_ControlLight called from " + psc.getSessionAddress());

      if( query_parameters.existsParameter(onParam) )
      {
         onText = (String) query_parameters.getFirstParameterToken(onParam, '=');
         System.out.println("Input "+onParam+" parm for onText: ["+onText+"]");
         wantOn=onText.equalsIgnoreCase("true");
         System.out.println("want on is "+wantOn);

         RootFactory theLDMF = psc.getServerPluginSupport().getFactoryForPSP();

         NewTask t = theLDMF.newTask();
         t.setPlan(theLDMF.getRealityPlan());
         t.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.CONTROLFLASHLIGHT]));


         psc.getServerPluginSupport().openLogPlanTransaction();
         NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
         npp.setPreposition("LightingMode");
         npp.setIndirectObject("toggle"); // turn on for now
         t.setPrepositionalPhrase((PrepositionalPhrase)npp);
         psc.getServerPluginSupport().closeLogPlanTransaction();

         psc.getServerPluginSupport().publishAddForSubscriber(t);


      }


      /*
      if( query_parameters.existsParameter(onParam) )
      {
         onText = (String) query_parameters.getFirstParameterToken(onParam, '=');
         System.out.println("Input "+onParam+" parm for onText: ["+onText+"]");
         wantOn=onText.equalsIgnoreCase("true");
         System.out.println("wanton is "+wantOn);
      }
      if (wantOn) {
        RootFactory theLDMF = psc.getServerPluginSupport().getFactoryForPSP();
        Task task=createTask(theLDMF, verbText);
        psc.getServerPluginSupport().publishAddForSubscriber(task);
        count++;
        createOutputPage("Added", task, out);
      } else {
        IncrementalSubscription subscription = null;
        final String myVerbText=verbText;
        UnaryPredicate taskPred = new UnaryPredicate() {
          public boolean execute(Object o) {
            boolean ret=false;
            if (o instanceof Task) {
              Task mt = (Task)o;
              ret= (mt.getVerb().equals(myVerbText));
            }
            return ret;
          }
        };

        subscription = (IncrementalSubscription)psc
          .getServerPluginSupport().subscribe(this, taskPred);
        Iterator iter = subscription.getCollection().iterator();
        if (iter.hasNext()) {
          Task task=null;
          int removedCount=0;
          while (iter.hasNext()) {
            task = (Task)iter.next();
            psc.getServerPluginSupport().publishRemoveForSubscriber(task);
            count--;
            removedCount++;
          }
          createOutputPage("Removed "+removedCount+" ", task, out);
        } else {
          createOutputPage("No More Tasks to be removed with "+verbText+" verb", null, out);
        }
      }
      */
    } catch (Exception ex) {
      out.println(ex.getMessage());
      ex.printStackTrace(out);
      System.out.println(ex);
      out.flush();
    }
  }

  private Task createTask(RootFactory theLDMF, String verbText) {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(verbText));
    return t;
  }

  /**
   * Print HTML that shows info about the task
   */
  private void createOutputPage(String addOrRem, Task task, PrintStream out) {
      // dump classnames and count to output stream
      out.println("<html><head></head><body>");
      if (task!=null) {
        out.println(addOrRem+" Task(s): "+task.getVerb()+"<br>");
      } else {
        out.println(addOrRem+"<br>");
      }
      out.println("Time: "+new Date()+"<br>");
      out.println("Count: "+count+"<br>");
      out.println("</body></html>");
      out.flush();
  }

  /**
   * A PSP can output either HTML or XML (for now).  The server
   * should be able to ask and find out what type it is.
   **/
  public boolean returnsXML() {
    return false;
  }

  public boolean returnsHTML() {
    return true;
  }

  /**  Any PlanServiceProvider must be able to provide DTD of its
   *  output IFF it is an XML PSP... ie.  returnsXML() == true;
   *  or return null
   **/
  public String getDTD()  {
    return null;
  }

  /**
   * The UISubscriber interface. (not needed)
   */
  public void subscriptionChanged(Subscription subscription) {
  }
}

