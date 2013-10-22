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

package org.cougaar.core.mobility.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * Servlet that allows the client to add {@link AgentControl} objects
 * to the blackboard and monitor their progress, such as agent
 * mobility.
 * <p>
 * The path of the servlet is "/move".
 * <p>
 * The URL parameters to this servlet are:
 * <ul><p>
 *   <li><tt>action=STRING</tt><br>
 *       Option action selection, where the default is "Refresh".
 *       <p>
 *       "Refresh" displays the current status.
 *       <p>
 *       "Add" creates a new AgentControl request.  Most of the
 *       parameters below are used to support this action.
 *       <p>
 *       "Remove" removes the AgentControl with the UID specified
 *       by the required "removeUID" parameter.
 *       <p>
 *       </li><p>
 *   <li><tt>op=String</tt><br>
 *       Required operation, which may be "Move", "Add", or 
 *       "Remove".  If none is specified then "Move" is assumed
 *       (for backwards compatibility).
 *   <li><tt>removeUID=String</tt><br>
 *       If the action is "Remove", this is the UID of the script
 *       to be removed.  Any running processes are killed.
 *       </li><p>
 *   <li><tt>mobileAgent=STRING</tt><br>
 *       Option agent to move.  Defaults to this servlet's agent.
 *       </li><p>
 *   <li><tt>originNode=STRING</tt><br>
 *       Option origin node for the mobile agent.  Defaults to 
 *       wherever the agent happens to be at the time of the submit.
 *       If set, the move will assert the agent starting node 
 *       location.
 *       </li><p>
 *   <li><tt>destNode=STRING</tt><br>
 *       Option destination node for the mobile agent.  Defaults 
 *       to wherever the agent happens to be at the time of
 *       the submit.
 *       </li><p>
 *   <li><tt>isForceRestart=BOOLEAN</tt><br>
 *       Only applies when the destNode is not specified or
 *       matches the current agent location.  If true, the agent
 *       will undergo most of the move work, even though it's
 *       already at the specified destination node.
 *       </li><p>
 * </ul>
 * <p>
 * Note the <b>SECURITY</b> issues of moving agents!
 */
public class MoveAgentServlet
extends BaseServletComponent
implements BlackboardClient
{
  protected MessageAddress localAgent;
  protected MessageAddress localNode;

  protected DomainService domain;
  protected AgentIdentificationService agentIdService;
  protected NodeIdentificationService nodeIdService;
  protected BlackboardService blackboard;

  protected MobilityFactory mobilityFactory;

  protected static final UnaryPredicate AGENT_CONTROL_PRED =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return (o instanceof AgentControl);
      }
    };


  @Override
protected String getPath() {
    return "/move";
  }

  @Override
protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  public void setAgentIdentificationService(
      AgentIdentificationService agentIdService) {
    this.agentIdService = agentIdService;
    if (agentIdService != null) {
      this.localAgent = agentIdService.getMessageAddress();
      //    } else {      // Revocation - nothing more to do
    }
  }

  public void setNodeIdentificationService(
      NodeIdentificationService nodeIdService) {
    this.nodeIdService = nodeIdService;
    if (nodeIdService != null) {
      this.localNode = nodeIdService.getMessageAddress();
      //    } else {      // Revocation - nothing more to do
    }
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  public void setDomainService(DomainService domain) {
    this.domain = domain;
    if (domain == null) {
      mobilityFactory = null;
    } else {
      mobilityFactory = 
        (MobilityFactory) domain.getFactory("mobility");
    }
  }

  // release services:
  @Override
public void unload() {
    super.unload();
    if (blackboard != null) {
      releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    if (domain != null) {
      releaseService(
          this, DomainService.class, domain);
      domain = null;
    }
    if (nodeIdService != null) {
      releaseService(
          this, NodeIdentificationService.class, nodeIdService);
      nodeIdService = null;
    }
    if (agentIdService != null) {
      releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
  }

  protected Collection queryAgentControls() {
    Collection ret = null;
    try {
      blackboard.openTransaction();
      ret = blackboard.query(AGENT_CONTROL_PRED);
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected AgentControl queryAgentControl(final UID uid) {
    if (uid == null) {
      throw new IllegalArgumentException("null uid");
    }
    UnaryPredicate pred = new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return 
          ((o instanceof AgentControl) &&
           (uid.equals(((AgentControl) o).getUID())));
      }
    };
    AgentControl ret = null;
    try {
      blackboard.openTransaction();
      Collection c = blackboard.query(pred);
      if ((c != null) && (c.size() >= 1)) {
        ret = (AgentControl) c.iterator().next();
      }
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected AgentControl createAgentControl(
      UID ownerUID, 
      MessageAddress target, 
      AbstractTicket ticket) {
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "Mobility factory (and domain) not enabled");
    }
    AgentControl ac = 
      mobilityFactory.createAgentControl(
          ownerUID, target, ticket);
    return ac;
  }

  protected void addAgentControl(AgentControl ac) {
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(ac);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  protected void removeAgentControl(AgentControl ac) {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(ac);
    } finally {
      blackboard.closeTransaction();
    }
  }

  /**
   * Servlet to handle requests.
   */
  private class MyServlet extends HttpServlet {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Override
   public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
      MyWorker mw = new MyWorker(req, res);
      mw.execute();
    }

    private class MyWorker {

      // from the "doGet(..)":
      private HttpServletRequest request;
      private HttpServletResponse response;

      // action:
      public static final String ACTION_PARAM = "action";
      public static final String ADD_VALUE = "Add";
      public static final String REMOVE_VALUE = "Remove";
      public static final String REFRESH_VALUE = "Refresh";
      public String action;

      // operation:
      public static final String OP_PARAM = "op";
      public static final String ADD_OP_VALUE = "Add";
      public static final String MOVE_OP_VALUE = "Move";
      public static final String REMOVE_OP_VALUE = "Remove";
      public String op;

      // remove uid:
      public static final String REMOVE_UID_PARAM = "removeUID";
      public String removeUID;

      // ticket options:
      public static final String MOBILE_AGENT_PARAM = "mobileAgent";
      public String mobileAgent;
      public static final String ORIGIN_NODE_PARAM = "originNode";
      public String originNode;
      public static final String DEST_NODE_PARAM = "destNode";
      public String destNode;
      public static final String IS_FORCE_RESTART_PARAM = "isForceRestart";
      public boolean isForceRestart;
      
      // worker constructor:
      public MyWorker(
          HttpServletRequest request,
          HttpServletResponse response) {
        this.request = request;
        this.response = response;
      }

      // handle a request:
      public void execute() throws IOException {
        parseParams();
        writeResponse();
      }

      private void parseParams() {

        // action:
        action = request.getParameter(ACTION_PARAM);

        // operation:
        op = request.getParameter(OP_PARAM);
        if ((op != null) && (op.length() == 0)) {
          op = null;
        }

        // remove param:
        removeUID = request.getParameter(REMOVE_UID_PARAM);
        if ((removeUID != null) && (removeUID.length() == 0)) {
          removeUID = null;
        }

        // ticket options:

        mobileAgent = request.getParameter(MOBILE_AGENT_PARAM);
        if ((mobileAgent != null) && (mobileAgent.length() == 0)) {
          mobileAgent = null;
        }

        destNode = request.getParameter(DEST_NODE_PARAM);
        if ((destNode != null) && (destNode.length() == 0)) {
          destNode = null;
        }

        originNode = request.getParameter(ORIGIN_NODE_PARAM);
        if ((originNode != null) && (originNode.length() == 0)) {
          originNode = null;
        }

        isForceRestart = "true".equalsIgnoreCase(
            request.getParameter(IS_FORCE_RESTART_PARAM));
      }

      private AgentControl createAgentControl() {
        MessageAddress mobileAgentAddr =
          (mobileAgent != null ?
           MessageAddress.getMessageAddress(mobileAgent) :
           localAgent);
        MessageAddress destNodeAddr = 
          (destNode != null ?
           (MessageAddress.getMessageAddress(destNode)) :
           null);
        MessageAddress target;
        AbstractTicket ticket;
        if (op == null ||
            op.equalsIgnoreCase(MOVE_OP_VALUE)) {
          MessageAddress originNodeAddr =
            (originNode != null ?
             (MessageAddress.getMessageAddress(originNode)) :
             null);
          target = 
            (originNodeAddr != null ?
             (originNodeAddr) : 
             mobileAgentAddr);
          if (destNodeAddr == null &&
              originNodeAddr != null) {
            destNodeAddr = originNodeAddr;
          }
          ticket =
            new MoveTicket(
                null,
                mobileAgentAddr,
                originNodeAddr,
                destNodeAddr,
                isForceRestart);
        } else {
          if (op.equalsIgnoreCase(ADD_OP_VALUE)) {
            if (destNodeAddr == null) {
              destNodeAddr = localNode;
            }
            target = destNodeAddr;
            ticket = 
              new AddTicket(
                  null,
                  mobileAgentAddr,
                  destNodeAddr);
          } else if (op.equalsIgnoreCase(REMOVE_OP_VALUE)) {
            target = 
              (destNodeAddr != null ? destNodeAddr : mobileAgentAddr);
            ticket = 
              new RemoveTicket(
                  null,
                  mobileAgentAddr,
                  destNodeAddr);
          } else {
            throw new IllegalArgumentException(
                "Invalid url-parameter: \""+OP_PARAM+"\"="+op);
          }
        }
        AgentControl ac = 
          MoveAgentServlet.this.createAgentControl(
              null, target, ticket);
        return ac;
      }

      private void writeResponse() throws IOException {
        if (ADD_VALUE.equals(action)) {
          try {
            AgentControl ac = createAgentControl();
            addAgentControl(ac);
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess();
        } else if (REMOVE_VALUE.equals(action)) {
          try {
            UID uid = UID.toUID(removeUID);
            AgentControl ac = queryAgentControl(uid);
            if (ac != null) {
              removeAgentControl(ac);
            }
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess();
        } else {
          writeUsage();
        }
      }

      private void writeUsage() throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        writeHeader(out);
        out.print(
            "<center>"+
            "<h2>Agent Control (usage)</h2>"+
            "</center>\n");
        writeForm(out);
        out.print(
            "</body></html>\n");
        out.close();
      }

      private void writeFailure(Exception e) throws IOException {
        // select response message
        response.setContentType("text/html");
        // build up response
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        writeHeader(out);
        out.print(
            "<center>"+
            "<h2>Agent Control (failed)</h2>"+
            "</center>"+
            "<p><pre>\n");
        e.printStackTrace(out);
        out.print(
            "\n</pre><p>"+
            "<h3>Please double-check these parameters:</h3>\n");
        writeForm(out);
        out.print(
            "</body></html>\n");
        out.close();
        // send error code
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            new String(baos.toByteArray()));
      }

      private void writeSuccess() throws IOException {
        // write response
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        writeHeader(out);
        out.print(
            "<center><h2>"+
            "Agent control (success)"+
            "</h2></center><p>\n");
        writeForm(out);
        out.print("</body></html>\n");
        out.close();
      }

      private void writeHeader(
          PrintWriter out) {
        out.print("<html><head><title>");
        out.print(localAgent);
        out.print(
            " agent control</title>\n"+
            "<script language=\"JavaScript\">\n"+
            "<!--\n"+
            "function selectOp() {\n"+
            "  var iop = document.f.op.selectedIndex;\n"+
            "  var sop = document.f.op.options[iop].text;\n"+
            "  enableOp(sop != \""+MOVE_OP_VALUE+"\");\n"+
            "}\n"+
            "function enableOp(b) {\n"+
            "  document.f.originNode.disabled=b;\n"+
            "  document.f.isForceRestart.disabled=b;\n"+
            "}\n"+
            "// -->\n"+
            "</script>\n"+
            "</head>"+
            "<body onLoad=\"selectOp();\">\n");
      }

      private void writeForm(
          PrintWriter out) {
        // begin form
        out.print("<form name=\"f\" method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print(
            "\">\n"+
            "<i>Agent:</i> ");
        out.print(localAgent);
        out.print(
            "<br>"+
            "<i>Node:</i> ");
        out.print(localNode);
        out.print(
            "<br>"+
            "<i>Time:</i> ");
        Date d = new Date();
        out.print(d);
        out.print(" (");
        out.print(d.getTime());
        out.print(
            ")"+
            "<p>"+
            "<h3>Local requests:</h3><p>");
        // show current AgentControl objects
        Collection c = queryAgentControls();
        int n = ((c != null) ? c.size() : 0);
        if (n == 0) {
          out.print("<i>none</i>");
        } else {
          out.print(
              "<table border=1 cellpadding=1\n"+
              " cellspacing=1 width=95%\n"+
              " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
              "<tr>"+
              "<td>\n"+
              "<font size=+1 color=mediumblue><b>UID</b></font>"+
              "</td>"+
              "<td>"+
              "<font size=+1 color=mediumblue><b>Ticket</b></font>"+
              "</td>"+
              "<td>"+
              "<font size=+1 color=mediumblue><b>Status</b></font>"+
              "</td>"+
              "</tr>\n");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            AgentControl ac = (AgentControl) iter.next();
            out.print("<tr><td>");
            out.print(ac.getUID());
            out.print("</td><td>");
            out.print(ac.getAbstractTicket());
            out.print("</td><td bgcolor=\"");
            int status = ac.getStatusCode();
            if (status == AgentControl.NONE) {
              out.print(
                  "#FFFFBB\">"+ // yellow
                  "In progress");
            } else {
              out.print(
                  (status != AgentControl.FAILURE) ?
                  "#BBFFBB\">" : // green
                  "#FFBBBB\">"); // red
              out.print(ac.getStatusCodeAsString());
            }
            out.print("</td></tr>\n");
          }
          out.print("</table>\n");
        }
        out.print(
            "<p><input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            REFRESH_VALUE+
            "\">\n");

        // allow user to remove an existing AgentControl
        out.print(
            "<p>"+
            "<h3>Remove an existing request:</h3>\n");
        if (n > 0) {
          out.print(
              "<select name=\""+
              REMOVE_UID_PARAM+
              "\">");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            AgentControl ac = (AgentControl) iter.next();
            UID uid = ac.getUID();
            out.print("<option value=\"");
            out.print(uid);
            out.print("\">");
            out.print(uid);
            out.print("</option>");
          }
          out.print(
              "</select>"+
              "<input type=\"submit\" name=\""+
              ACTION_PARAM+
              "\" value=\""+
              REMOVE_VALUE+
              "\">\n");
        } else {
          out.print("<i>none</i>");
        }

        // allow user to submit a new AgentControl request
        out.print(
            "<p>"+
            "<h3>Create a new request:</h3>\n");
        out.print(
            "<table>\n"+
            "<tr><td>"+
            "Operation"+
            "</td><td>\n"+
            "<select name=\""+
            OP_PARAM+
            "\" onChange=\"selectOp();\">\n"+
            "<option");
        if (ADD_OP_VALUE.equalsIgnoreCase(op)) {
          out.print(" selected");
        }
        out.print(
            ">"+ADD_OP_VALUE+"</option>\n"+
            "<option");
        if (op == null ||
            MOVE_OP_VALUE.equalsIgnoreCase(op)) {
          out.print(" selected");
        }
        out.print(
            ">"+MOVE_OP_VALUE+"</option>\n"+
            "<option");
        if (REMOVE_OP_VALUE.equalsIgnoreCase(op)) {
          out.print(" selected");
        }
        out.print(
            ">"+REMOVE_OP_VALUE+"</option>\n"+
            "</select>\n"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Mobile Agent"+
            "</td><td>\n"+
            "<input type=\"text\" name=\""+
            MOBILE_AGENT_PARAM+
            "\" size=\"30\"");
        if (mobileAgent != null) {
          out.print(" value=\"");
          out.print(mobileAgent);
          out.print("\"");
        }
        out.print(
            ">"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Origin Node"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            ORIGIN_NODE_PARAM+
            "\" size=\"30\"");
        if (originNode != null) {
          out.print(" value=\"");
          out.print(originNode);
          out.print("\"");
        }
        out.print(
            ">"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Destination Node"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            DEST_NODE_PARAM+
            "\" size=\"30\"");
        if (destNode != null) {
          out.print(" value=\"");
          out.print(destNode);
          out.print("\"");
        }
        out.print(
            ">"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Force Restart"+
            "</td><td>"+
            "<select name=\""+
            IS_FORCE_RESTART_PARAM+
            "\">"+
            "<option value=\"true\"");
        if (isForceRestart) {
          out.print(" selected");
        }
        out.print(
            ">true</option>"+
            "<option value=\"false\"");
        if (!(isForceRestart)) {
          out.print(" selected");
        }
        out.print(
            ">false</option>"+
            "</select>\n"+
            "</td></tr>\n"+
            "<tr><td colwidth=2>"+
            "<input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            ADD_VALUE+
            "\">"+
            "<input type=\"reset\">"+
            "</td></tr>\n"+
            "</table>\n"+
            "</form>\n");
      }
    }
  }


  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return getPath();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    //
    // see "ComponentPlugin" for details.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }

  @Override
public String toString() {
    return "\""+getPath()+"\" servlet";
  }
}
