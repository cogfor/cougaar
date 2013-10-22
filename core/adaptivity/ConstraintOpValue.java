/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;


/** 
 * A phrase fragment used to express a boolean comparison and a set of
 * valid values to compare against.
 **/
public class ConstraintOpValue implements java.io.Serializable {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
ConstraintOperator operator;
  OMCRangeList allowedValues;
  
  /**
   * Constructor 
   **/
  public ConstraintOpValue() {
  }

  /**
   * Set the operator.
   * @param op the new operator
   **/
  public void setOperator(ConstraintOperator op) {
    operator = op;
  }

  /**
   * Set the list of value ranges against which the operator can
   * compare a condition value. The actual interpretation of the
   * allowed values depends on the operator.
   * @param l the list of value ranges.
   **/
  public void setAllowedValues(OMCRangeList l) {
    allowedValues = l;
  }
  
  /** 
   * Get the effective value of the allowed values. This is always the
   * the min of the first range.
   * @return the value as a Comparable (String, Integer, Double, etc.)
   **/
  public Comparable getValue() {
    return allowedValues.getEffectiveValue();
  }

  /**
   * Get the ranges of allowed values.
   * @return all allowed ranges as imposed by this constraint
   **/
  public OMCRangeList getAllowedValues() {
    return allowedValues;
  }
  
  /** 
   * The relationship between the condition or operating mode and the
   * value.
   * @return ConstraintOperator */
  public ConstraintOperator getOperator() {
    return operator;
  }

  @Override
public String toString() {
    return operator + " " + allowedValues;
  }
}
