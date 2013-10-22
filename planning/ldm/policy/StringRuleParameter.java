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
 * An StringRuleParameter is a RuleParameter that returns an arbitrary string
 */
public class StringRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected String my_value;

  /**
   * Constructor  - Initially not set
   */
  public StringRuleParameter(String param_name) { 
    my_value = null;
    my_name = param_name;
  }

  public StringRuleParameter() {
  }

  /**
   * Parameter type is String
   */
  public int ParameterType() { return RuleParameter.STRING_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  /**
   * Get parameter value (String)
   * @return Object parameter value (String). Note : could be null.
   */
  public Object getValue() {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param  new_value : must be String
   * @throws RuleParameterIllegalValueException (all strings accepted)
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException {
    boolean success = false;
    if (new_value instanceof String) {
      my_value = (String)new_value;
      success = true;
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.STRING_PARAMETER, "Argument must be a string.");
  }

  /**
   * @param test_value must be String
   * @return true if Object is a string, false otherwise
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof String) {
      return true;
    }
    return false;
  }


  public String toString() {
    return "#<STRING_PARAMETER : " + my_value + ">";
  }

  public Object clone() {
    StringRuleParameter srp = new StringRuleParameter(my_name);
    try {
      srp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return srp;
  }

}


