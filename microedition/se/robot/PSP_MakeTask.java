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
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.microedition.shared.Constants;

import org.cougaar.core.blackboard.IncrementalSubscription;

/**
 */
public class PSP_MakeTask extends PSP_BaseAdapter
  implements PlanServiceProvider, UISubscriber
{
  /** A zero-argument constructor is required for dynamically loaded PSPs,
   *         required by Class.newInstance()
   **/
  public PSP_MakeTask()
  {
    super();
  }

  /**
   * This constructor includes the URL path as arguments
   */
  public PSP_MakeTask( String pkg, String id ) throws RuntimePSPException
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
    try {
      String verbParam="verb";
      String verbText="ControlFlashlight";
      String actionParam="action";
      String actionText="make";
      String speedParam="speed";
      String speedText=null;
      String velocityParam="velocity";
      String velocityText=null;
      String degreesParam="degrees";
      String degreesText=null;
      String hemisText=null;
      String hemisParam=Constants.Robot.prepositions[Constants.Robot.TURRETDIRECTIONPREP]; // "TurretHemisphere"
      String hemisphere=null;

      System.out.println("PSP_MakeTask called from " + psc.getSessionAddress()+" and count is "+count);


      if( query_parameters.existsParameter(verbParam) )
      {
         verbText = (String) query_parameters.getFirstParameterToken(verbParam, '=');
         System.out.println("Input "+verbParam+" parm for verbText: "+verbText);
      }
      if( query_parameters.existsParameter(actionParam) )
      {
         actionText = (String) query_parameters.getFirstParameterToken(actionParam, '=');
         System.out.println("Input "+actionParam+" parm for actionText: "+actionText);
      }
      System.out.println("PSP_MakeTask with verb= " + verbText + " action = "+actionText);
      if( query_parameters.existsParameter(speedParam) )
      {
         speedText = (String) query_parameters.getFirstParameterToken(speedParam, '=');
         System.out.println("Input "+speedParam+" parm for value: "+speedText);
      }
      if( query_parameters.existsParameter(velocityParam) )
      {
         velocityText = (String) query_parameters.getFirstParameterToken(velocityParam, '=');
         System.out.println("Input "+velocityParam+" parm for value: "+velocityText);
      }
      if( query_parameters.existsParameter(degreesParam) )
      {
         degreesText = (String) query_parameters.getFirstParameterToken(degreesParam, '=');
         System.out.println("Input "+degreesParam+" parm for value: "+degreesText);
      }
      if( query_parameters.existsParameter(hemisParam) )
      {
         hemisText = (String) query_parameters.getFirstParameterToken(hemisParam, '=');
         System.out.println("Input "+hemisParam+" parm for value: "+hemisText);
         if (hemisText.equalsIgnoreCase("middle")) hemisphere= ""+Constants.Robot.TURRET_MIDDLE ;
         if (hemisText.equalsIgnoreCase("left")) hemisphere= ""+Constants.Robot.TURRET_LEFT ;
         if (hemisText.equalsIgnoreCase("right")) hemisphere= ""+Constants.Robot.TURRET_RIGHT ;
      }

      // create task
      if (!"remove".equalsIgnoreCase(actionText)) {
        RootFactory theLDMF = psc.getServerPluginSupport().getFactoryForPSP();
        Task task=createTask(theLDMF, verbText);
        addPreposition(theLDMF, (NewTask)task, speedParam, speedText);
        addPreposition(theLDMF, (NewTask)task, velocityParam, velocityText);
        addPreposition(theLDMF, (NewTask)task, degreesParam, degreesText);
        addPreposition(theLDMF, (NewTask)task, hemisParam, hemisphere);

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
          Task task = (Task)iter.next();
          psc.getServerPluginSupport().publishRemoveForSubscriber(task);
          count--;
          createOutputPage("Removed", task, out);
        } else {
          createOutputPage("No More Tasks to be removed with "+verbText+" verb", null, out);
        }
      }
    } catch (Exception ex) {
      out.println(ex.getMessage());
      ex.printStackTrace(out);
      System.out.println(ex);
      out.flush();
    }
  }

  private void addPreposition(RootFactory theLDMF, NewTask t, String prep, String val) {
    if (prep==null || val==null) return ;
    NewPrepositionalPhrase npp= theLDMF.newPrepositionalPhrase();
    npp.setPreposition(prep);
    npp.setIndirectObject(val);
    t.setPrepositionalPhrase(npp);
  }

//  private Task createTask(RootFactory theLDMF, String verbText, String prep, String val) {
//    NewTask t = theLDMF.newTask();
//    t.setPlan(theLDMF.getRealityPlan());
//    t.setVerb(Verb.getVerb(verbText));
//
//    if (prep==null || val==null) return t;
//    NewPrepositionalPhrase npp= theLDMF.newPrepositionalPhrase();
//    npp.setPreposition(prep);
//    npp.setIndirectObject(val);
//    t.setPrepositionalPhrase(npp);
//    return t;
//  }

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
        out.println(addOrRem+" Task: "+task.getVerb()+"<br>");
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

