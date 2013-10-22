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

package org.cougaar.planning.ldm.plan;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.util.TimeSpan;



/**
 * basic implementation of a cluster object factory.
 * Most of the factory methods do little but call the zero-arg constructor
 * of the FooImpl class.
 */
public class ClusterObjectFactoryImpl implements ClusterObjectFactory {
  protected final LDMServesPlugin ldm;
  private final MessageAddress cid;
  private HashMap IDHashMap;
  private ClassLoader ldmcl;
  private UIDServer myUIDServer;
  
  public ClusterObjectFactoryImpl(LDMServesPlugin ldm, MessageAddress cid) {
    this.ldm = ldm;
    this.cid = cid;
    if (cid == null) {
      throw new IllegalArgumentException("Null agent address");
    }
    myUIDServer = ldm.getUIDServer();
    if (myUIDServer == null) {
      throw new IllegalArgumentException("Null UID server");
    }
    ldmcl = ldm.getLDMClassLoader();
    IDHashMap = new HashMap(89);
  }
  
  protected Class loadClass(String className) throws ClassNotFoundException {
    if (ldmcl == null) {
      return Class.forName(className);
    } else {
      return ldmcl.loadClass(className);
    }
  }

  public NewAlert newAlert() {
    AlertImpl ai = new AlertImpl();
    ai.setUID(getNextUID());
    return (NewAlert)ai;
  }


  public NewAlertParameter newAlertParameter() {
    return new AlertParameterImpl();
  }

  public NewComposition newComposition() {
    return new CompositionImpl();
  }
  public NewConstraint newConstraint() {
    return new ConstraintImpl(); 
  }

  public NewReport newReport() {
    NewReport report = new ReportImpl();
    return report;
  }
  
  public NewAssignedRelationshipElement newAssignedRelationshipElement() {
    return new AssignedRelationshipElementImpl();
  }

  public NewAssignedRelationshipElement newAssignedRelationshipElement(Asset assetA,
                                                                       Role roleA,
                                                                       Asset assetB,
                                                                       long startTime,
                                                                       long endTime) {

    return new AssignedRelationshipElementImpl(assetA,
                                               roleA,
                                               assetB,
                                               roleA.getConverse(),
                                               startTime, 
                                               endTime);
  }

  public NewAssignedRelationshipElement newAssignedRelationshipElement(Relationship relationship) {
    return new AssignedRelationshipElementImpl(relationship);
  }

  public NewAssignedAvailabilityElement newAssignedAvailabilityElement() {
    return new AssignedAvailabilityElementImpl();
  }

  public NewAssignedAvailabilityElement newAssignedAvailabilityElement(Asset assignee,
                                                                       long startTime,
                                                                       long endTime) {

    return new AssignedAvailabilityElementImpl(assignee, startTime, endTime);
  }

  public NewItineraryElement newItineraryElement() {
    return new ItineraryElementImpl();
  } 

  public NewLocationRangeScheduleElement newLocationRangeScheduleElement() {
    return new LocationRangeScheduleElementImpl();
  }

  public NewLocationScheduleElement newLocationScheduleElement() {
    return new LocationScheduleElementImpl();
  }

  public NewScheduleElement newScheduleElement(Date start, Date end) {
    return new ScheduleElementImpl(start, end);
  }

  public NewTransferableTransfer newTransferableTransfer() {
    return new TransferableTransferImpl();
  }

  public NewTransferableAssignment newTransferableAssignment(){
    TransferableAssignmentImpl nta = new TransferableAssignmentImpl();
    nta.setSource(cid);
    nta.setDestination(cid);
    return nta;
  }

  public NewTransferableRescind newTransferableRescind(){
    TransferableRescindImpl nta = new TransferableRescindImpl();
    nta.setSource(cid);
    nta.setDestination(cid);
    return nta;
  }

  public NewTransferableVerification newTransferableVerification(Transferable t) {
    TransferableVerificationImpl ntv = new TransferableVerificationImpl(t);
    ntv.setSource(cid);
    ntv.setDestination(cid);
    return ntv;
  }

