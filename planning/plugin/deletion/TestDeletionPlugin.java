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

package org.cougaar.planning.plugin.deletion;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Constraint;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewComposition;
import org.cougaar.planning.ldm.plan.NewConstraint;
import org.cougaar.planning.ldm.plan.NewMPTask;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin constructs a logplan consisting of tasks and their
 * dispositions that can be deleted. It does the following:
 *   Inserts root tasks from time to time having timed activity.
 *   Expands root tasks into sequences of subtasks having time
 *       constraints.
 *   Aggregates subtasks of the same time within a time window.
 *   Allocates aggregated tasks to local or remote assets using a simple
 *       scheduling algorithm
 *   Watches for deletion of expired tasks and modifies its scheduling
 *       data to account for the deleted tasks
 * Root tasks have no OfType preposition.
 * Expansion of root tasks yields subtasks having OfType phrase
 * Aggregation of subtasks produce tasks at the next level. These
 * tasks have no OfType if they are to be allocated to another agent
 **/
public class TestDeletionPlugin extends SimplePlugin {
    /** Subscriptions to tasks **/

    private IncrementalSubscription tasksToExpand;
    private IncrementalSubscription tasksToAggregate;
    private IncrementalSubscription tasksToAllocateLocally;
    private IncrementalSubscription tasksToAllocateRemotely;
    private Vector[] mpTasks;
    private IncrementalSubscription selfOrgs;

    /** Something to allocate subtasks to. **/
    private Asset theRootAsset; // DO of root tasks
    private Asset theExpAsset;  // DO of expansion subtasks
    private Asset theAggAsset;  // DO of aggregation mptasks
    private Asset theAllocAsset;  // subject of allocations

    /** The Role we use for allocations **/
    private Role testProviderRole;

    private Entity selfOrg;

    private Entity provider;

    /**
     * The verbs that we use. Tasks to be expanded are
     * TestDeletionExpand and Tasks to be aggregated are
     * TestDeletionAggregate.
     **/
    
    private static Verb testDeletionExpand = Verb.get("TestDeletionExpand");
    private static Verb testDeletionAggregate = Verb.get("TestDeletionAggregate");

    /** The preposition used to specify task level **/
    private static final String LEVEL = "AtLevel";

    /** The preposition used to specify subtask type **/
    private static final String SUBTYPE = "Type";

    private static final int N_SUBTYPES = 3;
    private static final int TYPE_ROOT = 1; // The subtype that is forwarded to another agent

    /** The preposition used to specify task duration **/
    private static final String DURATION = "OfDuration";

    private static final long ONE_DAY  = 86400000L;
    private static final long ONE_HOUR =  3600000L;

    private long minRootTaskDelay     = 30 * ONE_DAY;
    private long maxRootTaskDelay     = 60 * ONE_DAY;
    private long minRootTaskDuration  =  3 * ONE_DAY;
    private long maxRootTaskDuration  = 14 * ONE_DAY;
    private long minInterTaskInterval =  1 * ONE_HOUR;
    private long maxInterTaskInterval = 24 * ONE_HOUR;
    private long AGGREGATION_PERIOD   =  5 * ONE_DAY;
    private long testDuration         =120 * ONE_DAY; // Run test for 120 days
    private int nRoots = 0;
    private int rootCount = 0;  // Number of roots so far
    private int level = 0;      // Our level
    private double clockRate = Double.NaN;
    private Alarm newRootTimer;
    private long testEnd;
    private boolean useProvider = false;
    private Random random = new Random();
    private LoggingService logger;

