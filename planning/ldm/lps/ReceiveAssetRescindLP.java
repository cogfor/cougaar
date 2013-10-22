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

import java.util.ArrayList;
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
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;



/**
 * Catch assets so that we can relink the relationships properly.
 **/

public class ReceiveAssetRescindLP
implements LogicProvider, MessageLogicProvider
{
  private static final Logger logger = Logging.getLogger(ReceiveAssetRescindLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;

  public ReceiveAssetRescindLP(
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
   *  perform updates -- per Rescind ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    // drop changes
    if (dir instanceof AssetRescind) {
      receiveAssetRescind((AssetRescind)dir);
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private void receiveAssetRescind(AssetRescind ar) {
    Asset localAsset = logplan.findAsset(ar.getAsset());
    if (localAsset == null) {
      logger.error("Rescinded asset - " + ar.getAsset() + 
		   " - not found in logplan.");
      return;
    }

    Asset localAssignee = logplan.findAsset(ar.getRescindee());
    if (localAssignee == null) {
      logger.error("Assignee asset - " + 
		   ar.getRescindee() + " - not found in logplan.");
      return;
    }


    if (related(ar.getAsset()) &&
        related(ar.getRescindee())) {
      updateRelationshipSchedules(ar, localAsset, localAssignee);
    }

    updateAvailSchedule(ar, localAsset, localAssignee);
  
    rootplan.change(localAsset, null);
    rootplan.change(localAssignee, null);
  }

  private void updateRelationshipSchedules(AssetRescind ar,
                                           Asset asset,
                                           Asset assignee) {

    RelationshipSchedule assetRelationshipSchedule = 
      ((HasRelationships) asset).getRelationshipSchedule();

    RelationshipSchedule assigneeRelationshipSchedule = 
      ((HasRelationships) assignee).getRelationshipSchedule();


    // Remove matching relationships
    Collection rescinds = convertToRelationships(ar, asset, assignee);

    assetRelationshipSchedule.removeAll(rescinds);

    assigneeRelationshipSchedule.removeAll(rescinds);
  }

  // Update availability info for the asset (aka transferring asset)
  // AvailableSchedule reflects availablity within the current agent
  private void updateAvailSchedule(AssetRescind ar,
                                   Asset asset,
                                   final Asset assignee) {

    NewSchedule assetAvailSchedule = 
      (NewSchedule)asset.getRoleSchedule().getAvailableSchedule();

    if (assetAvailSchedule == null) {
      if (logger.isDebugEnabled()) {
	logger.debug("Asset for rescinded asset transfer " +
		     asset.getItemIdentificationPG().getItemIdentification() + 
		     " did not have an availability schedule.");

      }

      assetAvailSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)asset.getRoleSchedule()).setAvailableSchedule(assetAvailSchedule);
    } 

    if (!related(asset)) {
    
      // Remove Matching Availabilities
      synchronized (assetAvailSchedule) {
        assetAvailSchedule.removeAll(ar.getSchedule());
      }

      // We're done
      return;
    }
       

    //For Assets with relationships, need to recompute the avail schedule
    //based on the relationship schedule

    // Remove all current entries denoting asset avail to assignee
    synchronized (assetAvailSchedule) {
      Collection remove = assetAvailSchedule.filter(new UnaryPredicate() {
	public boolean execute(Object o) {
	  return ((o instanceof AssignedAvailabilityElement) &&
		  (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
	}  
      });
      assetAvailSchedule.removeAll(remove);
      
      // Get all relationships between asset and assignee
      RelationshipSchedule relationshipSchedule = 
        ((HasRelationships) asset).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships((HasRelationships) assignee,
                                                      new MutableTimeSpan());
      
      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        
        // Add a new avail element
        assetAvailSchedule.add(ldmf.newAssignedAvailabilityElement(assignee,
                                                                   schedule.getStartTime(),
                                                                   schedule.getEndTime()));
      }
    } // end sync block
  }

  protected Collection convertToRelationships(AssetRescind ar,
                                              Asset asset,
                                              Asset assignee) {
    ArrayList relationships = new ArrayList(ar.getSchedule().size());

    // Safe because ar.getSchedule is an AssignedRelationshipScheduleImpl.
    // AssignedRelationshipImpl supports iterator. (Assumption is that
    // AssignedRelaionshipImpl is only used/processed by LPs.)
    for (Iterator iterator = ar.getSchedule().iterator(); 
         iterator.hasNext();) {
      AssignedRelationshipElement rescindElement = 
        (AssignedRelationshipElement)iterator.next();
      
      Relationship relationship = ldmf.newRelationship(rescindElement,
                                                       asset,
                                                       assignee);
      
      relationships.add(relationship);
    }

    return relationships;
  }
}
 






