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
 * Represents an abstract Task entry within the COMPLETION PSP.
 **/
public abstract class AbstractTask
  implements XMLable, DeXMLable, Serializable{
  
  //Variables:
  ////////////
  
  protected final static String UID_TAG = "UID";
  protected final static String UID_URL_TAG = "UID_URL";
  protected final static String PARENT_UID_TAG = "ParentUID";
  protected final static String PARENT_UID_URL_TAG = "ParentUID_URL";
  protected final static String CONFIDENCE_TAG = "Confidence";
  protected final static String PLAN_ELEMENT_TAG = "PlanElement";
  
  protected String uid;
  protected String uid_url;
  protected String parentUID;
  protected String parentUID_url;
  protected double confidence;
  protected String planElement;
  protected String verb;
  
  //Constructors:
  ///////////////
  
  public AbstractTask(){
  }
  
  //Tag override:
  ///////////////
  protected abstract String getNameTag();

  //Setters:
  //////////
  
  public void setUID(String uid) {
    this.uid = uid;
  }

  public void setUID_URL(String uid_url) { 
    this.uid_url = uid_url;
  }

  public void setParentUID(String parentUID) {
    this.parentUID = parentUID;
  }

  public void setParentUID_URL(String parentUID_url) { 
    this.parentUID_url = parentUID_url;
  }

  public void setConfidence(double confidence) { 
    this.confidence = confidence;
  }

  public void setPlanElement(String planElement) { 
    this.planElement = planElement;
  }

  public void setVerb(String verb) {
    this.verb = verb;
  }
  
  //Getters:
  //////////
  
  public String getUID() {
    return uid;
  }

  public String getUID_URL() {
    return uid_url;
  }

  public String getParentUID() {
    return parentUID;
  }

  public String getParentUID_URL() {
    return parentUID_url;
  }

  public double getConfidence() {
    return confidence;
  }

  public String getPlanElement() {
    return planElement;
  }

  public String getVerb() {
    return verb;
  }

  //XMLable members:
  //----------------
  
  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException{
    w.optagln(getNameTag());
    
    w.tagln(UID_TAG, getUID());
    w.tagln(UID_URL_TAG, getUID_URL());
    w.tagln(PARENT_UID_TAG, getParentUID());
    w.tagln(PARENT_UID_URL_TAG, getParentUID_URL());
    w.tagln(CONFIDENCE_TAG, getConfidence());
    w.tagln(PLAN_ELEMENT_TAG, getPlanElement());
    
    w.cltagln(getNameTag());
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
    if (name.equals(getNameTag())) {
    } else if (name.equals(UID_TAG)) {
      setUID(data);
    } else if (name.equals(UID_URL_TAG)) {
      setUID_URL(data);
    } else if (name.equals(PARENT_UID_TAG)) {
      setParentUID(data);
    } else if (name.equals(PARENT_UID_URL_TAG)) {
      setParentUID_URL(data);
    } else if (name.equals(CONFIDENCE_TAG)) {
      try {
        setConfidence(Double.parseDouble(data));
      } catch (NumberFormatException e) {
        throw new UnexpectedXMLException("Malformed Number: " + 
            name + " : " + data);
      }
    } else if (name.equals(PLAN_ELEMENT_TAG)) {
      setPlanElement(data);
    } else {
      throw new UnexpectedXMLException("Unexpected tag: "+name);
    }
  }
  
  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being DeXMLized
   **/
  public boolean closeTag(String name)
    throws UnexpectedXMLException {
    return name.equals(getNameTag());
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
    throws UnexpectedXMLException{
  }

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 728328929999383874L;
}
