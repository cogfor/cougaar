/*
 * <copyright>
 * 
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.io;

import java.util.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.shared.tinyxml.*;

public class XMLTokenList {

  private class Parser extends HandlerBase {
    String lastKey = "";
    public void elementStart(String name, Hashtable attr) throws ParseException {
      lastKey = name;
      attrtable = attr;
    }

    public void charData(String charData) {
      NameTablePair ntp = new NameTablePair(charData, attrtable);
      add(lastKey, ntp);
    }
  }

  private Hashtable table = new Hashtable();
  public Hashtable attrtable = null;
  public XMLTokenList(String document) {
    try {
      XMLInputStream aStream = new XMLInputStream(document);
      // get parser instance
      XMLParser aParser = new XMLParser();
      // set this class as the handler for the parser
      aParser.setDocumentHandler(new Parser());
      // set the input stream
      aParser.setInputStream(aStream);
      // and parse the xml
      aParser.parse();
    } catch (ParseException e) {
      // e.printStacktrace() is still a dummy in CLDC1.0
      System.out.println("Error parsing token list:"+e.toString());
    }

  }

  public void add(String key, NameTablePair data) {
    Vector v = (Vector)table.get(key);
    if (v == null) {
      v = new Vector();
      table.put(key, v);
    }
    v.addElement(data);
  }

  private Vector emptyVector = new Vector();

  public Vector getTokenVect(String token) {
    Vector ret = (Vector)table.get(token);
    if (ret == null)
      ret = emptyVector;
    return ret;
  }
}
