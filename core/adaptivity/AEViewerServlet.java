/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.util.UnaryPredicate;

/**
 * Servlet to view adaptivity objects and edit operating mode policies
 */
public class AEViewerServlet
extends ComponentServlet
implements BlackboardClient
{

  /**
    * 
    */
  private static final long serialVersionUID = 1L;
  public static final String FRAME = "frame";
  public static final String AE_FRAME = "ae";

  public static final String OPERATINGMODE = "OperatingMode";
  public static final String POLICY = "Policy";
  public static final String CHANGE = "change";
  public static final String UID = "uid";
  public static final String NAME = "name";
  public static final String KERNEL = "kernel";
  public static final String VALUE = "value";

  private LoggingService logger = LoggingService.NULL;
  private BlackboardQueryService blackboardQuery;
  private BlackboardService blackboard;

  private OMComparator omComparator = new OMComparator();
  private OMPComparator ompComparator = new OMPComparator();

  private static UnaryPredicate conditionPredicate = 
    new UnaryPredicate() { 
	/**
       * 
       */
      private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
	  if (o instanceof Condition) {
	    return true;
	  }
	  return false;
	}
      };

  private static UnaryPredicate omPredicate = 
    new UnaryPredicate() { 
	/**
       * 
       */
      private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
	  if (o instanceof OperatingMode) {
	    return true;
	  }
	  return false;
	}
      };

  private static UnaryPredicate omPolicyPredicate = 
    new UnaryPredicate() { 
	/**
       * 
       */
      private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
	  if (o instanceof OperatingModePolicy) {
	    return true;
	  }
	  return false;
	}
      };

  public void setLoggingService(LoggingService logger) {
    if (logger != null) { 
      this.logger = logger;
    }
  }

  public void setBlackboardQueryService(BlackboardQueryService blackboardQuery) {
    this.blackboardQuery = blackboardQuery;
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  public String getBlackboardClientName() {
    return getPath();
  }

  public long currentTimeMillis() {
    return -1;  // N/A
  }

  @Override
public void unload() {
    if (blackboard != null) {
      releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    if (blackboardQuery != null) {
      releaseService(
          this, BlackboardQueryService.class, blackboardQuery);
      blackboardQuery = null;
    }
    if (logger != LoggingService.NULL) {
      releaseService(this, LoggingService.class, logger);
      logger = LoggingService.NULL;
    }
    super.unload();
  }

  @Override
public void doGet(HttpServletRequest request, HttpServletResponse response) {
    String frame = request.getParameter(FRAME);
    response.setContentType("text/html");
    try {
      PrintWriter out = response.getWriter();
      if (!(AE_FRAME.equals(frame))) {
        // generate outer frame page
        writeTopFrame(out);
      } else {
        // generate real AE frame
        String objectType = request.getParameter(CHANGE);
        if (objectType != null) {
          if (objectType.equals(POLICY)) {
            changePolicy(request, out);
          } else if (objectType.equals(OPERATINGMODE)) {
            changeOperatingMode(request, out);
          }
        } else {
          /* send adaptivity objects */
          sendData(out);
        }
      }
      out.close();
    } catch (java.io.IOException ie) { ie.printStackTrace(); }
  }

  private void changePolicy(HttpServletRequest request, PrintWriter out) {

    String uid = request.getParameter(UID);

    // get the string representing the policy
    String policyString = request.getParameter(KERNEL);
    StringReader reader = new StringReader(policyString);
    OperatingModePolicy[] policies = null;
    try {
      // Use the parser to create a new policy
      Parser parser = new Parser(reader, logger);
      policies = parser.parseOperatingModePolicies();
    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
    } finally {
      reader.close();
    }

    // find the existing policy on the blackboard
    Collection blackboardCollection 
      = blackboardQuery.query(new UIDPredicate(uid));
    OperatingModePolicy bbPolicy = (OperatingModePolicy)blackboardCollection.iterator().next();

    // set the existing policy's kernel to be that of the newly
    // parsed policy
    bbPolicy.setPolicyKernel(policies[0].getPolicyKernel());
    
    blackboard.openTransaction();
    // write the updated policy to the blackboard
    blackboard.publishChange(bbPolicy);
    blackboard.closeTransaction();
    
    out.println("<html><head></head><body><h2>Policy Changed</h2><br>" );
    out.println(bbPolicy.toString());
  }

  private void changeOperatingMode(HttpServletRequest request, PrintWriter out) {

    // get the string representing the operating mode
    String omName = request.getParameter(NAME);
    // find the existing operating mode on the blackboard
    Collection blackboardCollection 
      = blackboardQuery.query(new OMByNamePredicate(omName));
    OperatingMode bbOM = (OperatingMode)blackboardCollection.iterator().next();

    String newValue = request.getParameter(VALUE);

    Class omClass = bbOM.getValue().getClass();

    // Is it a String?
    try {
      if (omClass == String.class) {
	// set it and be done with it.
	bbOM.setValue(newValue);
      } 
      else {
	// If not, hope that whatever it is has a String constructor
	Constructor cons = null;
	try {
	  cons = omClass.getConstructor( new Class[] {String.class});
	} catch (NoSuchMethodException nsme) {
	  System.err.println("AEViewerServlet: Error, no String constructor for OperatingMode containing class " + omClass + " " + nsme);
	  out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
	  out.println("There is no String constructor for OperatingMode containing class " +omClass + " " + nsme);
	  return;
	} catch (RuntimeException re) {
	  out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
	  out.println(re);
	  return;
	}
	
	if (cons != null) {
	  // Make a new one of whatever it is and set OM value
	  Comparable newThing = 
	    (Comparable) cons.newInstance((Object[]) new String[] {newValue});
	  bbOM.setValue(newThing);
	} else {
	  out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
	  out.print("Can't set ");
	  out.print(bbOM.getName());
	  out.print("to " + newValue);
	  out.println("<br>No constructor " + omClass +"(String)");
	}
      }
    } catch (IllegalArgumentException iae) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(newValue);
      out.print(" is not a valid value for ");
      out.println(bbOM.getName());
      out.print("<br>");
      out.println(iae);
      return;
    } catch (java.lang.reflect.InvocationTargetException ite) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(newValue);
      out.print(" is not a valid value for ");
      out.println(bbOM.getName());
      out.print("<br>");
      out.println(ite);
      return;
    } catch (InstantiationException ie) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(ie);
      return;
    } catch (IllegalAccessException iae) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(iae);
      return;
    } catch (RuntimeException re) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(re);
      return;
    }
    
    blackboard.openTransaction();
    // write the updated operating mode to the blackboard
    blackboard.publishChange(bbOM);
    blackboard.closeTransaction();
    
    out.println("<html><head></head><body><h2>OperatingMode Changed</h2><br>" );
    out.println(bbOM.toString());
  }
  
  /**
   * Suck the Policies, Conditions, and Operating Modes out of the
   * blackboard and send them to the requestor
   */
  private void sendData(PrintWriter out) {
    out.println("<html><head></head><body>");

    writeAgentSelector(out);

    writeConditions(out);
    
    out.println("<h2><CENTER>Operating Modes</CENTER></h2>" );
    writeOMTable(out);

    out.print("<H2><CENTER>Operating Mode Policies</CENTER></H2><P>\n");
    writePolicyTable(out);
  }

  private void writeTopFrame(PrintWriter out) {
    // generate outer frame page:
    //   top:    select "/agent"
    //   bottom: real AE frame
    out.print(
        "<html><head><title>AE Viewer</title></head>"+
        "<frameset rows=\"10%,90%\">\n"+
        "<frame src=\""+
        "/agents?format=select&suffix="+
        getEncodedAgentName()+
        "\" name=\"agentFrame\">\n"+
        "<frame src=\"/$"+
        getEncodedAgentName()+
        getPath()+"?"+FRAME+"="+AE_FRAME+
        "\" name=\""+AE_FRAME+"\">\n"+
        "</frameset>\n"+
        "<noframes>Please enable frame support</noframes>"+
        "</html>\n");
  }

  private void writeAgentSelector(PrintWriter out) {

    out.print(
	      "<script language=\"JavaScript\">\n"+
	      "<!--\n"+
	      "function mySubmit() {\n"+
              "  var obj = top.agentFrame.document.agent.name;\n"+
              "  var encAgent = obj.value;\n"+
              "  if (encAgent.charAt(0) == '.') {\n"+
              "    alert(\"Please select an agent name\")\n"+
              "    return false;\n"+
              "  }\n"+
              "  document.myForm.target=\""+AE_FRAME+"\"\n"+
	      "  document.myForm.action=\"/$\"+encAgent+\""+
              getPath()+"\"\n"+
	      "  return true\n"+
	      "}\n"+
	      "// -->\n"+
	      "</script>\n"+
              "<h2><center>Adaptivity Viewer at "+
              getEncodedAgentName()+
	      "</center></h2>\n"+
	      "<form name=\"myForm\" method=\"get\" "+
	      "onSubmit=\"return mySubmit()\">\n"+
              "<input type=hidden name=\""+FRAME+"\" value=\""+AE_FRAME+"\">\n"+
              "Select an agent above, <input type=submit name=\"formSubmit\" value=\"Reload\">"+
              "<br>\n</form>");
  }

  /**
   * Creates a sorted HTML list of the conditions in the blackboard
   **/
  private void writeConditions(PrintWriter out) {
    out.println("<h2><CENTER>Conditions</CENTER></h2><br>" );
    Collection conditions = blackboardQuery.query(conditionPredicate);

    // Sort the Conditions. Is there a better way of doing this?
    TreeSet sortedConditions = new TreeSet();
    for (Iterator it = conditions.iterator(); it.hasNext();) {
      sortedConditions.add(it.next().toString());
    }
    out.print("<UL>");
    for (Iterator it = sortedConditions.iterator(); it.hasNext();) {
      out.print("<LI>");
      out.println(it.next().toString());
    }
    out.println("</UL>");
  }

  /**
   * Create a HTML table with a form in each row for editing a policy
   */
  private void writeOMTable(PrintWriter out) {
    out.println ("<table>");
    out.println("<tr><th>OperatingMode Name</th><th>Valid Values</th><th>Value</th></tr>");

    // Sort the OperatingModes
    List oms = (List) blackboardQuery.query(omPredicate);
    try {
      Collections.sort(oms, omComparator);
    } catch (ClassCastException e) {
      e.printStackTrace();
    }
				 
    for (Iterator it = oms.iterator(); it.hasNext();) {
      
      out.print("<FORM METHOD=\"GET\" ACTION=\"/$");
      out.print(getEncodedAgentName());
      out.print(getPath());
      out.println(
          "\"><input type=hidden name=\""+FRAME+
          "\" value=\""+AE_FRAME+"\">\n");
      
      OperatingMode om = (OperatingMode) it.next();

      out.print("<td>");
      out.print(om.getName());
      out.println("</td>");

      out.print("<td>");
      out.print(om.getAllowedValues().toString());
      out.println("</td>");

      out.print("<td>");
      out.print("<INPUT TYPE=text NAME=");
      out.print(VALUE);
      out.print(" VALUE=\"");
      out.print(om.getValue().toString());
      out.print("\"SIZE=20>");
      out.println("</td>");

      out.print("<td> <INPUT TYPE=submit value=\"Submit\"> </td>");

      out.print("<tr> <td><INPUT TYPE=hidden NAME=");
      out.print(CHANGE);
      out.print(" VALUE=\"");
      out.print(OPERATINGMODE);
      out.println("\"</td>");

      out.print("<td>");
      out.print("<INPUT TYPE=hidden NAME=");
      out.print(NAME);
      out.print(" VALUE=\"");
      out.print(om.getName());
      out.println("\"> </td> </tr>");
      out.println("</form>");
    }
    out.println("</table>");	
  }


  /**
   * Create a HTML table with a form in each row for editing a policy
   */
  private void writePolicyTable(PrintWriter out) {
    out.println ("<table>\n");
    out.println("<tr><th>Name</th><th>Authority</th><th>UID</th><th>Kernel</th></tr>");

    // Sort the OperatingModePolicies
    List policies = (List) blackboardQuery.query(omPolicyPredicate);
    try {
      Collections.sort(policies, ompComparator);
    } catch (ClassCastException e) {
      e.printStackTrace();
    }

    for (Iterator it = policies.iterator(); it.hasNext();) {
      
      out.print(
          "<FORM METHOD=\"GET\" ACTION=\"/$"+
          getEncodedAgentName()+
          getPath()+
          "\" target=\""+AE_FRAME+"\">\n"+
          "<input type=hidden name=\""+FRAME+
          "\" value=\""+AE_FRAME+"\">\n");
      
      OperatingModePolicy omp = (OperatingModePolicy) it.next();
      out.println ("<tr> <td>");
      out.print(omp.getName());
      out.println("</td><td>");
      out.println(omp.getAuthority());
      out.println("</td><td>");
      out.print(omp.getUID());
      out.println("</td>");

      out.print("<td> <INPUT TYPE=\"text\" NAME=");
      out.print(KERNEL);
      out.print(" VALUE=\"");
      out.print(omp.getPolicyKernel().toString());
      out.print(";\"SIZE=80> </td>");

      out.println("<td> <input type=submit value=\"Submit\"></td></tr>");

      out.print("<td> <INPUT TYPE=hidden NAME=");
      out.print(CHANGE);
      out.print(" VALUE=\"");
      out.print(POLICY);
      out.println("\" <td>");

      out.print("<td> <INPUT TYPE=hidden NAME=");
      out.print(UID);
      out.print(" VALUE=\"");
      out.print(omp.getUID());
      out.print("\" </td>");

      out.println("</form>");
    }
    out.println("</table></body></html>");	
  }

  private class UIDPredicate implements UnaryPredicate { 
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   String uid;
    public UIDPredicate(String uidString) {
      uid = uidString;
    }
	
    public boolean execute(Object o) {
      if (o instanceof OperatingModePolicy) {
	OperatingModePolicy omp = (OperatingModePolicy) o;
	if (uid.equals(omp.getUID().toString())) {
	  return true;
	}
      }
      return false;
    }
  }

  private class OMByNamePredicate implements UnaryPredicate { 
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   String name;
    public OMByNamePredicate(String omName) {
      name = omName;
    }
	
    public boolean execute(Object o) {
      if (o instanceof OperatingMode) {
	OperatingMode om = (OperatingMode) o;
	if (name.equals(om.getName())) {
	  return true;
	}
      }
      return false;
    }
  }

  private class OMComparator implements Comparator {

    // alphabetical sort
    public int compare(Object o1, Object o2) {
      if ((o1 instanceof OperatingMode) &&
	  (o2 instanceof OperatingMode)) {
	OperatingMode om1 = (OperatingMode) o1;
	OperatingMode om2 = (OperatingMode) o2;
	
	String om1Name = om1.getName();
	String om2Name = om2.getName();
      
	return om1Name.compareTo(om2Name);
      } 
      throw new ClassCastException("Expecting OperatingMode");
    }

    @Override
   public boolean equals(Object other) {
      if (other instanceof OMComparator) {
	return true;
      }
      return false;
    }

   @Override
   public int hashCode() {
      // TODO Auto-generated method stub
      return super.hashCode();
   }
  }


  private class OMPComparator implements Comparator {

    // alphabetical sort
    public int compare(Object o1, Object o2) {
      if ((o1 instanceof OperatingModePolicy) &&
	  (o2 instanceof OperatingModePolicy)) {
	OperatingModePolicy omp1 = (OperatingModePolicy) o1;
	OperatingModePolicy omp2 = (OperatingModePolicy) o2;
	
	String omp1Name = omp1.getName();
	String omp2Name = omp2.getName();
      
	return omp1Name.compareTo(omp2Name);
      } 
      throw new ClassCastException("Expecting OperatingModePolicy");
    }

    @Override
   public boolean equals(Object other) {
      if (other instanceof OMComparator) {
	return true;
      }
      return false;
    }

   @Override
   public int hashCode() {
      // TODO Auto-generated method stub
      return super.hashCode();
   }
  }
}

