/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
 
package org.cougaar.planning.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.servlet.data.hierarchy.HierarchyData;
import org.cougaar.planning.servlet.data.hierarchy.Organization;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * <pre>
 * Servlet worker that gathers Organizational hierarchy information 
 * from a Cougaar society.
 *
 * Takes three parameters : 
 * - recurse, which controls whether to recurse down the hierarchy
 * - format, which specifies whether the response is a data stream, xml, or html.
 * - allRelationships, which returns all the relationships for an agent
 *   html is in the familiar CLUSTERS_R format.
 *
 * An example URL is :
 * 
 * http://localhost:8800/$TRANSCOM/hierarchy?recurse=true&format=xml
 *
 * This says to recurse from the TRANSCOM agent and return an XML document.                       
 * 
 * Example output from this query :
 *
 * <?xml version="1.0" ?> 
 * <Hierarchy RootID="TRANSCOM">
 *   <Org>
 *     <OrgID>TRANSCOM</OrgID>
 *     <Name>TRANSCOM</Name>
 *     <Rel OrgID="GlobalAir" Rel="0"/>
 *     <Rel OrgID="GlobalSea" Rel="0"/>
 *   </Org>
 *   <Org>
 *     <OrgID>GlobalAir</OrgID>
 *     <Name>GlobalAir</Name>
 *     <Rel OrgID="PlanePacker" Rel="0"/>
 *   </Org>
 *   ....
 * </Hierarchy>
 *
 * NOTE : If any agent hangs, the whole request will hang...
 * </pre>
 */
