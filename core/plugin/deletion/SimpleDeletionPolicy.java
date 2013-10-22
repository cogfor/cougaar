/* 
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.core.plugin.deletion;

import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * A simple {@link DeletionPolicy} providing a fixed set of values
 * established when constructed. Setters are protected an may be overridden in
 * subclasses.
 */
public class SimpleDeletionPolicy  
  implements DeletionPolicy, org.cougaar.core.util.UniqueObject {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private String name;
  private UID myUID = null;
  private UnaryPredicate predicate;
  private long deletionDelay;
  private int priority;

  private static class DefaultDeletionPolicyPredicate implements UnaryPredicate {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return true;
    }
  };

  public SimpleDeletionPolicy(
    String name,
    UnaryPredicate predicate,
    long deletionDelay,
    int priority) {
    setName(name);
    setPredicate(predicate);
    setDeletionDelay(deletionDelay);
    setPriority(priority);
  }
  
  /**
   * Package access constructor create a default policy (having NO_PRIORITY
   * priority).
   * @param deletionDelay
   */
  SimpleDeletionPolicy(long deletionDelay) {
    this(
      "Default deletion policy",
      new DefaultDeletionPolicyPredicate(),
      deletionDelay,
      NO_PRIORITY);
  }
  
  public static boolean isDefaultDeletionPolicy(DeletionPolicy policy) {
    if (policy instanceof SimpleDeletionPolicy) {
      return ((SimpleDeletionPolicy) policy).isDefaultDeletionPolicy();
    }
    return false;
  }

  private boolean isDefaultDeletionPolicy() {
    return getPredicate() instanceof DefaultDeletionPolicyPredicate;
  }

  protected void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  protected void setPredicate(UnaryPredicate predicate) {
    this.predicate = predicate;
  }

  public UnaryPredicate getPredicate() {
    return predicate;
  }

  protected void setPriority(int priority) {
    if (priority < MIN_PRIORITY
      && !this.isDefaultDeletionPolicy()) {
      throw new IllegalArgumentException("Invalid priority");
    }
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }

  protected void setDeletionDelay(long deletionDelay) {
    this.deletionDelay = deletionDelay;
  }

  public long getDeletionDelay() {
    return deletionDelay;
  }

  // UniqueObject interface
  /**
   * @return the UID of a UniqueObject.  If the object was created
   * correctly (e.g. via a Factory), will be non-null.
   */
  public UID getUID() {
    return myUID;
  }

  /**
   * Set the UID of a UniqueObject.  This should only be done by
   * an LDM factory.  Will throw a RuntimeException if
   * the UID was already set.
   */
  public void setUID(UID uid) {
    if (myUID != null) {
      RuntimeException rt = new RuntimeException("Attempt to call setUID() more than once.");
      throw rt;
    }

    myUID = uid;
  }
}

