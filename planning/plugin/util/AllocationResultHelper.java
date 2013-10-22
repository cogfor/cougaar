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

package org.cougaar.planning.plugin.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectRate;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;

/**
 * Manages AllocationResults having phased results representing
 * varying quantities and rates over time.
 **/
public class AllocationResultHelper {
  public class Phase {
    public AspectValue[] result;
    private Phase(int ix) {
      result = (AspectValue[]) phasedResults.get(ix);
    }

    public AspectValue getAspectValue(int type) {
      return result[getTypeIndex(type)];
    }

    public long getStartTime() {
      if (startix < 0) throw new RuntimeException("No START_TIME for " + task);
      return (long) result[startix].getValue();
    }

    public long getEndTime() {
      if (endix < 0) throw new RuntimeException("No END_TIME for " + task);
      return (long) result[endix].getValue();
    }

    public double getQuantity() {
      if (qtyix < 0) throw new RuntimeException("No QUANTITY for " + task);
      return result[qtyix].getValue();
    }
  }

  /** The Task whose disposition we are computing a result. **/
  private Task task;

  /** The index into the arrays of the START_TIME aspect **/
  private int startix = -1;

  /** The index into the arrays of the END_TIME aspect **/
  private int endix = -1;

  /** The index into the arrays of the QUANTITY aspect **/
  private int qtyix = -1;

  /** The new perfectResult **/
  private AspectValue[] perfectResult;

  /** The new phased results **/
  private List phasedResults = new ArrayList();

  /** The AspectType map **/
  private int[] typeMap;

  /** Has this allocation result been changed **/
  private boolean isChanged = false;

  private AllocationResult ar;

  public AllocationResultHelper(Task task, PlanElement pe) {
    AspectValue[] taskAVS = getAspectValuesOfTask(task);
    this.task = task;
    ar = null;
    if (pe != null) {
      ar = pe.getEstimatedResult();
    }
    if (ar != null) {
      if (ar.isPhased()) {
	phasedResults = ar.getPhasedAspectValueResults();
      } else {
	phasedResults = new ArrayList(1);
	phasedResults.add(ar.getAspectValueResults());
      }
      setTypeIndexes((AspectValue[]) phasedResults.get(0));
      //              checkPhases(phasedResults);
    } else {
      phasedResults = new ArrayList(1);
      phasedResults.add(taskAVS);
      setTypeIndexes(taskAVS);
    }
    perfectResult = getPerfectResult(taskAVS);
  }

  private AspectValue[] getAspectValuesOfTask(Task task) {
    List avs = new ArrayList();
    synchronized (task) {
      for (Enumeration e = task.getPreferences(); e.hasMoreElements(); ) {
	Preference pref = (Preference) e.nextElement();
	AspectValue best = pref.getScoringFunction().getBest().getAspectValue();
	avs.add(best);
      }
    }
    return (AspectValue[]) avs.toArray(new AspectValue[avs.size()]);
  }

  private AspectValue[] getPerfectResult(AspectValue[] avs) {
    AspectValue[] result = new AspectValue[avs.length];
    result = AspectValue.clone((AspectValue[]) phasedResults.get(0));
    for (int i = 0; i < avs.length; i++) {
      result[getTypeIndex(avs[i].getAspectType())] = avs[i];
    }
    return result;
  }

  public AllocationResult getAllocationResult() {
    return getAllocationResult(1.0);
  }

  public AllocationResult getAllocationResult(double confrating) {
    AspectValue[] ru = computeRollup();
    return getAllocationResult(confrating, isSuccess(ru), ru);
  }

  public AllocationResult getAllocationResult(double confrating,
					      boolean isSuccess)
  {
    return getAllocationResult(confrating, isSuccess, computeRollup());
  }

  private AllocationResult getAllocationResult(double confrating,
					       boolean isSuccess,
					       AspectValue[] ru)
  {
    return new AllocationResult(confrating, isSuccess,
				ru, phasedResults);
  }

  private boolean isSuccess(AspectValue[] ru) {
    int ix = 0;
    synchronized (task) {
      for (Enumeration e = task.getPreferences(); e.hasMoreElements(); ix++) {
	Preference pref = (Preference) e.nextElement();
	ScoringFunction sf = pref.getScoringFunction();
	AspectValue av = ru[ix];
	double thisScore = sf.getScore(av);
	if (thisScore >= ScoringFunction.HIGH_THRESHOLD) return false;
      }
    }
    return true;
  }

