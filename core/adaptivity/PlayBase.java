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
 * Base class for Plays and OperatingModePolicies. Most of the
 * functionality of the subclasses resides here. A Play or
 * OperatingModePolicy has a ConstrainingClause that can be evaluated
 * to determine if it applies to the current Conditions and a list
 * (array) of operating mode constraints that specify the values that
 * the modes should be given.
 **/
public class PlayBase implements java.io.Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private ConstrainingClause ifClause;
  private ConstraintPhrase[] operatingModeConstraints;

  /**
   * Constructor
   * @param ifClause the 'if' clause
   * @param omConstraints the constraints on operating modes
   **/
  public PlayBase(ConstrainingClause ifClause, ConstraintPhrase[] omConstraints) {
    this.ifClause = ifClause;
    this.operatingModeConstraints = omConstraints;
  }

  /** 
   * Gets the if clause
   * @return the 'if' ConstrainingClause
   */
  public ConstrainingClause getIfClause() {
    return ifClause;
  }

  /**
   * Gets the array of ConstraintPhrases to be applied to the
   * operating modes.
   * @return the array of ConstraintPhrases.
   **/
  public ConstraintPhrase[] getOperatingModeConstraints() {
    return operatingModeConstraints;
  }

  /**
   * Gets the Play or OperatingModePolicy as a String. The form of the
   * String is approximately the same as the input to the Parser.
   * @return The Play or OperatingModePolicy as a String.
   **/
  @Override
public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(ifClause);
    for (int i = 0; i < operatingModeConstraints.length; i++) {
      buf.append(":")
        .append(operatingModeConstraints[i]);
    }
    return buf.toString();
  }

  @Override
public int hashCode() {
    int hc = ifClause.hashCode();
    for (int i = 0; i < this.operatingModeConstraints.length; i++) {
      hc = 31 * hc + operatingModeConstraints[i].hashCode();
    }
    return hc;
  }

  @Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (this.getClass() != o.getClass()) return false;
    PlayBase that = (PlayBase) o;
    if (!this.ifClause.equals(that.ifClause)) return false;
    if (this.operatingModeConstraints.length != that.operatingModeConstraints.length) return false;
    for (int i = 0; i < this.operatingModeConstraints.length; i++) {
      if (!this.operatingModeConstraints[i].equals(that.operatingModeConstraints[i])) return false;
    }
    return true;
  }
}
