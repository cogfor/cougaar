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

package org.cougaar.microedition.demo;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;

import org.cougaar.lib.planserver.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;

import org.cougaar.microedition.se.domain.*;


public class PSP_ReportHeatIndex extends PSP_BaseAdapter implements PlanServiceProvider, UseDirectSocketOutputStream, UISubscriber
{

  class AllRecords implements UnaryPredicate
  {
    public boolean execute(Object o)
    {
      if (o instanceof HeatIndexRecord)
      {
        HeatIndexRecord rec = (HeatIndexRecord)o;
        return true;
      }
      return false;
    }
  }

  public PSP_ReportHeatIndex()
  {
    super();
  }

  public PSP_ReportHeatIndex(String pkg, String id) throws RuntimePSPException
  {
    setResourceLocation(pkg, id);
  }

  public boolean test(HttpInput query_parameters, PlanServiceContext sc)
  {
    super.initializeTest(); // IF subclass off of PSP_BaseAdapter.java
    return false;  // This PSP is only accessed by direct reference.
  }

  public void execute(
      PrintStream cout,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu) throws Exception
  {
    Collection recs = psc.getServerPluginSupport().queryForSubscriber( new AllRecords());
    HeatIndexRecordComparator hirc = new HeatIndexRecordComparator();

    TreeSet timeordered = new TreeSet(hirc);

    Iterator rec_iter = recs.iterator();
    while (rec_iter.hasNext())
    {
      HeatIndexRecord hirec = (HeatIndexRecord)rec_iter.next();
      timeordered.add(hirec);
    }

    rec_iter = timeordered.iterator();

    cout.print(
    "<TABLE border=1>\n" +
    "  <TR>\n" +
    "    <TD width=350 valign=top><P>Time</P></TD>\n" +
    "    <TD width=50 valign=top><P>Pod ID</P></TD>\n" +
    "    <TD width=150 valign=top><P>Heat Index</P></TD>\n" +
    "  </TR>\n");


    while (rec_iter.hasNext())
    {
      HeatIndexRecord hirec = (HeatIndexRecord)rec_iter.next();

       cout.print(
        "  <TR>\n" +
        "    <TD width=350 valign=top><P>"+hirec.GetRecordTime()+"</P></TD>\n" +
        "    <TD width=50 valign=top><P>"+hirec.GetPodId()+"</P></TD>\n" +
        "    <TD width=150 valign=top><P>"+hirec.GetHeatIndex()+"</P></TD>\n" +
        "  </TR>\n");
    }

    cout.print("</TABLE>\r\n\n");
  }

  public boolean returnsXML()
  {
    return false;
  }

  public boolean returnsHTML()
  {
    return true;
  }

  public String getDTD()
  {
    return null;
  }

  public void subscriptionChanged(Subscription subscription)
  {
  }

  class HeatIndexRecordComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      HeatIndexRecord rec1 = (HeatIndexRecord)o1;
      HeatIndexRecord rec2 = (HeatIndexRecord)o2;

      if(rec1.GetRecordTime().getTime() < rec2.GetRecordTime().getTime()) return (-1);
      if(rec1.GetRecordTime().getTime() > rec2.GetRecordTime().getTime()) return(1);

      return 0;
    }

    public boolean equals(Object o1, Object o2)
    {
      HeatIndexRecord rec1 = (HeatIndexRecord)o1;
      HeatIndexRecord rec2 = (HeatIndexRecord)o2;

      if(rec1.GetRecordTime().getTime() == rec2.GetRecordTime().getTime()) return true;

      return false;
    }
  }
}
