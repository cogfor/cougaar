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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.util.CSVUtility;

/**
 * Parse an INI stream into an <code>ComponentDescription[]</code>.
 */
public final class INIParser {

  private INIParser() { 
    // no need for a constructor -- these are static utility methods
  }

  public static ComponentDescription[] parse(String filename) {
    try {
      return parse(new FileInputStream(filename));
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public static ComponentDescription[] parse(InputStream in) {
    return parse(new BufferedReader(new InputStreamReader(in)));
  }

  /** Pattern for matching insertionPoint &amp; priority */
  final static Pattern namePattern = Pattern.compile("([a-zA-Z0-9.]+)(\\((.+)\\))?");

  /**
   * Read an INI file from a <code>BufferedReader</code> to create
   * <code>ComponentDescription</code>s.
   *
   * Expected format is:<pre>
   *   # option comment lines and empty lines
   *   [ ignored section headers ]
   *   insertionPoint = classname
   *   # or specify String arguments, which are saved as a List "param"
   *   insertionPoint = classname(arg0, arg1, argN)
   *   # more "insertionPoint = " lines
   * </pre> and is parsed into an array of ComponentDescriptions.
   * <p>
   * These fields are not currently supported:<br>
   *   codebase, certificate, lease, policy
   * <p>
   * Two insertion points have special backwards-compatability<ul>
   *   <li>"cluster" == "Node.AgentManager.Agent"</li>
   *   <li>"plugin" == "Node.AgentManager.Agent.PluginManager.Plugin"</li>
   * </ul>
   * <p>
   * Any insertion point (including aliases) may be postfixed with "(<em>PRIORITY</em>)"
   * where "<em>PRIORITY</em>" is one of the component priority names specified in
   * ComponentDescription (e.g. HIGH, INTERNAL, BINDER, COMPONENT, LOW, or STANDARD).  Note that
   * the name must be in upper case, no punctuation and no extra whitespace.  An example
   * is: "plugin(LOW)=org.cougaar.test.MyPlugin", which would specify that MyPlugin should be
   * loaded as a low priority plugin (e.g. after most other plugins and internal components).
   * <p>
   * Also "cluster=name" and is converted into<pre>
   *   "Node.AgentManager.Agent=org.cougaar.core.agent.SimpleAgent(name)"
   * </pre> as a default classname.
   * <p>
   * These old "cluster" values are <u>ignored</u>:<ul>
   *   <li>"class"   (now specified in the Node's INI!)</li>
   *   <li>"uic"     (ignored)</li>
   *   <li>"cloned"  (ignored)</li>
   * </ul>
   * <br>
   * <em>Note:</em> This is used by the file-based <code>ComponentInitializerService</code>,
   * which is declared to return only items <em>below</em> the given insertion point. 
   * However, this method returns <em>all</em> items, regardless
   * of insertion point. Callers must beware they may get themselves. 
   * @see org.cougaar.core.component.ComponentDescription
   */
  public static ComponentDescription[] parse(BufferedReader in) {
    List descs = new ArrayList();
    int line = 0;
    try {

readDescriptions:
      while (true) {

        // read an entry
        String s = null;
        while (true) {
          // read the current line
          line++;
          String tmp = in.readLine();
          if (tmp == null) {
            // end of file
            if (s != null) {
              System.err.println(
                  "Warning: INI file ends in ignored \""+s+"\"");
            }
            break readDescriptions;
          }
          if (tmp.endsWith("\\")) {
            tmp = tmp.substring(0, tmp.length()-1);
            if (!(tmp.endsWith("\\"))) {
              // line continuation
              s = ((s != null) ? (s + tmp) : tmp);
              continue;
            }
          }
          // finished line
          s = ((s != null) ? (s + tmp) : tmp);
          break;
        }

        s = s.trim();
        int eqIndex;
        if ((s.length() == 0) ||
            (s.startsWith("#")) ||
            ((eqIndex = s.indexOf("=")) <= 0)) {
          // ignore empty lines, "#comments", and non-"name=value" lines
          continue;
        }

        String name = s.substring(0, eqIndex).trim(); 
        String value = s.substring(eqIndex+1).trim(); 

        // special case name/value pairs
        if (name.equals("name") ||
            name.equals("class") ||
            name.equals("uic") ||
            name.equals("cloned")) {
          // ignore!
          continue;
        }

        // name is the insertion point
        String insertionPoint = name;
        int priority = -1;      // illegal priority (undefined) - we'll default it later
        {
          Matcher m = namePattern.matcher(insertionPoint);
          if (m.matches()) {   
            // group 1 is the new name
            String n = m.group(1);
            if (n != null) insertionPoint=n;

            // group 3 is the priority or null
            String p = m.group(3);
            if (p != null) {
              try {
                priority = ComponentDescription.parsePriority(p);
              } catch (IllegalArgumentException iae) {
                System.err.println("Warning: illegal component priority line "+line+": "+s);
              } 
            }
          } else {
            System.err.println("Warning: unparsable component description line "+line+": "+s);
          }
        }

        // FIXME only a simplistic property of "List<String>" is supported
        //
        // parse value into classname and optional parameters
        String classname;
        List vParams = null;
        int p1;
        int p2;
        if (((p1 = value.indexOf('(')) > 0) && 
            ((p2 = value.lastIndexOf(')')) > p1)) {
          classname = value.substring(0, p1);
          vParams = CSVUtility.parseToList(value.substring((p1+1), p2));
        } else {
          classname = value;
        }

        // fix the insertion point for backwards compatibility
        if (insertionPoint.equals("plugin")) {
          // should load into an Agent
          insertionPoint = PluginBase.INSERTION_POINT;
        } else if (insertionPoint.equals("cluster")) {
          if (vParams == null) {
            // fix "cluster=name" to be "cluster=classname(name)"
            vParams = new ArrayList(1);
            vParams.add(classname);
            classname = "org.cougaar.core.agent.SimpleAgent";
          }
          // should load into a Node
          insertionPoint = Agent.INSERTION_POINT;
        }

        if (insertionPoint.startsWith(".")) {
          System.err.println(
              "Warning: insertionPoint starts with \".\" on line "+line+": "+s);
          continue;
        }

        if (priority == -1) {
          // default binders to PRIORITY_BINDER instead of STANDARD 
          if (insertionPoint.endsWith(".Binder")) {
            priority = ComponentDescription.PRIORITY_BINDER;
          } else {
            priority = ComponentDescription.PRIORITY_COMPONENT;
          }
        }

        if (vParams != null) {
          vParams = Collections.unmodifiableList(vParams);
        }
        // FIXME unsupported fields: codebase, certificate, lease, policy
        //
        // create a new ComponentDescription
        ComponentDescription cd =
          new ComponentDescription(
              classname,        // name
              insertionPoint,
              classname,
              null,             // codebase
              vParams,
              null,             // certificate
              null,             // lease
              null,             // policy
              priority          // priority, of course.
              );

        // save
        descs.add(cd);
      }
      in.close();
    } catch (IOException ioe) {
      System.err.println("Error: " + ioe);
    }

    return (ComponentDescription[])
      descs.toArray(
          new ComponentDescription[descs.size()]);
  }

  /**
   * Write an "INI" file from the given <code>ComponentDescription[]</code>.
   *
   * @throws ClassCastException if the ComponentDescription[] contains 
   *    illegal elements.
   */
  public static void write(PrintStream out, ComponentDescription[] descs) {
    out.print("# machine-generated ini file\n");
    int ndescs = ((descs != null) ? descs.length : 0);
    out.print("# "+ndescs+" component"+((ndescs>1)?"s":"")+":\n");
    for (int i = 0; i < ndescs; i++) {
      ComponentDescription descI = descs[i];
      out.print(descI.getInsertionPoint());
      out.print("=");
      out.print(descI.getClassname());
      List vParams = (List)descI.getParameter();
      int nvParams = vParams.size();
      if (nvParams > 0) {
        out.print("(");
        int k = 0;
        out.print((String)vParams.get(k));
        while (++k < nvParams) {
          out.print(", ");
          out.print((String)vParams.get(k));
        }
        out.print(")");
      }
      out.print("\n");
    }
  }

  public static void main(String[] args) {
    System.out.println("testme!");
    String fname = args[0];
    System.out.println("read: "+fname);
    ComponentDescription[] descs = parse(fname);
    System.out.println("write to stdout");
    write(System.out, descs);
  }

}
