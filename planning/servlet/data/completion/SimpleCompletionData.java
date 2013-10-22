/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.planning.servlet.data.completion;

import java.io.IOException;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.xml.sax.Attributes;

/**
 * Represents the data leaving the Completion PSP --
 * <code>SimpleCompletionData</code> only contains counters of the
 * Tasks, not <code>AbstractTask</code>s.
 *
 * @see FullCompletionData
 * @see CompletionData
 **/
public class SimpleCompletionData extends CompletionData{

  //Variables:
  ////////////

  public static final String NAME_TAG = "SimpleCompletion";

  public static final String NUMBER_OF_UNPLANNED_TASKS_ATTR = 
    "NumUnplannedTasks";
  public static final String NUMBER_OF_UNESTIMATED_TASKS_ATTR = 
    "NumUnestimatedTasks";
  public static final String NUMBER_OF_UNCONFIDENT_TASKS_ATTR = 
    "NumUnconfidentTasks";
  public static final String NUMBER_OF_FAILED_TASKS_ATTR =
    "NumFailedTasks";
  public static final String NUMBER_OF_ROOT_PS_TASKS_ATTR =
    "NumRootProjectSupplyTasks";
  public static final String NUMBER_OF_ROOT_SUPPLY_TASKS_ATTR =
    "NumRootSupplyTasks";
  public static final String NUMBER_OF_ROOT_TRANSPORT_TASKS_ATTR =
    "NumRootTransportTasks";

  protected int numUnplannedTasks;
  protected int numUnestimatedTasks;
  protected int numUnconfidentTasks;
  protected int numFailedTasks;

  //Constructors:
  ///////////////

  public SimpleCompletionData() {
  }

  //Setters:
  //////////

  public void setNumberOfUnplannedTasks(int numUnplannedTasks) {
    this.numUnplannedTasks = numUnplannedTasks;
  }

  public void setNumberOfUnestimatedTasks(int numUnestimatedTasks) {
    this.numUnestimatedTasks = numUnestimatedTasks;
  }

  public void setNumberOfUnconfidentTasks(int numUnconfidentTasks) {
    this.numUnconfidentTasks = numUnconfidentTasks;
  }

  public void setNumberOfFailedTasks(int numFailedTasks) {
    this.numFailedTasks = numFailedTasks;
  }

  //Getters:
  //////////

  public int getNumberOfUnplannedTasks() {
    return numUnplannedTasks;
  }

  public int getNumberOfUnestimatedTasks() {
    return numUnestimatedTasks;
  }

  public int getNumberOfUnconfidentTasks() {
    return numUnconfidentTasks;
  }

  public int getNumberOfFailedTasks() {
    return numFailedTasks;
  }

  public UnplannedTask getUnplannedTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  public UnestimatedTask getUnestimatedTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  public UnconfidentTask getUnconfidentTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  public FailedTask getFailedTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException {
    w.optagln(NAME_TAG);
    w.tagln(TIME_MILLIS_ATTR, getTimeMillis());
    w.tagln(RATIO_ATTR, getRatio());
    w.tagln(NUMBER_OF_TASKS_ATTR, getNumberOfTasks());
    w.tagln(NUMBER_OF_UNPLANNED_TASKS_ATTR, getNumberOfUnplannedTasks());
    w.tagln(NUMBER_OF_UNESTIMATED_TASKS_ATTR, getNumberOfUnestimatedTasks());
    w.tagln(NUMBER_OF_UNCONFIDENT_TASKS_ATTR, getNumberOfUnconfidentTasks());
    w.tagln(NUMBER_OF_FAILED_TASKS_ATTR, getNumberOfFailedTasks());
    w.tagln(NUMBER_OF_ROOT_PS_TASKS_ATTR, getNumberOfRootProjectSupplyTasks());
    w.tagln(NUMBER_OF_ROOT_SUPPLY_TASKS_ATTR, getNumberOfRootSupplyTasks());
    w.tagln(NUMBER_OF_ROOT_TRANSPORT_TASKS_ATTR, getNumberOfRootTransportTasks());
    w.cltagln(NAME_TAG);
  }

  //DeXMLable members:
  //------------------

  /**
   * Report a startElement that pertains to THIS object, not any
   * sub objects.  Call also provides the elements Attributes and data.  
   * Note, that  unlike in a SAX parser, data is guaranteed to contain 
   * ALL of this tag's data, not just a 'chunk' of it.
   * @param name startElement tag
   * @param attr attributes for this tag
   * @param data data for this tag
   **/
  public void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException {
   try {
     if (name.equals(NAME_TAG)) {
     } else if (name.equals(TIME_MILLIS_ATTR)) {
       timeMillis = Long.parseLong(data);
     } else if (name.equals(RATIO_ATTR)) {
       ratio = Double.parseDouble(data);
     } else if (name.equals(NUMBER_OF_TASKS_ATTR)) {
       numTasks = Integer.parseInt(data);
     } else if (name.equals(NUMBER_OF_UNPLANNED_TASKS_ATTR)) {
       numUnplannedTasks = Integer.parseInt(data);
     } else if (name.equals(NUMBER_OF_UNESTIMATED_TASKS_ATTR)) {
       numUnestimatedTasks = Integer.parseInt(data);
     } else if (name.equals(NUMBER_OF_UNCONFIDENT_TASKS_ATTR)) {
       numUnconfidentTasks = Integer.parseInt(data);
     } else if (name.equals(NUMBER_OF_FAILED_TASKS_ATTR)) {
       numFailedTasks = Integer.parseInt(data);
     } else if (name.equals(NUMBER_OF_ROOT_PS_TASKS_ATTR)) {
       numRootProjectSupplyTasks = Integer.parseInt(data);
     } else if (name.equals(NUMBER_OF_ROOT_SUPPLY_TASKS_ATTR)) {
       numRootSupplyTasks = Integer.parseInt(data);
     } else if (name.equals(NUMBER_OF_ROOT_TRANSPORT_TASKS_ATTR)) {
       numRootTransportTasks = Integer.parseInt(data);
     } else {
       throw new UnexpectedXMLException("Unexpected tag: "+name);
     }
   } catch (NumberFormatException e) {
     throw new UnexpectedXMLException("Malformed Number: " +
                                      name + " : " + data);
   }
  }

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being DeXMLized
   **/
  public boolean closeTag(String name)
    throws UnexpectedXMLException {
    return name.equals(NAME_TAG);
  }

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  public void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException {
      throw new UnexpectedXMLException("Unexpected object: " + obj);
  }

  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 2789998238928387475L;
}
