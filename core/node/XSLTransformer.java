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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Generic XSL transformer that supports two-stage XSL transforms.
 * <p> 
 * If neither the "default" or "dynamic" XSL filenames are specified,
 * and the "use_xsl_stylesheet" is disabled or the XML file lacks a
 * top-level "&lt;?xml-stylesheet ..?&gt;" instruction, then
 * no transform is done and the XML file is simply parsed to the
 * ContentHandler.  Since no transform is done, this is somewhat
 * similar to:<pre> 
 *    cat XML_FILE 
 * </pre>
 * If the "default" XSL filename is set, or there's a top-level
 * "&lt;?xml-stylesheet ..?&gt;", then a single XSL transform is
 * performed, similar to:<pre>
 *    java\
 *      org.apache.xalan.xslt.Process\
 *      -in XML_FILE\
 *      -xsl DEFAULT_XSL_FILE
 * </pre>
 * The interesting case is when the "dynamic" XSL filename is set, in
 * which case two transforms are done:  the first transform on the XML
 * file creates an in-memory XSL file, and the second transform
 * applies the in-memory XSL to the XML file again:<pre>
 *    java\
 *      org.apache.xalan.xslt.Process\
 *      -in XML_FILE\
 *      -xsl DYNAMIC_XSL_FILE &gt; TMP_XSL
 *    java\
 *      org.apache.xalan.xslt.Process\
 *      -in XML_FILE\
 *      -xsl TMP_XSL
 * </pre>
 * <p>
 * Cougaar's XML society config parser uses the two-stage XSL
 * transform.  First "make_society.xsl" is used to extract the unique
 * agent XSL template "include" rules, creating an XSL file similar
 * to "society.xsl".  Next the XML file is re-transform with these
 * included template rules.
 * <p> 
 * Note that this class is <i>not</i> thread-safe!  Clients should
 * create a new instance per call or use an outer synchronize lock.
 */ 
public class XSLTransformer {

  private final Logger logger = Logging.getLogger(getClass());

  private String xml_input_file;
  private boolean validate;
  private boolean use_xml_stylesheet;
  private String t2_xsl_input_file;
  private String t1_xsl_input_file;
  private Map t1_xsl_params;
  private Map t2_xsl_params;

  private ContentHandler handler;
  private Resolver resolver;

  private SAXTransformerFactory saxTFactory;
  private XMLReader xml_reader;

  private Thread t1_pipe_thread;
  private TransformerHandler t2_transformer_handler;

  public XSLTransformer() {
    resolver = new ConfigFinderBasedResolver(ConfigFinder.getInstance());
  }

  /**
   * Set the name name of the XML file. E.g. "mySociety.xml"
   */
  public void setXMLFileName(String xml_file_name) {
    this.xml_input_file = xml_file_name;
  }
  public String getXMLFileName() {
    return xml_input_file;
  }

  /**
   * Enable validation of the XML file content format.
   */
  public void setValidate(boolean validate) {
    this.validate = validate;
  }
  public boolean getValidate() {
    return validate;
  }

  /**
   * Enable check for an XML file stylesheet instruction.
   * E.g. "&lt;?xml-stylesheet type="text/xml" href="society.xsl"?&gt;"
   */
  public void setUseXMLStylesheet(boolean use_xml_stylesheet) {
    this.use_xml_stylesheet = use_xml_stylesheet;
  }
  public boolean getUseXMLStylesheet() {
    return use_xml_stylesheet;
  }

  /**
   * If <code>(getUseXMLStylesheet() == false)</code>, or the XML
   * file does not specify an XSL stylesheet, then use this file.
   * E.g. "society.xsl".
   */ 
  public void setDefaultXSLFileName(String default_xsl_file_name) {
    this.t2_xsl_input_file = default_xsl_file_name;
  }
  public String getDefaultXSLFileName() {
    return t2_xsl_input_file;
  }

  /**
   * If <code>(getUseXMLStylesheet() == false)</code>, or the XML
   * file does not specify an XSL stylesheet, and
   * <code>(getDefaultXSLFileName() == null)</code>, then use this
   * XSL file to dynamically generate the XSL file.
   * E.g. "make_society.xsl"
   */
  public void setDynamicXSLFileName(String dynamic_xsl_file_name) {
    this.t1_xsl_input_file = dynamic_xsl_file_name;
  }
  public String getDynamicXSLFileName() {
    return t1_xsl_input_file;
  }

  /**
   * If the default_xsl_file_name will be used, set these XSL
   * parameters for that template file.
   * E.g. "wpserver=false"
   */
  public void setDefaultXSLParams(Map default_xsl_params) {
    this.t2_xsl_params = default_xsl_params;
  }
  public Map getDefaultXSLParams() {
    return t2_xsl_params;
  }

