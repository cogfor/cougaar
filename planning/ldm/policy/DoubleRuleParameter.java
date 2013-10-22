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
 * An DoubleRuleParameter is a RuleParameter with specified/protected
 * double bounds that returns a Double
 */
public class DoubleRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected double my_min;
  protected double my_max;
  protected Double my_value;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public DoubleRuleParameter(String param_name, double min, double max)
  { 
    my_min = min; my_max = max; my_value = null;
    my_name = param_name;
  }

  public DoubleRuleParameter(String param_name)
  { 
    my_name = param_name;
  }

  public DoubleRuleParameter() {
  }

  /**
   * Parameter type is DOUBLE
   */
  public int ParameterType() { return RuleParameter.DOUBLE_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public double getMin() {
    return my_min;
  }
    
  public void setMin(double min) {
    my_min = min;
  }

  public double getMax() {
    return my_max;
  }

  public void setMax(double max) {
    my_max = max;
  }

  public void setBounds(double min, double max) { 
    if (min > max) {
      throw new java.lang.IllegalArgumentException("min  - " + min + 
                                                   " - must be greater than max - " + max);
    }
    my_min = min; 
    my_max = max;
  }

  public double getLowerBound() {
    return getMin();
  }

  public double getUpperBound() {
    return getMax();
  }

  /**
   * Get parameter value (Double)
   * @return Object parameter value (Double). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param new_value : must be Double
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Double) {
      Double new_double = (Double)new_value;
      if ((new_double.intValue() >= my_min) && 
	  (new_double.intValue() <= my_max)) {
	my_value = new_double;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.DOUBLE_PARAMETER, 
	 "Double must be between " + my_min + " and " + my_max);
  }

  /**
   * 
   * @param test_value : must be Double
   * @return true if test_value is within the acceptable range
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Double) {
      Double new_double = (Double)test_value;
      if ((new_double.doubleValue() >= my_min) && 
	  (new_double.doubleValue() <= my_max))
	return true;
    }
    return false;
      
  }

  public static void Test() 
  {
    DoubleRuleParameter drp = new DoubleRuleParameter("testDoubleParam", 3.14, 10.73);

    if (drp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      drp.setValue(new Double(11.11));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      drp.setValue(new Double(1.2));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    Double d4 = new Double(4.5);
    try {
      drp.setValue(d4);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(drp.getValue() != d4) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("DRP = " + drp);
    System.out.println("DoubleRuleParameter test complete.");

  }

  public String toString() 
  {
    return "#<DOUBLE_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "] >";
  }

  public Object clone() {
    DoubleRuleParameter dp = new DoubleRuleParameter(my_name, my_min, my_max);
    try {
      dp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return dp;
  }


}
