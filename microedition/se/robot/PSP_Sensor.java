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
import org.cougaar.core.blackboard.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.lib.planserver.*;
import java.io.*;
import java.util.*;



/**
 */
class GetSensorTempPredicate implements UnaryPredicate {
  public boolean execute(Object o) {
    if ( o instanceof Allocation ) {
      Allocation a = (Allocation) o;
      return a.getTask().getVerb().equals("Measure") &&
         (a.getTask().getPrepositionalPhrase(PSP_Sensor.sensorTempName) != null);
    }
    return false;
  }
}

class GetSensorBrightPredicate implements UnaryPredicate {
  public boolean execute(Object o) {
    if ( o instanceof Allocation ) {
      Allocation a = (Allocation) o;
      return a.getTask().getVerb().equals("Measure") &&
         (a.getTask().getPrepositionalPhrase(PSP_Sensor.sensorBrightName) != null);
    }
    return false;
  }
}

/**
 * This PSP responds with

 */
public class PSP_Sensor extends PSP_BaseAdapter implements PlanServiceProvider, UISubscriber {
  static final String sensorTempName = "Temperature";
  static final String sensorBrightName = "Light";

  /** A zero-argument constructor is required for dynamically loaded PSPs,
   *         required by Class.newInstance()
   **/
  public PSP_Sensor()
  {
    super();
  }

  /**
   * This constructor includes the URL path as arguments
   */
  public PSP_Sensor( String pkg, String id ) throws RuntimePSPException
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
    // I need to get Bill and ray to match the names of the two measurements types.
    String inputSensorName;
    String sensorHttpParam="sensor";
    String tempNameHdr  = "temperature";
    String brightNameHdr = "brightness";


    try {
//      System.out.println("PSP_Sensor called from " + psc.getSessionAddress());

      if( ! query_parameters.existsParameter(sensorHttpParam) )
      {
         out.println("Please enter http query parameters.");
      }
      else
      {
         Collection sensorColl=null;
         String outNameHdr=null;
         inputSensorName = (String) query_parameters.getFirstParameterToken(sensorHttpParam, '=');
         if ((inputSensorName == null) || inputSensorName.equals("")) {
           out.println("0.0");
           return;
         }
         if (inputSensorName.indexOf("&") > 0)
           inputSensorName = inputSensorName.substring(inputSensorName.indexOf("&"));
         System.out.println("PSP_Sensor sensor=" + inputSensorName);
         if( inputSensorName.equalsIgnoreCase(tempNameHdr) )
         {
            outNameHdr = tempNameHdr;
            sensorColl = psc.getServerPluginSupport().queryForSubscriber(new GetSensorTempPredicate());
         }
         else if( inputSensorName.equalsIgnoreCase(brightNameHdr) )
         {
            outNameHdr = brightNameHdr;
            sensorColl = psc.getServerPluginSupport().queryForSubscriber(new GetSensorBrightPredicate());
         }

         if ((sensorColl == null) || sensorColl.isEmpty()) { // no data
           out.println(random(inputSensorName));
//           out.println("0.0");
         } else {
           Iterator iter = sensorColl.iterator();
           while (iter.hasNext()) {
              Allocation alloc = (Allocation)iter.next();
              AllocationResult ar = alloc.getReceivedResult();
              out.println(ar.getValue(0));
           }
         }
      }
    } catch (Exception ex) {
      out.println(ex.getMessage());
      ex.printStackTrace(out);
      System.out.println(ex);
      out.flush();
    }
  }

  String random(String sensor) {
    double ret = 0.0;
    if (sensor.equalsIgnoreCase("temperature"))
      ret = 20.0 + (Math.random() * 5.0);
    else if (sensor.equalsIgnoreCase("brightness"))
      ret = 1.0 + (Math.random() * 4.0);
    System.out.println("Sensor_PSP: Faking output: "+ret);
    return Double.toString(ret);
  }
  /**
   * A PSP can output either HTML or XML (for now).  The server
   * should be able to ask and find out what type it is.
   **/
  public boolean returnsXML() {
    return false;
  }

  public boolean returnsHTML() {
    return false;
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

