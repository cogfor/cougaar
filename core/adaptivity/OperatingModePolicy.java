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

import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;

/** 
 * OperatingModePolicy specifies constraints on values of Operating
 * modes. It consists of an if clause expressing the conditions under
 * which the policy applies and an array of restrictions on the values
 * of some {@link OperatingMode}s.
 **/

 /* IF clause, then clause
  * If THREATCON > 3 then
  *     (encription > 128) && (encryption < 512).
  */

public class OperatingModePolicy implements Policy, Publishable  {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private UID uid = null;
  private String policyName = "";
  private String authority;
  private PolicyKernel policy;

  public OperatingModePolicy (PolicyKernel pk) {
    policy = pk;
  }
  
  /**
   * Constructor 
   * @param ifClause the 'if' ConstrainingClause 
   * @param omConstraints an array of constraints to apply to {@link OperatingMode}s
   */
  public OperatingModePolicy (ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints) {
    this(new PolicyKernel(ifClause, omConstraints));
  }
  
  /**
   * Constructor 
   * @param ifClause the 'if' ConstrainingClause 
   * @param omConstraints an array of constraints to apply to {@link OperatingMode}s
   */
  public OperatingModePolicy (String policyName,
			      ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints) {
    this(ifClause, omConstraints);
    this.policyName = policyName;
  }

  /**
   * Constructor 
   * @param ifClause the 'if' ConstrainingClause 
   * @param omConstraints an array of constraints to apply to {@link OperatingMode}s
   */
  public OperatingModePolicy (String policyName,
			      ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints,
			      String authority) {
    this(policyName, ifClause, omConstraints);
    this.authority = authority;
  }

  /**
   * Returns the originator or creator (authority) of the policy. This
   * is part of the implementation of the Policy interface.
   * @return the name of the authority
   **/
  public String getAuthority() { return authority; }
  
  public void setAuthority(String authority) {
    if (this.authority != null) throw new RuntimeException("Attempt to change Policy Authority");
    this.authority = authority;
  }

  public String getName() {
    return policyName;
  }

  public void setName(String name) {
    if (policyName != null) throw new RuntimeException("Attempt to change Policy Name");
    policyName = name;
  }

  // UniqueObject interface
  public UID getUID() {
    return uid;
  }

  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (this.uid != null) throw new RuntimeException("Attempt to change UID: " + uid);
    this.uid = uid;
  }

  public PolicyKernel getPolicyKernel() {
    return policy;
  }

  protected void setPolicyKernel(PolicyKernel pk) {
    policy = pk;
  }


  /* convenience methods */
  public ConstrainingClause getIfClause() {
    return policy.getIfClause();
  }

  public ConstraintPhrase[] getOperatingModeConstraints() {
    return policy.getOperatingModeConstraints();
  }
  
  @Override
public String toString() {
    StringBuffer sb = new StringBuffer(getName());
    sb.append(" ");
    sb.append(policy.getIfClause().toString());
    ConstraintPhrase[] cp = policy.getOperatingModeConstraints();
    for (int i=0; i < cp.length; i++) {
      sb.append(": ");
      sb.append(cp[i].toString());
    }
    return sb.toString();
  }

  public boolean isPersistable() {
    return true;
  }
}