  public boolean isChanged() {
    return isChanged;
  }

  private void checkPhases(List phasedResults) {
    if (startix < 0 || endix < 0) return;
    for (int i = 0, n = phasedResults.size(); i < n; i++) {
      AspectValue[] pi = (AspectValue[]) phasedResults.get(i);
      long si = getStartTime(pi);
      long ei = getEndTime(pi);
      for (int j = i + 1; j < n; j++) {
	AspectValue[] pj = (AspectValue[]) phasedResults.get(j);
	long sj = getStartTime(pj);
	long ej = getEndTime(pj);
	if (sj >= ei || si >= ej) continue;
	System.err.println("Bad phases " + ar);
	int p = 0;
	for (Iterator it = phasedResults.iterator(); it.hasNext(); ) {
	  System.err.println("Phase " + p);
	  AspectValue[] phase = (AspectValue[]) it.next();
	  for (int q = 0; q < phase.length; q++) {
	    System.err.println("   " + phase[q]);
	  }
	}
	Thread.dumpStack();
	System.exit(1);
      }
    }
  }

  public int getPhaseCount() {
    //          checkPhases(phasedResults);
    return phasedResults.size();
  }

  public Phase getPhase(int i) {
    return new Phase(i);
  }

  /**
   * Set a successful value over a period of time
   **/
  public void setBest(int type, long startTime, long endTime) {
    int ix = getTypeIndex(type);
    Preference pref = task.getPreference(type);
    AspectValue av = pref.getScoringFunction().getBest().getAspectValue();
    set(ix, av, startTime, endTime);
  }

  /**
   * Set a failed value over a period of time. New phased results
   * are edited into the results as needed.
   **/
  public void setFailed(int type, long startTime, long endTime) {
    int ix = getTypeIndex(type);
    AspectValue av = perfectResult[ix].dupAspectValue(0.0);
    set(ix, av, startTime, endTime);
  }

  private int getTypeIndex(int type) {
    if (type < 0 || typeMap == null || type >= typeMap.length) {
      throw new IllegalArgumentException("Type " + type + " not found");
    }
    return typeMap[type];
  }