  public Policy newPolicy( String policyType ) {

     String className=policyType;

     // check to see if class is fully delimeted
     if (policyType.indexOf('.') == -1)
       className = "org.cougaar.planning.ldm.policy." + policyType;

     Policy myPol = null;
     try {
       // Class myClass = Class.forName( className );
       // use the LDM Classloader
       Class myClass = loadClass(className);
       myPol = (Policy) myClass.newInstance();
     } catch ( ClassNotFoundException ce ) {
	 ce.printStackTrace();
     } catch ( InstantiationException ie ) {
	 ie.printStackTrace();
     } catch ( IllegalAccessException iae ) {
	 iae.printStackTrace();
     }
     if (myPol != null){
       myPol.setUID( getNextUID() );
       myPol.setOwner(cid);
     }
     return myPol;
 }

  public NewTask newTask() {
    return newTask(null);
  }
  public NewTask newTask(UID uid) {
    if (uid == null) uid = getNextUID();
    NewTask nt = new TaskImpl(uid);
    //set default source and destination to this cluster
    nt.setSource(cid);
    nt.setDestination(cid);
    return nt; 
  }
  public NewMPTask newMPTask() {
    NewMPTask mpt = new MPTaskImpl( getNextUID() );
    mpt.setSource(cid);
    mpt.setDestination(cid);
    return mpt;
  }

  public NewTask shadowTask(Task t) {
    if (! (t instanceof TaskImpl))
      throw new RuntimeException("Cannot copy this task for transmission:"+t);
    
    UID uid = t.getUID();
    
    NewTask nt;
    if (t instanceof MPTask) {
      nt = new MPTaskImpl(uid);
      ((NewMPTask) nt).setParentTasks( ((MPTask) t).getParentTasks());
    } else {
      nt = new TaskImpl(uid);
      nt.setParentTaskUID(t.getParentTaskUID());
    }
    nt.setSource(cid);
    nt.setDestination(cid);

    nt.setPlan(t.getPlan());
    nt.setVerb(t.getVerb());
    nt.setDirectObject(t.getDirectObject());
    nt.setPrepositionalPhrases(t.getPrepositionalPhrases());
    nt.setPreferences(t.getPreferences());
    nt.setPriority(t.getPriority());
    return nt;
  }

  /** @deprecated Use org.cougaar.planning.ldm.plan.Verb constructor instead **/
  public Verb getVerb(String v) {
    return Verb.getVerb(v);
  }

  public NewWorkflow newWorkflow() {
    WorkflowImpl wf = new WorkflowImpl(cid, getNextUID()); 
    return wf;
  }

  public NewAssetAssignment newAssetAssignment() {
    NewAssetAssignment naa = new AssetAssignmentImpl();
    //set default source and destination to this cluster
    naa.setSource(cid);
    naa.setDestination(cid);
    return naa;
  }
  public NewNotification newNotification() {
    NewNotification nn = new NotificationImpl();
    //set default source and destination to this cluster
    nn.setSource(cid);
    nn.setDestination(cid);
    return nn;
  }

  public NewDeletion newDeletion() {
    NewDeletion nd = new DeletionImpl();
    //set default source and destination to this cluster
    nd.setSource(cid);
    nd.setDestination(cid);
    return nd;
  }

  public NewAssetVerification newAssetVerification() {
    return new AssetVerificationImpl();
  }
 
  public NewAssetVerification newAssetVerification(Asset asset, 
                                                   Asset assignee,
                                                   Schedule schedule) { 
    return new AssetVerificationImpl(asset, assignee, schedule);
  }
 
  public NewPrepositionalPhrase newPrepositionalPhrase() {
    return new PrepositionalPhraseImpl();
  }

  public PrepositionalPhrase newPrepositionalPhrase(String s, Object io) {
    return new PrepositionalPhraseImpl(s, io);
  }
  
  public TaskRescind newTaskRescind(Task task, MessageAddress destination){
    return new TaskRescindImpl(cid, destination, getRealityPlan(), task);
  }

  public TaskRescind newTaskRescind(UID taskUID, MessageAddress destination){
    return new TaskRescindImpl(cid, destination, getRealityPlan(), taskUID, false);
  }

  public TaskRescind newTaskRescind(UID taskUID, MessageAddress destination, boolean deleted){
    return new TaskRescindImpl(cid, destination, getRealityPlan(), taskUID, deleted);
  }

