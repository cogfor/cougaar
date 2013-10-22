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
 * Describes the status of an allocation.
 */
public class MicroAllocationResult implements Encodable {

  public MicroAllocationResult() {
  }

  protected static String tag = "MicroAllocationResult";
  protected static String aspectTag = "MicroAllocationResultAspect";
  private boolean success;
  private long risk;  /* in thousandths */
  private String auxData;
  private long confidenceRating;  /* in thousandths */
  private long[] values = new long[0];  /* in thousandths */
  private int[] aspects = new int[0];
  /**
   * XML encode this object and all sub-objects.
   */
  public void encode(StringBuffer str) {
    str.append("<");
    str.append(tag);
    str.append(" success=\""+success+"\" ");
    str.append("risk=\""+risk+"\" ");
    str.append("confidenceRating=\""+confidenceRating+"\" ");
    str.append("numAspects=\""+aspects.length+"\" ");
    str.append(">");
    for (int i=0; i<aspects.length; i++) {
      str.append("<"+aspectTag+" aspect=\""+aspects[i]+"\" value=\""+values[i]+"\"/>");
    }
    str.append("</");
    str.append(tag);
    str.append(">");
  }


  public String toString() {
    StringBuffer sb=new StringBuffer();
    encode(sb);
    return sb.toString();
  }


  /**
   * Is the disposition of this task OK?
   */
  public void setSuccess(boolean newSuccess) {
    success = newSuccess;
  }

  /**
   * Is the disposition of this task OK?
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Get the results (array is parallel to getAspects())
   */
  public long[] getValues() {
    return values;
  }

  /**
   * Set the results (array is parallel to getAspects())
   */
  public void setValues(long[] newValues) {
    values = newValues;
  }

  /**
   * Set the amount of risk associated with performing this task.
   * @param newRisk (0 = no risk, 1 = certain death)
   */
  public void setRisk(long newRisk) {
    risk = newRisk;
  }

  /**
   * Get the amount of risk associated with performing this task.
   * @return 0 = no risk, 1 = certain death
   */
  public long getRisk() {
    return risk;
  }

  /**
   * Attach an arbitrary string to this allocation result.
   */
  public void setAuxData(String newAuxData) {
    auxData = newAuxData;
  }

  /**
   * Get an arbitrary string attached to this allocation result.
   */
  public String getAuxData() {
    return auxData;

  }

  /**
   * Set how likely correct the answer(s) is/are.
   * @param newConfidenceRating 0 = unsure, 1.0 = completely sure.
   */
  public void setConfidenceRating(long newConfidenceRating) {
    confidenceRating = newConfidenceRating;
  }

  /**
   * Get how likely correct the answer(s) is/are.
   * @return 0 = unsure, 1.0 = completely sure.
   */
  public long getConfidenceRating() {
    return confidenceRating;
  }

  /**
   * Set the results aspects (array is parallel to getValues())
   */
  public void setAspects(int[] newAspects) {
    aspects = newAspects;
  }

  /**
   * Get the results aspects (array is parallel to getValues())
   */
  public int[] getAspects() {
    return aspects;
  }

  /**
   * Add a new aspect and value to the current set.
   */
  public void addAspectValuePair(int aspect, long value) {
    int [] newAspects = new int[aspects.length + 1];
    long [] newValues = new long[aspects.length + 1];

    System.arraycopy(aspects, 0, newAspects, 0, aspects.length);
    System.arraycopy(values, 0, newValues, 0, aspects.length);

    newAspects[aspects.length] = aspect;
    newValues[aspects.length] = value;

    aspects = newAspects;
    values = newValues;

  }

}
