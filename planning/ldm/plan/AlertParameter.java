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

/** AlertParameter interface
 *
 *
 * BOZO - Use of AlertParameter is not clearly defined. Object will probably change
 * when we attempt to actually use it.
 **/

public interface AlertParameter extends java.io.Serializable {

  /**
   * An object whose contents would be meaningful to a UI user who must 
   * respond to the Alert that this AlertParameter is part of.
   **/
  Object getParameter();

  /**
   * A description of the AlertParameter for display in the UI to tell
   * a user what and why he is seeing it.
   **/
  String getDescription();

  /**
   * The answer to the question posed by this AlertParameter. This method
   * would be used by the UI to fill in the user's response, if any.
   **/
  Object getResponse();

  /**
   * Should this parameter be visible. Invisible parameters simply
   * carry information needed to handle the alert when it is
   * acknowledged.
   **/
  boolean isVisible();

  /**
   * Should this parameter be editable. Uneditable parameters simply
   * supply additional information to the operator, but the operator
   * can't change them.
   **/
  boolean isEditable();
}
