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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.service.LoggingService;

/**
 * A container for Plays providing operations to constrain plays with
 * {@link OperatingModePolicy OperatingModePolicy}s.
 **/
public class Playbook
{
  private Play[] originalPlays = new Play[0];
  private Play[] constrainedPlays = new Play[0];
  private List constraints = new ArrayList();
  private LoggingService logger;

  public Playbook(LoggingService logger) {
    this.logger = logger;
  }

  public synchronized void addConstraint(OperatingModePolicy omp) {
    constraints.add(omp);
    constrainPlays(omp);
  }

  public synchronized void removeConstraint(OperatingModePolicy omp) {
    constraints.remove(omp);
    constrainPlays();
  }

  public synchronized void setPlays(Play[] plays) {
    originalPlays = plays;
    constrainPlays();
  }

  public synchronized Play[] getCurrentPlays() {
    return constrainedPlays;
  }

  private void constrainPlays() {
    constrainedPlays = originalPlays;
    for (Iterator i = constraints.iterator(); i.hasNext(); ) {
      if (logger.isDebugEnabled()) {
        logger.debug(constrainedPlays.length + " plays");
      }
      constrainPlays((OperatingModePolicy) i.next());
    }
    if (logger.isDebugEnabled()) {
      logger.debug(constrainedPlays.length + " plays");
    }
  }

  /**
   * Rewrite the playbook so that all the plays conform to a new
   * OperatingModePolicy. The procedure is as follows: Scan the plays
   * to find those plays that set OperatingModes overlapping with the
   * OperatingModes of the policy. Then check if the conditions under
   * which the policy applies overlap with the conditions under which
   * the play applies. If there is overlap, rewrite the play as
   * follows: Change the if clause of the original play so it applies
   * only when the policy does not. Add a new play that applies when
   * both the original play would have applied and the policy applies.
   * The OperatingModes of the new play are those of the original play
   * but with the ranges of values reduced to those values that the
   * play and the policy have in common. If the set of allowed values
   * for any OperatingMode is empty, then no play is added. Finally,
   * the OperatingModePolicy itself is inserted as a Play to cover the
   * cases where the policy applies, but no play does. It is not
   * necessary to add any play predicates to this play, because by
   * construction all plays are compatible with the policy.
   **/
  private void constrainPlays(OperatingModePolicy omp) {
    List newConstrainedPlays = new ArrayList(constrainedPlays.length);
    ConstrainingClause ompIfClause = omp.getIfClause();
    ConstraintPhrase[] ompConstraints = omp.getOperatingModeConstraints();
    Map ompModes = new HashMap();
    // First, append the constraint itself as a Play so it dominates all following plays
    newConstrainedPlays.add(new Play(omp.getIfClause(), omp.getOperatingModeConstraints()));
    for (int i = 0; i < ompConstraints.length; i++) {
      ConstraintPhrase cp = ompConstraints[i];
      String proxyName = cp.getProxyName();
      ompModes.put(proxyName, cp);
    }
    for (int i = 0; i < constrainedPlays.length; i++) {
      Play play = constrainedPlays[i];
      ConstraintPhrase[] playConstraints = play.getOperatingModeConstraints();
      ConstraintPhrase[] newConstraints = null;
      for (int j = 0; j < playConstraints.length; j++) {
        ConstraintPhrase cp = playConstraints[j];
        String proxyName = cp.getProxyName();
        if (!ompModes.containsKey(proxyName)) {
          if (newConstraints != null) {
            newConstraints[j] = playConstraints[j]; // No conflict here
          }
        } else {
          if (newConstraints == null) {
            newConstraints = new ConstraintPhrase[playConstraints.length];
            System.arraycopy(playConstraints, 0, newConstraints, 0, j);
          }
        }
      }
      if (newConstraints == null) {
        newConstrainedPlays.add(play); // No overlap. Keep the play as is
      } else {
        // First write a Play that applies when the original play
        // applies, but the constraint policy does not apply
        ConstrainingClause playIfClause = play.getIfClause();
        ConstrainingClause newIfClause = new ConstrainingClause();
        newIfClause.push(playIfClause);
        newIfClause.push(ompIfClause);
        newIfClause.push(BooleanOperator.NOT);
        newIfClause.push(BooleanOperator.AND);
        Play newPlay = new Play(newIfClause, playConstraints);
        newConstrainedPlays.add(newPlay);

        // Now write a Play that applies when both the original play
        // and the constraint policy apply
        newIfClause = new ConstrainingClause();
        newIfClause.push(playIfClause);
        newIfClause.push(ompIfClause);
        newIfClause.push(BooleanOperator.AND);
        for (int j = 0; j < playConstraints.length; j++) {
          if (newConstraints[j] != null) continue; // Ok as is
          ConstraintPhrase cp = playConstraints[j];
          String proxyName = cp.getProxyName();
          ConstraintPhrase ompConstraint = (ConstraintPhrase) ompModes.get(proxyName);
          OMCRangeList intersection =
            cp.getAllowedValues().intersect(ompConstraint.getAllowedValues());
          if (intersection.isEmpty()) {
            // Completely incompatible. This could be bad. It means
            // that a play designed to work well in some conditions
            // has been completely disallowed by the policy over those
            // conditions. We record the name of the operating mode
            // for which no suitable was available and use the value
            // specified by the policy.
            if (logger.isWarnEnabled()) {
              logger.warn("Policy "
                          + omp
                          + " disallows all settings for "
                          + proxyName
                          + " of play "
                          + play);
            }
            newConstraints[j] = ompConstraint;
          } else {
            // Some values are still ok so use them
            newConstraints[j] =
              new ConstraintPhrase(proxyName, cp.getOperator(), intersection);
          }
        }
        newPlay = new Play(newIfClause, newConstraints);
        newConstrainedPlays.add(newPlay);
      }
    }
    constrainedPlays = (Play[]) newConstrainedPlays.toArray(constrainedPlays);
  }
}
