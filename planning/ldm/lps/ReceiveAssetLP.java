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
import java.util.Vector;

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocalPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;
import org.cougaar.planning.ldm.plan.AssetAssignment;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewRelationshipSchedule;
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


/**
 * take an incoming AssetAssignment Directive and
 * add Asset to the LogPlan w/side-effect of also disseminating to
 * other subscribers.
 **/
public class ReceiveAssetLP
implements LogicProvider, MessageLogicProvider {
  private static Logger logBase = Logging.getLogger(ReceiveAssetLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;
  private final Logger logger;

  private static TimeSpan ETERNITY = new MutableTimeSpan();

  public ReceiveAssetLP(
      RootPlan rootplan,
      LogPlan logplan,
      PlanningFactory ldmf,
      MessageAddress self) {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
    this.self = self;
    logger = new LoggingServiceWithPrefix(logBase, self.toString() + ": ");
  }

  public void init() {
  }

  /**
   * Adds Assets  to LogPlan... Side-effect = other subscribers
   * also updated.
   **/
  public void execute(Directive dir, Collection changes)
  {
    if (dir instanceof AssetAssignment) {
      AssetAssignment aa  = (AssetAssignment)dir;
      if (logger.isDebugEnabled()) logger.debug("Received " + aa);
      receiveAssetAssignment(aa);
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private void receiveAssetAssignment(AssetAssignment aa) {
    // figure out the assignee
    Asset assigneeT = aa.getAssignee();// assignee from message
    Asset assigneeL = logplan.findAsset(assigneeT); // local assignee instance
    
    if (assigneeL == null) {
      logger.error("Unable to find receiving asset " + 
                   assigneeT + " in "+self);
      return;
    }

    // figure out the asset being transferred
    Asset assetT = aa.getAsset();   // asset from message
    Asset assetL = logplan.findAsset(assetT);// local instance of asset

    Asset asset = assetL;

    if (asset == null) {
      if (aa.isUpdate() || aa.isRepeat()) {
        logger.error("Received Update Asset Transfer, but cannot find original "+ aa);
      } 
      asset = createLocalAsset(assetT);
    }

    
    boolean changeRelationshipRequired = updateRelationships(aa, asset,
							     assigneeL);

    boolean changeAvailabilityRequired = fixAvailSchedule(aa, asset, 
							  assigneeL);

    boolean changePGRequired = false;

    if (assetL != null) {
      // If we already had a matching asset - update with property groups
      // from the asset transfer.
      changePGRequired = updatePG(assetT, assetL);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("ReceiveAssetLP: changeRelationshipRequired = " + 
		   changeRelationshipRequired +
		   " changeAvailabilityRequired = " +
		   changeAvailabilityRequired +
		   " changePGRequired = " +
		   changePGRequired + 
		   " for AssetAssignment - " + aa +
		   " transfering asset - " + asset +
		   " receiving asset - " + assigneeL);
    }

      
    // publish the add or change
    if (assetL == null) {            // add it if it wasn't already there
      rootplan.add(asset);
    } else if (changeRelationshipRequired) {
      Collection changeReports = new ArrayList();
      changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      rootplan.change(asset, changeReports);
    } else if (changeAvailabilityRequired || changePGRequired) {
      rootplan.change(asset, null);
    }
  }
    

  private void removeExistingRelationships(Collection aaRelationships,
                                           NewRelationshipSchedule transferringSchedule,
                                           NewRelationshipSchedule receivingSchedule) {
    HasRelationships receivingAsset = 
      receivingSchedule.getHasRelationships();

    for (Iterator aaIterator = aaRelationships.iterator();
         aaIterator.hasNext();) {
      Relationship relationship = (Relationship) aaIterator.next();
      
      Role role = (relationship.getA().equals(receivingAsset)) ?
        relationship.getRoleA() : relationship.getRoleB();
      
      Collection remove = 
        transferringSchedule.getMatchingRelationships(role,
                                                      receivingAsset,
                                                      ETERNITY);
      
      // The relationship between the receiving and transferring assets should
      // be represented with identical Relationships in the two 
      // RelationshipSchedules.
      transferringSchedule.removeAll(remove);
      receivingSchedule.removeAll(remove);
    }
  }

  // Update availability info for the transferred asset
  // AvailableSchedule reflects availability within the current agent
  private boolean fixAvailSchedule(AssetAssignment aa, Asset asset,
                                final Asset assignee) {
    if (aa.getSchedule() == null) {
      return false;
    }

    NewSchedule availSchedule = 
      (NewSchedule)asset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      availSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)asset.getRoleSchedule()).setAvailableSchedule(availSchedule);
    }
    boolean change = false;

    synchronized (availSchedule) {

      // Find all existing entries which refer to the receiving asset
      Collection currentAvailability = 
	availSchedule.filter(new UnaryPredicate() {
	public boolean execute(Object o) {
	  return ((o instanceof AssignedAvailabilityElement) &&
		  (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
	}
      });

      if (related(asset) && related(assignee)) {

        //Construct aggregate avail info from the relationship schedule
        RelationshipSchedule relationshipSchedule = 
          ((HasRelationships)asset).getRelationshipSchedule();
        Collection matchingRelationships = 
          relationshipSchedule.getMatchingRelationships((HasRelationships)assignee,
                                                        ETERNITY);
        
        // If any relationships, construct a single avail element with the 
        // min start and max end
        if (!matchingRelationships.isEmpty()) {
          Schedule matchingRelationshipsSchedule = 
	    ldmf.newSchedule(new Enumerator(matchingRelationships));
	  AssignedAvailabilityElement aggregateAvailability = 
	    ldmf.newAssignedAvailabilityElement(assignee,
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
          availSchedule.add(ldmf.newAssignedAvailabilityElement(assignee, 
                                                                avail.getStartTime(),
                                                                avail.getEndTime()));
	  }
	}
      }
    } // end sync block


    return change;
  }

  protected Collection convertToRelationships(AssetAssignment aa,
                                              Asset transferring,
                                              Asset receiving) {
    if ((aa.getSchedule() == null) ||
	(!related(transferring)) ||
	(!related(receiving))) {
      return new ArrayList();
    }

    ArrayList relationships = new ArrayList(aa.getSchedule().size());

    //Safe because aaSchedule should be an AssignedRelationshipSchedule - 
    // supports iterator(). (Assumption is that AssignedRelationshipSchedule
    // is only used by LPs.)
    for (Iterator iterator = aa.getSchedule().iterator();
         iterator.hasNext();) {
      AssignedRelationshipElement aaRelationship = 
        (AssignedRelationshipElement)iterator.next();
      
      Relationship localRelationship = ldmf.newRelationship(aaRelationship,
                                                            transferring,
                                                            receiving);
      relationships.add(localRelationship);
    }
    
    return relationships;
  }

  protected Asset createLocalAsset(Asset aaAsset) {
    // Clone to ensure that we don't end up with cross agent asset 
    // references
    Asset asset = ldmf.cloneInstance(aaAsset);

    if (related(asset)) {
      HasRelationships hasRelationships = (HasRelationships) asset;
      hasRelationships.setLocal(false);
      hasRelationships.setRelationshipSchedule(ldmf.newRelationshipSchedule(hasRelationships));
    }

    return asset;
  } 

  protected boolean  localScheduleUpdateRequired(AssetAssignment aa,
						 Collection aaRelationships,
						 Asset assetL, 
						 Asset assigneeL) {
    if ((aa.getSchedule() == null) ||
	(!related(assetL)) ||
	(!related(assigneeL))) {
      return false;
    }

    if (!aa.isUpdate() && !aa.isRepeat()) {
      return true;
    }

    if (!aa.getAsset().equals(assetL)) {
      throw new IllegalArgumentException("ReceiveAssetLP.localScheduleUpdateRequired()" +
					 " attempt to compare different Assets - " +
					 aa.getAsset() + " != " + assetL);
    }

    if (!aa.getAssignee().equals(assigneeL)) {
      throw new IllegalArgumentException("ReceiveAssetLP.localScheduleUpdateRequired()" +
					 " attempt to compare different Assets - " +
					 aa.getAssignee() + " != " + assigneeL);
    }

    RelationshipSchedule assetLRelationshipSchedule = 
      ((HasRelationships) assetL).getRelationshipSchedule();

    RelationshipSchedule assigneeLRelationshipSchedule = 
      ((HasRelationships) assigneeL).getRelationshipSchedule();


    for (Iterator iterator = aaRelationships.iterator();
         iterator.hasNext();) {
      final Relationship relationship = (Relationship) iterator.next();
      
      Collection matchingAssetL = 
	assetLRelationshipSchedule.getMatchingRelationships(new UnaryPredicate() {
	  public boolean execute(Object obj) {
	    Relationship matchCandidate = (Relationship)obj;
	    return (relationship.equals(matchCandidate));
	  }
	}
	  );


      Collection matchingAssigneeL = 
	assigneeLRelationshipSchedule.getMatchingRelationships(new UnaryPredicate() {
	  public boolean execute(Object obj) {
	    Relationship matchCandidate = (Relationship)obj;
	    return (relationship.equals(matchCandidate));
	  }
	}
	  );
	
  

      if (matchingAssetL.isEmpty() ||
	  matchingAssigneeL.isEmpty()) {
	return true;
      }
    }
    
    return false;
  }

  protected boolean updatePG(Asset aaAsset, Asset localAsset) {
    Vector transferredPGs = aaAsset.fetchAllProperties();
    boolean update = false;

    for (Iterator pgIterator = transferredPGs.iterator();
	 pgIterator.hasNext();) {
      Object next = pgIterator.next();
      
      //Don't overwrite LocalPGs
      if (!(next instanceof LocalPG)) {
	if (next instanceof PropertyGroup) {
	  PropertyGroup transferredPG = (PropertyGroup) next;
	  PropertyGroup localPG = 
	    localAsset.searchForPropertyGroup(transferredPG.getPrimaryClass());
	  if ((localPG == null) ||
	      (!localPG.equals(transferredPG))) {
	    if (logger.isDebugEnabled()) {
	      logger.debug("ReceiveAssetLP: pgs not equal " +
			   " localPG = " + localPG +
			   " transferredPG = " + transferredPG);
	    }
	    localAsset.addOtherPropertyGroup(transferredPG);
	    update = true;
	  }
	} else if (next instanceof PropertyGroupSchedule) {
	  PropertyGroupSchedule transferredPGSchedule = 
	    (PropertyGroupSchedule) next;
	  PropertyGroupSchedule localPGSchedule = 
	    localAsset.searchForPropertyGroupSchedule(transferredPGSchedule.getClass());
	  if ((localPGSchedule == null) ||
	      (!localPGSchedule.equals(transferredPGSchedule))) {
	    if (logger.isDebugEnabled()) {
	      logger.debug("ReceiveAssetLP: pgschedules not equal " +
			   " localPGSchedule = " + localPGSchedule +
			   " transferredPG = " + transferredPGSchedule);
	    }
	    localAsset.addOtherPropertyGroupSchedule(transferredPGSchedule);
	    update = true;
	  }
	} else {
	  logger.error("ReceiveAssetLP: unrecognized property type - " + 
		       next + " - on transferred asset " + localAsset);
	}
      }
    }

    return update;
  }

  protected boolean updateRelationships(AssetAssignment aa,
					Asset localAsset,
					Asset localAssignee) {
    Collection aaRelationships = 
      convertToRelationships(aa, localAsset, localAssignee);

    boolean update = 
      localScheduleUpdateRequired(aa, aaRelationships,localAsset, 
				  localAssignee);
    
    //Only munge relationships pertinent to the transfer - requires that 
    //both receiving and transferring assets have relationship schedules
    if (update) {
      HasRelationships dob = ((HasRelationships) localAsset);
      NewRelationshipSchedule rs_a = 
	(NewRelationshipSchedule)dob.getRelationshipSchedule();
      
      HasRelationships iob = ((HasRelationships) localAssignee);
      NewRelationshipSchedule rs_assignee = 
        (NewRelationshipSchedule)iob.getRelationshipSchedule();
      
      if (aa.isUpdate() || aa.isRepeat()) { 
        removeExistingRelationships(aaRelationships, rs_a, rs_assignee);
      } 
      
      // Add relationships from the AssetAssignment
      rs_a.addAll(aaRelationships);
      rs_assignee.addAll(aaRelationships);
      
      Collection changeReports = new ArrayList();
      changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      rootplan.change(localAssignee, changeReports);

      if (logger.isDebugEnabled()) {
	logger.debug("ReceiveAssetLP: updateRelationships for " + 
		     localAssignee + 
		     " with AssetAssignment - " + aa);
      }
    }

    return update;
  }

  protected boolean updateSchedules(AssetAssignment aa, Asset localAsset,
				    Asset localAssignee) {
    boolean updateRelationships = updateRelationships(aa, localAsset, 
						      localAssignee); 

    boolean updateAvailSchedule = fixAvailSchedule(aa, localAsset, 
						   localAssignee);

    return updateAvailSchedule || updateRelationships;
  }
}