  public AssetRescind newAssetRescind(Asset asset, Asset rescindeeAsset,
                                      Schedule rescindSchedule) {
    if (!rescindeeAsset.hasClusterPG()) {
       throw new IllegalArgumentException("bad argument: cof.newAssetRescind: rescindeeAsset - " + rescindeeAsset + " does not have a ClusterPG");
    }

    if ((asset instanceof HasRelationships) &&
        (rescindeeAsset instanceof HasRelationships) &&
        (!(rescindSchedule instanceof AssignedRelationshipScheduleImpl))) {
      throw new IllegalArgumentException("bad argument: cof.newAssetRescind: schedule must be " + AssignedRelationshipScheduleImpl.class + " for assets which implement HasRelationships");
    } 
      
    return new AssetRescindImpl(cid, 
                                rescindeeAsset.getClusterPG().getMessageAddress(),
                                getRealityPlan(),
                                asset,
                                rescindeeAsset,
                                rescindSchedule);
  }
    
    

  /** Build a new simple schedule that contains only one start and end
   * date.
   * @param start - The start Date of the schedule
   * @param end - the end or finish Date of the schedule
   * @return Schedule - a schedule object containing one scheduleelement
   * @see java.util.Date
   **/
  public NewSchedule newSimpleSchedule(Date start, Date end) {
    ScheduleElement se = new ScheduleElementImpl(start, end);
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(ScheduleElementType.SIMPLE);
    s.setScheduleElement(se);
    return  s;
  }

  /** Build a new simple schedule that contains only one start and end
   * date.
   * @param startTime - The start time of the schedule
   * @param endTime - the end or finish time of the schedule
   * @return Schedule - a schedule object containing one scheduleelement
   * @see java.util.Date
   **/
  public NewSchedule newSimpleSchedule(long startTime, long endTime) {
    ScheduleElement se = new ScheduleElementImpl(startTime, endTime);
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(ScheduleElementType.SIMPLE);
    s.setScheduleElement(se);
    return  s;
  }

  /** Create an assigned relationship schedule.  This schedule is a container
   * of AssignedRelationshipElements. Should only be used by logic providers
   * in handling new/modified/removed AssetTransfers 
   * @param elements Enumeration{AssignedRelationshipElement}
   * @see org.cougaar.planning.ldm.plan.AssignedRelationshipElement
   **/
  public NewSchedule newAssignedRelationshipSchedule(Enumeration elements) {
    ScheduleImpl s = new AssignedRelationshipScheduleImpl();
    s.setScheduleElements(elements);
    return s;
  }

  /** Create an empty assigned relationship schedule. Schedule elements added
   * later must be AssignedRelationshipElements. Should only be used by logic 
   * providers in handling new/modified/removed AssetTransfers 
   * @see org.cougaar.planning.ldm.plan.AssignedRelationshipElement
   **/
  public NewSchedule newAssignedRelationshipSchedule() {
    ScheduleImpl s = new AssignedRelationshipScheduleImpl();
    return s;
  }

  /** Build an asset transfer availabity schedule.
   * @param availElements Enumeration{AssignedAvailabilityElement}
   * @see org.cougaar.planning.ldm.plan.AssignedAvailabilityElement 
   **/
  public NewSchedule newAssignedAvailabilitySchedule(Enumeration availElements) {
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleType(ScheduleType.ASSIGNED_AVAILABILITY);
    s.setScheduleElementType(ScheduleElementType.ASSIGNED_AVAILABILITY);
    s.setScheduleElements(availElements);
    return s;
  }

  /** Build a an asset transfer availabity schedule
   * @see org.cougaar.planning.ldm.plan.AssignedAvailabilityElement 
   **/
  public NewSchedule newAssignedAvailabilitySchedule() {
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleType(ScheduleType.ASSIGNED_AVAILABILITY);
    s.setScheduleElementType(ScheduleElementType.ASSIGNED_AVAILABILITY);
    return s;
  }
  
  /** Create a location schedule.  This schedule is a container of 
   * LocationScheduleElements.
   * @param locationElements Enumeration{LocationScheduleElement}
   * @see org.cougaar.planning.ldm.plan.LocationScheduleElement
   **/
  public NewSchedule newLocationSchedule(Enumeration locationElements) {
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(ScheduleElementType.LOCATION);
    s.setScheduleElements(locationElements);
    return s;
  }
  
  /** Create a location range schedule.  This schedule has a container
   * of LocationRangeScheduleElements.
   * @param locationRangeElements Enumeration{LocationRangeScheduleElement}
   * @see org.cougaar.planning.ldm.plan.LocationRangeScheduleElement
   **/
  public NewSchedule newLocationRangeSchedule(Enumeration locationRangeElements) {
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(ScheduleElementType.LOCATIONRANGE);
    s.setScheduleElements(locationRangeElements);
    return s;
  }
  
