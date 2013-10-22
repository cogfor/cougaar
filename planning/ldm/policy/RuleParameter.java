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
 * A RuleParameter is generic object containing a parameter value
 *
 * Values may be one of several types:
 *    INTEGER - Integer value type (within given bounds)
 *    DOUBLE - Double value type (within given bounds)
 *    STRING - String value type
 *    LONG - Long value type (within given bounds)
 *    ENUMERATION - Enumeration value type (String from given list)
 *    BOOLEAN - Boolean value type
 *    CLASS - Java class value type (implementing given interface)
 *    KEY - Set of String values (with default) indexed off a key
 *    RANGE - Set of values (String or RuleParameter) (with default) indexed 
 *        from a list of integer ranges
 *
 */
public interface RuleParameter extends Cloneable {

  /**
   * Define list of constant parameter types:
   */
  int INTEGER_PARAMETER = 1;
  int DOUBLE_PARAMETER = 2;
  int STRING_PARAMETER = 3;
  int ENUMERATION_PARAMETER = 4;
  int BOOLEAN_PARAMETER = 5;
  int CLASS_PARAMETER = 6;
  int KEY_PARAMETER = 7;
  int RANGE_PARAMETER = 8;
  int LONG_PARAMETER = 9;
  int PREDICATE_PARAMETER = 10;

  /**
   * Type of given parameter
   * @return int type of given parameter
   */
  int ParameterType();

  /**
   * Get parameter object value for parameter
   * @return Object with given parameter value. Note : could be null.
   */
  Object getValue();

  /**
   * Set parameter object value 
   * @param new_value - the new value to be set
   * @throws RuleParameterIllegalValueException if value set is illegal for 
   * given parameter
   */
  void setValue(Object new_value) 
       throws RuleParameterIllegalValueException;

  /**
   * Test the value to see if it is valid.
   * @param test_object - the value to be tested
   * @return true if the test_object is within the allowable range, false
   * otherwise.
   **/
  boolean inRange(Object test_object);

  /**
   * @return the name of the parameter
   **/
  String getName();

  Object clone();
}