    private UnaryPredicate expandPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            if (o instanceof Task) {
                Task task = (Task) o;
                return (task.getVerb().equals(testDeletionExpand)
                        && getLevel(task) == level);
            }
            return false;
        }
    };

    private UnaryPredicate aggregatePredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            if (o instanceof Task) {
                Task task = (Task) o;
                return (task.getVerb().equals(testDeletionAggregate)
                        && getLevel(task) == level);
            }
            return false;
        }
    };

    private UnaryPredicate allocateLocallyPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            if (o instanceof Task) {
                Task task = (Task) o;
                if (task.getVerb().equals(testDeletionExpand) && getLevel(task) == level + 1) {
                    return !useProvider || getSubtype(task) != TYPE_ROOT;
                }
            }
            return false;
        }
    };

    private UnaryPredicate allocateRemotelyPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            if (o instanceof Task) {
                Task task = (Task) o;
                if (task.getVerb().equals(testDeletionExpand) && getLevel(task) == level + 1) {
                    return useProvider && getSubtype(task) == TYPE_ROOT;
                }
            }
            return false;
        }
    };

    public void setLoggingService(LoggingService ls) {
        logger = ls;
    }

    public void load() {
        super.load();
        if (!(logger instanceof LoggingServiceWithPrefix)) {
            logger = LoggingServiceWithPrefix.add(logger, getMessageAddress().toString() + ": ");
        }
    }

    public void setupSubscriptions() {
        Vector params = getParameters();
        switch (params.size()) {
        default:
        case 5: clockRate = Double.parseDouble((String) params.elementAt(4));
        case 4: testDuration = parseInterval((String) params.elementAt(3));
        case 3: nRoots = Integer.parseInt((String) params.elementAt(2));
        case 2: useProvider = ((String) params.elementAt(1)).trim().toLowerCase().equals("true");
        case 1: level = Integer.parseInt((String) params.elementAt(0));
        case 0: break;
        }
        logger.info("       level=" + level);
        logger.info(" useProvider=" + useProvider);
        logger.info("      nRoots=" + nRoots);
        logger.info("testDuration=" + testDuration);
        logger.info("   clockRate=" + clockRate);
        testProviderRole = Role.getRole("TestDeletionProvider");
        theRootAsset = theLDMF.createInstance(theLDMF.createPrototype(Asset.class, "TestRoot"));
        theExpAsset = theLDMF.createInstance(theLDMF.createPrototype(Asset.class, "TestExp"));
        theAggAsset = theLDMF.createInstance(theLDMF.createPrototype(Asset.class, "TestAgg"));
        theAllocAsset = theLDMF.createInstance(theLDMF.createPrototype(Asset.class, "TestAlloc"));
        publishAdd(theRootAsset);
        publishAdd(theExpAsset);
        publishAdd(theAggAsset);
        publishAdd(theAllocAsset);
        selfOrgs = (IncrementalSubscription) subscribe(new UnaryPredicate() {
            public boolean execute(Object o) {
                if (o instanceof Entity) {
                    return ((Entity) o).isSelf();
                }
                return false;
            }
        });
        if (!useProvider) setupSubscriptions2();
    }

    private void setupSubscriptions2() {
        mpTasks = new Vector[N_SUBTYPES];
        for (int i = 0; i < mpTasks.length; i++) {
            mpTasks[i] = new Vector();
        }
        tasksToExpand = (IncrementalSubscription) subscribe(expandPredicate);
        tasksToAggregate = (IncrementalSubscription) subscribe(aggregatePredicate);
        tasksToAllocateLocally = (IncrementalSubscription) subscribe(allocateLocallyPredicate);
        tasksToAllocateRemotely = (IncrementalSubscription) subscribe(allocateRemotelyPredicate);
        testEnd = currentTimeMillis() + testDuration;
        setNewRootTimer();
        if (!Double.isNaN(clockRate)) {
            getDemoControlService().setSocietyTimeRate(clockRate);
        }
    }

    public void execute() {
        if (logger.isDebugEnabled()) logger.debug("TestDeletionPlugin.execute() at " + dateFormat.format(new Date(currentTimeMillis())));
        long startTime = currentTimeMillis();
        if (useProvider) {
            if (provider == null) {
                if (selfOrgs.hasChanged()) {
                    if (selfOrg == null) {
                        checkSelfOrgs(selfOrgs.getAddedList());
                    }
                    if (selfOrg == null) return;
                    checkProvider();
                }
            }
            if (provider == null) return;
        }
        if (tasksToExpand.hasChanged()) {
            handleExpTasksAdded(tasksToExpand.getAddedList());
            handleExpTasksChanged(tasksToExpand.getChangedList());
            handleExpTasksRemoved(tasksToExpand.getRemovedList());
        }
        if (tasksToAggregate.hasChanged()) {
            handleSubTasksAdded(tasksToAggregate.getAddedList());
            handleSubTasksChanged(tasksToAggregate.getChangedList());
            handleSubTasksRemoved(tasksToAggregate.getRemovedList());
        }
        if (tasksToAllocateLocally.hasChanged()) {
            handleAggTasksAdded(tasksToAllocateLocally.getAddedList(), false);
            handleAggTasksChanged(tasksToAllocateLocally.getChangedList(), false);
            handleAggTasksRemoved(tasksToAllocateLocally.getRemovedList(), false);
        }
        if (tasksToAllocateRemotely.hasChanged()) {
            handleAggTasksAdded(tasksToAllocateRemotely.getAddedList(), true);
            handleAggTasksChanged(tasksToAllocateRemotely.getChangedList(), true);
            handleAggTasksRemoved(tasksToAllocateRemotely.getRemovedList(), true);
        }
            
        if (newRootTimer != null && newRootTimer.hasExpired()) {
            newRootTimer = null;
            addRootTask();
            rootCount++;
            setNewRootTimer();
        }
        long endTime = currentTimeMillis();
        long elapsed = endTime - startTime;
        if (elapsed > maxElapsed) {
            if (logger.isDebugEnabled()) logger.debug("time to run execute(): " + elapsed);
            maxElapsed = elapsed;
        }
    }
    private long maxElapsed = 0L;

    private void checkSelfOrgs(Enumeration orgs) {
        if (orgs.hasMoreElements()) {
            selfOrg = (Entity) orgs.nextElement();
        }
    }

    private void checkProvider() {
        Collection c = selfOrg.getRelationshipSchedule().getMatchingRelationships(testProviderRole);
        if (c.size() > 0) {
            Relationship relationship = (Relationship) c.iterator().next();
            if (relationship.getRoleA().equals(testProviderRole)) {
                provider = (Entity) relationship.getA();
            } else {
                provider = (Entity) relationship.getB();
            }
            setupSubscriptions2(); // Ready to go
        }
    }

    private long randomLong(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min));
    }

    private void setNewRootTimer() {
        if ((testDuration <= 0L || currentTimeMillis() < testEnd)
            && (nRoots < 0 || rootCount < nRoots)) {
            long interval = randomLong(minInterTaskInterval, maxInterTaskInterval);
            newRootTimer = wakeAfter(interval);
            if (logger.isDebugEnabled()) logger.debug("Next wakeup after " + (interval/3600000.0) + " hours");
        } else {
            if (logger.isDebugEnabled()) logger.debug("No wakeup: " + testDuration + ", " + nRoots);
        }
    }

    private void handleExpTasksAdded(Enumeration tasks) {
        while (tasks.hasMoreElements()) {
            Task expTask = (Task) tasks.nextElement();
            try {
                checkTaskValid(expTask);
                if (logger.isDebugEnabled()) logger.debug("Exp task added: " + format(expTask));
                expandTask(expTask);
            } catch (RuntimeException re) {
                if (logger.isErrorEnabled()) logger.error("handleExpTasksAdded: " + re);
                failTask(expTask);
            }
        }
    }

    private void handleExpTasksChanged(Enumeration tasks) {
        while (tasks.hasMoreElements()) {
            Task expTask = (Task) tasks.nextElement();
            try {
                checkTaskValid(expTask);
                if (logger.isDebugEnabled()) logger.debug("Exp task changed: " + format(expTask));
            } catch (RuntimeException re) {
                if (logger.isErrorEnabled()) logger.error("handleExpTasksChanged: " + re);
            }
        }
    }

    private void handleExpTasksRemoved(Enumeration tasks) {
        while (tasks.hasMoreElements()) {
            Task expTask = (Task) tasks.nextElement();
            if (logger.isDebugEnabled()) logger.debug("Exp task removed: " + format(expTask));
            // There's nothing to do
        }
    }

    private void handleSubTasksAdded(Enumeration tasks) {
        while (tasks.hasMoreElements()) {
            Task subTask = (Task) tasks.nextElement();
            try {
                checkTaskValid(subTask);
                if (logger.isDebugEnabled()) logger.debug("subTask added: " + format(subTask));
                aggregateSubtask(subTask);
            } catch (RuntimeException re) {
                if (logger.isErrorEnabled()) logger.error("handleSubTasksAdded: " + re);
                failTask(subTask);
            }
        }
    }

    private void handleSubTasksChanged(Enumeration tasks) {
        while (tasks.hasMoreElements()) {
            Task subTask = (Task) tasks.nextElement();
            try {
                checkTaskValid(subTask);
                if (logger.isDebugEnabled()) logger.debug("subTask changed: " + format(subTask));
            } catch (RuntimeException re) {
                if (logger.isErrorEnabled()) logger.error("handleSubTasksChanged: " + re);
            }
        }
    }

    private void handleSubTasksRemoved(Enumeration tasks) {
        while (tasks.hasMoreElements()) {
            Task subTask = (Task) tasks.nextElement();
            if (logger.isDebugEnabled()) logger.debug("subTask removed:. " + format(subTask));
        }
    }

    private void handleAggTasksAdded(Enumeration tasks, boolean remote) {
        while (tasks.hasMoreElements()) {
            Task aggTask = (Task) tasks.nextElement();
            try {
                checkTaskValid(aggTask);
                if (logger.isDebugEnabled()) logger.debug("aggTask added: " + format(aggTask));
                allocateAggtask(aggTask, remote);
            } catch (RuntimeException re) {
                if (logger.isErrorEnabled()) logger.error("handleAggTasksAdded: " + re);
                failTask(aggTask);
            }
        }
    }

    private void handleAggTasksChanged(Enumeration tasks, boolean remote) {
        while (tasks.hasMoreElements()) {
            Task aggTask = (Task) tasks.nextElement();
            try {
                checkTaskValid(aggTask);
                if (logger.isDebugEnabled()) logger.debug("aggTask changed: " + format(aggTask));
                reallocateAggtask(aggTask, remote);
            } catch (RuntimeException re) {
                if (logger.isErrorEnabled()) logger.error("handleAggTasksChanged: " + re);
            }
        }
    }

    private void handleAggTasksRemoved(Enumeration tasks, boolean remote) {
        while (tasks.hasMoreElements()) {
            Task aggTask = (Task) tasks.nextElement();
            int subtype = getSubtype(aggTask);
            mpTasks[subtype].remove(aggTask);
            advanceScheduleStartTime(aggTask, remote);
        }
    }

    private void checkTaskValid(Task task) {
        Date date = task.getCommitmentDate();
        long t;
        if (date != null) {
            t = date.getTime();
        } else {
            try {
                t = PluginHelper.getStartTime(task);
            } catch (IllegalArgumentException iae) {
                try {
                    t = PluginHelper.getEndTime(task);
                } catch (IllegalArgumentException iae2) {
                    t = -1L;
                }
            }
        }
        if (t == -1L) {
            throw new RuntimeException("Task has no valid time");
        }
        long now = currentTimeMillis();
        if (now >= t) {
            throw new RuntimeException(dateFormat.format(new Date(now))
                                       + " is past the commitment time of: "
                                       +  format(task));
        }
    }

    /**
     * Tasks are expanded into an number of subtasks. Constraints are
     * erected between the tasks restricting them to strictly
     * sequential execution. The duration of each subtask is between
     * 50% and 100% of equal fractions of the parent task duration.
     **/
    private void expandTask(Task expTask) {
        int nsubs = getSubtaskCount(expTask);
        long parentDuration = getDuration(expTask);
        long nominalDuration = parentDuration / nsubs;
        long parentStart = PluginHelper.getStartTime(expTask);
        long startTime = parentStart;
        Vector subs = new Vector(nsubs);
        Vector constraints = new Vector(nsubs + 1);
        Task previousTask = null;
        for (int i = 0; i < nsubs; i++) {
            long duration = (long) (random.nextDouble() * nominalDuration);
            NewTask subtask = createTask(testDeletionAggregate, getLevel(expTask), i, theExpAsset,
                                         startTime, startTime + nominalDuration, duration);
            publishAdd(subtask);
            startTime += nominalDuration;
            subs.addElement(subtask);
            if (previousTask == null) {
                NewConstraint constraint = theLDMF.newConstraint();
                constraint.setConstrainingTask(expTask);
                constraint.setConstrainingAspect(AspectType.START_TIME);
                constraint.setConstrainedTask(subtask);
                constraint.setConstrainedAspect(AspectType.START_TIME);
                constraint.setConstraintOrder(Constraint.AFTER);
                constraints.addElement(constraint);
            } else {
                NewConstraint constraint = theLDMF.newConstraint();
                constraint.setConstrainingTask(previousTask);
                constraint.setConstrainingAspect(AspectType.END_TIME);
                constraint.setConstrainedTask(subtask);
                constraint.setConstrainedAspect(AspectType.START_TIME);
                constraint.setConstraintOrder(Constraint.AFTER);
                constraints.addElement(constraint);
            }
            previousTask = subtask;
        }
        NewConstraint constraint = theLDMF.newConstraint();
        constraint.setConstrainingTask(expTask);
        constraint.setConstrainingAspect(AspectType.END_TIME);
        constraint.setConstrainedTask(previousTask);
        constraint.setConstrainedAspect(AspectType.END_TIME);
        constraint.setConstraintOrder(Constraint.BEFORE);
        constraints.addElement(constraint);
        AllocationResult ar =
            PluginHelper.createEstimatedAllocationResult(expTask, theLDMF, 1.0, true);
        Expansion exp = PluginHelper.wireExpansion(expTask, subs, theLDMF, ar);
        NewWorkflow wf = (NewWorkflow) exp.getWorkflow();
        wf.setConstraints(constraints.elements());
        publishAdd(exp);
    }

    private void failTask(Task task) {
        AllocationResult ar = PluginHelper.createEstimatedAllocationResult(task, theLDMF, 1.0, false);
        Disposition disp = theLDMF.createFailedDisposition(task.getPlan(), task, ar);
        publishAdd(disp);
    }

    /**
     * Find an aggregation for which the timespan can accomodate the
     * subtask. If a suitable aggregation is not found, create a new
     * one. A suitable aggregation is one for which all tasks fall
     * completely within an AGGREGATION_PERIOD of time. That is, the
     * interval between the earliest start time and the latest end
     * time does not exceed AGGREGATION_PERIOD.
     **/
    private void aggregateSubtask(Task subtask) {
        int subtype = getSubtype(subtask);
        long startTime = PluginHelper.getStartTime(subtask);
        long endTime = PluginHelper.getEndTime(subtask);
        long now = currentTimeMillis();
        long minTime = now + 2 * ONE_DAY;
        if (startTime < minTime) {
            if (logger.isDebugEnabled()) logger.debug("subtask starts too soon after "
                               + dateFormat.format(new Date(now))
                               + ": "
                               + format(subtask));
            failTask(subtask);
            return;
        }
        for (Iterator i = mpTasks[subtype].iterator(); i.hasNext(); ) {
            NewMPTask mpTask = (NewMPTask) i.next();
            long mpStartTime = PluginHelper.getStartTime(mpTask);
            if (mpStartTime > minTime) {
                continue;       // Can't use if about to start
            }
            long mpEndTime = PluginHelper.getEndTime(mpTask);
            if (mpStartTime + AGGREGATION_PERIOD > endTime
                && mpEndTime - AGGREGATION_PERIOD < startTime) {
                createAggregation(subtask, mpTask);
                return;
            }
        }
        NewMPTask mpTask = createMPTask(testDeletionExpand, getLevel(subtask) + 1, subtype, theAggAsset,
                                        startTime, endTime, endTime - startTime);
        mpTasks[subtype].add(mpTask);
        publishAdd(mpTask);
        createAggregation(subtask, mpTask);
    }

    private void createAggregation(Task subtask, NewMPTask mpTask) {
        long startTime = PluginHelper.getStartTime(subtask);
        if (startTime < PluginHelper.getStartTime(mpTask)) {
            setStartTimePreference(mpTask, startTime);
        }
        long endTime = PluginHelper.getEndTime(subtask);
        if (endTime > PluginHelper.getEndTime(mpTask)) {
            setEndTimePreference(mpTask, endTime);
        }
        AllocationResult ar =
            PluginHelper.createEstimatedAllocationResult(subtask, theLDMF,
                                                         1.0, true);
        Aggregation agg =
            PluginHelper.wireAggregation(subtask, mpTask, theLDMF, ar);
        publishChange(mpTask);
        publishAdd(agg);
    }

    private void setStartTimePreference(NewTask mpTask, long newStartTime) {
        ScoringFunction sf;
        Preference pref;
        sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.START_TIME,
                                                                   newStartTime));
        pref = theLDMF.newPreference(AspectType.START_TIME, sf);
        mpTask.setPreference(pref);
