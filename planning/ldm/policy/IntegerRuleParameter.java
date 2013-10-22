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
 * An IntegerRuleParameter is a RuleParameter with specified/protected
 * integer bounds that returns an Integer
 */
public class IntegerRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected Integer my_value;
  protected int my_min;
  protected int my_max;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public IntegerRuleParameter(String param_name, int min, int max, int value)
    throws RuleParameterIllegalValueException
  {
    this(param_name, min, max);
    setValue(new Integer(value));
  }

  public IntegerRuleParameter(String param_name, int min, int max) {
    my_min = min; my_max = max; my_value = null;
    my_name = param_name;
  }

  public IntegerRuleParameter(String param_name)
  { 
    my_value = null;
    my_name = param_name;
  }

  public IntegerRuleParameter() {
  }

  /**
   * Parameter type is INTEGER
   */
  public int ParameterType() { return RuleParameter.INTEGER_PARAMETER; }


  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public int getMin() {
    return my_min;
  }
    
  public void setMin(int min) {
    my_min = min;
  }

  public int getMax() {
    return my_max;
  }

  public void setMax(int max) {
    my_max = max;
  }

  public void setBounds(int min, int max) {
    if (min > max) {
      throw new java.lang.IllegalArgumentException("min  - " + min + 
                                                   " - must be greater than max - " + max);
    }
    my_min = min; 
    my_max = max;
  }

  public int getLowerBound() {
    return getMin();
  }

  public int getUpperBound() {
    return getMax();
  }

  /**
   * Get parameter value (Integer)
   * @return Object parameter value (Integer). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  public int intValue() {
    return my_value.intValue();
  }

  /**
   * Set parameter value
   * @param new_value : must be Integer
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Integer) {
      Integer new_integer = (Integer)new_value;
      if ((new_integer.intValue() >= my_min) && 
	  (new_integer.intValue() <= my_max)) {
	my_value = new_integer;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.INTEGER_PARAMETER, 
	 "Integer must be between " + my_min + " and " + my_max);
  }

  /**
   * 
   * @param test_value : must be Integer
   * @return true if test_value is within the acceptable range
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Integer) {
      Integer new_integer = (Integer)test_value;
      if ((new_integer.intValue() >= my_min) && 
	  (new_integer.intValue() <= my_max))
	return true;
    }
    return false;
      
  }


  public String toString() 
  {
    return "#<INTEGER_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "] >";
  }

  public Object clone() {
    IntegerRuleParameter irp = new IntegerRuleParameter(my_name, my_min, my_max);
    try {
      irp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return irp;
  }

  public static void Test() 
  {
    IntegerRuleParameter irp = new IntegerRuleParameter("testIntParam", 3, 10);

    if (irp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      irp.setValue(new Integer(11));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      irp.setValue(new Integer(1));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    Integer i4 = new Integer(4);
    try {
      irp.setValue(i4);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(irp.getValue() != i4) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("IRP = " + irp);
    System.out.println("IntegerRuleParameter test complete.");

  }


}
