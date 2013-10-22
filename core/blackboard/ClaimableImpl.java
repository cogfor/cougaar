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

package org.cougaar.core.blackboard;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;

import org.cougaar.bootstrap.SystemProperties;

/** 
 * Implementation of the {@link Claimable} API.
 * <p> 
 * Extends PublishableAdapter so that subclasses may
 * implicitly collect changes.
 *
 * @property org.cougaar.core.blackboard.Claimable.debug
 * If set to true, every Claimable instance will keep additional
 * information for debugging claim conflicts, that is, multiple
 * plugins attempting to operate directly on the same blackboard
 * objects.  This information adds significant additional memory
 * load.
 */
public class ClaimableImpl 
  extends PublishableAdapter    // sigh.
  implements Claimable 
{
  private static boolean isDebugging = false;
  static {
    isDebugging = SystemProperties.getBoolean(
        "org.cougaar.core.blackboard.Claimable.debug", isDebugging);
  }

  private transient Object claimer = null;
  
  private transient Throwable claimerStack = null;

  private static final Object postRehydrationClaimer = new Object();

  public final boolean isClaimed() { return (claimer!=null); }

  public final Object getClaim() { return claimer; }

  public final String getClaimClassName() { 
    // for beanInfo use
    return ((claimer != null) ? claimer.getClass().getName() : "null"); 
  }

  public final void setClaim(Object pch) {
    doClaim(pch, pch, "setClaim", " to ");
  }

  public final synchronized boolean tryClaim(Object pch) {
    if (claimer == null) {
      // got the claim
      _claim(pch);
      return true;
    } else if (pch == claimer) {
      // already owned the claim - probably bogus, but...
      return true;              
    } else {
      return false;
    }
  }

  public final void resetClaim(Object pch) {
    doClaim(pch, null, "resetClaim", " from ");
  }

  private synchronized void doClaim(Object pch, Object newClaimer, String verb, String prep) {
    if (pch instanceof PrivilegedClaimant) {
      // PrivilegedClaimant can do what he wants
    } else if (claimer == postRehydrationClaimer) {
      // Actual claimer lost thru rehydration, allow anything
    } else if (pch == null) {
      // Must have a valid pch
      complain("Tried to " + verb + " of " + this + prep + "null.");
    } else if (pch == claimer) {
      // Current claimer can do what he wants
    } else if (claimer != null) {
      // Already claimed by somebody else
      complain("Tried to " + verb + " of " + this + prep + pch +
               "\n\tbut it was " + claimer + ".");
    }
    _claim(newClaimer);
  }

  // must be calling within a synchronized block
  private void _claim(Object newClaimer) {
    // Always carry out the request even if complaints were issued
    claimer = newClaimer;
    // Remember how we got here for debugging.
    if (isDebugging) {
      claimerStack = new Throwable();
    }
  }
    

  /**
   * true when we've complained once and told the user how to enable loud mode. 
   * Only used when in non-loud mode.
   */
  private static boolean hasComplained = false;

  private void complain(String complaint) {
    synchronized (System.err) { 
      System.err.println(complaint);
      if (isDebugging) {
        System.err.println("Current stack:"); 
        Thread.dumpStack();
        System.err.println("Claimer stack:");
        if (claimerStack != null)
          claimerStack.printStackTrace();
        else
          System.err.println("(Never been claimed)");
      } else {
        if (! hasComplained) {
          System.err.println(
              "(Set system property"+
              " org.cougaar.core.blackboard.Claimable.debug=true"+
              " for details)");
          hasComplained=true;
        }
      }
    }
  }

  private void readObject(ObjectInputStream is)
    throws NotActiveException, ClassNotFoundException, IOException {
    is.defaultReadObject();
    claimer = postRehydrationClaimer;
  }
}