  /**
   * If the dynamic_xsl_file_name will be used, set these XSL
   * parameters for that template file.
   * E.g. "defaultAgent=SimpleAgent.xsl"
   */
  public void setDynamicXSLParams(Map dynamic_xsl_params) {
    this.t1_xsl_params = dynamic_xsl_params;
  }
  public Map getDynamicXSLParams() {
    return t1_xsl_params;
  }

  /**
   * Set the content handler for the transformed XML output.
   */
  public void setContentHandler(ContentHandler handler) {
    this.handler = handler;
  }
  public ContentHandler getContentHandler() {
    return handler;
  }

  /**
   * Set the file resolver (interface defined at the end of this
   * class), where the default is a ConfigFinder-based resolver.
   */
  public void setResolver(Resolver resolver) {
    this.resolver = resolver;
  }
  public Resolver getResolver() {
    return resolver;
  }

  @Override
public String toString() {
    return 
      "(XSLTransformer"+
      " xml_file_name="+getXMLFileName()+
      " validate="+getValidate()+
      " use_xml_stylesheet="+getUseXMLStylesheet()+
      " default_xsl_file_name="+getDefaultXSLFileName()+
      " dynamic_xsl_file_name="+getDynamicXSLFileName()+
      " default_xsl_params="+getDefaultXSLParams()+
      " dynamic_xsl_params="+getDynamicXSLParams()+
      ")";
  }

  public void assertIsFullyConfigured() {
    String s =
      xml_input_file == null ? "XMLFileName is null" :
      handler == null ? "ContentHandler is null" :
      resolver == null ? "Resolver is null" :
      null;
    if (s != null) {
      throw new RuntimeException(s);
    }
  }

  /**
   * Run the parser.
   * @throws ParseException if there's a parsing error
   */ 
  public void parse() throws ParseException {

    assertIsFullyConfigured();

    try {
      // create logger
      if (logger.isInfoEnabled()) {
        logger.info(
            "Parsing"+
            (validate ? " validated" : "")+
            " XML file \""+ xml_input_file+"\"");
      }

      // create transformer factory
      TransformerFactory tFactory =
        TransformerFactory.newInstance();
      if ((!tFactory.getFeature(SAXSource.FEATURE)) ||
          (!tFactory.getFeature(SAXResult.FEATURE))) {
        throw new RuntimeException(
            "XSLT TransformerFactory doesn't support the "+
            "\"SAXSource.FEATURE\" and/or the \"SAXResult.FEATURE\"");
          }
      saxTFactory = (SAXTransformerFactory) tFactory;
      saxTFactory.setURIResolver(resolver);

      // create reusable xml reader
      String xml_driver = SystemProperties.getProperty("org.xml.sax.driver");
      if (xml_driver == null) {
        xml_reader = XMLReaderFactory.createXMLReader();
      } else {
        xml_reader = XMLReaderFactory.createXMLReader(xml_driver);
      }
      xml_reader.setEntityResolver(resolver);

      // find the appropriate t2_transformer_handler.
      //
      // if dynamic xsl is used then a "t1_pipe_thread" will be
      // launched.
      //
      // if xsl is disabled then both the "t2_transformer_handler" and
      // "t1_pipe_thread" will be null.
      findTransformerHandler();

      // enable optional validation
      if (validate) { 
        xml_reader.setFeature(
            "http://xml.org/sax/features/validation",
            validate);
      }

      // set XML content handler to our handler
      if (t2_transformer_handler == null) {
        xml_reader.setContentHandler(handler);
      } else {
        prepareTransformerHandler();
      }

      // open society xml file (again if we're using XSL!)
      InputStream t2_xml_input_stream = 
        resolver.open(xml_input_file);

      // parse
      xml_reader.parse(new InputSource(t2_xml_input_stream));

      if (t1_pipe_thread != null) {
        // wait for dynamic xsl thread
        joinForPipeThread();
      }

    } catch (Exception e) {

      // examine exception and generate a better one
      throw cleanupException(e);

    } finally {
      // cleanup
      saxTFactory = null; 
      xml_reader = null; 
      t1_pipe_thread = null;
      t2_transformer_handler = null;
    }
  }
  
  private void findTransformerHandler()
    throws IOException, TransformerConfigurationException, SAXException {

    if (use_xml_stylesheet) {
      // look for xsl header in xml file, e.g.:
      //  <?xml-stylesheet type="text/xml" href="society.xsl"?>
      findXmlStylesheetTransformerHandler();
      if (t2_transformer_handler != null) {
        return;
      }
    }

    if (t1_xsl_input_file == null) {
      if (t2_xsl_input_file == null) {
        // no xsl!   leave t2_transformer_handler as null
        if (logger.isInfoEnabled()) {
          logger.info("Not using XSL");
        }
        return;
      }

      // use default xsl (e.g. "society.xsl")
      findDefaultTransformerHandler();
      return;
    }
   
    if (t2_xsl_input_file == null) {
      // dynamic xsl (e.g. "make_society.xsl")
      //
      // also launches t1_pipe_thread
      findDynamicTransformerHandler();
      return;
    }

    // error
    throw new RuntimeException(
        "Specified both a dynamic XSL generator ("+
        t1_xsl_input_file+") and a default XSL ("+
        t2_xsl_input_file+")");
  }

