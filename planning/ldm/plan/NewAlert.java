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

/** NewAlert interface
 *
 **/

public interface NewAlert extends Alert {

  /**
   * Text to be displayed by UI to explain the Alert to the user.
   **/
  void setAlertText(String alertText);

  /**
   * Parameters that contain objects to be acted upon, or chosen from among
   **/
  void setAlertParameters(AlertParameter[] param);
  
  /**
   * Indicates whether the Alert has been acted upon
   **/
  void setAcknowledged(boolean ack);

  /**
   * Indicates Alert severity
   * Should be one of the values defined in the Alert interface.
   */
  void setSeverity(int severity);

  /**
   * Indicates Alert type. 
   * BOZO - I presume this means the type of activity which generated the alert - 
   * transportation, ... Valid types should be defined within the Alert interface.
   */
  void setType(int type);

  /**
   * Indicates whether UI user is required to take action on this alert
   **/
  void setOperatorResponseRequired(boolean required);

  /**
   * The answer to the Alert. The AlertParameters can also have responses
   **/
  void setOperatorResponse(Object response);

}
