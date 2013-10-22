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
import java.util.ArrayList;
import java.util.List;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.xml.sax.Attributes;

/**
 * Represents the data leaving the Completion PSP --
 * <code>FullCompletionData</code> captures all the <code>AbstractTask</code>s.
 *
 * @see SimpleCompletionData
 * @see CompletionData
 **/
public class FullCompletionData extends CompletionData{

  //Variables:
  ////////////

  public static final String NAME_TAG = "FullCompletion";

  protected List unplannedTasks;
  protected List unestimatedTasks;
  protected List unconfidentTasks;
  protected List failedTasks;

  //Constructors:
  ///////////////

  public FullCompletionData() {
    unplannedTasks = new ArrayList();
    unestimatedTasks = new ArrayList();
    unconfidentTasks = new ArrayList();
    failedTasks = new ArrayList();
  }

  //Setters:
  //////////

  public void addUnplannedTask(UnplannedTask ut) {
    unplannedTasks.add(ut);
  }

  public void addUnestimatedTask(UnestimatedTask uet) {
    unestimatedTasks.add(uet);
  }

  public void addUnconfidentTask(UnconfidentTask uct) {
    unconfidentTasks.add(uct);
  }

  public void addFailedTask(FailedTask ft) {
    failedTasks.add(ft);
  }

  //Getters:
  //////////

  public int getNumberOfUnplannedTasks() {
    return unplannedTasks.size();
  }

  public int getNumberOfUnestimatedTasks() {
    return unestimatedTasks.size();
  }

  public int getNumberOfUnconfidentTasks() {
    return unconfidentTasks.size();
  }

  public int getNumberOfFailedTasks() {
    return failedTasks.size();
  }

  public UnplannedTask getUnplannedTaskAt(int i) {
    return (UnplannedTask)unplannedTasks.get(i);
  }

  public UnestimatedTask getUnestimatedTaskAt(int i) {
    return (UnestimatedTask)unestimatedTasks.get(i);
  }

  public UnconfidentTask getUnconfidentTaskAt(int i) {
    return (UnconfidentTask)unconfidentTasks.get(i);
  }

  public FailedTask getFailedTaskAt(int i) {
    return (FailedTask)failedTasks.get(i);
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
    for (int i = 0; i < getNumberOfUnplannedTasks(); i++) {
      getUnplannedTaskAt(i).toXML(w);
    }
    for (int i = 0; i < getNumberOfUnestimatedTasks(); i++) {
      getUnestimatedTaskAt(i).toXML(w);
    }
    for (int i = 0;i < getNumberOfUnconfidentTasks(); i++) {
      getUnconfidentTaskAt(i).toXML(w);
    }
    for (int i = 0;i < getNumberOfFailedTasks(); i++) {
      getFailedTaskAt(i).toXML(w);
    }
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
    if (obj instanceof UnplannedTask) {
      addUnplannedTask((UnplannedTask)obj);
    } else if (obj instanceof UnestimatedTask) {
      addUnestimatedTask((UnestimatedTask)obj);
    } else if (obj instanceof UnconfidentTask) {
      addUnconfidentTask((UnconfidentTask)obj);
    } else if (obj instanceof FailedTask) {
      addFailedTask((FailedTask)obj);
    } else {
      throw new UnexpectedXMLException("Unexpected object: " + obj);
    }
  }

  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 8898898989829387181L;
}
