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
 * An LongRuleParameter is a RuleParameter with specified/protected
 * long bounds that returns an Long
 */
public class LongRuleParameter implements RuleParameter, java.io.Serializable {

  protected String my_name;
  protected Long my_value;
  protected long my_min;
  protected long my_max;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public LongRuleParameter(String param_name, long min, long max, long value)
    throws RuleParameterIllegalValueException {
    this(param_name, min, max);
    setValue(new Long(value));
  }

  public LongRuleParameter(String param_name, long min, long max) {
    my_min = min; my_max = max; my_value = null;
    my_name = param_name;
  }

  public LongRuleParameter(String param_name) { 
    my_value = null;
    my_name = param_name;
  }

  public LongRuleParameter() {
  }

  /*
   * Parameter type is LONG
   */
  public int ParameterType() { return RuleParameter.LONG_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public long getMin() {
    return my_min;
  }
    
  public void setMin(long min) {
    my_min = min;
  }

  public long getMax() {
    return my_max;
  }

  public void setMax(long max) {
    my_max = max;
  }

  public void setBounds(long min, long max) {
    if (min > max) {
      throw new java.lang.IllegalArgumentException("min  - " + min + 
                                                   " - must be greater than max - " + max);
    }
    my_min = min; 
    my_max = max;
  }

  public long getLowerBound() {
    return getMin();
  }

  public long getUpperBound() {
    return getMax();
  }

  /**
   * Get parameter value (Long)
   * @return Object parameter value (Long). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  public long longValue() {
    return my_value.longValue();
  }

  /**
   * Set parameter value
   * @param new_value must be Long
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Long) {
      Long new_long = (Long)new_value;
      if ((new_long.longValue() >= my_min) && 
	  (new_long.longValue() <= my_max)) {
	my_value = new_long;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.LONG_PARAMETER, 
	 "Long must be between " + my_min + " and " + my_max);
  }

  /**
   * 
   * @param test_value : must be Long
   * @return true if test_value is within the acceptable range
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Long) {
      Long new_long = (Long)test_value;
      if ((new_long.longValue() >= my_min) && 
	  (new_long.longValue() <= my_max))
	return true;
    }
    return false;
      
  }


  public String toString() 
  {
    return "<#LONG_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "]>";
  }


    
  public Object clone() {
    LongRuleParameter irp = new LongRuleParameter(my_name, my_min, my_max);
    try {
      irp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return irp;
  }

  public static void Test() 
  {
    LongRuleParameter irp = new LongRuleParameter("testIntParam", 3, 10);

    if (irp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      irp.setValue(new Long(11));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      irp.setValue(new Long(1));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    Long i4 = new Long(4);
    try {
      irp.setValue(i4);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(irp.getValue() != i4) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("IRP = " + irp);
    System.out.println("LongRuleParameter test complete.");

  }


}





