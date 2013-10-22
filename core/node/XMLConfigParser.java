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

package org.cougaar.core.node;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cougaar.bootstrap.SystemProperties;

/**
 * Utility class used by the {@link
 * XMLComponentInitializerServiceComponent} to parse the agent
 * configurations.
 * <p>
 * <pre>
 * @property org.cougaar.node.name
 *   The name for this Node.
 *
 * @property org.cougaar.society.xml.validate 
 *    Indicates if the XML parser should be validating or not.
 *    Defaults to "false".
 * @property org.cougaar.node.validate
 *    Same as "-Dorg.cougaar.society.xml.validate" 
 * @property org.cougaar.society.xsl.checkXML
 *    Check for an XSL stylesheet, e.g.:
 *      &lt;?xml-stylesheet type="text/xml" href="society.xsl"?&gt;
 *    Defaults to "true".
 * @property org.cougaar.society.xsl.default.file
 *    Default XSL stylesheet if "-Dorg.cougaar.society.xsl.checkXML"
 *    is false or an xml-stylesheet is not found.  Defaults to
 *    null. 
 * @property org.cougaar.society.xsl.dynamic.file
 *    Dynamic XSL stylesheet that generates the XSL stylesheet
 *    based upon the XML file contents, unless an XSL stylesheet
 *    is specified in the XML file
 *    (-Dorg.cougaar.society.xsl.checkXML) or specified
 *    (-Dorg.cougaar.society.xsl.default.file).  Defaults to
 *    "make_society.xsl".
 * @property org.cougaar.society.xsl.param.*
 *    XSL parameters passed to the xml-stylesheet or default XSL
 *    stylesheet, where the above system property prefix is
 *    removed.  For example, if a system property is:
 *       -Dorg.cougaar.society.xsl.param.foo=bar
 *    then the parameter "foo=bar" will be passed to the XSL
 *    file's optional parameter:
 *       &lt;xsl:param name="foo"&gt;my_default&lt;/xsl:param&gt;
 * </pre>
 */ 
public final class XMLConfigParser {

  // xsl options:

  private static final boolean VALIDATE =
    SystemProperties.getBoolean("org.cougaar.society.xml.validate") ||
    SystemProperties.getBoolean("org.cougaar.core.node.validate");

  private static final boolean USE_XML_STYLESHEET = 
    SystemProperties.getBoolean(
        "org.cougaar.society.xsl.checkXML",
        true);

  private static final String DEFAULT_XSL_FILE_NAME =
    SystemProperties.getProperty(
        "org.cougaar.society.xsl.default.file",
        null);

  private static final String DYNAMIC_XSL_FILE_NAME = 
    SystemProperties.getProperty(
        "org.cougaar.society.xsl.dynamic.file",
        "make_society.xsl");

  private static final String XSL_PARAM_PROP_PREFIX =
    "org.cougaar.society.xsl.param.";
  private static final Map XSL_PARAMS =
    getSystemPropertiesAndTrim(XSL_PARAM_PROP_PREFIX);

  public static final Map parseAgents(
      String filename,
      String nodename,
      String agentname,
      Map param_overrides) {

    XSLTransformer xslt = new XSLTransformer();

    // set xml filename
    xslt.setXMLFileName(filename);

    // set defaults
    xslt.setValidate(VALIDATE);
    xslt.setUseXMLStylesheet(USE_XML_STYLESHEET);
    xslt.setDefaultXSLFileName(DEFAULT_XSL_FILE_NAME);
    xslt.setDynamicXSLFileName(DYNAMIC_XSL_FILE_NAME);

    // override some of the default XSL params:
    xslt.setDefaultXSLParams(
        override_params(XSL_PARAMS, param_overrides));
    xslt.setDynamicXSLParams(
        override_params(XSL_PARAMS, param_overrides));

    // set our sax handler
    XMLConfigHandler handler =
      new XMLConfigHandler(nodename, agentname);
    xslt.setContentHandler(handler);

    try {
      // parse, call our handler
      xslt.parse();
    } catch (Exception e) {
      String msg = "Unable to parse XML file \""+filename+"\"";
      // look for a simple file-not-found error
      FileNotFoundException fnfe = null;
      for (Throwable t = e; t != null; t = t.getCause()) {
        if (t instanceof FileNotFoundException) {
          fnfe = (FileNotFoundException) t;
          break;
        }
      }
      if (fnfe == null) {
        // xslt error?  append extra detail
        msg += ", detail="+xslt;
      } else {
        // simple file-not-found
        if (fnfe.getMessage() != null) { 
          msg += ", file not found: "+fnfe.getMessage();
        }
      }
      throw new RuntimeException(msg, e);
    }

    // ask handler for agents
    Map ret = handler.getAgents(); 

    return ret;
  }

  /**
   * Utility method to find all system properties with the specified
   * prefix and create an unmodifiable map of the trailing
   * "name=value" pairs.
   * E.g. if prefix is "a.b.", and system properties are "-Da.b.x=y"
   * and "-Da.b.p=q", then this method will return "{x=y, p=q}".
   */
  private static final Map getSystemPropertiesAndTrim(String prefix) {
    Properties props =
      SystemProperties.getSystemPropertiesWithPrefix(
          prefix);
    Map ret;
    if (props.isEmpty()) {
      ret = Collections.EMPTY_MAP;
    } else {
      ret = new HashMap(props.size());
      for (Enumeration en = props.propertyNames();
          en.hasMoreElements();
          ) {
        String name = (String) en.nextElement();
        String key = name.substring(prefix.length());
        String value = props.getProperty(name);
        ret.put(key, value);
      }
      ret = Collections.unmodifiableMap(ret);
    }
    return ret;
  }

  /** copy the "original" map and putAll the "overrides" */
  private static Map override_params(
      Map original,
      Map overrides) {
    if (overrides == null || overrides.isEmpty()) {
      return original;
    }
    Map ret = new HashMap(original);
    ret.putAll(overrides);
    ret = Collections.unmodifiableMap(ret);
    return ret;
  }
}
