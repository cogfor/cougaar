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

package org.cougaar.core.relay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.cougaar.core.blackboard.ChangeReport;

/**
 * A {@link ChangeReport} to change the targets of a relay.
 * <p>
 * This ChangeReport must be used used when publishing changes to the
 * <b>target</b> set of a Relay (as opposed to the relay's content).
 * Failure to do so will cause dangling relay targets in agents that
 * are no longer in the target set.
 * <p>
 * Usage is:<pre>
 *   Collection changes = Collections.singleton(new RelayChangeReport(relay));
 *   relay.setTargets(newTargets);
 *   blackboard.publishChange(relay, changes);
 * <pre>
 * The details of how you change the targets of your relay
 * implementation are, of course, your responsibility, but whatever
 * method you use, it is critical that the RelayChangeReport be
 * created before you change the targets since the change report
 * carries a copy of the old set to the RelayLP which uses it to
 * insure that the old targets are correctly reconciled with the new
 * targets.
 */
public class RelayChangeReport implements ChangeReport {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private Collection oldTargets;

  /**
   * Constructor from a Relay.Source. The about-to-become-old targets
   * are recorded.
   */
  public RelayChangeReport(Relay.Source rs) {
    Set targets = rs.getTargets();
    oldTargets = new ArrayList(targets.size());
    oldTargets.addAll(targets);
  }

  /**
   * Get the recorded list of old target addresses. For use by the
   * RelayLP.
   */
  Collection getOldTargets() {
    return oldTargets;
  }
}
