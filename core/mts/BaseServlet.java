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

package org.cougaar.core.mts;

import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.ServletService;

/**
 * Basic {@link javax.servlet.Servlet} with a refresh URL-parameter.
 */
public abstract class BaseServlet extends HttpServlet 
{
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private MessageAddress nodeID;

  public BaseServlet(ServiceBroker sb) {
    ServletService servletService = sb.getService(this, ServletService.class, null);
    if (servletService == null) {
      throw new RuntimeException("Unable to obtain ServletService");
    }

    NodeIdentificationService node_id_svc = sb.getService(this, NodeIdentificationService.class, null);
    nodeID = node_id_svc.getMessageAddress();


    // register our servlet
    try {
      servletService.register(getPath(), this);
    } catch (Exception e) {
      throw new RuntimeException("Unable to register servlet at path <"
          +getPath()+ ">: " +e.getMessage());
    }
  }

  protected abstract String getPath();
  protected abstract String getTitle();
  protected abstract void printPage(HttpServletRequest request,
      PrintWriter out);


  public MessageAddress getNodeID() {
    return nodeID;
  }

  @Override
public void doGet(HttpServletRequest request,
      HttpServletResponse response) 
    throws java.io.IOException 
    {
      String refresh = request.getParameter("refresh");
      int refreshSeconds = 
        ((refresh != null) ?
         Integer.parseInt(refresh) :
         0);

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      out.print("<html><HEAD>");
      if (refreshSeconds > 0) {
        out.print("<META HTTP-EQUIV=\"refresh\" content=\"");
        out.print(refreshSeconds);
        out.print("\">");
      }
      out.print("<TITLE>");
      out.print(getTitle());
      out.print("</TITLE></HEAD><body><H1>");
      out.print(getTitle());
      out.print("</H1>");

      out.print("Date: ");
      out.print(new java.util.Date()+"\n");

      printPage(request,out);
      out.print("<p><p><br>RefreshSeconds: ");	
      out.print(refreshSeconds);

      out.print("</body></html>\n");

      out.close();
    }
}
