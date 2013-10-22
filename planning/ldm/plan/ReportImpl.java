/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
package org.cougaar.planning.ldm.plan;

import java.io.Serializable;
import java.util.Date;

import org.cougaar.core.util.UID;

/** Report Implementation
 *
 *
 * Informational report contains a text and an associated date. 
 **/

public class ReportImpl 
  implements Report, NewReport, Serializable {

  protected String myText; // answers the question "Right, what's all this, then?"
  protected Date myDate; // Date associated with message. (When created?)
  private UID myUID;

  /**
   * Constructor - takes no args
   */
  public ReportImpl() {
    myText = null;
    myDate = null;
    myUID = null;
  }

  /**
   * Constructor - takes text, date, and UID args
   *
   * @param text String with text of report
   * @param date Date associated with report (probably creation date)
   * @param uid  UID for report
   */
  public ReportImpl(String text,
                         Date date,
                         UID uid) {
    myText = text;
    myDate = date;
    myUID = uid;
  }
  
  /**
   * setText - set text for message
   * 
   * @param reportText String with new text
   */
  public void setText(String reportText) {
    myText = reportText;
  }

  /**
   * getText - return text of message
   * 
   * @return String with text of the report
   */
  public String getText() {
    return myText;
  }

  /**
   * setDate - set date associated with the report
   *
   * @param date Date to be associated with the report
   */
  public void setDate(Date date) {
    myDate = date;
  }

  /**
   * getDate - return date associated with the report
   * 
   * @return Date associated with the report
   */
  public Date getDate() {
    return myDate;
  }

  /**
   * setUID - set uid for the object
   * Why is this public?  Does it make sense to allow random changes to 
   * UID?
   *
   * @param uid UID assigned to object
   */
  public void setUID(UID uid) {
    myUID = uid;
  }
  
  /**
   * getUID - get uid for the object
   *
   * @return UID assigned to object
   */
  public UID getUID() { 
    return myUID;
  }
}
 
