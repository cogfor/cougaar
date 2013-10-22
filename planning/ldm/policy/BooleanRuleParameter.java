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
 * A BooleanRuleParameter is a RuleParameter that contains a single true
 * or false value
 */
public class BooleanRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected Boolean my_value;

  /**
   * Constructor  - Initially not set
   */
  public BooleanRuleParameter(String param_name)
  { 
    my_value = null;
    my_name = param_name;
  }

  /**
   * Constructor with value set
   */
  public BooleanRuleParameter(String param_name, boolean value)
  { 
    my_value = new Boolean(value);
    my_name = param_name;
  }

  public BooleanRuleParameter() {
  }

  /**
   * Parameter type is Boolean
   */
  public int ParameterType() { return RuleParameter.BOOLEAN_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  /**
   * Get parameter value (Boolean)
   * @return Object parameter value (Boolean). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param new_value : must be Boolean
   * @throws RuleParameterIllegalValueException (only Boolean accepted)
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Boolean) {
      my_value = (Boolean)new_value;
      success = true;
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.BOOLEAN_PARAMETER, "Argument must be a Boolean.");
  }

  /**
   * @param test_value : must be Boolean
   * @return true if Object is a Boolean, false otherwise
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Boolean) {
      return true;
    }
    return false;
  }


  public String toString() 
  {
    return "#<BOOLEAN_PARAMETER : " + my_value + ">";
  }

  public Object clone() {
    BooleanRuleParameter brp = new BooleanRuleParameter(my_name);
    try {
      brp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return brp;
  }

}