  private void findXmlStylesheetTransformerHandler()
    throws IOException, TransformerConfigurationException {

    // look for xsl header in xml file, e.g.:
    //  <?xml-stylesheet type="text/xml" href="society.xsl"?>

    InputStream pi_xml_input_stream =
      resolver.open(xml_input_file);

    Source stylesheet_source = 
      saxTFactory.getAssociatedStylesheet(
          new StreamSource(pi_xml_input_stream),
          null,  // media
          null,  // title
          null); // charset

    if (logger.isInfoEnabled()) {
      logger.info(
          (stylesheet_source == null ?
           "Did not find associated XSL stylesheet" :
           ("Found associated XSL stylesheet: "+
            stylesheet_source.getSystemId())));
    }

    if (stylesheet_source == null) {
      return;
    }

    t2_transformer_handler =
      saxTFactory.newTransformerHandler(
          stylesheet_source);
  }

  private void findDefaultTransformerHandler() 
    throws IOException, TransformerConfigurationException {

    // explicit xsl (e.g. "society.xsl")
    if (logger.isInfoEnabled()) {
      logger.info(
          "Using default XSL stylesheet: "+t2_xsl_input_file);
    }

    InputStream t2_xsl_input_stream =
      resolver.open(t2_xsl_input_file);

    t2_transformer_handler =
      saxTFactory.newTransformerHandler(
          new StreamSource(t2_xsl_input_stream));
  }

  private void findDynamicTransformerHandler() 
    throws IOException, TransformerConfigurationException, SAXException {

    // dynamically generate xsl (e.g. "make_society.xsl")
    //
    // run the t1_xsl file on the xml file to generate an
    // in-memory t2_xsl.  We use a threaded pipe to connect the
    // t1_xsl output to our handler's transformer.
    //
    // t1:   (xsl generator)
    //   XML input:  "xml_input_file"
    //   XSL input:  "make_society.xsl"
    //   XSL output: pipe_out
    // t2:   (the generated xsl)
    //   XML input:  "xml_input_file"
    //   XSL input:  pipe_in
    //   XML output: handler
    // handler:
    //   parse config

    if (logger.isInfoEnabled()) {
      logger.info(
          "Using dynamic XSL stylesheet: "+t1_xsl_input_file);
    }

    final PipedOutputStream t1_pipe_output_stream =
      new PipedOutputStream();
    PipedInputStream t2_pipe_input_stream =
      new PipedInputStream();
    t1_pipe_output_stream.connect(t2_pipe_input_stream);

    // perform these actions in our thread, to simplify
    // debugging
    final InputStream t1_xml_input_stream;
    final Transformer t1_transformer;
    {
      // open society xml file
      t1_xml_input_stream =
        resolver.open(xml_input_file);

      // open preprocessing xsl file
      InputStream t1_xsl_input_stream =
        resolver.open(t1_xsl_input_file);

      // create outer transform handler
      t1_transformer = 
        saxTFactory.newTransformer(
            new StreamSource(t1_xsl_input_stream));
      t1_transformer.setURIResolver(resolver);

      // set optional xsl parameters
      if (t1_xsl_params != null) { 
        for (Iterator iter = t1_xsl_params.entrySet().iterator();
            iter.hasNext();
            ) {
          Map.Entry me = (Map.Entry) iter.next();
          String key = (String) me.getKey();
          String value = (String) me.getValue();
          t1_transformer.setParameter(key, value);
        }
      }
    }

    // create runnable to transform our xml to the xsl pipe
    Runnable r1 = new Runnable() {
      public void run() {
        try {
          // transform to xsl pipe
          t1_transformer.transform(
              new StreamSource(t1_xml_input_stream),
              new StreamResult(t1_pipe_output_stream));
        } catch (Exception e) {
          logger.error("failed", e);
        } finally {
          try {
            t1_pipe_output_stream.flush();
            t1_pipe_output_stream.close();
          } catch (Exception e2) {
            logger.error("failed", e2);
          }
        }
      }
    };

    // launch our t1 pipe thread (no ThreadService yet!)
    t1_pipe_thread = new Thread(r1); 
    t1_pipe_thread.start();

    // read templates from xsl pipe
    TemplatesHandler t2_templates_handler =
      saxTFactory.newTemplatesHandler();
    xml_reader.setContentHandler(t2_templates_handler);
    xml_reader.parse(new InputSource(t2_pipe_input_stream));
    Templates t2_templates = t2_templates_handler.getTemplates();

    // create transformer
    t2_transformer_handler =
      saxTFactory.newTransformerHandler(t2_templates);
  }

