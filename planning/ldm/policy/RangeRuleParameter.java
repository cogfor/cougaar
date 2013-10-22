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
 * An RangeRuleParameter is a RuleParameter with a list of 
 * integer-range delineated values and a default value. When the
 * getValue is called with the Key argument, and some value is defined
 * within some range, that value is returned. Otherwise, the default
 * is returned.
 */
public class RangeRuleParameter implements RuleParameter,
  java.io.Serializable {
  protected String my_name;
  protected RangeRuleParameterEntry []my_ranges;
  protected Object my_default_value;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public RangeRuleParameter(String param_name, RangeRuleParameterEntry []ranges)
  { 
    my_ranges = ranges; 
    my_default_value = null;
    my_name = param_name;
  }


  public RangeRuleParameter(String param_name)
  { 
    my_name = param_name;
    my_default_value = null;
  }

  public RangeRuleParameter() {
  }

  /**
   * Parameter type is RANGE
   */
  public int ParameterType() { return RuleParameter.RANGE_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public RangeRuleParameterEntry[] getRanges() {
    return my_ranges;
  }

  public void setRanges(RangeRuleParameterEntry []ranges)
  { 
    my_ranges = ranges; 
    my_default_value = null;
  }

  /**
   * Get parameter value
   * @return parameter value. Note : could be null.
   */
  public Object getValue()
  {
    return my_default_value; 
  }

  /**
   * Get parameter value keyed by int
   * If key fits into one of the defined ranges, return associated
   * value, otherwise return default value.
   * @return parameter value. Note : could be null.
   */
  public Object getValue(int key)
  {
      Object value = my_default_value;
      for(int i = 0; i < my_ranges.length; i++) {
	  if ((my_ranges[i].getRangeMin() <= key) &&
	      (my_ranges[i].getRangeMax() >= key)) {
	      value = my_ranges[i].getValue();
	      break;
	  }
      }

    return value; 
  }

  /**
   * Set parameter value
   * @param new_value : must be String within given list
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    my_default_value = new_value;
  }

  /**
   * @param test_value 
   * @return always returns true 
   */
  public boolean inRange(Object test_value)
  {
    return true;
  }

  public String toString() 
  {
    return "#<RANGE_PARAMETER : " + my_default_value + 
      " [" + Range_List() + "] >";
  }

  public Object clone() {
    RangeRuleParameter rrp = new RangeRuleParameter(my_name);
    if (my_ranges != null) {
      rrp.setRanges((RangeRuleParameterEntry[])my_ranges.clone());
    }
    try {
      rrp.setValue(my_default_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return rrp;
  }

  public static void main(String []args) {
    RangeRuleParameterEntry p1 = 
      new RangeRuleParameterEntry("LOW", 1, 3);
    RangeRuleParameterEntry p2 = 
      new RangeRuleParameterEntry(new Integer(37), 4, 6);
    RangeRuleParameterEntry p3 = 
      new RangeRuleParameterEntry("HIGH", 7, 9);
    
    RangeRuleParameterEntry []ranges = {p1, p2, p3};
    RangeRuleParameter rrp = 
      new RangeRuleParameter("testRangeParam", ranges);
    
    if (rrp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      rrp.setValue("DFLT");
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting illegal set condition");
    }

    for(int i = 0; i <= 10; i++) {
      Object value = rrp.getValue(i);
      System.out.println("Value for " + i + " = " + value);
    }
    
    System.out.println("RRP = " + rrp);
    System.out.println("RuleRuleParameter test complete.");
    
  }
  
  protected String Range_List() {
    String list = "";
    for(int i = 0; i < my_ranges.length; i++) {
      list += my_ranges[i];
      if (i != my_ranges.length-1)
	list += "/";
    }
    return list;
  }
}


