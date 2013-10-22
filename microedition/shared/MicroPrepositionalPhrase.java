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

/**
 * Qualifier describing how a task should be carried out.  These are two
 * arbitrary strings.
 */
public class MicroPrepositionalPhrase implements Encodable {

  public MicroPrepositionalPhrase() {
  }

  public MicroPrepositionalPhrase(String preposition, String indirectObject) {
    setPreposition(preposition);
    setIndirectObject(indirectObject);
  }

  private String preposition;
  private String indirectObject;

  public String getPreposition() {
    return preposition;
  }

  public void setPreposition(String newPreposition) {
    preposition = newPreposition;
  }

  public void setIndirectObject(String newIndirectObject) {
    indirectObject = newIndirectObject;
  }

  public String getIndirectObject() {
    return indirectObject;
  }

  protected static String tag = "MicroPrepositionalPhrase";
  /**
   * XML encode this object and all sub-objects
   */
  public void encode(StringBuffer str) {
    str.append("<");
    str.append(tag);
    str.append(" preposition=\"" + getPreposition() + "\"");
    str.append(" indirectObject=\"" + getIndirectObject() + "\"");
    str.append("/>");
  }



}