  /** Create a schedule that contains different types of scheduleelements.
   * Note that ScheduleElement has multiple subclasses which are excepted.
   * @param scheduleElements Enumeration{ScheduleElement}
   * @see org.cougaar.planning.ldm.plan.ScheduleElement
   **/
  public NewSchedule newSchedule(Enumeration scheduleElements) {
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(ScheduleElementType.MIXED);
    s.setScheduleElements(scheduleElements);
    return s;
  }
    
  /**
   * newRelationship - returns a Relationship
   *
   * @param role1 Role for object1
   * @param object1 HasRelationships which has role1
   * @param object2 HasRelationships which is the other half of the 
   * relationship, role set at role1.getConverse()
   * @param timeSpan TimeSpan for which the relationship is valid
   */
  public Relationship newRelationship(Role role1, 
                                         HasRelationships object1,
                                         HasRelationships object2,
                                         TimeSpan timeSpan) {
    return new RelationshipImpl(timeSpan, 
                                role1,
                                object1,
                                object2);
  }

  /**
   * newRelationship - returns a Relationship
   *
   * @param role1 Role for object1
   * @param object1 HasRelationships which has role1
   * @param object2 HasRelationships which is the other half of the 
   * relationship, role set at role1.getConverse()
   * @param startTime long which specifies the start of the relationship 
   * @param endTime long which specifies the end of the relationship. 
   * @see org.cougaar.util.TimeSpan
   */
  public Relationship newRelationship(Role role1, 
                                         HasRelationships object1,
                                         HasRelationships object2,
                                         long startTime,
                                         long endTime) {
    return new RelationshipImpl(startTime,
                                endTime,
                                role1,
                                object1,
                                object2);
  }

  /**
   * newRelationship - returns a Relationship based on the specified
   * AssignedRelationshipElement
   *
   * @param assignedRelationship AssignedRelationshipElement to be converted
   * @param asset1 Asset to be used in converting the 
   * AssignedRelationshipElement
   * @param asset2 Asset other asset to be used in converting the 
   * AssignedRelationshipElement
   */
  public Relationship newRelationship(AssignedRelationshipElement assignedRelationship,
                                         Asset asset1,
                                         Asset asset2) {
    if (!(asset1 instanceof HasRelationships) ||
        !(asset2 instanceof HasRelationships)) {
      throw new IllegalArgumentException("bad argument: cof.newRelationship: both assets must implement HasRelationships");
    }
    
    String itemIDA  = assignedRelationship.getItemIDA();
    String itemIDB = assignedRelationship.getItemIDB();
    String asset1ID = asset1.getItemIdentificationPG().getItemIdentification();
    String asset2ID = asset2.getItemIdentificationPG().getItemIdentification();

    HasRelationships objectA = null;

    if (itemIDA.equals(asset1ID)) {
      objectA = (HasRelationships)asset1;
    } else if (itemIDA.equals(asset2ID)) {
      objectA = (HasRelationships)asset2;
    } else {
      throw new IllegalArgumentException("bad argument: cof.newRelationship: itemIDA in AssignedRelationshipElement - " + 
                                         assignedRelationship + " - does not match either of the specified assets - " + asset1 + ", " + asset2);
    }

    HasRelationships objectB = null;

    if (itemIDB.equals(asset1ID)) {
      objectB = (HasRelationships)asset1;
    } else if (itemIDB.equals(asset2ID)) {
      objectB = (HasRelationships)asset2;
    } else {
      throw new IllegalArgumentException("bad argument: cof.newRelationship: itemIDB in AssignedRelationshipElement - " + 
                                         assignedRelationship + " - does not match either of the specified assets - " + asset1 + ", " + asset2);
    }
      
    return newRelationship(assignedRelationship.getRoleA(),
                           objectA, 
                           objectB, 
                           assignedRelationship.getStartTime(),
                           assignedRelationship.getEndTime());
  }

  /** Build an empty relationship schedule for the specified HasRelationships.
   * @param hasRelationships HasRelationships to which the relationship 
   * schedule will apply
   **/
  public NewRelationshipSchedule newRelationshipSchedule(HasRelationships hasRelationships) { 
    RelationshipScheduleImpl schedule = 
      new RelationshipScheduleImpl(hasRelationships);
    return  schedule;
  }

