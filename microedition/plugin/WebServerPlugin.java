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

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.io.*;

/**
 */
public class WebServerPlugin extends PluginAdapter
{
  UnaryPredicate getOthersPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o)
      {
	if(o instanceof MicroTask)
          return false;
	if(o instanceof Resource)
          return false;
	if(o instanceof MicroAgent)
          return false;
	if(o instanceof MicroAllocation)
          return false;
        return true;
      }
    };
    return myPred;
  }

  UnaryPredicate getTasksPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o)
      {
	if(o instanceof MicroTask)
          return true;
        return false;
      }
    };
    return myPred;
  }

  UnaryPredicate getResourcePred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o)
      {
	if(o instanceof Resource)
          return true;
        return false;
      }
    };
    return myPred;
  }

  UnaryPredicate getAgentsPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o)
      {
	if(o instanceof MicroAgent)
          return true;
        return false;
      }
    };
    return myPred;
  }

  private static final String provideserverporttask = "ProvideWebServerPortIdentification";
  private static final int portidaspect = 100;

  UnaryPredicate getPortIDTaskPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroTask) {
          MicroTask mt = (MicroTask)o;
          return mt.getVerb().equals(provideserverporttask);
        }
        return false;
      }
    };
    return myPred;
  }

  UnaryPredicate getPortIDAllocationPred()
  {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroAllocation) {
          MicroTask mt = ((MicroAllocation)o).getTask();
          return mt.getVerb().equals(provideserverporttask);
        }
        return false;
      }
    };
    return myPred;
  }

  private Subscription taskSub;
  private Subscription resourceSub;
  private Subscription agentSub;
  private Subscription portidallocSub;
  private Subscription portidtaskSub;
  private Subscription otherSub;

  private int port = 80;
  private boolean debugging = false;

  private Hashtable webserverports = null;

  public void setupSubscriptions()
  {
    System.out.println("WebServerPlugin::setupSubscriptions");

    if (getParameters() != null)
    {
      Hashtable t = getParameters();
      if (t.containsKey("debug"))
      {
        debugging = true;
	System.out.println("WebServerPlugin debugging is on.");
      }
      if (t.containsKey("port"))
      {
        port = Integer.parseInt((String)t.get("port"));
	System.out.println("WebServerPlugin port is "+port);
      }
    }
    else
    {
      System.out.println("WebServerPlugin: setupSubscriptions No Parameters specified");
    }

    taskSub = subscribe(getTasksPred());
    resourceSub = subscribe(getResourcePred());
    agentSub = subscribe(getAgentsPred());
    portidtaskSub = subscribe(getPortIDTaskPred());
    portidtaskSub.setSeeOwnChanges(false);
    portidallocSub = subscribe(getPortIDAllocationPred());
    portidallocSub.setSeeOwnChanges(false);
    otherSub = subscribe(getOthersPred());

    webserverports = new Hashtable();
    Integer myport = new Integer(port);
    webserverports.put(getNodeName(), myport);

    try
    {
      if(debugging) System.out.println("Getting object class...");
      ServerSocketME serversocket = (ServerSocketME) MicroEdition.getObjectME(Class.forName("org.cougaar.microedition.io.ServerSocketME"));
      if(debugging) System.out.println("Server socket obtained...");
      serversocket.open(port);
      if(debugging) System.out.println("Server socket opened...");
      if(serversocket != null)
      {
	Thread ls = new Thread(new ListenOnSocket(serversocket));
	ls.start();
      }
    }
    catch (Exception e)
    {
      System.err.println("WebServerPlugin: failed to establish socket");
      e.printStackTrace();
    }
  }

  public void execute()
  {

    //Examine new microagents in the society
    Enumeration enm = agentSub.getAddedList().elements();
    while(enm.hasMoreElements())
    {
      MicroAgent magent = (MicroAgent)enm.nextElement();

      if(webserverports.containsKey(magent.getAgentId().getName()))
	continue; //don't spawn a task for this

      Integer temportid = new Integer(0);
      webserverports.put(magent.getAgentId().getName(), temportid);

      MicroTask newmicrotask = new MicroTask(getNodeName());
      newmicrotask.setVerb(provideserverporttask);
      publishAdd(newmicrotask);

      MicroAllocation mallo = new MicroAllocation(magent, newmicrotask);
      publishAdd(mallo);

       if(debugging)
       {
	  System.out.println("WebServerPlugin:: New MicroAgent: "+magent.getAgentId().getName());
          System.out.println("                  Task allocated: "+newmicrotask.getUniqueID());
       }
    }

    enm = agentSub.getRemovedList().elements();
    while(enm.hasMoreElements())
    {
      MicroAgent magent = (MicroAgent)enm.nextElement();

      if(webserverports.containsKey(magent.getAgentId().getName()))
      {
	webserverports.remove(magent.getAgentId().getName());
      }
    }

    //Examine allocation results for port id reporting
    enm = portidallocSub.getChangedList().elements();
    while(enm.hasMoreElements())
    {
      MicroAllocation ma = (MicroAllocation)enm.nextElement();

      String assetname = null;
      MicroAgent magent = (MicroAgent)ma.getAsset();
      if(magent != null)
      {
	AgentId aid = magent.getAgentId();
	if(aid != null) assetname = aid.getName();
      }

      MicroAllocationResult mar = ma.getReportedResult();
      if (mar == null) continue;

      int [] aspects = mar.getAspects();
      long [] values = mar.getValues();

      if(debugging)
      {
	System.out.println("WebServerPlugin: Allocation changed on task   : " +ma.getTask().getUniqueID());
	System.out.println("WebServerPlugin: Allocation changed from asset: " +assetname);
      }


      if(assetname != null)
      {
	if ( aspects.length > 0)
	{
	  if(aspects[0] == portidaspect)
	  {
	    int index = 0;
	    Integer serverport = new Integer((int)(values[0]/1000));
	    if(webserverports.containsKey(assetname))
	    {
	      if(debugging)
	      {
		System.out.println("Setting local hashtable: key = " +assetname);
		System.out.println("Setting local hashtable: obj = " +serverport);
	      }
	      //replace
	      webserverports.put(assetname, serverport);
	    }
	  }
	}
      }
    }

    //Examine the tasks that enquire for own port id
    enm = portidtaskSub.getAddedList().elements();
    while(enm.hasMoreElements())
    {
      MicroTask mt = (MicroTask)enm.nextElement();

      //see if this is my own task
      org.cougaar.microedition.util.StringTokenizer st = new org.cougaar.microedition.util.StringTokenizer(mt.getUniqueID(), "/");
      if(st.hasMoreTokens())
      {
	if(getNodeName().equals(st.nextToken()))
	{
	  if (debugging) System.out.println("My added task "+mt.getUniqueID()+" ignored");
	  continue;
	}
      }

      int size = 1;
      long values[] = new long[size];
      int aspects[] = new int[size];

      aspects[0] = portidaspect;
      values[0] = port*1000;

      MicroAllocation ma = new MicroAllocation(null, mt);
      MicroAllocationResult mar = new MicroAllocationResult();
      ma.setReportedResult(mar);
      mar.setSuccess(true);
      mar.setAspects(aspects);
      mar.setValues(values);

      mt.setAllocation(ma);
      publishChange(ma);
      publishChange(mt);

      //create allocation result for enquirers
      if(debugging)
      {
	System.out.println("WebServerPlugin: Task added "+mt.getUniqueID());
	System.out.println("                 Reporting Allocation results "+values[0]);
      }
    }

  }

  public void printTasks(PrintStream stream)
  {
    stream.print(
    "<H2>MicroTasks</H2>\n" +
    "<TABLE border=1>\n" +
    "  <TR>\n" +
    "    <TD width=200 valign=top><P>Verb</P></TD>\n" +
    "    <TD width=200 valign=top><P>Prepositions</P></TD>\n" +
    "    <TD width=200 valign=top><P>Allocations</P></TD>\n" +
    "  </TR>\n");


    Enumeration enm = taskSub.getMemberList().elements();
    while(enm.hasMoreElements())
    {
      //task verb information
      MicroTask t = (MicroTask)enm.nextElement();
      if(debugging) System.out.println( "Task = "+t.getVerb() );
      stream.print("  <TR>\n");
      stream.print("    <TD width=200 valign=top><P>"+t.getVerb()+"</P></TD>\n");

      //task prepositions
      String prepstring = "";
      if (t.getPrepositionalPhrases() != null)
      {

	for (Enumeration prepenm = t.getPrepositionalPhrases().elements();
	      prepenm.hasMoreElements(); )
	{
	  MicroPrepositionalPhrase mpp =(MicroPrepositionalPhrase)enm.nextElement();
	  if (mpp !=null)
	  {
	    prepstring = prepstring + mpp.getPreposition() + "=" + mpp.getIndirectObject() + ",";
	  }
	}
      }
      else
      {
	prepstring = "None";
      }

      if(debugging) System.out.println( "Preps = "+prepstring );
      stream.print("    <TD width=200 valign=top><P>"+prepstring+"</P></TD>\n");

      //task allocations
      String allocationstring = "";
      MicroAllocation talloc = t.getAllocation();
      if(talloc != null)
      {
	MicroAllocationResult res = talloc.getReportedResult();
	if(res != null)
	{
	  int [] aspects = res.getAspects();
	  long [] values = res.getValues();
	  for(int i=0; i < aspects.length; i++)
	  {
	    allocationstring = allocationstring + aspects[i] +"="+values[i]+",";
	  }
	}
	else
	{
	  allocationstring = "None";
	}
      }
      else
      {
	allocationstring = "None";
      }

      if(debugging) System.out.print( "Allocs = "+allocationstring );

      stream.print("    <TD width=200 valign=top><P>"+allocationstring+"</P></TD>\n");

      stream.print("  </TR>\n");

    }

    stream.print("</TABLE>\r\n\n");
    stream.print("<hr align=Left width=100% size=2>\n");

  }

  public void printOthers(PrintStream stream)
  {
    stream.print(
    "<H2>Other</H2>\n" +
    "<TABLE border=1>\n" +
    "  <TR>\n" +
    "    <TD width=200 valign=top><P>Type</P></TD>\n" +
    "    <TD width=200 valign=top><P>String</P></TD>\n" +
    "  </TR>\n");


    Enumeration enm = otherSub.getMemberList().elements();
    while(enm.hasMoreElements())
    {
      Object other = (Object)enm.nextElement();
      stream.print("  <TR>\n");
      stream.print("    <TD width=200 valign=top><P>"+other.getClass().getName()+"</P></TD>\n");
      stream.print("    <TD width=200 valign=top><P>"+other.toString()+"</P></TD>\n");
      stream.print("  </TR>\n");

    }

    stream.print("</TABLE>\r\n\n");
    stream.print("<hr align=Left width=100% size=2>\n");

  }

  public void printResources(PrintStream stream)
  {
    stream.print(
    "<H2>Resources</H2>\n" +
    "<TABLE border=1>\n" +
    "  <TR>\n" +
    "    <TD width=200 valign=top><P>Type</P></TD>\n" +
    "    <TD width=100 valign=top><P>Name</P></TD>\n" +
    "    <TD width=200 valign=top><P>Parameters</P></TD>\n" +
    "    <TD width=200 valign=top><P>Value</P></TD>\n" +
    "  </TR>\n");


    Enumeration enm = resourceSub.getMemberList().elements();
    while(enm.hasMoreElements())
    {
      //task verb information
      Resource r = (Resource)enm.nextElement();
      if(debugging) System.out.println( "Resource = "+r.getName());

      stream.print("  <TR>\n");
      stream.print("    <TD width=200 valign=top><P>"+r.getClass().getName()+"</P></TD>\n");
      stream.print("    <TD width=100 valign=top><P>"+r.getName()+"</P></TD>\n");

      if(debugging) System.out.println( "Resource class = "+r.getClass().getName());

      String parameterstr = "";

      Hashtable ht = r.getParameters();
      if(ht != null)
      {
	Enumeration htkeys = ht.keys();

	while(htkeys.hasMoreElements())
	{
	  Object key   = (Object)htkeys.nextElement();
	  Object keyvalue = ht.get(key);
	  parameterstr = parameterstr +"["+key+"]="+keyvalue+" ";
	}
      }
      else
      {
	parameterstr = "None";
      }
      if(debugging) System.out.println( "Resource parameters = "+parameterstr);
      stream.print("    <TD width=200 valign=top><P>"+parameterstr+"</P></TD>\n");

      if(r instanceof SensorResource)
      {
	SensorResource sr = (SensorResource)r;
	String sensorvaluestring = sr.getValue()+ " " +sr.getUnits();
        stream.print("    <TD width=200 valign=top><P>"+sensorvaluestring+"</P></TD>\n");
      }
      else
      {
	stream.print("    <TD width=200 valign=top><P>NA</P></TD>\n");
      }
      stream.print("  </TR>\n");

    }

    stream.print("</TABLE>\r\n\n");
    stream.print("<hr align=Left width=100% size=2>\n");

  }

  public void printAgents(PrintStream stream)
  {
    stream.print(
    "<H2>MicroAgents</H2>\n" +
    "<TABLE border=1>\n" +
    "  <TR>\n" +
    "    <TD width=200 valign=top><P>Name</P></TD>\n" +
    "    <TD width=200 valign=top><P>IPAddress</P></TD>\n" +
    "    <TD width=200 valign=top><P>Capabilities</P></TD>\n" +
    "  </TR>\n");


    Vector alreadylisted = new Vector();
    Enumeration enm = agentSub.getMemberList().elements();
    while(enm.hasMoreElements())
    {
      //task verb information
      MicroAgent magent = (MicroAgent)enm.nextElement();
      if(debugging) System.out.println( "MicroAgent Name = "+magent.getAgentId().getName());
      if(debugging) System.out.println( "MicroAgent Address = "+magent.getAgentId().getIpAddress());
      if(debugging) System.out.println( "MicroAgent Port = "+magent.getAgentId().getPort());

      if(alreadylisted.contains(magent.getAgentId().getName())) //prevents listing same agent twice
	continue;

      alreadylisted.addElement(magent.getAgentId().getName());

      int wsport = 0;
      String linkstring = null;

      if(webserverports.containsKey(magent.getAgentId().getName()))
      {
	Integer temp = (Integer)webserverports.get(magent.getAgentId().getName());
	wsport = temp.intValue();

	linkstring =
             "<a href=http://"+magent.getAgentId().getIpAddress()+":"+wsport+"/>" +
             magent.getAgentId().getName() + "</a>";

      }

      if (wsport == 0)
      {
        //this assumes the agent is an SE domain agent
        linkstring =
             "<a href=http://"+magent.getAgentId().getIpAddress()+":5555/$"+magent.getAgentId().getName()+"/alpine/demo/?list?psp>" +
             magent.getAgentId().getName() + "</a>";
      }

      if(debugging)
	System.out.println( "MicroAgent WebServer Port = "+wsport);

      stream.print("  <TR>\n");

      stream.print("    <TD width=200 valign=top><P>"+linkstring+"</P></TD>\n");
      stream.print("    <TD width=200 valign=top><P>"+magent.getAgentId().getIpAddress()+"</P></TD>\n");

      if(debugging)
	System.out.println( "Capabilities = "+magent.getAgentId().getCapabilities());

      stream.print("    <TD width=200 valign=top><P>"+magent.getAgentId().getCapabilities()+"</P></TD>\n");
      stream.print("  </TR>\n");

    }

    stream.print("</TABLE>\r\n\n");
    stream.print("<hr align=Left width=100% size=2>\n");

  }

  public void printHeader(PrintStream stream)
  {
    stream.print(
       "HTTP/1.0 200 Document follows\r\n" +
       "Content-Type: text/html\r\n" +
       "\r\n" +
       "<!DOCTYPE HTML PUBLIC " +
                "\"-//W3C//DTD HTML 3.2//EN\">\n" +
       "<HTML>\n" +
       "<HEAD>\n" +
       "  <TITLE>Cougaar MicroEdition Blackboard</TITLE>\n" +
       "</HEAD>\n" +
       "\n" +
       "<BODY>\n" +
       "<H1>Cougaar ME Blackboard at <font color=#ff0000>"+ getNodeName() +"</font></H1>\n" +
       "<PRE>\n");

  }

  public void printTrailer(PrintStream stream)
  {
    stream.print(
      "</PRE>\n" +
      "</BODY>\n" +
      "</HTML>\r\n\r\n");
  }

  class ConnectionEstablished implements Runnable
  {
    private SocketME s;

    public ConnectionEstablished(SocketME newsocket){ s = newsocket;}

    public void readUntil()
    {
      try
      {
	if(debugging)
	  System.out.println("Reading input from port...");

	char endchar[] = {'\r', '\n', '\r', '\n'};
	String terminus = new String(endchar);

	char lastchars[] = {'0', '0', '0', '0'};
	String laststr = new String(lastchars);

	InputStreamReader in = new InputStreamReader(s.getInputStream());

	while(true)
	{
	  try
	  {
	    //shift characters
	    lastchars[0] = lastchars[1];
	    lastchars[1] = lastchars[2];
	    lastchars[2] = lastchars[3];
	    lastchars[3]  = (char)in.read();

	    if(debugging) System.out.print( lastchars[3] );

	    //check if match
	    laststr = new String(lastchars);
	    if(laststr.equals(terminus))
	    {
	      if(debugging)
		System.out.println("Termination characters read.");
	      break;
	    }
	  }
	  catch(Exception e)
	  {
	    System.out.println("Read data exception");
	    break;
	  }
	}

	//closing on the JVM causes the server not to work. It is
	//required on the KVM however
        String meconfig = System.getProperty("microedition.configuration");
	if(meconfig != null) in.close();

	if(debugging)
	  System.out.println("...reading done");

      }
      catch (Exception e)
      {
	System.out.println("Error in reading from client.");
      }
    }

    public void run()
    {

      if(debugging)
	System.out.println("WebServerPlugin::ConnectionEstablished thread");

      readUntil();

      try
      {
	if(debugging)
	  System.out.println("Opening output stream...");

	PrintStream out = new PrintStream(s.getOutputStream());

	if(debugging)
	  System.out.println("Sending client page...");

	openTransaction();
	printHeader(out);
	printTasks(out);
	printResources(out);
	printAgents(out);
	printOthers(out);
	printTrailer(out);
	closeTransaction();

	out.flush();

	//closing output stream seems to mess up JVM but is required on KVM
	String meconfig = System.getProperty("microedition.configuration");
	if(meconfig != null) out.close();

	if(debugging)
	  System.out.println("...done");

      }
      catch (Exception e)
      {
	System.out.println("Error in writing to client.");
      }

      try
      {
	s.close();
      }
      catch(Exception e)
      {
	System.out.println("Exception closing socket connection.");
      }
    }
  }

  class ListenOnSocket implements Runnable
  {

    private ServerSocketME server = null;

    public ListenOnSocket(ServerSocketME ss)
    {
      server = ss;
    }

    public void run()
    {
      if(debugging)
	System.out.println("WebServerPlugin is listening on socket.");

      while(true)
      {
	try
	{
	  SocketME socket = server.accept();
	  if(socket != null)
          {
	    Thread conn = new Thread(new ConnectionEstablished(socket));
	    conn.start();
          }
	}
	catch (Exception e)
	{
	  e.printStackTrace();
	}
      }
    }
  }
}
