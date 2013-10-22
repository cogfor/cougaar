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

import org.cougaar.util.UnaryPredicate;

/** 
 *
 **/

/**
 * An PredicateRuleParameter is a RuleParameter with a UnaryPredicate
 * as its value. The inRange method is implemented to apply the
 * predicate to the test object.
 **/
public class PredicateRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected UnaryPredicate thePredicate;

  /**
   * Constructor sets the predicate
   **/
  public PredicateRuleParameter(String param_name, UnaryPredicate aPredicate) { 
    my_name = param_name;
    thePredicate = aPredicate;
  }

  public PredicateRuleParameter() {
  }

  /**
   * Parameter type is PREDICATE
   */
  public int ParameterType() {
    return RuleParameter.PREDICATE_PARAMETER;
  }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  /**
   * Get parameter value (UnaryPredicate)
   * @return Object parameter value (UnaryPredicate). Note : could be null.
   */
  public Object getValue() {
    return thePredicate;
  }

  /**
   * Convenience accessor not requiring casting the result
   **/
  public UnaryPredicate getPredicate() {
    return thePredicate;
  }

  /**
   * Set parameter value
   * @param newPredicate must be a UnaryPredicate
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object newPredicate) 
       throws RuleParameterIllegalValueException
  {
    if (!(newPredicate instanceof UnaryPredicate)) {
      throw new RuleParameterIllegalValueException
	(RuleParameter.PREDICATE_PARAMETER, 
	 "Object must be a UnaryPredicate");
    }
    thePredicate = (UnaryPredicate) newPredicate;
  }

  /**
   * 
   * @param test_value : Any object
   * @return true if test_value is acceptable to the predicate
   */
  public boolean inRange(Object test_value) {
    if (thePredicate == null) return false;
    return thePredicate.execute(test_value);
  }

  public String toString() 
  {
    return "#<PREDICATE_PARAMETER : " + thePredicate.toString();
  }

  public Object clone() {
    return new PredicateRuleParameter(my_name, thePredicate);
  }

}
