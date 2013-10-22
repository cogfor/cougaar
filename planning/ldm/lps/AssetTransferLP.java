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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.blackboard.AnonymousChangeReport;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RestartLogicProvider;
import org.cougaar.core.domain.RestartLogicProviderHelper;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.asset.LocalPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;
import org.cougaar.planning.ldm.plan.AssetAssignment;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewAssetAssignment;
import org.cougaar.planning.ldm.plan.NewAssetVerification;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** AssetTransferLP is a "LogPlan Logic Provider":
  *
  * it provides the logic to capture
  * PlanElements that are AssetTransfers and send AssetAssignment tasks
  * to the proper remote agent.
  **/

public class AssetTransferLP
implements LogicProvider, EnvelopeLogicProvider, RestartLogicProvider
{
  private static final Logger logger = Logging.getLogger(AssetTransferLP.class);
  private static final TimeSpan ETERNITY = new MutableTimeSpan();
  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final MessageAddress self;
  private final PlanningFactory ldmf;

  public AssetTransferLP(
      RootPlan rootplan,
      LogPlan logplan,
      PlanningFactory ldmf,
      MessageAddress self) {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
    this.self = self;
  }

  public void init() {
  }

  /**
   * @param o Envelopetuple,
   *          where tuple.object
   *             == PlanElement with an Allocation to an agent ADDED to LogPlan
   *
   * If the test returned true i.e. it was an AssetTransfer...
   * create an AssetAssignment task and send it to a remote Agent 
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj;
    if ((o.isAdd() || o.isChange()) &&
        (obj = o.getObject()) != null &&
         obj instanceof AssetTransfer) {

      AssetTransfer at = (AssetTransfer) obj;
      AssetAssignment assetassign;
    
      // create an AssetAssignment task
      boolean sendRelationships = o.isAdd();
      if (!sendRelationships && 
          ((changes != AnonymousChangeReport.LIST) &&
           (changes != null))) {
        for (Iterator i = changes.iterator(); i.hasNext(); ) {
          ChangeReport changeReport = (ChangeReport) i.next();
          if (changeReport instanceof RelationshipSchedule.RelationshipScheduleChangeReport) {
            sendRelationships = true;
            break;
          }
        }
      }
      assetassign =
        createAssetAssignment(at, 
                              (o.isChange()) ? 
                              AssetAssignment.UPDATE : AssetAssignment.NEW,
                              sendRelationships);
      if (assetassign != null) {
        // Give the AssetAssignment to the blackboard for transmission
        if (logger.isDebugEnabled()) logger.debug("Sending " + assetassign);
        rootplan.sendDirective(assetassign);
      } else {
        if (logger.isDebugEnabled()) logger.debug("Not sending AssetAssignment for " + at);
      }
    }
  }

  // RestartLogicProvider implementation

  /**
   * Agent restart handler. Resend all our assets to the restarted
   * agent marking them as "REPEAT". Also send AssetVerification
   * messages for all the assets we have received from the restarted
   * agent. The restarted agent will rescind them if they are no
   * longer valid.
   **/
  public void restart(final MessageAddress cid) {
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof AssetTransfer) {
          AssetTransfer at = (AssetTransfer) o;
          MessageAddress assignee = 
            at.getAssignee().getClusterPG().getMessageAddress();
          return 
            RestartLogicProviderHelper.matchesRestart(
                self, cid, assignee);
        }
        return false;
      }
    };
    Enumeration e = rootplan.searchBlackboard(pred);
    while (e.hasMoreElements()) {
      AssetTransfer at = (AssetTransfer) e.nextElement();
      rootplan.sendDirective(createAssetAssignment(at, AssetAssignment.REPEAT, true));
    }
    pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Asset) {
          Asset asset = (Asset) o;
          ClusterPG clusterPG = asset.getClusterPG();
          if (clusterPG != null) {
            MessageAddress assetCID = clusterPG.getMessageAddress();
            return
              RestartLogicProviderHelper.matchesRestart(
                  self, cid, assetCID);
          }
        }
        return false;
      }
    };
    for (e = rootplan.searchBlackboard(pred); e.hasMoreElements(); ) {
      Asset asset = (Asset) e.nextElement();
      
      if (related(asset)) {

        HashMap hash = new HashMap(3);

        RelationshipSchedule relationshipSchedule = 
          (RelationshipSchedule)((HasRelationships)asset).getRelationshipSchedule();

        Collection relationships = new ArrayList(relationshipSchedule);
        for (Iterator iterator = relationships.iterator();
             iterator.hasNext();) {
          Relationship relationship = (Relationship)iterator.next();
          
          Asset otherAsset = 
            (Asset)relationshipSchedule.getOther(relationship);
          
          NewSchedule verifySchedule = (NewSchedule)hash.get(otherAsset);
          if (verifySchedule == null) {
            verifySchedule = ldmf.newAssignedRelationshipSchedule();
            hash.put(otherAsset, verifySchedule);
          }
          
          verifySchedule.add(ldmf.newAssignedRelationshipElement(relationship));
        }

        for (Iterator iterator = hash.keySet().iterator();
             iterator.hasNext();) {
          Asset receivingAsset = (Asset)iterator.next();
          
          Schedule verifySchedule = (Schedule)hash.get(receivingAsset);
          
          NewAssetVerification nav = 
            ldmf.newAssetVerification(ldmf.cloneInstance(asset),
                                      ldmf.cloneInstance(receivingAsset),
                                      verifySchedule);
          nav.setSource(self);
          nav.setDestination(asset.getClusterPG().getMessageAddress());
          rootplan.sendDirective(nav);
        }
      } else {
        // BOZO - we have not tested transferring non-org assets
        logger.error("AssetTransferLP - unable to verify transfer of " +
                           asset + "\n.");
      }
        
    }
  }
  
  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private AssetAssignment createAssetAssignment(AssetTransfer at, byte kind,
                                                boolean sendRelationships)
  {
    NewAssetAssignment naa = ldmf.newAssetAssignment();

    /* copy the asset so we don't share roleschedule across
     * agent boundaries.
     */
    Asset transferredAsset = ldmf.cloneInstance(at.getAsset());
    Vector transferredPGs = transferredAsset.fetchAllProperties();

    // Remove all LocalPGs - these shouldn't go out of the agent.
    for (Iterator pgIterator = transferredPGs.iterator();
	 pgIterator.hasNext();) {
      Object next = pgIterator.next();
      
      //Don't propagate LocalPGs
      if (next instanceof LocalPG) {
	if (next instanceof PropertyGroup) {
	  transferredAsset.removeOtherPropertyGroup(next.getClass());
	} else if (next instanceof PropertyGroupSchedule) {
	  transferredAsset.removeOtherPropertyGroupSchedule(next.getClass());
	}
      }
    }

    naa.setAsset(transferredAsset);
    
    naa.setPlan(ldmf.getRealityPlan());
    
    naa.setAssignee(ldmf.cloneInstance(at.getAssignee()));

    naa.setSource(at.getAssignor());
    naa.setDestination(at.getAssignee().getClusterPG().getMessageAddress());

    naa.setKind(kind);

    Schedule s = null;          // Null if relationships not being sent

    Asset asset = naa.getAsset();
    Asset assignee = naa.getAssignee();

    // Only fuss with relationship schedules if both Asset & Assignee implement
    // HasRelationships
    if (related(asset) & related(assignee)) {
      if (sendRelationships) {
        s = makeAARelationshipSchedule(naa, at);

      }
    } else {
      s = ldmf.newSchedule(at.getSchedule().getAllScheduleElements());
    }

    naa.setSchedule(s);

    // Ensure that local info reflects the transfer
    if (!updateLocalAssets(at, naa)) {
      return null;
    }

    // Clear asset and assignee relationship, role, and available schedules to ensure 
    // that there are no references to other organizations.
    clearSchedule(asset);
    clearSchedule(assignee);

    return naa;
  }
         
  private Schedule makeAARelationshipSchedule(NewAssetAssignment naa, 
                                              final AssetTransfer at) { 
    
    // construct appropriate relationship schedule for the asset assignment
    Schedule aaAssetSchedule = 
      ldmf.newAssignedRelationshipSchedule();

    RelationshipSchedule relationshipSchedule = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule();
    Collection relationships = relationshipSchedule.filter(new UnaryPredicate() {
      public boolean execute(Object o) {
        Relationship relationship = (Relationship)o;        

        // Verify that all relationships are with the receiver
        if (!(relationship.getA().equals(at.getAssignee())) &&
            !(relationship.getB().equals(at.getAssignee()))) {
          logger.error("AssetTransferLP: Relationships on the " + 
                             " AssetTransfer must be limited to the " + 
                             " transferring and receiving asset.\n" + 
                             "Dropping relationship " + relationship + 
                             " on transfer of " + at.getAsset() + " to " + 
                             at.getAssignee());
          return false;
        } else {
          return true;
        }
      }
    });

    for (Iterator iterator = relationships.iterator();
         iterator.hasNext();) {
      Relationship relationship = (Relationship)iterator.next();
      
      Asset a = (relationship.getA().equals(naa.getAsset())) ?
        naa.getAsset() : naa.getAssignee();
      Asset b = (relationship.getB().equals(naa.getAsset())) ?
        naa.getAsset() : naa.getAssignee();
      AssignedRelationshipElement element = 
        ldmf.newAssignedRelationshipElement(a,
                                            relationship.getRoleA(),
                                            b,
                                            relationship.getStartTime(),
                                            relationship.getEndTime());
      aaAssetSchedule.add(element);
    }
    
    return aaAssetSchedule;
  }

  private boolean updateLocalAssets(AssetTransfer at, AssetAssignment aa) {
    Asset localTransferringAsset = logplan.findAsset(at.getAsset());
    if (localTransferringAsset == null) {
      logger.error("AssetTransferLP: unable to process AssetTransfer - " + 
                         at.getAsset() + " - transferring to " + 
                         at.getAssignee()+ " - is not local to this agent.");
      return false;
    } else if (localTransferringAsset == at.getAsset()) {
      logger.error("AssetTransferLP: Transferring Assets in AssetTransfer are == but should be " +
                   " copies. AssetTransfer is " + at.getUID() + 
                   " Asset is "+ localTransferringAsset);
      return false;
    }
    
    Asset receivingAsset = at.getAssignee();
    Asset localReceivingAsset = logplan.findAsset(receivingAsset);

    if (localReceivingAsset == null) {
      receivingAsset = ldmf.cloneInstance(receivingAsset);
      if (related(receivingAsset)){
        ((HasRelationships)receivingAsset).setRelationshipSchedule(ldmf.newRelationshipSchedule((HasRelationships)receivingAsset));
      }
    } else {
      receivingAsset = localReceivingAsset;

      if (localReceivingAsset == at.getAssignee()) {
        logger.error("AssetTransferLP: Assignee Assets in AssetTransfer are == but should be " +
                     " copies. AssetTransfer is " + at.getUID() + 
                     " Asset is "+localReceivingAsset);
	return false;
      }
    }

    boolean changeRelationshipRequired = 
	fixRelationshipSchedule(at, aa, localTransferringAsset, 
				receivingAsset);

    boolean changeAvailabilityRequired = 
      fixAvailSchedule(aa,
		       receivingAsset, 
		       localTransferringAsset);

    if (logger.isDebugEnabled()) {
      logger.debug("AssetTransferLP: changeRelationshipRequired = " + 
		   changeRelationshipRequired +
		   " changeAvailabilityRequired = " +
		   changeAvailabilityRequired + 
		   " for AssetTransfer - " + at +
		   " transfering asset - " + localTransferringAsset +
		   " receiving asset - " + receivingAsset + 
		   " local receiving asset - " + localReceivingAsset);
    }

    publishAsset(receivingAsset, 
		 (localReceivingAsset == null),
		 changeRelationshipRequired,
		 changeAvailabilityRequired);

    publishAsset(localTransferringAsset,
		 false, // local asset already exists on the blackboard
		 changeRelationshipRequired,
		 changeAvailabilityRequired);

    return true;
  }


  // Update availability info for the receiving asset
  // AvailableSchedule reflects availablity within the current agent
  private boolean fixAvailSchedule(AssetAssignment aa, 
				   final Asset receivingAsset, 
				   final Asset transferringAsset) {

    NewSchedule availSchedule = 
      (NewSchedule)transferringAsset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      availSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)transferringAsset.getRoleSchedule()).setAvailableSchedule(availSchedule);
    } 

    boolean change = false;

    synchronized (availSchedule) {
      // Find all existing entries which refer to the receiving asset
      Collection currentAvailability = 
	availSchedule.filter(new UnaryPredicate() {
	public boolean execute(Object o) {
	  return ((o instanceof AssignedAvailabilityElement) &&
		  (((AssignedAvailabilityElement)o).getAssignee().equals(receivingAsset)));
	}
      });
      
      if ((related(transferringAsset) && related(receivingAsset))) {
        //Construct aggregate avail info from the relationship schedule
        RelationshipSchedule relationshipSchedule = 
          ((HasRelationships)transferringAsset).getRelationshipSchedule();
        Collection matchingRelationships =  
          relationshipSchedule.getMatchingRelationships((HasRelationships)receivingAsset,
                                                        ETERNITY);
        
        // If any relationships, construct a single avail element with the 
        // min start and max end
        if (!matchingRelationships.isEmpty()) {
          Schedule matchingRelationshipsSchedule = 
	    ldmf.newSchedule(new Enumerator(matchingRelationships));
	  AssignedAvailabilityElement aggregateAvailability = 
	    ldmf.newAssignedAvailabilityElement(receivingAsset,
						matchingRelationshipsSchedule.getStartTime(),
						matchingRelationshipsSchedule.getEndTime());

	  // Compare to existing entries - only change if required.
	  if (!currentAvailability.isEmpty()) {
	    Schedule currentSchedule = 
	      ldmf.newSchedule(new Enumerator(currentAvailability));
	    if ((currentSchedule.getStartTime() != matchingRelationshipsSchedule.getStartTime()) ||
		(currentSchedule.getEndTime() != matchingRelationshipsSchedule.getEndTime())) {
	      availSchedule.removeAll(currentAvailability);
	      availSchedule.add(aggregateAvailability);
	    } else {
	      // No change required
	      return false;
	    }
	  } else {
	    availSchedule.add(aggregateAvailability);
	    change = true;
	  }
	}
      } else {

	if (((aa.isUpdate() || aa.isRepeat()) &&
	    (!currentAvailability.isEmpty())) &&
	    (currentAvailability.size() == aa.getSchedule().size())) {

	  // Compare to existing entries - only change if required.
	  Schedule currentSchedule = 
	    ldmf.newSchedule(new Enumerator(currentAvailability));
	  
	  for (Iterator localIterator = 
		 new ArrayList(currentSchedule).iterator(),
	       aaIterator = 
		 new ArrayList(aa.getSchedule()).iterator();
	       localIterator.hasNext();) {
	    ScheduleElement localElement = 
	      (ScheduleElement) localIterator.next();
	    ScheduleElement aaElement = (ScheduleElement) aaIterator.next();
	    
	    // compare timespan
	    if ((localElement.getStartTime() != aaElement.getStartTime()) ||
		(localElement.getEndTime() != aaElement.getEndTime())) {
	      availSchedule.removeAll(currentAvailability);
	      change = true;
	      break;
	    }
	  }
	} else {
	  change = true;
	}

	if (change) {
	  //Don't iterate over schedule directly because Schedule doesn't
	  //support iterator().
	  for (Iterator iterator = new ArrayList(aa.getSchedule()).iterator();
	       iterator.hasNext();) {
	    ScheduleElement avail = (ScheduleElement)iterator.next();
	    availSchedule.add(ldmf.newAssignedAvailabilityElement(receivingAsset, 
								  avail.getStartTime(),
								  avail.getEndTime()));
	  }
	}
      } 
    } // end sync block

    return change;
  }

  private boolean fixRelationshipSchedule(AssetTransfer at,
					  AssetAssignment aa, 
					  Asset transferringAsset, 
					  Asset receivingAsset) {
    if (!(related(transferringAsset) && related(receivingAsset))) {
      return false;
    }
    if ((aa.isUpdate() || aa.isRepeat())) {
      // Check whether info already represented in the local assets
      // No need to publish change if local assets are already current.
      if (!localScheduleUpdateRequired((HasRelationships) at.getAsset(),
				       (HasRelationships) transferringAsset) &&
	  !localScheduleUpdateRequired((HasRelationships) at.getAssignee(),
				       (HasRelationships) receivingAsset)) {
	return false;
      }
	  
      //Remove existing relationships
      removeExistingRelationships(at, 
                                  (HasRelationships)transferringAsset,
                                  (HasRelationships)receivingAsset);
    }

    // Add transfer relationships to local assets
    Collection localRelationships = 
      convertToLocalRelationships(at,
                                  transferringAsset,
                                  receivingAsset);

    RelationshipSchedule transferringSchedule = 
      ((HasRelationships)transferringAsset).getRelationshipSchedule();
    transferringSchedule.addAll(localRelationships);

    RelationshipSchedule receivingSchedule =
      ((HasRelationships)receivingAsset).getRelationshipSchedule();
    receivingSchedule.addAll(localRelationships);
    
    return true;
  }

  private boolean localScheduleUpdateRequired(HasRelationships atAsset,
					      HasRelationships localAsset) {

    if (!atAsset.equals(localAsset)) {
      throw new IllegalArgumentException("AssetTransferLP.localScheduleUpdateRequired()" +
					 " attempt to compare different Assets - " +
					 atAsset + " != " + localAsset);
    }
   


    RelationshipSchedule localRelationshipSchedule = 
      localAsset.getRelationshipSchedule();

    // Can't iterate over a schedule so pull elements into an ArrayList and
    // iterate over that
    for (Iterator iterator = 
	   new ArrayList(atAsset.getRelationshipSchedule()).iterator();
         iterator.hasNext();) {
      final Relationship relationship = (Relationship) iterator.next();
      
      Collection matching = 
	localRelationshipSchedule.getMatchingRelationships(new UnaryPredicate() {
	  public boolean execute(Object obj) {
	    Relationship matchCandidate = (Relationship)obj;
	    return (relationship.equals(matchCandidate));
	  }
	}
							   );
	  

      if (matching.isEmpty()) {
	return true;
      }
    }
    return false;
  }

  private void removeExistingRelationships(AssetTransfer at,
                                           HasRelationships transferringAsset,
                                           HasRelationships receivingAsset) {

    RelationshipSchedule receivingSchedule = 
      receivingAsset.getRelationshipSchedule();
    RelationshipSchedule transferringSchedule = 
      transferringAsset.getRelationshipSchedule();
    
    RelationshipSchedule atRelationshipSchedule = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule();
    
    for (Iterator atIterator = 
	   new ArrayList(atRelationshipSchedule).iterator();
         atIterator.hasNext();) {
      Relationship relationship = (Relationship) atIterator.next();
      
      Role role = (relationship.getA().equals(receivingAsset)) ?
        relationship.getRoleA() : relationship.getRoleB();
      
      Collection remove = 
        transferringSchedule.getMatchingRelationships(role,
                                                      receivingAsset,
                                                      ETERNITY);
      transferringSchedule.removeAll(remove);
      
      role = (relationship.getA().equals(transferringAsset)) ?
        relationship.getRoleA() :relationship.getRoleB();
      remove = 
        receivingSchedule.getMatchingRelationships(role,
                                                   transferringAsset,
                                                   ETERNITY);
      receivingSchedule.removeAll(remove);
    } 
  }

  protected Collection convertToLocalRelationships(AssetTransfer at,
                                                   Asset localTransferringAsset,
                                                   Asset receivingAsset) {
    RelationshipSchedule atRelationshipSchedule = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule();
    ArrayList atRelationships = new ArrayList(atRelationshipSchedule);

    ArrayList localRelationships = new ArrayList(atRelationships.size());

    for (Iterator iterator = atRelationships.iterator(); 
         iterator.hasNext();) {
      Relationship atRelationship = (Relationship)iterator.next();
      
      Asset A = 
        (atRelationship.getA().equals(at.getAsset())) ?
        localTransferringAsset : receivingAsset;
      Asset B = 
        (atRelationship.getB().equals(at.getAsset())) ?
        localTransferringAsset : receivingAsset;
      Relationship localRelationship = 
        ldmf.newRelationship(atRelationship.getRoleA(),
                             (HasRelationships)A,
                             (HasRelationships)B,
                             atRelationship.getStartTime(),
                             atRelationship.getEndTime());
      localRelationships.add(localRelationship);
    }

    return localRelationships;
  }

  // Clear relationship, role and availble schedules to ensure that there 
  // are no dangling references to other organizations.
  private void clearSchedule(Asset asset) {
    if (related(asset)) {
      ((HasRelationships ) asset).setRelationshipSchedule(null);
    }

    if (asset.getRoleSchedule() != null) {
      asset.getRoleSchedule().clear();

      if (asset.getRoleSchedule().getAvailableSchedule() != null) {
        asset.getRoleSchedule().getAvailableSchedule().clear();
      }
    }
  }

  private void publishAsset(Asset asset, 
			    boolean newAsset,
			    boolean changeRelationshipRequired,
			    boolean changeAvailabilityRequired) {

    if (newAsset) {
      rootplan.add(asset);
      if (logger.isDebugEnabled()) {
	logger.debug("publishAsset: publish added " + asset + 
		     " uid = " + asset.getUID());
      }
    } else  {
      boolean publishRequired = false;
      Collection changes = null;

      if (changeRelationshipRequired) {
	changes = new ArrayList();
	changes.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
	publishRequired = true;
      } else if (changeAvailabilityRequired) {
	publishRequired = true;
      }
      
      if (publishRequired) {
	if (logger.isDebugEnabled()) {
	  logger.debug("publishAsset: publish changed " + asset + 
		       " uid = " + asset.getUID());
	}
	rootplan.change(asset, changes);
      }
    }
  }
}