  private void prepareTransformerHandler() 
    throws SAXNotRecognizedException, SAXNotSupportedException {
    Transformer t2_transformer =
      t2_transformer_handler.getTransformer(); 

    // set config finder
    t2_transformer.setURIResolver(resolver);

    // set optional xsl parameters
    if (t2_xsl_params != null) { 
      for (Iterator iter = t2_xsl_params.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String key = (String) me.getKey();
        String value = (String) me.getValue();
        t2_transformer.setParameter(key, value);
      }
    }

    xml_reader.setContentHandler(t2_transformer_handler);
    xml_reader.setProperty(
        "http://xml.org/sax/properties/lexical-handler", 
        t2_transformer_handler);

    // set our handler
    t2_transformer_handler.setResult(new SAXResult(handler));
  }

  private void joinForPipeThread() 
    throws InterruptedException {
    boolean lastTry = false;
    while (true) {
      if (t1_pipe_thread == null || !t1_pipe_thread.isAlive()) {
        // usual case, t1 transform closed the pipe
        return;
      }
      if (lastTry) {
        // do join
        break;
      }
      lastTry = true;
      // wait a little
      Thread.sleep(100); 
    }
    if (logger.isWarnEnabled()) {
      logger.warn(
          "Waiting for XSL transform pipe to finish,"+
          " possible deadlock or IO problem!");
    }
    t1_pipe_thread.join();
    if (logger.isWarnEnabled()) {
      logger.warn("XSL transform pipe finished");
    }
  }

  private ParseException cleanupException(Exception e) {

    String msg =
        "Exception parsing society XML file "+xml_input_file;
    Exception cause = e;

    if (e instanceof SAXException) {
      SAXException sx = (SAXException) e;
      // extract nested sax exception
      if (sx instanceof SAXParseException) {
        SAXParseException spx = (SAXParseException) sx;
        msg +=
          ", locator="+
          "(publicId="+spx.getPublicId()+
          " systemId="+spx.getSystemId()+
          " lineNumber="+spx.getLineNumber()+
          " columnNumber="+spx.getColumnNumber()+
          ")";
      }
      Exception e2 = sx.getException();
      if (e2 != null) {
        cause = e2;
      }
    }

    return new ParseException(msg, cause);
  }

  /**
   * An {@link XSLTransformer} XML/XSL parsing exception.
   */
  public static class ParseException 
      extends Exception {
        /**
    * 
    */
   private static final long serialVersionUID = 1L;
      public ParseException(String s) {
          super(s);
        }
        public ParseException(String s, Throwable t) {
          super(s, t);
        }
        public ParseException(Throwable t) {
          super(t);
        }
  }

  /**
   * An {@link InputStream} resolver for reading files referenced
   * in XSL/XML.
   */
  public interface Resolver extends EntityResolver, URIResolver {
    // ConfigFinder:
    InputStream open(String aURL) throws IOException;

    // EntityResolver:
    InputSource resolveEntity(
        String publicId, String systemId)
      throws SAXException, IOException;

    // URIResolver:
    Source resolve(String href, String base)
      throws TransformerException;
  }

  /**
   * {@link XSLTransformer.Resolver} that uses the {@link
   * ConfigFinder}.
   */
  public static class ConfigFinderBasedResolver 
      implements Resolver {
        private final ConfigFinder configFinder;

        public ConfigFinderBasedResolver(
            ConfigFinder configFinder) {
          this.configFinder = configFinder;
        }

        // ConfigFinder:
        public InputStream open(String aURL) throws IOException {
          try {
            return configFinder.open(aURL);
          } catch (FileNotFoundException fnfe) {
            // look for exact filename, e.g. "c:\test.xml"
            try {
              File f = (new File(aURL));
              if (f.isFile()) {
                return new FileInputStream(f);
              }
            } catch (Exception e) {
              // ignore, throw original exception
            }
            throw fnfe;
          }
        }

        // EntityResolver:
        public InputSource resolveEntity(
            String publicId, String systemId)
          throws SAXException, IOException {
          InputStream is;
          try {
            is = configFinder.open(systemId);
          } catch (Exception e) {
            throw new SAXException(
                "Unable to resolve entity (publicId="+
                publicId+", systemId="+systemId+")",
                e);
          }
          return new InputSource(is);
        }

        // URIResolver:
        public Source resolve(String href, String base)
          throws TransformerException {
          InputStream is;
          try {
            is = configFinder.open(href);
          } catch (Exception e) {
            throw new TransformerException(
                "Unable to resolve URI (href="+href+", base="+
                base+")",
                e);
          }
          return new StreamSource(is);
        }
  }
}
