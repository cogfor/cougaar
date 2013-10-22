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

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.cougaar.bootstrap.Bootstrapper;
import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BindingUtility;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceBrokerSupport;
import org.cougaar.util.Configuration;

/**
 * This component is the root component of the
 * <a href="http://www.cougaar.org">Cougaar Agent Architecture</a>,
 * containing the {@link #main} method. 
 * <p>
 * Usage:<pre>
 *    <tt>java [props] org.cougaar.core.node.Node [props] [--help]</tt>
 * </pre> where the "props" are "-D" System Properties, such as:<pre>
 *    "-Dorg.cougaar.node.name=NAME" -- name of the Node
 * </pre>
 * <p>
 * A node refers to a per-JVM component that contains the Cougaar
 * agents and per-JVM services.  The primary job of the node is to:
 * <ul>
 *   <li>Provide the initial launch methods</li>
 *   <li>Initialize the system properties</li>
 *   <li>Create the root ServiceBroker for the component model</li>
 *   <li>Create the NodeIdentificationService</li>
 *   <li>Create the initial ComponentInitializerService</li>
 *   <li>Create the AgentManager</li>
 *   <li>Create the NodeAgent, which in turn creates the other
 *       agents for this node</li>
 * </ul>
 * <p>
 *
 * @property org.cougaar.node.name
 *   The (required) name for this Node.
 *
 * @property org.cougaar.core.node.InitializationComponent
 *   Used to specify which service component to use.  Can be passed
 *   in short hand (<em>DB</em>, <em>XML</em>, <em>File</em>) or as
 *   a fully specified class:
 *   <em>org.cougaar.core.node.DBComponentInitializerServiceComponent</em>
 *
 * @property org.cougaar.filename
 *   The file name (.ini) for starting this Node, which defaults to 
 *   (<em>org.cougaar.node.name</em>+".ini") if both
 *   <em>org.cougaar.filename</em> and
 *   <em>org.cougaar.experiment.id</em> are not specified.  If this
 *   property is specified then <em>org.cougaar.experiment.id</em>
 *    must not be specified.
 *
 * @property org.cougaar.experiment.id
 *   The experiment identifier for running this Node; see 
 *   <em>org.cougaar.filename</em> for details.
 *
 * @property org.cougaar.install.path
 *   The <em>base</em> path for finding jar and configuration files.
 */
