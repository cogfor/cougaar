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


package org.cougaar.planning.ldm.lps;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.AssetRescind;
import org.cougaar.planning.ldm.plan.AssetVerification;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;

import org.cougaar.util.TimeSpans;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
  * take an incoming AssetVerification Directive and
  * confirm Asset on the LogPlan w/side-effect of also disseminating to
  * other subscribers.
  **/
public class ReceiveAssetVerificationLP
implements LogicProvider, MessageLogicProvider
{
  private static final Logger logger = Logging.getLogger(ReceiveAssetVerificationLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;

  public ReceiveAssetVerificationLP(
      RootPlan rootplan,
      LogPlan logplan,
      PlanningFactory ldmf) {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
  }

  public void init() {
  }

  /**
   * Verifies that Assets are in the LogPlan. Sends rescinds if not.
   **/
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof AssetVerification) {
      AssetVerification av  = (AssetVerification) dir;

    // One of ourselves
      Asset localAsset = logplan.findAsset(av.getAsset());
      Schedule rescindSchedule = null;

      if (localAsset == null) {
        //Rescind everything
        rescindSchedule = av.getSchedule();
      } else {
        if (!(av.getAsset() instanceof HasRelationships) ||
            !(av.getAssignee() instanceof HasRelationships)) {
          rescindSchedule = getUnmatchedAvailability(av, localAsset);
        } else {
          rescindSchedule = getUnmatchedRelationships(av, (HasRelationships) localAsset);
        }
      } 
        
      if ((rescindSchedule != null) &&
          rescindSchedule.size() > 0) {
        AssetRescind arm = 
          ldmf.newAssetRescind(ldmf.cloneInstance(av.getAsset()), 
                               ldmf.cloneInstance(av.getAssignee()),
                               rescindSchedule);
        rootplan.sendDirective((Directive) arm); 
      }
    } 
  }
  
  /** getUnmatchedAvailability - returns a Schedule with all ScheduleElements which
   * are not matched by the local availability schedule.
   *
   * @param av AssetVerification which schedule is verified
   * @param localAsset Asset local copy of the asset being verified
   * @return Schedule of AssignedRelationshipElements which do not match local info
   */
  private Schedule getUnmatchedAvailability(AssetVerification av, 
                                            Asset localAsset) {
    NewSchedule rescindSchedule = null;
    
    Schedule availSchedule = localAsset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      // rescind everything
      rescindSchedule = ldmf.newAssignedRelationshipSchedule();
      rescindSchedule.addAll(av.getSchedule());
      return rescindSchedule;
    }    

    //iterate over verification schedule and make sure it maps to local info
    for (Iterator iterator = av.getSchedule().iterator();
         iterator.hasNext();) {
      ScheduleElement verify = (ScheduleElement)iterator.next();
    
      Iterator localIterator = availSchedule.iterator();
      
      boolean found = false;
      while (localIterator.hasNext()) {
        ScheduleElement localAvailability = 
          (ScheduleElement)localIterator.next();

        if (!(localAvailability instanceof AssignedAvailabilityElement)) {
          continue;
        }
        
        Asset assignee = ((AssignedAvailabilityElement) localAvailability).getAssignee();
        
        if ((assignee.equals(av.getAssignee())) &&
            (verify.getStartTime() == localAvailability.getStartTime()) &&
            (verify.getEndTime() == localAvailability.getEndTime())) {
          found = true;
          break;
        }
      }
      
      if (!found) {
        if (rescindSchedule == null) {
          rescindSchedule = 
            ldmf.newAssignedRelationshipSchedule();
        }
        ((NewSchedule)rescindSchedule).add(verify);
      }
    }

    return rescindSchedule;
  }

  /** getUnmatchedRelationships - returns a Schedule with all AssignedRelationshipElements which
   * are not matched by the local relationship schedule.
   *
   * @param av AssetVerification which schedule is verified
   * @param localAsset HasRelationships local copy of the asset being verified
   * @return Schedule of AssignedRelationshipElements which do not match local info
   */
  private Schedule getUnmatchedRelationships(AssetVerification av, 
                                             HasRelationships localAsset) {
    NewSchedule rescindSchedule = null;
    //HasRelationships asset = (HasRelationships)av.getAsset();
    HasRelationships assignee = (HasRelationships)av.getAssignee();
    
    Collection localRelationships = 
      localAsset.getRelationshipSchedule()
      .getMatchingRelationships(assignee, 
				TimeSpans.getSpan(av.getSchedule().getStartTime(), 
						  av.getSchedule().getEndTime()));
    String assetID = av.getAsset().getItemIdentificationPG().getItemIdentification();
    String assigneeID = av.getAssignee().getItemIdentificationPG().getItemIdentification();
    
    //iterate over verification schedule and make sure it maps to local info
    for (Iterator iterator = av.getSchedule().iterator();
         iterator.hasNext();) {
      AssignedRelationshipElement verify = (AssignedRelationshipElement)iterator.next();
      String itemIDA = verify.getItemIDA();
      String itemIDB = verify.getItemIDB();
      
      if (!(itemIDA.equals(assetID) &&
            itemIDB.equals(assigneeID)) &&
          !(itemIDA.equals(assigneeID) &&
            itemIDB.equals(assetID))) {
        logger.error("Schedule element - " + verify + 
		     " does not match asset - " +
		     av.getAsset() + " - and assignee - " + 
		     av.getAssignee()); 
        continue;
      }
      
      Iterator localIterator = localRelationships.iterator();
      
      boolean found = false;
      while (localIterator.hasNext()) {
        Relationship localRelationship = 
          (Relationship)localIterator.next();
        HasRelationships localA = localRelationship.getA();
        HasRelationships localB = localRelationship.getB();
        
        if ((localA instanceof Asset) &&
            (localB instanceof Asset)) {
          String localItemIDA = 
            ((Asset)localA).getItemIdentificationPG().getItemIdentification();
          String localItemIDB = 
            ((Asset)localB).getItemIdentificationPG().getItemIdentification();
          
          if (((verify.getRoleA().equals(localRelationship.getRoleA()) &&
                itemIDA.equals(localItemIDA) &&
                verify.getRoleB().equals(localRelationship.getRoleB()) &&
                itemIDB.equals(localItemIDB)) ||
               (verify.getRoleB().equals(localRelationship.getRoleA()) &&
                itemIDB.equals(localItemIDA) &&
                verify.getRoleA().equals(localRelationship.getRoleB()) &&
                itemIDA.equals(localItemIDB))) &&
              verify.getStartTime() == localRelationship.getStartTime() &&
              verify.getEndTime() == localRelationship.getEndTime()) {
            found = true;
            break;
          }
        }
      }
      
      if (!found) {
        if (rescindSchedule == null) {
          rescindSchedule = 
            ldmf.newAssignedRelationshipSchedule();
        }
        ((NewSchedule)rescindSchedule).add(verify);
      }
    }

    return rescindSchedule;
  }
    
}




