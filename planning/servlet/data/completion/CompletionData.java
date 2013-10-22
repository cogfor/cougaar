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
import java.io.Serializable;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;
import org.xml.sax.Attributes;

/**
 * Abstract representation of the data leaving the Completion PSP.
 *
 * @see FullCompletionData
 * @see SimpleCompletionData
 **/
public abstract class CompletionData implements XMLable, DeXMLable, Serializable{

  //Variables:
  ////////////

  public static final String NAME_TAG = "Completion";

  public static final String TIME_MILLIS_ATTR = 
    "TimeMillis";
  public static final String RATIO_ATTR = 
    "Ratio";
  public static final String NUMBER_OF_TASKS_ATTR = 
    "NumTasks";

  protected long timeMillis;
  protected double ratio;
  protected int numTasks;
  protected int numRootProjectSupplyTasks;
  protected int numRootSupplyTasks;
  protected int numRootTransportTasks;

  //Constructors:
  ///////////////

  public CompletionData() {
  }

  //Setters:
  //////////

  public void setTimeMillis(long timeMillis) {
    this.timeMillis = timeMillis;
  }

  public void setRatio(double ratio) {
    // assert (0.0 <= ratio && ratio <= 1.0);
    this.ratio = ratio;
  }

  public void setNumberOfTasks(int numTasks) {
    this.numTasks = numTasks;
  }

  public void setNumberOfRootProjectSupplyTasks(int numTasks) {
    numRootProjectSupplyTasks = numTasks;
  }

  public void setNumberOfRootTransportTasks(int numTasks) {
    numRootTransportTasks = numTasks;
  }

  public void setNumberOfRootSupplyTasks(int numTasks) {
    numRootSupplyTasks = numTasks;
  }

  //Getters:
  //////////

  public long getTimeMillis() {
    return timeMillis;
  }

  /** number between 0.0 and 1.0, inclusive. */
  public double getRatio() {
    return ratio;
  }

  public int getNumberOfTasks() {
    return numTasks;
  }

  public int getNumberOfRootProjectSupplyTasks() {
    return numRootProjectSupplyTasks;
  }

  public int getNumberOfRootSupplyTasks() {
    return numRootSupplyTasks;
  }

  public int getNumberOfRootTransportTasks() {
    return numRootTransportTasks;
  }

  public abstract int getNumberOfUnplannedTasks();

  public abstract int getNumberOfUnestimatedTasks();

  public abstract int getNumberOfUnconfidentTasks();

  public abstract int getNumberOfFailedTasks();

  public int getNumberOfFullySuccessfulTasks() {
    return 
      (getNumberOfTasks() -
       (getNumberOfUnplannedTasks() +
        getNumberOfUnestimatedTasks() +
        getNumberOfUnconfidentTasks() +
        getNumberOfFailedTasks()));
  }

  public abstract UnplannedTask getUnplannedTaskAt(int i);

  public abstract UnestimatedTask getUnestimatedTaskAt(int i);

  public abstract UnconfidentTask getUnconfidentTaskAt(int i);

  public abstract FailedTask getFailedTaskAt(int i);

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public abstract void toXML(XMLWriter w) throws IOException;

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
  public abstract void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException;

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being DeXMLized
   **/
  public abstract boolean closeTag(String name)
    throws UnexpectedXMLException;

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  public abstract void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException;

  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 1234679540398212345L;
}