public class Node
extends ContainerSupport
{
  public static final String INSERTION_POINT = "Node";

  private static final String FILENAME_PROP = "org.cougaar.filename";
  private static final String EXPTID_PROP = "org.cougaar.experiment.id";
  public static final String INITIALIZER_PROP =
      "org.cougaar.core.node.InitializationComponent";

  private List params;

  /**
   * Node entry point.
   * <p>
   * If org.cougaar.useBootstrapper is true, this method will load all jars
   * file on the jar path (typically "lib/" and "sys/").  Otherwise, this
   * method will rely solely on the classpath.
   *
   * @see #launch
   */
  // @deprecated
  public static void main(String[] args) {
    boolean useBootstrapper;
    try {
      useBootstrapper = 
        SystemProperties.getBoolean("org.cougaar.useBootstrapper", true);
    } catch (Exception e) {
      useBootstrapper = true;
    }
    if (useBootstrapper) {
      System.err.println(
          "-Dorg.cougaar.useBootstrapper is deprecated."+
          "  Invoke Bootstrapper directly.");
      Bootstrapper.launch(Node.class.getName(), args);
    } else {
      launch(args);
    }
  }

  /**
   * The real entry-point for Node, which is generally invoked via the
   * bootstrapper.
   *
   * @see org.cougaar.bootstrap.Bootstrapper
   */
  public static void launch(Object[] args) {
    // create the root service broker and binding site
    final ServiceBroker rootsb = new ServiceBrokerSupport() {};
    BindingSite rootbs = 
      new BindingSite() {
        public ServiceBroker getServiceBroker() { return rootsb; }
        public void requestStop() { }
      };

    // create and load our node
    try {
      Node myNode = new Node();
      if (args != null) {
        myNode.setParameter(args);
      }
      BindingUtility.activate(myNode, rootbs, rootsb);
    } catch (Throwable e) {
      // catch all exceptions and exit gracefully
      System.out.println(
          "Caught an exception at the highest try block."+
          "  Exception is: " +
          e );
      e.printStackTrace();
    }

    // the node-internal threads keep it alive, until "shutdown()" is called
  }

  @Override
protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  @Override
protected ServiceBroker createChildServiceBroker(BindingSite bs) {
    // node uses the root service broker
    return getServiceBroker();
  }

  @Override
protected ComponentDescriptions findInitialComponentDescriptions() {
    return null;
  }

  /**
   * Set our "Object[]" args, called by {@link #launch}.
   */
  public void setParameter(Object obj) {
    Object o = obj;
    if (o instanceof Object[]) {
      o = Arrays.asList((Object[]) o);
    }
    if (!(o instanceof List)) {
      throw new RuntimeException(
          "Expecting an Object[] or List, not "+
          (o == null ? "null" : o.getClass().getName()));
    }
    params = (List) o;
  }

  /**
   * This method initializes and loads the node.
   */
  @Override
public void load() {
    super.load();

    // take params
    List args = new ArrayList();
    if (params != null) {
      args.addAll(params);
      params = null;
    }

    // set any externally-defined system properties:
    //   1) from a "SetPropertiesComponent" (e.g. see NodeApplet)
    //   2) from a local ".properties" file (deprecated)
    //   3) from the command line (also checks for "--help")
    ComponentDescription set_props_desc = getPropertiesDescription(args);
    if (set_props_desc != null) {
      add(set_props_desc);
    }
    loadSystemProperties();
    if (!setSystemProperties(args)) {
      return; // must be "--help"
    }

    // display the version info
    printVersion(true);

    // add the component initializer service (i.e. configuration service)
    ComponentDescription init_desc = getInitializerDescription();
    if (init_desc != null) {
      add(init_desc);
    }

    // add the agent manager, which loads the node-agent
    add(new ComponentDescription(
          "org.cougaar.core.agent.AgentManager",
          "Node.Component",
          "org.cougaar.core.agent.AgentManager",
          null,  //codebase
          args,
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_HIGH));
  }

  /** Get and remove the {@link SetPropertiesComponent} from the list. */
  private static ComponentDescription getPropertiesDescription(List l) {
    // We must set our -Ds very early on, before any call to 
    //   SystemProperties.get*"
    // so we load this component here.
    //
    // In particular, we must set our -Ds before we attempt to access any
    // properties in this class (e.g. in "printVersion") or our Logger.
    //
    // Long-term we should probably remove this component and have the external
    // container call an equivalent library method.
    List props_params = null;
    for (int i = 0; i < l.size(); i++) {
      Object o = l.get(i);
      if (o instanceof Object[]) {
        o = Arrays.asList((Object[]) o);
      }
      if (!(o instanceof List)) continue;
      List p = (List) o;
      if (p.size() != 4) continue;
      if (!"Node.AgentManager.Agent.Component".equals(p.get(0))) continue;
      if (!"HIGH".equals(p.get(1))) continue;
      Object c = p.get(2);
      if (c instanceof Class) {
        c = ((Class) c).getName();
      }
      if (!"org.cougaar.core.node.SetPropertiesComponent".equals(c)) continue;
      l.remove(i--);

      props_params = p.subList(3, p.size());
    }

    if (props_params == null) return null;
    return new ComponentDescription(
        "org.cougaar.core.node.SetPropertiesComponent",
        "Node.Component",
        "org.cougaar.core.node.SetPropertiesComponent",
        null, //codebase
        props_params,
        null, //certificate
        null, //lease
        null, //policy
        ComponentDescription.PRIORITY_HIGH);
  }

  /** Get the {@link ComponentInitializerService} component description. */
  private ComponentDescription getInitializerDescription() {
    // The ComponentInitializerService defines our configuration.
    //
    // We must load this component before or very early on in the AgentManager,
    // since the AgentManager requires this service to find the Agent-level
    // binders and configure the node-agent.
    //
    // We load this service in the Node, since its ServiceBroker is above the
    // "root" AgentManager's ServiceBroker.  This allows a node-agent component
    // to override the "root" implementation of this service, as illustrated
    // in ConfiguratorBase.
    //
    // We plan to refactor the initializer design.  For future reference,
    // here's a sketch of the proposed design:
    //
    //   - The Node (or higher-level external container) will define an
    //       a) ApplicationConfigurationService (== XML parser), and an
    //       b) EnvironmentConfigurationService (== XSL template)
    //
    //   - The ComponentInitializationService (CIS) will use the above
    //     services, and will be relatively trivial.
    //
    //   - The ApplicationConfigurationService (ACS) will be analogous to our
    //     current XMLConfigHandler.  It will provide the agent's application-
    //     specific component list and the name of its environment-specific
    //     template.  The API will be:
    //       AppStruct get(String agentName, Map appOptions);
    //     where:
    //       class AppStruct {
    //         String agentName;
    //         String envName;    // e.g. "SimpleAgent.xsl"
    //         Map envOptions;    // usually null
    //         List appCompDescs; // e.g. domain-specific plugins
    //       }
    //     A null agentName will return the node-agent's entry.
    //     The appOptions will usually be null, but may be used in the future,
    //       (e.g. to support a high-level role-based agent configuration).
    //     The returned envOptions will typically be null, except for special
    //     cases where options must be passed from the application XML to
    //     the environment XSL, e.g.:
    //       1) The WP Server marker component
    //       2) The list of local agents, for the AgentLoader
    //     It will also be used to support per-agent template options, e.g.:
    //       <agent ...>
    //         <env_option name="servlets" value="false"/>
    //         ...
    //       </agent>
    //
    //   - The EnvironmentConfigurationService (ECS) will be analogous to our
    //     current XSLTransformer.  It will interpret an XSL file with
    //     name=value options to compute the list of infrastructure components.
    //     The API will be:
    //       EnvStruct get(String envName, Map envOptions);
    //     where:
    //       class EnvStruct {
    //         List envCompDescs; // e.g. "StandardBlackboard", etc.
    //       }
    //     Usually the envOptions will be null and will default to our XSL
    //       -Dorg.cougaar.society.xsl.param.$name=$value
    //     map entries.
    //     The envName will be an XSL filename, e.g.
    //       SimpleAgent.xsl
    //     The XSL template will be applied against an empty in-memory XML
    //     file.  Instead of inlining the application XML components, the XSL
    //     template will inline "cutpoint" marker components, e.g.:
    //       <component class="cutpoint" name="HIGH" insertionpoint="..."/>
    //
    //   - The CIS implementation will lookup an agent's AppStruct in the ACS,
    //     then it's EnvStruct in the ECS, then insert the appCompDescs in
    //     between the matching cutpoints in the envCompDescs.  This merged
    //     result defines the complete agent's configuration and will be
    //     cached.
    //
    // The above design will allow "addAgent(...)" to specify the list of
    // appCompDescs but still apply the template.  The ConfiguratorBase will
    // be replaced by a new ACS implementation.
    String classname = SystemProperties.getProperty(INITIALIZER_PROP);
    if (classname == null) {
      // get initializer, defaults to XML
      classname =
        (SystemProperties.getProperty(FILENAME_PROP) != null ? "File" :
         SystemProperties.getProperty(EXPTID_PROP) != null ? "DB" :
         "XML");
      SystemProperties.setProperty(INITIALIZER_PROP, classname);
    }
    if (classname.equals("null")) return null;
    if (classname.indexOf(".") < 0) {
      // if full class name not specified, intuit it
      classname =
        "org.cougaar.core.node." +
        classname +
        "ComponentInitializerServiceComponent";
    }
    return new ComponentDescription(
        classname,
        Node.INSERTION_POINT + ".Component",
        classname,
        null, //codebase
        null, //params
        null, //certificate
        null, //lease
        null, //policy
        ComponentDescription.PRIORITY_HIGH);
  }

  /**
   * Convert any command-line args into System Properties.
   * <p>
   * System properties are preferred, since it simplifies the
   * configuration to just a non-ordered Set of "-D" properties.  
   * <p>
   * The only non "-D" command line arguments are:<pre>
   *   -n ARG         equivalent to "-Dorg.cougaar.node.name=ARG"
   *   -c             ignored, ancient "clear database" switch
   *   --?version     display version information and exit
   *   --?info        display terse version information and exit
   *   --?help        display usage help and exit
   *   <i>other</i>   display error message and exit
   * </pre>
   * <p>
   * Also supported are post-classname "-D" command-line properties:
   *    "java .. classname -Darg .." 
   * which will override the usual "java -Darg .. classname .."
   * properties.  For example, "java -Dx=y classname -Dx=z" is
   * equivalent to "java -Dx=z classname".  This can be useful when 
   * writing scripts that simply append properties to a command-line.
   * <p>
   * @return false if node should exit
   */
  private static boolean setSystemProperties(List args) {
    // separate the args into "-D" properties and normal arguments
    for (int i = 0; i < args.size(); i++) {
      Object oi = args.get(i);
      if (!(oi instanceof String)) continue; 
      String argi = (String) oi;
      if (argi.startsWith("-D")) {
        // add a "late" system property
        int sepIdx = argi.indexOf('=');
        if (sepIdx < 0) {
          SystemProperties.setProperty(argi.substring(2), "");
        } else {
          SystemProperties.setProperty(
              argi.substring(2, sepIdx), argi.substring(sepIdx+1));
        }
      } else if (argi.equals("-n")) {
        // old "-n node" pattern
        String name = (String) args.get(++i);
        SystemProperties.setProperty("org.cougaar.node.name", name);
      } else if (argi.equals("-c")) {
        // ignore
      } else {
        // some form of exit
        if (argi.equals("-version") || argi.equals("--version")) {
          printVersion(true);
        } else if (argi.equals("-info") || argi.equals("--info")) {
          printVersion(false);
        } else if (argi.equals("-help") || argi.equals("--help")) {
          System.out.print(
              "Usage: java [JVM_OPTIONS] [-D..] "+
              Node.class.getName()+" [-D..] [ARGS]"+
              "\nA Node manages and executes Cougaar agents.\n"+
              "\n  -Dname=value        set configuration property."+
              "\n  -version, --version output version information and exit."+
              "\n  -info, --info       output terse version information and exit."+
              "\n  -help, --help       display this help and exit.\n"+
              "\nSee <http://www.cougaar.org> for further help and bug reports.\n");
        } else {
          System.err.println(
              "Node: unrecognized option `"+argi+"'"+
              "\nTry `Node --help' for more information.");
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Parse and load system properties from a well-known file, as
   * defined by a system property
   * (default "$INSTALL/configs/common/system.properties")
   * it will only load properties which do not already have a value.
   * <p>
   * Property values are interpreted with Configuration.resolveValue to
   * do installation substitution.
   *
   * @property org.cougaar.core.node.properties
   * @note The property org.cougaar.core.node.properties must be
   *   defined as a standard java -D argument, as it is evaluated
   *   extremely early in the Node boot process.  
   */
  private static void loadSystemProperties() {
    String u = 
      SystemProperties.getProperty("org.cougaar.core.node.properties");
    if (u == null) {
      u = "$INSTALL/configs/common/system.properties";
    }

    try {
      URL cip = Configuration.canonicalizeElement(u);
      if (cip != null) {
        Properties p = new Properties();
        InputStream in = cip.openStream();
        try {
          p.load(in);
        } finally {
          in.close();
        }
        for (Iterator it = p.keySet().iterator();
            it.hasNext();
            ) {
          String key = (String) it.next();
          if (SystemProperties.getProperty(key) == null) {
            try {
              String value = p.getProperty(key);
              value = Configuration.resolveValue(value);
              SystemProperties.setProperty(key, value);
            } catch (RuntimeException re) {
              re.printStackTrace();
            }
          }
        }
      } // if cip not null
    } catch (Exception e) {
      // failed to open input stream
      // or canonicalizeElement had a MalformedURLException
      // or...

      // Failed to loadSystemProperties
      //e.printStackTrace();
      //      System.err.println("Failed to loadSystemProperties from " + u, e);
    }
  }

  private static void printVersion(boolean fullFormat) {
    String version = null;
    long buildTime = -1;
    String repositoryTag = null;
    boolean repositoryModified = false;
    long repositoryTime = -1;
    try {
      Class vc = Class.forName("org.cougaar.Version");
      Field vf = vc.getField("version");
      Field bf = vc.getField("buildTime");
      version = (String) vf.get(null);
      buildTime = bf.getLong(null);
      Field tf = vc.getField("repositoryTag");
      Field rmf = vc.getField("repositoryModified");
      Field rtf = vc.getField("repositoryTime");
      repositoryTag = (String) tf.get(null);
      repositoryModified = rmf.getBoolean(null);
      repositoryTime = rtf.getLong(null);
    } catch (Exception e) {
      // Failed to get version info, reflection problem
    }

    if (!(fullFormat)) {
      System.out.println(
          "COUGAAR\t"+version+"\t"+buildTime+
          "\t"+repositoryTag+"\t"+repositoryModified+
          "\t"+repositoryTime);
      return;
    } 

    synchronized (System.out) {
      System.out.print("COUGAAR ");
      if (version == null) {
        System.out.println("(unknown version)");
      } else {
        System.out.println(
            version+" built on "+
            ((buildTime > 0) ? 
             ((new Date(buildTime)).toString()) : 
             "(unknown time)"));
      }
      System.out.println(
          "Repository: "+
          ((repositoryTag != null) ? 
           (repositoryTag + 
            (repositoryModified ? " (modified)" : "")) :
           "(unknown tag)")+
          " on "+
          ((repositoryTime > 0) ? 
           ((new Date(repositoryTime)).toString()) :
           "(unknown time)"));
      String vminfo = SystemProperties.getProperty("java.vm.info");
      String vmv = SystemProperties.getProperty("java.vm.version");
      System.out.println("VM: JDK "+vmv+" ("+vminfo+")");
      String os = SystemProperties.getProperty("os.name");
      String osv = SystemProperties.getProperty("os.version");
      System.out.println("OS: "+os+" ("+osv+")");
    }
  }
}
