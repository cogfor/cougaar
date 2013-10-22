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

package org.cougaar.core.util;

import java.io.PrintWriter;

/**
 * A simple XML formatter.
 * <p>
 * For an example use, see the {@link ComponentViewServlet}. 
 */
public final class XMLWriter {

  private final PrintWriter out;

  private boolean pending;

  private int indentCount = 0;
  private String indentString = "               ";

  public XMLWriter(PrintWriter out) {
    this.out = out;
  }

  public void header() {
    out.println("<?xml version='1.0'?>");
  }
  public void comment(String s) {
    if (pending) {
      out.println(">");
      pending = false;
    }
    out.println(
        indent()+
        "<!-- "+encode(s)+" -->");
  }
  public void begin(String tag) {
    if (pending) {
      out.println(">");
    }
    out.print(
        indent()+
        "<"+tag);
    pending = true;
    moreIndent();
  }
  public void attr(String name, int i) {
    attr(name, Integer.toString(i));
  }
  public void attr(String name, String value) {
    if (!pending) {
      throw new RuntimeException(
          "Unable to write attribute("+name+", "+value+
          ") outside of <tag>!");
    }
    out.print(" "+name+"=\'"+encode(value)+"\'");
  }
  public void end(String tag) {
    lessIndent();
    if (pending) {
      out.println("/>");
      pending = false;
    } else {
      out.println(
          indent()+
          "</"+tag+">");
    }
  }
  public void value(String tag, long l) {
    value(tag, Long.toString(l));
  }
  public void value(String tag, String content) {
    if (content == null) {
      begin(tag);
      end(tag);
      return;
    }
    if (pending) {
      out.println(">");
      pending = false;
    }
    out.println(
        indent()+
        "<"+tag+">"+encode(content)+"</"+tag+">");
  }

  // encoding this string be XML-safe, by replacing all:
  //   "<" becomes "&lt;"
  //   ">" becomes "&gt;"
  private String encode(String s) {
    if (s != null) {
      if (s.indexOf('>') >= 0) {
        s = s.replaceAll("<", "&lt;");
      }
      if (s.indexOf('<') >= 0) {
        s = s.replaceAll(">", "&gt;");
      }
    }
    return s;
  }

  private String indent() {
    if (indentCount <= 0) {
      return "";
    }
    if (indentCount > indentString.length()) {
      indentString += "               ";
    }
    return indentString.substring(0, indentCount);
  }
  private void moreIndent() {
    indentCount += 2;
  }
  private void lessIndent() {
    indentCount -= 2;
  }
}