  /**
   * Edit the exiting results to reflect a particular value of the
   * indicated aspect over a given time period. Find existing
   * segments with different values that overlap the new segment and
   * adjust their times to not overlap. Then try to combine the new
   * segment with existing results having the same value and
   * adjacent or overlapping times. Finally, if the new segment
   * cannot be combined with any existing segment, add a new
   * segment. This does not fix the rollup result since that depends
   * on what aspect is edited.
   * @param valueix the index in the arrays of the aspect to change
   * @param value the new value for the time period
   * @param startTime the time when the value starts to apply
   * @param endTime the time when the value no longer applies.
   * @return true if a change was made.
   **/
  List newResults = null;
  private void set(int valueix, AspectValue av, long s, long e) {
    long startTime = s;
    long endTime = e;
    if (newResults == null)
      newResults = new ArrayList(phasedResults.size() + 2); // At most two new results
    boolean covered = false;
    boolean thisChanged = false;
    AspectValue[] newResult;
    long minTime = getStartTime(perfectResult);
    long maxTime = getEndTime(perfectResult);
    if (minTime < maxTime) {
      /* Only process if there is overlap between the arguments and
	 the start/end time aspects of the perfectResult **/
      if (startTime >= maxTime) {
	return; // Does not apply
      }
      endTime = Math.min(endTime, maxTime);
      if (endTime <= minTime) {
	return; // Does not apply
      }
      startTime = Math.max(startTime, minTime);

      for (Iterator i = phasedResults.iterator(); i.hasNext(); ) {
	AspectValue[] oneResult = (AspectValue[]) i.next();
	long thisStart = getStartTime(oneResult);
	long thisEnd   = getEndTime(oneResult);
	AspectValue thisValue = oneResult[valueix];
	if (thisValue.equals(av)) { // Maybe combine these
	  newResult = AspectValue.clone(oneResult);
	  if (startTime <= thisEnd && endTime >= thisStart) { // Overlaps
	    if (thisStart < startTime) startTime = thisStart;
	    if (thisEnd > endTime) endTime = thisEnd;
	    thisChanged = true;
	    continue;
	  } else {
	    newResults.add(newResult);
	  }
	} else {
	  if (startTime < thisEnd && endTime > thisStart) { // Overlaps
	    if (startTime > thisStart) { // Initial portion exists
	      newResult = AspectValue.clone(oneResult);
	      newResult[endix] = TimeAspectValue.create(AspectType.END_TIME, startTime);
	      newResults.add(newResult);
	    }
	    if (endTime < thisEnd) { // Final portion exists
	      newResult = AspectValue.clone(oneResult);
	      newResult[startix] = TimeAspectValue.create(AspectType.START_TIME, endTime);
	      newResults.add(newResult);
	    }
	    thisChanged = true;
	  } else {
	    newResult = AspectValue.clone(oneResult);
	    newResults.add(newResult);
	  }
	}
      }
    } else {
      if (startTime > minTime || endTime <= minTime) return;
      if (perfectResult[valueix].equals(av)) return;
      newResult = AspectValue.clone(perfectResult);
      newResult[valueix] = av;
      newResults.add(newResult);
      thisChanged = true;
      covered = true;
    }
    if (!covered) {
      newResult = AspectValue.clone(perfectResult);
      newResult[startix] = TimeAspectValue.create(AspectType.START_TIME, startTime);
      newResult[endix]   = TimeAspectValue.create(AspectType.END_TIME, endTime);
      newResult[valueix] = av;
      newResults.add(newResult);
      thisChanged = true;
    }
    if (!thisChanged) {
      return; // No changes were made
    }
    isChanged = true;
    //          checkPhases(newResults);
    phasedResults.clear();
    phasedResults.addAll(newResults);
    newResults.clear();
  }

  private AspectValue[] computeRollup() {
    double[] sums = new double[perfectResult.length];
    double[] divisor = new double[perfectResult.length];
    boolean first = true;
    Arrays.fill(sums, 0.0);
    Arrays.fill(divisor, 1.0);
    for (Iterator iter = phasedResults.iterator(); iter.hasNext(); ) {
      AspectValue[] oneResult = (AspectValue[]) iter.next();
      for (int i = 0; i < oneResult.length; i++) {
	AspectValue av = oneResult[i];
	double v = av.getValue();
	int type = av.getAspectType();
	boolean doAverage = (av instanceof AspectRate || type > AspectType._LAST_ASPECT);
	if (first) {
	  sums[i] = v;
	} else {
	  switch (type) {
	  default:
	    if (doAverage) divisor[i] += 1.0;
	    sums[i] += v;
	    break;
	  case AspectType.START_TIME:
	    sums[i] = Math.min(sums[i], v);
	    break;
	  case AspectType.END_TIME:
	    sums[i] = Math.max(sums[i], v);
	    break;
	  }
	}
      }
      first = false;
    }
    AspectValue[] ru = AspectValue.clone(perfectResult);
    for (int i = 0; i < ru.length; i++) {
      ru[i] = ru[i].dupAspectValue(sums[i] / divisor[i]);
    }
    return ru;
  }

  private long getStartTime(AspectValue[] avs) {
    return (long) avs[(startix >= 0) ? startix : endix].getValue();
  }

  private long getEndTime(AspectValue[] avs) {
    return (long) avs[(endix >= 0) ? endix : startix].getValue();
  }

  private void setTypeIndexes(AspectValue[] avs) {
    int maxIndex = AspectType._ASPECT_COUNT;
    for (int i = 0; i < avs.length; i++) {
      maxIndex = Math.max(maxIndex, avs[i].getAspectType());
    }
    typeMap = new int[maxIndex + 1];
    Arrays.fill(typeMap, -1);
    for (int i = 0; i < avs.length; i++) {
      int type = avs[i].getAspectType();
      typeMap[type] = i;
    }
    startix = getTypeIndex(AspectType.START_TIME);
    endix = getTypeIndex(AspectType.END_TIME);
    qtyix = getTypeIndex(AspectType.QUANTITY);
  }
}