public class HierarchyWorker
  extends ServletWorker {
  public static boolean VERBOSE = false;
  static {
      VERBOSE = Boolean.getBoolean("org.cougaar.mlm.ui.psp.transit.HierarchyServlet.verbose");
  }
  // These constants are from GLM -- they permit us to put this servlet into core
  // At some future time we may want to have them be parameters to the servlet
  public static final Role SUBORD_ROLE = Role.getRole ("Subordinate");
  public static final Role ADMIN_SUBORD_ROLE = Role.getRole ("AdministrativeSubordinate");
  public static final String SUPERIOR_SUFFIX = "Superior";
  public static final String PROVIDER_SUFFIX = "Provider";

  public static final String CONVERSE_OF_PREFIX = "ConverseOf";

  protected boolean recurse;
  protected boolean allRelationships;
  protected Set visitedOrgs = new HashSet ();
  protected boolean testing = false;
  protected static final int AGENTS_IN_ROW = 10;

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   * <p>
   * For example, on Agent "X" the URI request path
   * will be "/$X/hello".
   */
  private final String myPath = "/Hierarchy";

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {
    return getClass().getName()+"("+myPath+")";
  }

  /**
   * Main method. <p>
   * Most of the work is done in getHierarchy.
   * This method mainly checks that the parameters are the right 
   * number and sets the format and recurse fields.
   * <p>
   * @see #getHierarchyData(HttpServletRequest,SimpleServletSupport,HasRelationships,boolean,boolean,Set)
   */
  public void execute(HttpServletRequest request, 
		      HttpServletResponse response,
		      SimpleServletSupport support) throws IOException, ServletException {
    super.execute (request, response, support);

    if (VERBOSE) {
      System.out.println("BEGIN hierarchy at "+support.getAgentIdentifier());
    }

    if (testing) {
      if (request.getRequestURI ().indexOf ("PlanePacker") != -1) {
	try {
	  Thread.sleep (30000);
	} catch (Exception e) {}
      }
    }
      
    // generate our response.
    getHierarchy (response.getOutputStream(), request, support, format, recurse, 
		  allRelationships, visitedOrgs);
	  
    if (VERBOSE) {
      System.out.println("FINISHED hierarchy at "+support.getAgentIdentifier());
    }
  }

  /** 
   * <pre>
   * sets both recurse and format 
   *
   * recurse is either true or false
   * format  is either data, xml, or html
   *
   * see class description for what these values mean
   * </pre>
   */
  protected void getSettings (String name, String value) {
    super.getSettings (name, value);
    if (eq("recurse", name)) {
      recurse = 
	((value == null) || 
	 (eq("true", value)));
    } else if (eq("allRelationships",name)) {
      if (eq("true", value))
	allRelationships=true;
    } else if (eq("visitedOrgs",name)) {
      StringTokenizer tokenizer = new StringTokenizer (value,",");
      while (tokenizer.hasMoreTokens ())
	visitedOrgs.add (tokenizer.nextToken ());

      if (VERBOSE)
	System.out.println ("getParams - Visited Org List is " + visitedOrgs);
    } else {
      if (VERBOSE)
	System.out.println ("NOTE : Ignoring parameter named " + name);
    }
  }

  protected String getPrefix () { return "Hierarchy at "; }

  /**
   * Fetch HierarchyData and write to output. <p>
   *
   * Main work is done by getHierarchyData, which returns a HierarchyData object.
   *
   * Output format is either data, xml, or html
   * @see #getHierarchyData
   */
  protected void getHierarchy(OutputStream out, HttpServletRequest request, 
			      SimpleServletSupport support,
			      int format, boolean recurse, boolean allRelationships,
			      Set visitedOrgs) {
    // get self org
    HasRelationships selfOrg = getSelfOrg(support);
    if (selfOrg == null) {
      throw new RuntimeException("No self org?");
    }

    // get hierarchy data
    try {
      HierarchyData hd = getHierarchyData(request, support, selfOrg, recurse, allRelationships, visitedOrgs);

      writeResponse (hd, out, request, support, format, allRelationships);
    } catch (Exception e) {
      System.err.println ("Got exception " + e + " getting hierarchy data for " + support.getAgentIdentifier());
      e.printStackTrace();
    }
  }

  /**
   * get the self organization.
   */
  protected HasRelationships getSelfOrg(SimpleServletSupport support) {
    // get self org
    Collection col = support.queryBlackboard (selfOrgP);
    if ((col != null) && 
        (col.size() == 1)) {
      Iterator iter = col.iterator();
      HasRelationships org = (HasRelationships) iter.next ();
      return org;
    } else {
      return null;
    }
  }

  /** test to find which org is yourself */
  private static final UnaryPredicate selfOrgP =
    new UnaryPredicate() {
	public boolean execute(Object o) {
	  return 
	    ((o instanceof HasRelationships) && ((HasRelationships)o).isLocal());
	}
      };

  /** populates a HierarchyData object given the self Org */
  protected HierarchyData getHierarchyData(HttpServletRequest request,
					   SimpleServletSupport support,
					   HasRelationships selfOrg,
					   boolean recurse,
					   boolean allRelationships,
					   Set visitedOrgs) {
    // create a "self" org
    String selfOrgName = 
      ((Asset)selfOrg).getClusterPG().getMessageAddress().toString();
    visitedOrgs.add (selfOrgName);
    // build list of orgs
    HierarchyData hd = new HierarchyData();
    Organization toOrg = new Organization();
    toOrg.setUID(selfOrgName);
    toOrg.setPrettyName(selfOrgName); // where is the pretty name kept?
    hd.setRootOrgID(selfOrgName);     // set rootId as self

    MutableTimeSpan mts = new MutableTimeSpan ();
    RelationshipSchedule schedule = 
      selfOrg.getRelationshipSchedule();
	
    Collection subordinates = new HashSet ();
	
    if (!allRelationships) {
      Collection subordinates1 = 
	schedule.getMatchingRelationships(SUBORD_ROLE,
					  mts.getStartTime(), mts.getEndTime());
      subordinates.addAll (subordinates1);
	
      Collection subordinates2 = 
	schedule.getMatchingRelationships(ADMIN_SUBORD_ROLE,
					  mts.getStartTime(), mts.getEndTime());

      subordinates.addAll (subordinates2);
    } else {
      subordinates.addAll (schedule.getMatchingRelationships(mts));
    }
	
    // add self org to hierarchy
    hd.addOrgData(toOrg);

    if (VERBOSE && false)
      System.out.println ("getHierarchyData - " + selfOrgName + 
			  " has these subs " + subordinates);

    if (subordinates.isEmpty()) {
      // no subordinates
      return hd;
    }

    Set recurseSubOrgSet = null;
    if (recurse) {
      recurseSubOrgSet = new HashSet();
    }

    // Safe to iterate over subordinates because getSubordinates() returns
    // a new Collection.
    for (Iterator schedIter = subordinates.iterator(); 
	 schedIter.hasNext();
	 ) {
      Relationship relationship = (Relationship)schedIter.next();
      Asset subOrg = (Asset)schedule.getOther(relationship);
      String role = schedule.getOtherRole(relationship).getName();

      String subOrgName = 
	subOrg.getClusterPG().getMessageAddress().toString();

      // client wants a numerical identifier for the role
      int roleId;
      if (!allRelationships) {
	if (role.equalsIgnoreCase("AdministrativeSubordinate")) {
	  // admin_subord
	  roleId = 
	    org.cougaar.planning.servlet.data.hierarchy.Organization.ADMIN_SUBORDINATE;
	} else {
	  // some other subord type
	  //   ** add more String.equals cases here **
	  roleId = 
	    org.cougaar.planning.servlet.data.hierarchy.Organization.SUBORDINATE;
	}
	toOrg.addRelation(subOrgName, roleId);
      }
      else if (!role.endsWith("Self")){
	toOrg.addRelation(subOrgName, role);
      }
      if (recurse &&
	  (!(selfOrgName.equals(subOrgName))) && // don't recurse on yourself
	  validRole (role) &&                    // only on customers, subordinates, etc. 
	  !visitedOrgs.contains(subOrgName)) {   // only ones we haven't visited before
	if (VERBOSE)                             // so we don't have circular paths
	  System.out.println ("self " + selfOrgName + " sub " + subOrgName + " role " + role + 
			      (validRole(role) ? " VALID " : " invalid"));
	recurseSubOrgSet.add(subOrgName);
      }
    }

    // if we are recursing, recurse on subordinates
    if (recurse) {
      visitedOrgs.addAll (recurseSubOrgSet);
      recurseOnSubords (recurseSubOrgSet, request, support, allRelationships, visitedOrgs, hd);
    }

    // return list
    return hd;
  }

  protected void recurseOnSubords (Set recurseSubOrgSet,
				   HttpServletRequest request,
				   SimpleServletSupport support,
				   boolean allRelationships, 
				   Set visitedOrgs,
				   HierarchyData hd) {
    for (Iterator iter = recurseSubOrgSet.iterator(); 
	 iter.hasNext();
	 ) {
      String subOrgName = (String)iter.next();
      // fetch the sub's data

      HierarchyData subHD = null;
      int tries = 5;
      long [] timeToWait = new long [] { 32000, 16000, 8000, 4000, 2000 };
      while (subHD == null && tries-- > 0) {
	subHD = fetchForSubordinate(request, support, subOrgName, allRelationships, visitedOrgs);
	if (subHD == null) {
	  if (VERBOSE && tries > 1) {
	    System.out.println("At " + new Date() + 
			       " In "+ support.getAgentIdentifier()+
			       ", fetch hierarchy from "+subOrgName+
			       " returned null, retry in " + 
			       timeToWait[tries] + " millis.");
	  }

	  synchronized(this) { 
	    try { wait(timeToWait[tries]); } catch (Exception e) {
	      System.out.println ("got exception " + e);
	    } 
	  }
	}
      }

      if (VERBOSE && (subHD == null)) {
	System.out.println("In "+ support.getAgentIdentifier()+
			   ", fetch hierarchy from "+subOrgName+
			   " returned null.");
      }

      // take Orgs from sub's hierarchy data
      int nSubHD = ((subHD != null) ? subHD.numOrgs() : 0);
      for (int i = 0; i < nSubHD; i++) {
	hd.addOrgData(subHD.getOrgDataAt(i));
      }
    }
  }

  /** 
   * <pre>
   * This prevents endless recursion, making it so we only follow
   * relationship links in one direction
   *
   * Specifically, we don't follow any agents that are our superiors,
   * providers, converse of a role, or our self.
   * </pre>
   */
  protected boolean validRole (String role) {
    return (!role.endsWith (SUPERIOR_SUFFIX) &&
	    !role.endsWith (PROVIDER_SUFFIX) &&
	    !role.startsWith (CONVERSE_OF_PREFIX) &&
	    !role.equals ("Self"));
  }

  /** 
   * recursion happens here -- for each subOrgName subordinate, a new 
   * servlet in the target agent will be executed
   */
  protected HierarchyData fetchForSubordinate(HttpServletRequest request, 
					      SimpleServletSupport support,
					      String subOrgName,
					      boolean allRelationships,
					      Set visitedOrgs) {
    HierarchyData hd = null;
    InputStream is = null;
    ObjectInputStream ois = null;

    try {
      // build URL for remote connection
      StringBuffer buf = new StringBuffer();
      buf.append("http://");
      buf.append(request.getServerName());
      buf.append(":");
      buf.append(request.getServerPort());
      buf.append("/$");
      buf.append(subOrgName);
      buf.append(support.getPath());
      buf.append("?data=true&recurse=true&visitedOrgs=");
      for (Iterator iter=visitedOrgs.iterator();iter.hasNext();) {
	buf.append (iter.next ());
	if (iter.hasNext())
	  buf.append (",");
      }
      if (allRelationships)
	buf.append("&allRelationships=true");
	
      String url = buf.toString();

      if (VERBOSE) {
        System.out.println(
			   "At " + new Date () + 
			   " - in "+ support.getAgentIdentifier()+
			   ", fetch hierarchy from "+subOrgName+
			   ", URL:\n"+url);
      }

      // open connection
      URL myURL = new URL(url);
      URLConnection myConnection = myURL.openConnection();
      is = myConnection.getInputStream();
      ois = new ObjectInputStream(is);

      // read single HierarchyData Object from subordinate
      hd = (HierarchyData)ois.readObject();

      if (VERBOSE) {
        System.out.println(
			   "In "+support.getAgentIdentifier()+
			   ", got "+
			   ((hd != null) ?
			    ("hierarchy["+hd.numOrgs()+"]") :
			    ("null"))+
			   " from "+subOrgName);
      }

    } catch (StreamCorruptedException sce) {
      if (VERBOSE) {
	System.err.println ("In "+support.getAgentIdentifier()+
			    ", got exception : ");
	sce.printStackTrace ();
      }
    } catch (FileNotFoundException fnf) {
      if (!allRelationships || true) {
	System.err.println ("In "+support.getAgentIdentifier()+
			    ", got exception : ");
	fnf.printStackTrace ();
      }
    } catch (Exception e) {
      System.err.println ("In "+support.getAgentIdentifier()+
			  ", got exception : ");
      e.printStackTrace();
    } finally {
      try {
	if (ois != null)
	  ois.close();
	if (is != null)
	  is.close();
      } catch (Exception e) {}
    }

    return hd;
  }

  /** 
   * Writes html with the list of found agents across the
   * top, with links to tables below for each agent.  The tables
   * show each agents relationships to other agents.  There are links
   * in those tables too, to the other referenced agents.
   */
  protected void writeResponse(XMLable result, 
			       OutputStream out, HttpServletRequest request, 
			       SimpleServletSupport support,
			       int format, boolean allRelationships) {
    if ((format == FORMAT_HTML) && allRelationships) {
      HierarchyData data = (HierarchyData) result;
      if (VERBOSE) 
	System.out.println ("HierarchyWorker.writeResponse - got data for " + data.numOrgs() + 
			    " orgs");
      PrintWriter writer = new PrintWriter (out);
      writer.println("<HTML><HEAD>\n<TITLE>"+getPrefix () + 
		     support.getAgentIdentifier()+
		     "</TITLE>\n" + 
		     "</HEAD><BODY>\n"+
		     "<H2><CENTER>" + getPrefix () + 
		     support.getAgentIdentifier()+
		     "</CENTER></H2><p><pre>\n");
      writer.flush ();

      writer.println("<a name=\"top\"/>" + 
		     "<TABLE align=center border=0 cellPadding=1 cellSpacing=1>");
      boolean rowEnded = false;
      int k = 0;
      data.sortOrgs ();
      for(;k<data.numOrgs();k++) {
	Organization org = data.getOrgDataAt(k);
	if ((k % AGENTS_IN_ROW) == 0) {
	  writer.println ("<TR>");
	  rowEnded = false;
	}
	writer.println("<TD><a href=\"#" + 
		       org.getPrettyName ()+ "\"/>"+
		       org.getPrettyName ()+ "</a></TD>");
	if ((k % AGENTS_IN_ROW) == AGENTS_IN_ROW-1) {
	  writer.println("</TR>");
	  rowEnded = true;
	}
      }
      for (int i = (k % AGENTS_IN_ROW); i < AGENTS_IN_ROW; i++)
	writer.print("<TD></TD>");
      if (!rowEnded)
	writer.println("</TR>");
      writer.println("</TABLE><P/><P/><br/>"); 

      for(int i=0;i<data.numOrgs();i++) {
	Organization org = data.getOrgDataAt(i);
	writer.println("<P>Agent relationships for <B><a name=\"" + 
		       org.getPrettyName ()+ "\" />"+
		       org.getPrettyName ()+ "</B>&nbsp;<a href=\"#top\">[top]</a></P>");
	writer.println("<TABLE align=center border=1 cellPadding=1 cellSpacing=1");
	writer.println("width=75% bordercolordark=#660000 bordercolorlight=#cc9966>");
	writer.println("<TR>");
	// writer.println("<TD width=\"25%\"> <FONT color=mediumblue ><B>Agent</FONT></B> </TD>");
	writer.println("<TD width=\"25%\"> <FONT color=mediumblue ><B>Assigned Agent</FONT></B></TD>");   
	writer.println("<TD width=\"75%\"> <FONT color=mediumblue ><B>Role </FONT></B></TD>");
	writer.println("</TR>");

	List relations = org.getRelations ();
	Collections.sort (relations);
	for(Iterator iter = relations.iterator(); iter.hasNext();) { 
	  Organization.OrgRelation relation = (Organization.OrgRelation) iter.next ();
	  String relatedOrg = relation.getRelatedOrg();
	  writer.println("<TR><TD><a href=\"#"+ relatedOrg + "\">"+
			 relatedOrg + "</a></TD><TD>"+
			 relation.getName()+
			 "</TD></TR>");
	}
	writer.println("</TABLE><P>"); 
      }
      writer.println("\n</BODY></HTML>\n");
      writer.flush ();
    }
    else {
      try {
	writeResponse (result, out, request, support, format);
      } catch (Exception e) {
	System.err.println ("Got exception " + e + " writing out response for " + support.getAgentIdentifier());
	e.printStackTrace();
      }
    }
  }
}