//          mpTask.setCommitmentDate(new Date(newStartTime));

    }

    private void setEndTimePreference(NewTask mpTask, long newEndTime) {
        ScoringFunction sf;
        Preference pref;
        sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.END_TIME,
                                                                   newEndTime));
        pref = theLDMF.newPreference(AspectType.END_TIME, sf);
        mpTask.setPreference(pref);
    }

    private void advanceScheduleStartTime(Task task, boolean remote) {
    }

    private int getLevel(Task task) {
        PrepositionalPhrase pp = task.getPrepositionalPhrase(LEVEL);
        if (pp == null) {
            if (logger.isDebugEnabled()) logger.debug("No LEVEL for " + format(task));
            return 0;
        }
        Integer io = (Integer) pp.getIndirectObject();
        return io.intValue();
    }

    private int getSubtype(Task task) {
        PrepositionalPhrase pp = task.getPrepositionalPhrase(SUBTYPE);
        Integer io = (Integer) pp.getIndirectObject();
        return io.intValue();
    }

    private static long getDuration(Task task) {
        PrepositionalPhrase pp = task.getPrepositionalPhrase(DURATION);
        Long io = (Long) pp.getIndirectObject();
        return io.longValue();
    }

    private static int getSubtaskCount(Task task) {
        return N_SUBTYPES;
    }

    private void reallocateAggtask(Task subtask, boolean remote) {
        AllocationResult ar =
            PluginHelper
            .createEstimatedAllocationResult(subtask, theLDMF, 1.0, true);
        Allocation alloc = (Allocation) subtask.getPlanElement();
        alloc.setEstimatedResult(ar);
        publishChange(alloc);
    }

    private void allocateAggtask(Task subtask, boolean remote) {
        Asset asset;
        if (remote) {
            asset = provider;
            if (logger.isDebugEnabled()) logger.debug("Using provider " + provider);
        } else {
            asset = theAllocAsset;
        }
        if (asset == null) return; // Wait 'til out provider checks reports for service
        AllocationResult ar =
            PluginHelper
            .createEstimatedAllocationResult(subtask, theLDMF, 1.0, true);
        Allocation alloc =
            theLDMF.createAllocation(subtask.getPlan(), subtask, asset,
                                     ar, testProviderRole);
        publishAdd(alloc);
    }

    private NewMPTask createMPTask(Verb verb, int level, int subtype, Asset asset,
                                   long startTime, long endTime, long duration)
    {
        NewMPTask task = theLDMF.newMPTask();
        fillTask(task, verb, level, subtype, asset, startTime, endTime, duration);
        NewComposition composition = theLDMF.newComposition();
        composition.setCombinedTask(task);
        task.setComposition(composition);
        return task;
    }
    private NewTask createTask(Verb verb, int level, int subtype, Asset asset,
                               long startTime, long endTime, long duration)
    {
        NewTask task = theLDMF.newTask();
        fillTask(task, verb, level, subtype, asset, startTime, endTime, duration);
        return task;
    }
    private void fillTask(NewTask task, Verb verb, int level, int subtype, Asset asset,
                          long startTime, long endTime, long duration)
    {
        task.setVerb(verb);
        NewPrepositionalPhrase pp;
        Vector phrases = new Vector(3);

        pp = theLDMF.newPrepositionalPhrase();
        pp.setPreposition(DURATION);
        pp.setIndirectObject(new Long(duration));
        phrases.addElement(pp);

        pp = theLDMF.newPrepositionalPhrase();
        pp.setPreposition(LEVEL);
        pp.setIndirectObject(new Integer(level));
        phrases.addElement(pp);

        pp = theLDMF.newPrepositionalPhrase();
        pp.setPreposition(SUBTYPE);
        pp.setIndirectObject(new Integer(subtype));
        phrases.addElement(pp);

        task.setPrepositionalPhrases(phrases.elements());
	task.setDirectObject(asset);
//          task.setCommitmentDate(new Date(startTime));
        ScoringFunction sf;
        Preference pref;
        long slop = ((endTime - startTime) - duration) / 2L;
        if (slop <= 0) {
            sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.START_TIME,
                                                                       startTime));
        } else {
            double slope = 1.0 / slop; // Slope such that score reaches 1.0 in slop msec
            sf = new ScoringFunction.AboveScoringFunction(AspectValue.newAspectValue(AspectType.START_TIME,
                                                                          startTime), slope);
        }
        pref = theLDMF.newPreference(AspectType.START_TIME, sf);
        task.setPreference(pref);
        if (slop <= 0) {
            sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.END_TIME,
                                                                       endTime));
        } else {
            double slope = 1.0 / slop; // Slope such that score reaches 1.0 in slop msec
            sf = new ScoringFunction.BelowScoringFunction(AspectValue.newAspectValue(AspectType.END_TIME,
                                                                          endTime), slope);
        }
        pref = theLDMF.newPreference(AspectType.END_TIME, sf);
        task.setPreference(pref);
	task.addObservableAspect(AspectType.START_TIME);
	task.addObservableAspect(AspectType.END_TIME);
    }

    private void addRootTask() {
        long startTime = currentTimeMillis() + randomLong(minRootTaskDelay, maxRootTaskDelay);
        long duration = randomLong(minRootTaskDuration, maxRootTaskDuration);
        long endTime = startTime + 2L * duration;
        NewTask task = createTask(testDeletionExpand, level, TYPE_ROOT, theRootAsset, startTime, endTime, duration);
        if (logger.isDebugEnabled()) logger.debug("Adding " + format(task));
        publishAdd(task);
    }

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HHmm");

    private String format(Task task) {
        return task.getDirectObject().getTypeIdentificationPG().getTypeIdentification()
            + ", level="
            + getLevel(task)
            + ", subtype="
            + getSubtype(task)
            + ", start="
            + dateFormat.format(new Date(PluginHelper.getStartTime(task)))
            + ", end="
            + dateFormat.format(new Date(PluginHelper.getEndTime(task)));
    }
}
