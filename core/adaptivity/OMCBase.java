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
 * The base class for OperatingMode and Condition implementations.
 * These two concepts differ only in that an OperatingMode can be set
 * by the AdaptivityEngine whereas a Condition is under the
 * control of the component establishing the Condition.
 **/
public class OMCBase implements java.io.Serializable {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private String name;
  private Comparable value;
  private OMCRangeList allowedValues;

  /**
   * Constructor using default initial value.
   * @param name the name of this
   * @param allowedValues the allowed value ranges. It is illegal to
   * set a value that is not in the allowed ranges.
   **/
  protected OMCBase(String name, OMCRangeList allowedValues) {
    this.name = name;
    this.allowedValues = allowedValues;
    this.value = allowedValues.getEffectiveValue();
  }

  /**
   * Constructor with a specific initialvalue.
   * @param name the name of this
   * @param allowedValues the allowed value ranges. It is illegal to
   * set a value that is not in the allowed ranges.
   * @param initialValue the initial value. Must not be null. All
   * subsequent values that are set for this must have the same class
   * as the initial value.
   **/
  protected OMCBase(String name, OMCRangeList allowedValues, Comparable initialValue) {
    this(name, allowedValues);
    setValue(initialValue);
  }

  /**
   * Gets the name
   * @return the name
   **/
  public String getName() {
    return name;
  }

  /**
   * Gets the list if allowed value ranges. The allowed value ranges
   * are set in the constructor and immutable thereafter.
   * @return the list of allowed value ranges.
   **/
  public OMCRangeList getAllowedValues() {
    return allowedValues;
  }

  /**
   * Gets the current value
   * @return the current value
   **/
  public Comparable getValue() {
    return value;
  }

  /**
   * Set a new value for this. The new value must be in the list of
   * allowed value ranges. This method is protected because the
   * ability to set a new value must be under the control of a
   * responsible component. Subclasses will override this with public
   * or package protected versions as needed.
   * @param newValue the new value. Must be an allowed value.
   **/
  protected void setValue(Comparable newValue) {
    if (!allowedValues.isAllowed(newValue)) {
      throw new IllegalArgumentException("setValue: " + getName() + " = " + newValue + " not allowed");
    }
    if (value.compareTo(newValue) == 0) return; // Already set to this value
    value = newValue;
  }

  /**
   * Furnish a useful string representation.
   * @return the name and value separated by an equals sign.
   **/
  @Override
public String toString() {
    return getName() + " = " + getValue();
  }
}
