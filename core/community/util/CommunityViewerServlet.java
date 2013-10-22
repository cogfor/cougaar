/*
 * <copyright>
 *
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.community.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.community.util.Semaphore;

import java.util.*;

/**
 * A servlet for viewing community state.
 * Load into any agent:
 *   plugin = org.cougaar.community.util.CommunityViewerServlet
 */
public class CommunityViewerServlet extends BaseServletComponent implements BlackboardClient{

  private CommunityService cs;
  private static LoggingService log;
  private PrintWriter out;
  private String agentId;

  /**
   * Hard-coded servlet path.
   * @return Servlet path
   */
  protected String getPath() {
    return "/communityViewer";
  }

  public void setCommunityService(CommunityService cs){
    this.cs = cs;
  }

  /**
   * Create the servlet.
   * @return Servlet
   */
  protected Servlet createServlet() {
    log =  getService(this, LoggingService.class, null);
    AgentIdentificationService ais = getService(
        this, AgentIdentificationService.class, null);
    if (ais != null) {
      this.agentId = ais.getMessageAddress().toString();
      releaseService(this, AgentIdentificationService.class, ais);
    }
    log = org.cougaar.core.logging.LoggingServiceWithPrefix.add(log, agentId + ": ");
    return new MyServlet();
  }

  private class MyServlet extends HttpServlet {
    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
        out = res.getWriter();
        parseParams(req);
    }
  }

  private String format = "", currentXML = "", communityShown;
  private String command = "", target = "";
  private void parseParams(HttpServletRequest request) throws IOException
    {
      format = "html";
      command = "";
      target = "";
      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis =
        new ServletUtil.ParamVisitor() {
          public void setParam(String name, String value) {
            if(name.equalsIgnoreCase("format"))
              format = value;
            if(name.equalsIgnoreCase("community")) {
              command = "showCommunity";
              target = value;
              communityShown = value;
            }
            if(name.equals("attributes")) {
              command = "showAttributes";
              target = value;
            }
          }
        };
      // visit the URL parameters
      ServletUtil.parseParams(vis, request);
      if(command.equals(""))
        showFrontPage();
      else {
        displayParams(command, target);
      }
    }

  Collection pcomms = new ArrayList();
  //The first page when user call this servlet will show all communities who are
  //direct parents of calling agent.
  private void showFrontPage() {
   final Semaphore s = new Semaphore(0);
   cs.listAllCommunities(new CommunityResponseListener() {
      public void getResponse(CommunityResponse resp) {
        pcomms = (Collection)resp.getContent();
        s.release();
      }
    });
    Collection local = cs.listParentCommunities(agentId.toString(), "(CommunityType=*)", null);

    out.print("<html><title>communityViewer</title>\n");
    out.print("<body>\n<h3>Local community:</h3><ol>\n");
    for(Iterator it = local.iterator(); it.hasNext();) {
      String name = (String)it.next();
      out.print("<li><a href=./communityViewer?community=" + name + ">" + name + "</a>\n");
    }
    out.print("</ol><br><br><h3>Remote community:</h3><ol>\n");
    for(Iterator it = pcomms.iterator(); it.hasNext();) {
      String name = (String)it.next();
      if(!local.contains(name))
        out.print("<li><a href=./communityViewer?community=" + name + ">" + name + "</a>\n");
    }
    out.print("</body>\n</html>\n");

  }

  private static Hashtable table = new Hashtable();
  private void displayParams(String command, String value){
    try{
      Community community = null;
      final Semaphore s = new Semaphore(0);
      community = cs.getCommunity(communityShown, new CommunityResponseListener(){
        public void getResponse(CommunityResponse resp){
          communityChangeNotification((Community)resp.getContent());
          s.release();
        }
      });
      if (community != null) {
        communityChangeNotification(community);
      } else {
        try {
          s.acquire();
        } catch (InterruptedException e) {}
      }
      community = (Community)table.get(communityShown);
      currentXML = community.toXml();
      if(format.equals("xml"))
        out.write(currentXML);
      else {
        if(command.equals("showCommunity")){
          out.print(getHTMLFromXML(currentXML, communityViewer));
        }
        else {
          String xml = "";
          if(value.equals(communityShown)) {
            //make sure this element do have attributes
            int temp1 = currentXML.indexOf("<Attributes>");
            int temp2 = currentXML.indexOf("<", currentXML.indexOf("Community"));
            if(temp1 == temp2){
              xml = currentXML.substring(0, currentXML.indexOf("</Attributes>"));
              xml += "</Attributes></Community>";
            }
            else {
              xml = "<Community Name=\"" + communityShown + "\"></Community>";
            }
          }else {
            String temp = "name=\"" + value + "\"";
            int index = currentXML.indexOf(temp);
            int firstIndex = currentXML.substring(0, index).lastIndexOf("<");
            int temp1 = currentXML.indexOf("<Attributes", firstIndex);
            int temp2 = currentXML.indexOf("<", firstIndex+1);
            if(temp1 == temp2){
              index = currentXML.indexOf("</Attributes>", firstIndex);
              int lastIndex = currentXML.indexOf(">", index+13);
              xml = currentXML.substring(firstIndex, lastIndex+1);
            }else {
              index = currentXML.indexOf("</", firstIndex);
              int lastIndex = currentXML.indexOf(">", index);
              xml = currentXML.substring(firstIndex, lastIndex+1);
            }
          }
          out.print(getHTMLFromXML(xml, attributesViewer));
        }
      }
    }catch(Exception e){e.printStackTrace();}
  }

  public static void communityChangeNotification(Community community){
    if(table.containsKey(community.getName())){
      table.remove(community.getName());
      table.put(community.getName(), community);
    }else {
      table.put(community.getName(), community);
    }
  }

  /**
   * Using xsl file to transform a xml file into html file.
   * @param xml given xml string
   * @param xsl name of xsl file
   * @return the html string
   */
  private String getHTMLFromXML(String xml, String xsl)
  {
    String html = "";
    try{
      TransformerFactory tFactory = TransformerFactory.newInstance();
      //File xslf = ConfigFinder.getInstance().locateFile(xsl);
      //xsl = "/configs/common/" + xsl;
      //InputStream in = CommunityViewerServlet.class.getResourceAsStream(xsl);
      //Transformer transformer = tFactory.newTransformer(new StreamSource(in));
      Transformer transformer = tFactory.newTransformer(new StreamSource(new StringReader(xsl)));
      StringWriter writer = new StringWriter();
      transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
      html = writer.toString();
    }catch(Exception e){log.error(e.getMessage());}
    return html;
  }

  /**
   * Selects CommunityDescriptors that are sent by remote community manager
   * agent.
   */
  private UnaryPredicate communityPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return (o instanceof Community);
  }};

  // BlackboardClient method:
  public String getBlackboardClientName() {
                return toString();
  }

  // unused BlackboardClient method:
  public long currentTimeMillis() {
    return new Date().getTime();
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    return false;
  }

  private static final String attributesViewer =
    "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
    "<xsl:output method=\"html\" indent=\"yes\"/>\n" +
    "<xsl:template match=\"/\">\n" +
    "<xsl:variable name=\"community\">" +
    "<xsl:value-of select=\"//Community/@name\" /> " +
    "</xsl:variable>" +
    "<xsl:variable name=\"agent\">" +
    "<xsl:value-of select=\"//Agent/@name\" />" +
    "</xsl:variable>" +
    "<xsl:variable name=\"communityLength\">" +
    "<xsl:value-of select=\"string-length($community)\" />" +
    "</xsl:variable>" +
    "<xsl:variable name=\"agentLength\">" +
    "<xsl:value-of select=\"string-length($agent)\" />" +
    "</xsl:variable>" +
    "<html><head><title><xsl:choose>" +
    "<xsl:when test=\"$communityLength > 0\">" +
    "<xsl:text>Community </xsl:text>" +
    "<xsl:value-of select=\"$community\" />" +
    "</xsl:when><xsl:otherwise>" +
    "<xsl:text>Agent </xsl:text><xsl:value-of select=\"$agent\" />" +
    "</xsl:otherwise></xsl:choose></title></head>" +
    "<body><center><br /><H1><xsl:choose>" +
    "<xsl:when test=\"$communityLength > 0\">" +
    "<xsl:text>Community </xsl:text><xsl:value-of select=\"$community\" />" +
    "</xsl:when><xsl:otherwise><xsl:text>Agent </xsl:text>" +
    "<xsl:value-of select=\"$agent\" /></xsl:otherwise></xsl:choose></H1><br />" +
    "<table border=\"1\" cellpadding=\"10\" cellspacing=\"0\">" +
    "<th><xsl:text>Attribute Id</xsl:text></th>" +
    "<th><xsl:text>Attribute Value</xsl:text></th>" +
    "<xsl:apply-templates select=\"//Attributes\" />" +
    "</table></center></body></html></xsl:template>" +
    "<xsl:template match=\"Attributes\">" +
    "<xsl:for-each select=\"//Attribute\">" +
    "<xsl:variable name=\"count\"><xsl:value-of select=\"count(.//Value)\" /></xsl:variable>" +
    "<xsl:variable name=\"id\"><xsl:value-of select=\"@id\" /></xsl:variable>" +
    "<xsl:choose><xsl:when test=\"$count=1\">" +
    "<tr><td><xsl:value-of select=\"$id\" /></td>" +
    "<td><xsl:value-of select=\".//Value\" /></td></tr></xsl:when>" +
    "<xsl:otherwise><xsl:for-each select=\".//Value\"><xsl:choose>" +
    "<xsl:when test=\"position() = 1\">" +
    "<tr><td><xsl:value-of select=\"$id\" /></td><td><xsl:value-of select=\".\" /></td>" +
    "</tr></xsl:when><xsl:otherwise>" +
    "<tr><td /><td><xsl:value-of select=\".\" /></td></tr>" +
    "</xsl:otherwise></xsl:choose></xsl:for-each></xsl:otherwise></xsl:choose></xsl:for-each>" +
    "</xsl:template></xsl:stylesheet>";

  private static final String communityViewer =
    "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" +
    "<xsl:output method=\"html\" indent=\"yes\"/>" +
    "<xsl:template match=\"/\"><xsl:variable name=\"community\">" +
    "<xsl:value-of select=\"//Community/@name\" /></xsl:variable>" +
    "<html><head><title><xsl:text>Community </xsl:text><xsl:value-of select=\"$community\" />" +
    "</title></head><body><br /><H1>" +
    "<xsl:element name=\"a\"><xsl:attribute name=\"href\">" +
    "<xsl:text>./communityViewer?attributes=</xsl:text>" +
    "<xsl:value-of select=\"$community\" /></xsl:attribute>" +
    "<xsl:text>Community </xsl:text><xsl:value-of select=\"$community\" />" +
    "</xsl:element></H1><br /><ol>" +
    "<xsl:for-each select=\"Community/Community\">" +
    "<li><xsl:element name=\"a\"><xsl:attribute name=\"href\">" +
    "<xsl:text>./communityViewer?community=</xsl:text>" +
    "<xsl:value-of select=\"@name\" /></xsl:attribute>" +
    "<xsl:text>Community </xsl:text><xsl:value-of select=\"@name\" />" +
    "</xsl:element></li></xsl:for-each>" +
    "<xsl:for-each select=\"//Agent\"><xsl:sort select=\"@name\" /><li>" +
    "<xsl:element name=\"a\"><xsl:attribute name=\"href\">" +
    "<xsl:text>./communityViewer?attributes=</xsl:text>" +
    "<xsl:value-of select=\"@name\" /></xsl:attribute>" +
    "<xsl:value-of select=\"@name\" /></xsl:element></li>" +
    "</xsl:for-each></ol></body></html></xsl:template></xsl:stylesheet>";

}