  /** Build a new relationship schedule for the specified HasRelationships.
   * @param hasRelationships HasRelationships to which the relationship 
   * schedule applies. N.B. hasRelationships must be included in all the 
   * Relationships on the schedule.
   * @param relationships Collection of Relationships for the specified 
   * HasRelationships.
   **/
  public NewRelationshipSchedule newRelationshipSchedule(HasRelationships hasRelationships, 
                                                         Collection relationships) {
    RelationshipScheduleImpl schedule = 
      new RelationshipScheduleImpl(hasRelationships, relationships);
    return  schedule;
  }

  public MessageAddress getMessageAddress() {
    return cid;
  }

  // represent "reality" with a shared Plan object
  public Plan getRealityPlan() { return PlanImpl.REALITY; }
  
  public UID getNextUID() {
    return myUIDServer.nextUID();
  }

  // Build a new AssetTransfer object
  public AssetTransfer createAssetTransfer(Plan aPlan, 
                                           Task aTask, 
                                           Asset anAsset, 
                                           Schedule aSchedule, 
                                           Asset toAsset,
                                           AllocationResult estimatedresult, 
                                           Role aRole) {
    if (aPlan == null ||
        aTask == null ||
        anAsset == null ||
        aSchedule == null ||
        toAsset == null ||
        aRole == null )
       throw new IllegalArgumentException("bad arguments: cof.createAssetTransfer("+aTask+", "+anAsset+","+aSchedule+","+toAsset+", "+aRole+")");
    
    if (!toAsset.hasClusterPG()) {
       throw new IllegalArgumentException("bad argument: cof.createAssetTransfer: toAsset - " + toAsset + " does not have a ClusterPG");
    }

    AssetTransfer at = new AssetTransferImpl(aPlan, aTask, anAsset, aSchedule,
                                             toAsset, cid, estimatedresult, 
                                             aRole);
    ((PlanElementImpl)at).setUID(getNextUID());
    return at;
  }

  // Build a new Allocation object
  public Allocation createAllocation(Plan aPlan, Task aTask, Asset anAsset, 
                                     AllocationResult estimatedresult, Role aRole) {
    if (aPlan == null ||
        aTask == null ||
        anAsset == null ||
        aRole == null )
      throw new IllegalArgumentException("bad arguments: cof.createAllocation("+aTask+", "+anAsset+", "+aRole+")");

    Allocation a = new AllocationImpl(aPlan, aTask, anAsset, estimatedresult, aRole);
    ((PlanElementImpl)a).setUID(getNextUID());
    return a;
  }
  
  //Build a new Expansion
  public Expansion createExpansion(Plan aPlan, Task aTask, Workflow aWorkflow, AllocationResult estimatedresult) {
    if (aPlan == null ||
        aTask == null ||
        aWorkflow == null)
      throw new IllegalArgumentException("bad arguments: cof.createExpansion("+aTask+", "+aWorkflow+")");
      
     Expansion e = new ExpansionImpl(aPlan, aTask, aWorkflow, estimatedresult);
     ((PlanElementImpl)e).setUID(getNextUID());
     return e;
  }
  
  // Build a new Aggregation
  public Aggregation createAggregation(Plan aPlan, Task aTask, Composition aComposition, AllocationResult estimatedresult) {
    if (aPlan == null ||
        aTask == null ||
        aComposition == null)
       throw new IllegalArgumentException("bad arguments: cof.createAggregation("+aTask+", "+aComposition+")");
       
    Aggregation ag = new AggregationImpl(aPlan, aTask, aComposition, estimatedresult);
    ((PlanElementImpl)ag).setUID(getNextUID());
    return ag;
  }
  
  // Build a new FailedDisposition
  public Disposition createFailedDisposition(Plan aPlan, Task aTask, AllocationResult failure) {
    if (aPlan == null ||
        aTask == null ||
        failure == null ||
        failure.isSuccess() )
       throw new IllegalArgumentException("bad arguments: cof.createFailedDisposition("+aTask+", "+failure+")");
       
    DispositionImpl fa = new DispositionImpl(aPlan, aTask, failure);
    fa.setUID(getNextUID());
    return fa;
  }

  public Disposition createDisposition(Plan aPlan, Task aTask, AllocationResult result) {
    if (aPlan == null ||
        aTask == null ||
        result == null )
       throw new IllegalArgumentException("bad arguments: cof.createDisposition("+aTask+", "+result+")");
       
    DispositionImpl fa = new DispositionImpl(aPlan, aTask, result);
    fa.setUID(getNextUID());
    return fa;
  }
  
