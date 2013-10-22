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
 * Describes the disposition of a MicroTask to an Asset.
 */
public class MicroAllocation implements Encodable {

  public MicroAllocation(MicroAsset asset, MicroTask task) {
    setAsset(asset);
    setTask(task);
    task.setAllocation(this);
  }
  private MicroAllocationResult reportedResult;

  /**
   * Get the results (status) of this allocation.
   */
  public MicroAllocationResult getReportedResult() {
    return reportedResult;
  }

  /**
   * Set the results (status) of this allocation.
   */
  public void setReportedResult(MicroAllocationResult newReportedResult) {
    reportedResult = newReportedResult;
  }

  protected static String tag = "MicroAllocation";
  private MicroAsset asset;
  private MicroTask task;
  /**
   * XML encode this object and all sub-objects.
   */
  public void encode(StringBuffer str) {
    str.append("<");
    str.append(tag);
    str.append(">");
    if (getReportedResult() != null)
      getReportedResult().encode(str);
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
   * Set the asset associated with this allocation.
   */
  public void setAsset(org.cougaar.microedition.shared.MicroAsset newAsset) {
    asset = newAsset;
  }
  /**
   * Get the asset associated with this allocation.
   */
  public org.cougaar.microedition.shared.MicroAsset getAsset() {
    return asset;
  }

  /**
   * Set the MicroTask associated with this allocation.
   */
  public void setTask(org.cougaar.microedition.shared.MicroTask newTask) {
    task = newTask;
  }
  /**
   * Get the MicroTask associated with this allocation.
   */
  public org.cougaar.microedition.shared.MicroTask getTask() {
    return task;
  }
}
