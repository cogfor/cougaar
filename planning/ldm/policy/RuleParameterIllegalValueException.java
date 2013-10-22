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


package org.cougaar.planning.ldm.policy;

 /** 
   *
   **/

/**
 * A RuleParameterException is an exception to be generated when setting
 * a parameter with values illegal for that parameter instance.
 */
public class RuleParameterIllegalValueException extends Exception {
  /**
   * Constructor - Contains parameter type and message
   * @param parameter_type for type code of parameter (from RuleParameter)
   * @param message 
   */
  public RuleParameterIllegalValueException
      (int parameter_type, String message) 
  { 
    my_parameter_type = parameter_type; 
    my_message = message; 
  }

  /**
   * Accessor to parameter type code
   * @return int parameter type code
   */
  public int ParameterType() { return my_parameter_type; }

  /**
   * Accessor to text message
   * @return String text message
   */
  public String Message() { return my_message; }

  protected int my_parameter_type;
  protected String my_message;
}

