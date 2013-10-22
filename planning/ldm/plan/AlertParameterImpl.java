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


/** AlertParameter Implementation
 *
 **/

public class AlertParameterImpl implements AlertParameter, NewAlertParameter {

  // Description of the parameter
  protected String myDescription;
  // Actual parameter
  protected Object myParameter;
  // Operator response
  protected Object myResponse;
  // Editable
  protected boolean editable = true;
  // Visible
  protected boolean visible = true;

  /**
   * Constructor - takes no arguments
   */
  public AlertParameterImpl() {
    myDescription = null;
    myParameter = null;
    myResponse = null;
  }

  /**
   * Constructor
   *
   * @param description String description for the parameter
   * @param parameter Object actual object associated with the parameter
   */
  public AlertParameterImpl(String description, Object parameter) {
    myDescription = description;
    myParameter = parameter;
  }

  /**
   * getParameter - return Object whose contents would be meaningful to a UI user 
   * who must respond to the Alert associated with this AlertParameter
   *
   * @return Object
   */
  public Object getParameter() {
    return myParameter;
  }

  /**
   * setParameter - set object whose contents would be meaningful to a UI user 
   * who must respond to the Alert associated with this AlertParameter.
   *
   * @param param Object
   */
  public void setParameter(Object param){
    myParameter = param;
  }

  /**
   * getDescription - returns a description of the AlertParameter for display in 
   * the UI to tell a user what and why he is seeing it.
   *
   * @return String
   */
  public String getDescription() {
    return myDescription;
  }

  /**
   * setDescription - sets a description of the AlertParameter for display in the
   * UI to tell a user what and why he is seeing it.
   *
   * @param paramDescription String
   */
  public void setDescription(String paramDescription) {
    myDescription = paramDescription;
  }

  /**
   * setResponse - saves the answer to the question posed by this AlertParameter. 
   * This method would be used by the UI to fill in the user's response, if any.
   **/
  public void setResponse(Object response) {
    myResponse = response;
  }

  /**
   * getRespose - The answer to the question posed by this AlertParameter. This method
   * would be used by the UI to fill in the user's response, if any.
   **/
  public Object getResponse() {
    return myResponse;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean newEditable) {
    editable = newEditable;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean newVisible) {
    visible = newVisible;
  }
}

