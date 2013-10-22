/*
 *
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.util.PropertyTree;

/**
 * Parser for the <code>PlanViewServlet</code>'s "Advanced Search" 
 * loading of the built-in predicates.
 *
 * @see #parse(BufferedReader) for input stream format
 */
public class PredTableParser {

  /** @see #parse(BufferedReader) */
  public static final PropertyTree parse(InputStream in) {
    return parse(new BufferedReader(new InputStreamReader(in)));
  }

  /** @see #parse(BufferedReader) */
  public static final PropertyTree parse(Reader in) {
    return parse(new BufferedReader(in));
  }

  /**
   * Parse the input to build a PropertyTree of encoded predicates
   * for the <code>PlanViewServlet</code>'s "Advanced Search" page.
   * <pre>
   * Expected file format is:
   *
   *   Entry separator lines start with "***"
   *
   *   Comment lines start with "**" and then can contain additional 
   *     characters (except having the third char be "*", since this
   *     would confuse things with the entry separator)
   *
   *   Entries must have at least two lines or they are ignored.  They
   *     must be prefixed AND followed by "***" lines.
   *
   *   The first line is the "key", and the following lines make the
   *     "value".  These are encoded with 
   *     <tt>ServletUtil.encodeForHTML</tt> and 
   *     <tt>ServletUtil.encodeForJava</tt> 
   *     to let the PlanViewServlet print them in HTML and Javascript.
   * </pre>
   * @see #main(String[]) for an example
   */
  public static final PropertyTree parse(BufferedReader in) {
    PropertyTree pt = new PropertyTree();
    try {
      // read entries
readEntries:
      while (true) {
        // read the key line
        String rawKey = in.readLine();
        if (rawKey == null) {
          // end of input
          break readEntries;
        }
        if (rawKey.startsWith("**")) {
          // ignore comment or empty entry
          continue readEntries;
        }
        // read the value lines
        String rawValue = null;
readValue:
        while (true) {
          String s = in.readLine();
          if (s == null) {
            // end of input
            break readEntries;
          }
          if (!(s.startsWith("**"))) {
            // value line, typical case
            if (rawValue != null) {
              rawValue += "\n"+s;
            } else {
              rawValue = s;
            }
          } else {
            // control line
            if ((s.length() <= 2) ||
                (s.charAt(2) != '*')) {
              // comment  ("**?");
              continue readValue;
            } else {
              // end entry ("***");
              if (rawValue != null) {
                break readValue;
              } else {
                // ignore 
                break readEntries;
              }
            }
          }
        }
        // encode the key and value for javascript use
        String encKey = ServletUtil.encodeForHTML(rawKey);
        String encValue = ServletUtil.encodeForJava(rawValue);
        // add to property tree
        pt.put(encKey, encValue);
      }
      in.close();
    } catch (IOException ioe) {
      System.err.println(
          "Unable to parse PlanViewServlet's predicates: "+ioe);
    }
    return pt;
  }

  /** testing utility, plus illustrates file format. */
  public static void main(String[] args) {
    String s = 
      "******"+
      "\n** comment "+
      "\n**"+
      "\n***"+
      "\n******"+
      "\nkey"+
      "\nvalue"+
      "\n******"+
      "\ncomplex (x < \"foo\" > z) \\ endKey"+
      "\n**comment"+
      "\n// java comment"+
      "\n//"+
      "\n/* another java comment \"here\"*/"+
      "\n"+
      "\nvalue"+
      "\n(y < \"z\")"+
      "\n  ** not a comment \\ here"+
      "\n  *** not the end of the entry"+
      "\n  more junk > blah"+
      "\nkeep double-encoded: \\\" \\n"+
      "\n"+
      "\nend..."+
      "\n***";
    System.out.println("Test with:\n"+s);
    // can replace this deprecated input with something newer...
    StringReader srin = 
      new StringReader(s);
    // parse!
    PropertyTree pt = parse(srin);
    int n = pt.size();
    System.out.println("Parsed["+n+"]");
    for (int i = 0; i < n; i++) {
      String ki = (String)pt.getKey(i);
      String vi = (String)pt.getValue(i);
      System.out.println(i+")");
      System.out.println("  key=|"+ki+"|");
      System.out.println("  value=|"+vi+"|");
    }
  }

}