  // Build a complete TransferableTransfer
  public TransferableTransfer createTransferableTransfer(Transferable aTransferable, Asset anAsset) {
    if (aTransferable == null || anAsset == null)
       throw new IllegalArgumentException("bad arguments: cof.createTransferableTransfer("+aTransferable+", "+anAsset+")");
       
    TransferableTransfer pt = new TransferableTransferImpl(aTransferable, anAsset);
    return pt;
  }
  
  /** @deprecated **/
  public AllocationResult newAllocationResult(double rating, boolean success, int[] aspecttypes, double[] result) {
      if (aspecttypes == null || result == null) {
        throw new IllegalArgumentException("bad arguments: cof.newAllocationResult("+aspecttypes+", "+result+")");
      }
       
    AllocationResult ar = new AllocationResult(rating, success, aspecttypes, result);
    return ar;
  }
  
  public AllocationResult newAllocationResult(double rating, boolean success, AspectValue[] avrs) {
    return AllocationResult.newAllocationResult(rating, success, avrs);
  }

  // Build a new PHASED AllocationResult
  /** @deprecated **/
  public AllocationResult newPhasedAllocationResult(double rating, boolean success, int[] aspecttypes, double[] rollup, Enumeration allresults) {
    if (aspecttypes == null ||
        rollup == null ||
        allresults == null ||
        aspecttypes.length == 0 ||
        rollup.length == 0 )
        throw new IllegalArgumentException("bad arguments: cof.newAllocationResult("+aspecttypes+", "+rollup+", "+allresults+")");
        
    AllocationResult par = new AllocationResult(rating, success, aspecttypes, rollup, allresults);
    return par;
  }
  
  /** @deprecated **/
  public AllocationResult newPhasedAllocationResult(double rating, boolean success, 
                                                    AspectValue[] avs, Enumeration allresults) {
    AllocationResult par = new AllocationResult(rating, success, avs, allresults);
    return par;
  }

  public AllocationResult newPhasedAllocationResult(double rating, boolean success, 
                                                    AspectValue[] avs, Collection allresults) {
    AllocationResult par = new AllocationResult(rating, success, avs, allresults);
    return par;
  }

  /** @deprecated **/
  public AllocationResult newAVPhasedAllocationResult(double rating, boolean success, 
                                                      AspectValue[] rollupavs, Collection phasedresults) {
    if (phasedresults == null ||
        rollupavs == null ||
        phasedresults.isEmpty() ||
        rollupavs.length == 0 )
        throw new IllegalArgumentException("bad arguments: cof.newAVPhasedAllocationResult("+rollupavs+", "+phasedresults+")");
        
    AllocationResult avpar = new AllocationResult(rating, success, rollupavs, phasedresults);
    return avpar;
  }
  
  /** @deprecated **/
  public AllocationResult newAVAllocationResult(double rating, boolean success, AspectValue[] aspectvalues) {
    if (aspectvalues == null ||
        aspectvalues.length == 0 ) {
        throw new IllegalArgumentException("bad arguments: cof.newAVAllocationResult("+aspectvalues+")");
      }
       
    AllocationResult avar = new AllocationResult(rating, success, aspectvalues);
    return avar;
  }

  //Build a new Preference
  public Preference newPreference(int aspecttype, ScoringFunction scorefunction) {
    if (aspecttype < 0 ||
        scorefunction == null )
        throw new IllegalArgumentException("bad arguments: cof.newPreferece("+aspecttype+", "+scorefunction+")");
        
     Preference pref = new PreferenceImpl(aspecttype, scorefunction);
     return pref;
   }
   
  //Build a new Preference with a weight
  public Preference newPreference(int aspecttype, ScoringFunction scorefunction, double aweight) {
    if (aspecttype < 0 ||
        scorefunction == null ||
        aweight > 1.0 ||
        aweight < 0.0 )
        throw new IllegalArgumentException("bad arguments: cof.newPreferece("+aspecttype+", "+scorefunction+", "+aweight+")");
        
     Preference pref = new PreferenceImpl(aspecttype, scorefunction, aweight);
     return pref;
   }
   
   //Build a new BulkEstimate
   public BulkEstimate newBulkEstimate(Task atask, List prefsets, double conf) {
     if (atask == null ||
         prefsets.isEmpty() ||
         conf < 0.0 ||
         conf > 1.0 )
         throw new IllegalArgumentException("bad arguments: cof.newBulkEstimate("+atask+", "+prefsets+", "+conf+")");
         
     BulkEstimate be = new BulkEstimateImpl(atask, prefsets, conf);
     return be;
   }
   
}
