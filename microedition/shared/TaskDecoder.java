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
package org.cougaar.microedition.shared;
import org.cougaar.microedition.shared.tinyxml.*;
import java.util.*;

/**
 * Translates an XML MicroTask into a MicroTask object
 */
public class TaskDecoder extends HandlerBase {

  MicroTask t;

  public TaskDecoder() {
  }

  /**
   * Not used. No CHARDATA in the MicroTask encoding.
   */
  public void charData(String charData) {
    System.out.println("CHARDATA: "+charData);
  }

  /**
   * Main method to decode a MicroTask.
   * @param data XML text of an encoded MicroTask.
   * @return the filled-in MicroTask.
   */
  public MicroTask decode(String data) {
    t = new MicroTask();
    try {
      XMLInputStream aStream = new XMLInputStream(data);
      // get parser instance
      XMLParser aParser = new XMLParser();
      // set this class as the handler for the parser
      aParser.setDocumentHandler(this);
      // set the input stream
      aParser.setInputStream(aStream);
      // and parse the xml
      aParser.parse();
    } catch (ParseException e) {
      // e.printStacktrace() is still a dummy in CLDC1.0
      System.out.println(e.toString());
    }
    return t;
  }

  /**
   * This method is called to indicate the start of an element (tag).
   *
   * @param name the name of the element (tag name)
   * @param attr a hashtable containing the explicitly supplied attributes (as strings)
   */
  public void elementStart(String name, Hashtable attr) throws ParseException {
    if (name.equals(MicroTask.tag)) {
      getTaskAttrs(attr);
    }
    else if (name.equals(MicroAllocation.tag)) {
      getAllocationAttrs(attr);
    }
    else if (name.equals(MicroAllocationResult.tag)) {
      getAllocationResultAttrs(attr);
    }
    else if (name.equals(MicroAllocationResult.aspectTag)) {
      getAllocationResultAspects(attr);
    }
    else if (name.equals(MicroPrepositionalPhrase.tag)) {
      getPrepositionalPhraseAttrs(attr);
    }
  }

  /**
   * Get the attributes associated with the MicroTask object
   */
  private void getTaskAttrs(Hashtable attr) {
    t.setVerb((String)attr.get("verb"));
    t.setUniqueID((String)attr.get("uniqueID"));
  }

  /**
   * Get the attributes assocaited with the MicroAllocation object.
   */
  private void getAllocationAttrs(Hashtable attr) {
    if (t.getAllocation() == null)
      t.setAllocation(new MicroAllocation(null, t));
  }

  /**
   * Get the attributes assocaited with the MicroAllocationResult object.
   */
  private void getAllocationResultAttrs(Hashtable attr) {
    if (t.getAllocation().getReportedResult() == null)
      t.getAllocation().setReportedResult(new MicroAllocationResult());

    MicroAllocationResult mar = t.getAllocation().getReportedResult();
    String str;

    str = ((String)attr.get("success")).toLowerCase();
    mar.setSuccess(str.equals("true"));
//    mar.setSuccess(Boolean.valueOf(str).booleanValue());

    str = (String)attr.get("risk");
    mar.setRisk(Long.parseLong(str));

    str = (String)attr.get("confidenceRating");
    mar.setConfidenceRating(Long.parseLong(str));
  }

  /**
   * Get the aspect attributes assocaited with the MicroAllocationResult object.
   */
  private void getAllocationResultAspects(Hashtable attr) {
    MicroAllocationResult mar = t.getAllocation().getReportedResult();
    String str;

    str = (String)attr.get("aspect");
    int aspect = Integer.valueOf(str).intValue();
    str = (String)attr.get("value");
    long value = Long.parseLong(str);

    mar.addAspectValuePair(aspect, value);
  }

  /**
   * Get the attributes assocaited with the MicroPrepositionalPhrase object.
   */
  private void getPrepositionalPhraseAttrs(Hashtable attr) {
    String prep = (String)attr.get("preposition");
    String obj = (String)attr.get("indirectObject");

    if ((prep != null) && (obj != null)) {
      t.addPrepositionalPhrase(new MicroPrepositionalPhrase(prep, obj));
    }

  }

  /**
   * A test stub
   */
/* No need to code-bloat the KVM
  public static void main(String [] args) {
    MicroTask t = new MicroTask();
    t.setVerb("AVerb");
    MicroAllocation ma = new MicroAllocation(null, t);
    MicroAllocationResult mar = new MicroAllocationResult();
    mar.setRisk(1);
    mar.setConfidenceRating(2);
    mar.setSuccess(true);
    int [] aspects = {1, 2, 3, 4};
    long [] values = {1100, 2200, 3300, 4400};
    mar.setAspects(aspects);
    mar.setValues(values);
    ma.setReportedResult(mar);
    t.setAllocation(ma);

    Vector v = new Vector();
    v.addElement(new MicroPrepositionalPhrase("Preposition1", "Ind-Obj1"));
    v.addElement(new MicroPrepositionalPhrase("Preposition2", "Ind-Obj2"));
    t.setPrepositionalPhrases(v);

    StringBuffer str = new StringBuffer();
    str.append("<?xml version=\"1.0\"?>");
    t.encode(str);

    System.out.println(str.toString());
    TaskDecoder d = new TaskDecoder();
    MicroTask new_t = d.decode(str.toString());
    System.out.println(new_t);
  }
*/
}
