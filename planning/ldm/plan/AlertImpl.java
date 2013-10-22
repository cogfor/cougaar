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

import org.cougaar.core.util.UID;

/** Alert Implementation
 *
 *
 * Open issues - 
 * what happens when no one is subscribed to listen for this alert
 * need an Alert type, otherwise all Alerts will go to all subscribers
 */

public class AlertImpl implements Alert, NewAlert {

  protected String myDisplayText; // answers the question "Right, what's all this, then?"
  protected AlertParameter[] myParameters; //description, object pairs
  protected Object myResponse; //option chosen
  protected boolean myAcknowledged; // true if this Alert has been acted upon
  protected int mySeverity;
  protected int myType;
  protected boolean myResponseRequired;
  
  private UID myUID;

  /**
   * Constructor - takes no args
   */
  public AlertImpl() {
    myDisplayText = null;
    myParameters = null;
    myResponse = null;
    myAcknowledged = false;
    mySeverity = Alert.UNDEFINED_SEVERITY;
    myType = Alert.UNDEFINED_TYPE;
    myResponseRequired = false;
    myUID = null;
  }

  /**
   * Constructor - 
   *
   * @param text String
   * @param operatorResponseRequired boolean
   * @param parameterOptions AlertParameter[]
   * @param severity int
   * @param uid UID
   */
  public AlertImpl(String text,
		   boolean operatorResponseRequired,
		   AlertParameter[] parameterOptions,
		   int severity,
                   int type,
		   UID uid) {
    myDisplayText = text;
    myResponseRequired = operatorResponseRequired;
    myParameters = parameterOptions;
    mySeverity = severity;
    myType = type;
    myUID = uid;
  }
  
  /**
   * setAlertText - sets display text for the alert
   */
  public void setAlertText(String alertText) {
    myDisplayText = alertText;
  }

  /**
   * getAlertText - returns the display text for the alert
   */
  public String getAlertText() {
    return myDisplayText;
  }

  /**
   * getAlertParameters - returns alert parameters associated with the alert
   *
   * @return AlertParameter[]
   */
  public AlertParameter[] getAlertParameters() {
    return myParameters;
  }

  /**
   * setAlertParameters - sets the alert parameters associated with the alert
   *
   * @param params AlertParameter[]
   */
  public void setAlertParameters(AlertParameter[] params) {
    myParameters = params;
  }

  /**
   * getOperatorResponse - return the operator response to the alert
   *
   * @return Object - operator response to the alert. May be null.
   */
  public Object getOperatorResponse () {
    return myResponse;
  }

  /**
   * setOperatorResponse - set the operator response to the alert
   * 
   * @param response Object
   */
  public void setOperatorResponse (Object response){
    myResponse = response;
  }


  /**
   * getOperatorResponseRequired - return boolean indicating whether alert requires an
   * operator response.
   *
   * @return boolean
   */
  public boolean getOperatorResponseRequired() {
    return myResponseRequired;
  }

  /**
   * setOperatorResponseRequired - sets boolean indicating whether alert requires an
   * operator response.
   */
  public void setOperatorResponseRequired(boolean req) {
    myResponseRequired = req;
  }

  /**
   * getSeverity - returns severity associated with the alert
   *
   * @return int 
   */
  public int getSeverity(){
    return mySeverity;
  }

  /**
   * setSeverity - sets severity associated with the alert
   * BOZO - This should be checking against defined severity values.
   *
   * @param severity int 
   */
  public void setSeverity(int severity) {
    if (!validSeverity(severity)) {
      //BOZO - how are we logging errors?
      System.out.println("AlertImpl.setSeverity(): unrecognized severity " + severity);
    }
    mySeverity = severity;
  }

  /**
   * validSeverity - checks whether severity is valid.
   * MIN_SEVERITY and MAX_SEVERITY used to check for valid severity values.
   * Simple minded but works so long as we can keep a contiguous set of severities.
   * @param severity int to be tested
   * @return boolean true if valid severity, else false
   */
  public boolean validSeverity(int severity) {
    if ((severity < MIN_SEVERITY) ||
        (severity > Alert.MAX_SEVERITY)) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * getType - returns alert type
   *
   * @return int 
   */
  public int getType(){
    return myType;
  }

  /**
   * setType - sets alert type
   * BOZO - Should be checking against some set of valid types.
   *
   * @param type int 
   */
  public void setType(int type) {
    myType = type;
  }

  /**
   * setAcknowledged - sets boolean indicating whether alert has been acknowleged
   * BOZO - Definition of 'acknowledged' is fuzzy. I assume that an alert which
   * requires an operator response can not acknowledged without the response.  I see 
   * no code enforcing this behaviour.
   * 
   * @param ack boolean
   */
  public void setAcknowledged(boolean ack) {
    myAcknowledged = ack;
  }

  /**
   * getAcknowledged - returns boolean indicating whether alert has been acknowledged
   *
   * @return boolean
   */
  public boolean getAcknowledged() {
    return myAcknowledged;
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
 
