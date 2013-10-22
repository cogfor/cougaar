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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * The "result" of allocating a task.
 **/

public class AllocationResult
  implements AspectType, AuxiliaryQueryType, Serializable, Cloneable
{
                                    
  // Final and cloned, so these can be accessed outside a lock:
  private final boolean isSuccess;
  private final float confrating;
  private final AspectValue[] avResults;
  private final ArrayList phasedavrs; // A List of AspectValue[], null if not phased

  // Mutable auxqueries array, typically null.
  // Locked by "avResults", since it's non-null.
  // We make this copy-on-write, to reduce cloning and locking
  // overhead, since we expect far more readers than writers.
  private String[] auxqueries;

  // Mutable memoized variables.
  // Locked by "avResults", since it's non-null.
  // 
  // Must call "clearMemos()" at end of "readObject", since
  // transient initializers are not called in deserialization.
  // To make things consistent, we use an explicit "clearMemos()"
  // at the end of all constructors.
  private transient int[] _ats;// Array of aspect types
  private transient int _lasttype; // Type of last type to index conversion
  private transient int _lastindex; // Index of last type to index conversion

  /** Constructor that takes a result in the form of AspectValues (NON-PHASED).
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspectvalues  The AspectValues(can be aspectvalue subclasses) that represent the results.  
   * @note Prior to Cougaar 10.0, there was a similar constructor which took an int[] and double[] instead of 
   * the current AspectValue[].  This change is required because most ApectValue types
   * are not longer represented by int/double pairs.  This constructor may be made 
   * private at some point in the future.
   */
  public AllocationResult(double rating, boolean success, AspectValue[] aspectvalues) {
    isSuccess = success;
    confrating = (float) rating;
    avResults = cloneAndCheckAVV(aspectvalues);
    phasedavrs = null;
    clearMemos();
  }

  /** Factory that takes a result in the form of AspectValues (NON-PHASED).
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param avs  The AspectValues(can be aspectvalue subclasses) that represent the results.  
   */
  public static AllocationResult newAllocationResult(double rating, boolean success, AspectValue[] avs) {
    return new AllocationResult(rating, success, avs);
  }

  /** @deprecated Use #AllocationResult(double,boolean,AspectValue[]) instead because
   * AspectValues are not all describable by double values.
   **/
  public AllocationResult(double rating, boolean success, int[] keys, double[] values) {
    this(rating, success, convertToAVA(keys,values));
  }

  /** Simple Constructor for a PHASED result
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param allresults  An Enumeration of either AspectValue[]s or Collection<AspectValue>s.
   * @note Prior to Cougaar 10.0, this constructor took an int[] and double[] instead of 
   * the current AspectValue[].  This change is required because most ApectValue types
   * are not longer represented by int/double pairs.
   * @deprecated Use #AllocationResult(double, boolean, AspectValue[], Collection) instead.
   */
  public AllocationResult(double rating, boolean success, AspectValue[] rollupavs, Enumeration allresults)
  {
    isSuccess = success;
    confrating = (float) rating;
    avResults = cloneAndCheckAVV(rollupavs);
    phasedavrs = copyPhasedResults(allresults);
    clearMemos();
  }

  /** @deprecated Use #AllocationResult(double,boolean,AspectValue[],Collection) instead because
   * AspectValues are not all describable by double values.
   **/
  public AllocationResult(double rating, boolean success, int[] keys, double[] values, Enumeration allresults) {
    this(rating, success, convertToAVA(keys,values), allresults);
  }


  /** Constructor that takes a PHASED result in the form of AspectValues.
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param phasedresults  A Collections of the phased results. The Collection should contain
   * one Collection or AspectValue[] of AspectValues for each phase of the results.  
   * @note The associated factory is preferred as the constructor may be made private in
   * a future version.
   */
  public AllocationResult(double rating, boolean success, AspectValue[] rollupavs, Collection phasedresults) {
    isSuccess = success;
    confrating = (float) rating;
    avResults = cloneAndCheckAVV(rollupavs);
    phasedavrs = copyPhasedResults(phasedresults);
    clearMemos();
  }

  /** AllocationResult factory that takes a PHASED result in the form of AspectValues.
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param phasedresults  A Collections of the phased results. The Collection should contain
   * one Collection or AspectValue[] of AspectValues for each phase of the results.  
   */
  public static AllocationResult newAllocationResult(double rating, boolean success, AspectValue[] rollupavs, Collection phasedresults) {
    return new AllocationResult(rating, success, rollupavs, phasedresults);
  }

  /**
   * Construct a merged AllocationResult containing AspectValues from
   * two AllocationResults. If both arguments have the same aspects,
   * the values from the first (dominant) result are used. The result
   * is never phased.
   * @note The associated factory is preferred as the constructor may be made private in
   * a future version.
   **/
  public AllocationResult(AllocationResult ar1, AllocationResult ar2) {
    assert isAVVValid(ar1.avResults);
    assert isAVVValid(ar2.avResults);

    int len1 = ar1.avResults.length;
    int len2 = ar2.avResults.length;
    List mergedavs = new ArrayList(len1 + len2);
  outer:
    for (int i = 0; i < len2; i++) {
      AspectValue av2 = ar2.avResults[i];
      int aspectType = av2.getAspectType();
      for (int j = 0; j < len1; j++) {
        if (aspectType == ar1.avResults[j].getAspectType()) {
          continue outer;       // Already have this AspectType
        }
      }
      mergedavs.add(av2);
    }
    mergedavs.addAll(Arrays.asList(ar1.avResults));
    int nAspects = mergedavs.size();
    avResults = (AspectValue[]) mergedavs.toArray(new AspectValue[nAspects]);
    confrating = (ar1.confrating * len1 + ar2.confrating * (nAspects - len1)) / nAspects;
    phasedavrs = null;

    String[] ar1_auxqueries = ar1.currentAuxQueries();
    boolean is_shared = false;
    if (ar1_auxqueries != null) {
      auxqueries = ar1_auxqueries;
      is_shared = true;
    }
    String[] ar2_auxqueries = ar2.currentAuxQueries();
    if (ar2_auxqueries != null) {
      if (auxqueries == null) {
        auxqueries = new String[AQTYPE_COUNT];
      }
      for (int i = 0; i < AQTYPE_COUNT; i++) {
        if (auxqueries[i] == null &&
            ar2_auxqueries[i] != null) {
          if (is_shared) {
            // copy-on-write, so we must clone ar1_auxqueries
            // before modifying it
            auxqueries = (String[]) auxqueries.clone();
            is_shared = false;
          }
          auxqueries[i] = ar2_auxqueries[i];
        }
      }
    }
    isSuccess = ar1.isSuccess() || ar2.isSuccess();
    clearMemos();
  }

  /**
   * Construct a merged AllocationResult containing AspectValues from
   * two AllocationResults. If both arguments have the same aspects,
   * the values from the first (dominant) result are used. The result
   * is never phased.
   **/
  public static AllocationResult newAllocationResult(AllocationResult ar1, AllocationResult ar2) {
    return new AllocationResult(ar1, ar2);
  }

  public Object clone() {
    return new AllocationResult(this);
  }

  private AllocationResult(AllocationResult ar) {
    confrating = ar.confrating;
    isSuccess = ar.isSuccess;
    avResults = (AspectValue[]) ar.avResults.clone();
    if (ar.phasedavrs == null) {
      phasedavrs = null;
    } else {
      phasedavrs = new ArrayList(ar.phasedavrs.size());
      for (Iterator i = ar.phasedavrs.iterator(); i.hasNext(); ) {
        AspectValue[] av = (AspectValue[]) i.next();
        phasedavrs.add(av.clone());
      }
    }
    auxqueries = ar.currentAuxQueries();
    clearMemos();
  }


  private int getIndexOfType(int aspectType) {
    assert Thread.holdsLock(avResults);
    if (aspectType == _lasttype) return _lastindex; // Use memoized value
    for (int i = 0 ; i < avResults.length; i++) {
      if (avResults[i].getAspectType() == aspectType) return i;
    }
    return -1;
  }

  //AllocationResult interface implementation.

  /** Get the result with respect to a given AspectType. 
   * If the AllocationResult is phased, this method will return
   * the summary value of the given AspectType.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @param aspectType
   * @return double The result of a given dimension. For example, 
   * getValue(AspectType.START_TIME) returns the Task start time.
   * Note : results are not required to contain data in each dimension - 
   * check the array of defined aspecttypes or ask if a specific
   * dimension is defined.  If there is a request for a value of an
   * undefined aspect, an IllegalArgumentException will be thrown.
   * @see org.cougaar.planning.ldm.plan.AspectType
   */
  public double getValue(int aspectType) {
    synchronized (avResults) {
      if (_lasttype == aspectType) 
        return avResults[_lastindex].getValue(); // return memoized value
      int i = getIndexOfType(aspectType);
      if (i >= 0)
        return avResults[i].getValue();
    }
    // didn't find it.
    throw new IllegalArgumentException("AllocationResult.getValue(int "
                                       + aspectType
                                       + ") - The AspectType is not defined by this Result.");
  }


  /** Get the AspectValue of the result with the specified type **/
  public AspectValue getAspectValue(int aspectType) {
    synchronized (avResults) {
      if (_lasttype == aspectType) 
        return avResults[_lastindex];
      int i = getIndexOfType(aspectType);
      if (i >= 0)
        return avResults[i];
    }
    // didn't find it.
    throw new IllegalArgumentException("AllocationResult.getAspectValue(int "
                                       + aspectType
                                       + ") - The AspectType is not defined by this Result.");
  }

  /** Quick check to see if one aspect is defined as opposed to
    * looking through the AspectType array.
    * @param aspectType  The aspect you are checking
    * @return boolean Represents whether this aspect is defined
    * @see org.cougaar.planning.ldm.plan.AspectType
    */
  public boolean isDefined(int aspectType) {
    synchronized (avResults) {
      int i = getIndexOfType(aspectType);
      if (i >= 0) {
        _lasttype = aspectType;
        _lastindex = i; // memoize lookup
        return true;
      }
    }
    return false;
  }
    
          
  /** @return boolean Represents whether or not the allocation 
   * was a success. If any Constraints were violated by the 
   * allocation, then the isSuccess() method returns false 
   * and the Plugin that created the subtask should
   * recognize this event. The Expander may re-expand, change the 
   * Constraints or Preferences, or indicate failure to its superior. 
   * The AspectValues of a failed allocation may be set by the Allocator
   * with values that would be more likely to be successful. 
   * The Expander can use these new values as suggestions when 
   * resetting the Preferences
   */
  public boolean isSuccess() {
    return isSuccess;
  }

  /** @return boolean Represents whether or not the allocation
   * result is phased.
   */
  public boolean isPhased() {
    return phasedavrs != null;
  }

  private void clearMemos() {
    synchronized (avResults) {
      _ats = null;
      _lasttype=-1;
      _lastindex=-1;
    }
  }

  /** A Collection of AspectTypes representative of the type and
   * order of the aspects in each the result.
   * @return int[]  The array of AspectTypes
   * @see org.cougaar.planning.ldm.plan.AspectType   
   */
  public int[] getAspectTypes() {
    synchronized (avResults) {
      if (_ats != null) return _ats;
      _ats = new int[avResults.length];
      for (int i = 0; i < avResults.length; i++) {
        _ats[i] = avResults[i].getAspectType();
      }
      return _ats;
    }
  }
  
  /** A collection of doubles that represent the result for each
   * AspectType.  If the result is phased, the results are 
   * summarized.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return double[]
   */
  public double[] getResult() {
    return convertToDouble(avResults);
  }

  private double[] convertToDouble(AspectValue[] avs) {
    double[] result = new double[avs.length];
    for (int i = 0; i < avs.length; i++) {
      result[i] = avs[i].getValue();
    }
    return result;
  }
  
  /** A collection of AspectValues that represent the result for each
   * preference.  Note that subclasses of AspectValue such as
   * TypedQuantityAspectValue may be used.  If this was not
   * defined through a constructor, one will be built from the result
   * and aspecttype arrays.  In this case only true AspectValue
   * objects will be build (no subclasses of AspectValues).
   * The AspectValues of a failed allocation may be set by the Allocator
   * with values that would be more likely to be successful. 
   * The Expander can use these new values as suggestions when 
   * resetting the Preferences.
   * @note Will always return a new AspectValue[]
   **/
  public AspectValue[] getAspectValueResults() {
    return (AspectValue[]) avResults.clone();
  }
        
  /** A collection of arrays that represents each phased result.
   * If the result is not phased, use AllocationResult.getResult()
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return Enumeration<AspectValue[]> 
   */  
  public Enumeration getPhasedResults() {
    if (!isPhased()) throw new IllegalArgumentException("Not phased");
    return new Enumeration() {
      Iterator iter = phasedavrs.iterator();
      public boolean hasMoreElements() {
        return iter.hasNext();
      }
      public Object nextElement() {
        AspectValue[] avs = (AspectValue[]) iter.next();
        return convertToDouble(avs);
      }
    };
  }
  
  /** A List of Lists that represent each phased result in the form
   * of AspectValues.
   * If the result is not phased, use getAspectValueResults()
   * @return A List<AspectValue[]>. If the AllocationResult is not phased, will return null.
   */
  public List getPhasedAspectValueResults() {
    if (phasedavrs == null) {
      return null;
    } else {
      return new ArrayList(phasedavrs);
    }
  }
        
  /** @return double The confidence rating of this result. */
  public double getConfidenceRating() {
    return confrating;
  }
  
  /** Return the String representing the auxilliary piece of data that 
   *  matches the query type given in the argument.  
   *  @param aqtype  The AuxiliaryQueryType you want the data for.
   *  @return String  The string representing the data matching the type requested 
   *  Note: may return null if nothing was defined
   *  @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
   *  @throws IllegalArgumentException if the int passed in as an argument is not a defined
   *  AuxiliaryQueryType
   **/
  public String auxiliaryQuery(int aqtype) {
    if ( (aqtype < 0) || (aqtype > LAST_AQTYPE) ) {
      throw new IllegalArgumentException("AllocationResult.auxiliaryQuery(int) expects an int "
        + "that is represented in org.cougaar.planning.ldm.plan.AuxiliaryQueryType");
    }
    synchronized (avResults) {
      return (auxqueries == null ? null : auxqueries[aqtype]);
    }
  }
  
  
  //NewAllocationResult interface implementations
  
  /** Set a single AuxiliaryQueryType and its data (String).
   *  @param aqtype The AuxiliaryQueryType
   *  @param data The string associated with the AuxiliaryQueryType
   *  @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
   **/
  public void addAuxiliaryQueryInfo(int aqtype, String data) {
    if ( (aqtype < 0) || (aqtype > LAST_AQTYPE) ) {
      throw new IllegalArgumentException("AllocationResult.addAuxiliaryQueryInfo(int, String) expects an int "
        + "that is represented in org.cougaar.planning.ldm.plan.AuxiliaryQueryType");
    }
    synchronized (avResults) {
      if (auxqueries == null) {
        auxqueries = new String[AQTYPE_COUNT];
      } else {
        // copy-on-write, to avoid cloning and minimize locking
        // overheads, assuming readers far outweight writers
        auxqueries = (String[]) auxqueries.clone();
      }
      auxqueries[aqtype] = data;
    }
  }

  /** get the current (immutable) shapshot of the auxqueries */
  private String[] currentAuxQueries() {
    synchronized (avResults) {
      return auxqueries;
    }
  }
  
  /** clone the array and filteri out nulls. */
  private static AspectValue[] cloneAndCheckAVV(
      AspectValue[] aspectvalues) {
    AspectValue[] ret = (AspectValue[]) aspectvalues.clone();
    assert isAVVValid(ret);
    // must clearMemos()
    return ret;
  }

  private static boolean isAVVValid(AspectValue[] av) {
    for (int i = 0; i < av.length; i++) {
      if (av[i] == null) return false;
    }
    return true;
  }
        
  /** copy the phased results in an Enumeration of AspectValue[] */
  private static ArrayList copyPhasedResults(Enumeration theResults) {
    ArrayList ret = new ArrayList();
    while (theResults.hasMoreElements()) {
      ret.add(convertAVO(theResults.nextElement()));
    }
    ret.trimToSize();
    return ret;
  }
  
  /** 
   * copy the results in a collection of AspectValue[] representing
   * each phase of the result
   */
  private static ArrayList copyPhasedResults(Collection theResults) {
    int n = theResults.size();
    ArrayList ret = new ArrayList(n);
    if (theResults instanceof List) {
      List trl = (List) theResults;
      for (int i=0; i<n; i++) {
        ret.add(convertAVO(trl.get(i)));
      }
    } else {
      for (Iterator it = theResults.iterator(); it.hasNext(); ) {
        ret.add(convertAVO(it.next()));
      }
    }
    return ret;
  }

  /** Convert an logical array of AspectValues to an actual AspectValue[], if needed **/
  // fixes bug 1968
  private static AspectValue[] convertAVO(Object o) {
    if (o instanceof AspectValue[]) {
      return (AspectValue[]) o;
    } else if (o instanceof Collection) {
      return (AspectValue[]) ((Collection)o).toArray(new AspectValue[((Collection)o).size()]);
    } else {
      throw new IllegalArgumentException("Each element of a PhaseResult must be in the form of an AspectValue[] or a Collection of AspectValues, but got: "+o);
    }
  }
    
  /** checks to see if the AllocationResult is equal to this one.
     */
  public boolean isEqual(AllocationResult that) {
    if (this == that) return true; // quick success
    if (that == null) return false; // quick fail
    if (!(this.isSuccess() == that.isSuccess() &&
          this.isPhased() == that.isPhased() &&
          this.getConfidenceRating() == that.getConfidenceRating())) {
      return false;
    }
       
    //check the real stuff now!
    //check the aspect types
    //check the summary results
    if (!AspectValue.nearlyEquals(this.avResults, that.avResults)) return false;
    // check the phased results
    if (isPhased()) {
      Iterator i1 = that.phasedavrs.iterator();
      Iterator i2 = this.phasedavrs.iterator();
      while (i1.hasNext()) {
        if (!i2.hasNext()) return false;
        if (!AspectValue.nearlyEquals((AspectValue[]) i1.next(), (AspectValue[]) i2.next())) return false;
      }
      if (i2.hasNext()) return false;
    }

    // check the aux queries
    
    String[] taux = that.currentAuxQueries();
    synchronized (avResults) {
      if (auxqueries != taux) {
        if (!Arrays.equals(taux, auxqueries)) return false;
      }
    }

    // must be equals...
    return true;
  }

  // added to support AllocationResultBeanInfo

  public String[] getAspectTypesAsArray() {
    String[] ret = new String[avResults.length];
    for (int i = 0; i < ret.length; i++)
      ret[i] =  AspectValue.aspectTypeToString(avResults[i].getAspectType());
    return ret;
  }

  public String getAspectTypeFromArray(int i) {
    if (i < 0 || i >= avResults.length)
      throw new IllegalArgumentException("AllocationResult.getAspectType(int " + i + " not defined.");
    return AspectValue.aspectTypeToString(avResults[i].getAspectType());
  }

  public String[] getResultsAsArray() {
    String[] resultStrings = new String[avResults.length];
    for (int i = 0; i < resultStrings.length; i++) {
      resultStrings[i] = getResultFromArray(i);
    }
    return resultStrings;
  }

  public String getResultFromArray(int i) {
    if (i < 0 || i >= avResults.length)
      throw new IllegalArgumentException("AllocationResult.getAspectType(int " + i + " not defined.");
    int type = avResults[i].getAspectType();
    double value = avResults[i].getValue();
    if (type == AspectType.START_TIME || 
	type == AspectType.END_TIME) {
      Date d = new Date((long) value);
      return d.toString();
    } else {
      return String.valueOf(value);
    }
  }

  /**
   * Return phased results.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return an array of an array of doubles
   **/
  public double[][] getPhasedResultsAsArray() {
    int len = (isPhased() ? phasedavrs.size() : 0);
    double[][] d = new double[len][];
    for (int i = 0; i < len; i++) {
      AspectValue[] avs = (AspectValue[]) phasedavrs.get(i);
      d[i] = convertToDouble(avs);
    }
    return d;
  }

  /**
   * Return a particular phase of a phased result as an array of doubles.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return the i-th phase as double[]
   **/
  public double[] getPhasedResultsFromArray(int i) {
    if (!isPhased()) return null;
    if (i < 0 || i >= phasedavrs.size()) return null;
    return convertToDouble((AspectValue[]) phasedavrs.get(i));
  }
    
  private void appendAVS(StringBuffer buf, AspectValue[] avs) {
    buf.append('[');
    for (int i = 0; i < avs.length; i++) {
      if (i > 0) buf.append(",");
      buf.append(avs[i]);
    }
    buf.append(']');
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("AllocationResult[isSuccess=");
    buf.append(isSuccess);
    buf.append(", confrating=");
    buf.append(confrating);
    appendAVS(buf, avResults);
    if (isPhased()) {
      for (int i = 0, n = phasedavrs.size(); i < n; i++) {
        buf.append("Phase ");
        buf.append(i);
        buf.append("=");
        appendAVS(buf, (AspectValue[]) phasedavrs.get(i));
      }
    }
    buf.append("]");
    return buf.toString();
  }


  /*
   * The AspectValues of a failed allocation may be set by the Allocator
   * with values that would be more likely to be successful. 
   * The Expander can use these new values as suggestions when 
   * resetting the Preferences. This method tells which AspectValues
   * have been changed or added by the Allocator.
   *
   * @param prefs the preference from the task corresponding to this allocation
   * @return the aspect values in the allocation that are different
   * from the the preference.
   */
  public AspectValue[] difference(Preference[] prefs) {
    AspectValue[] diffs = new AspectValue[avResults.length];
    int diffCount = 0;

    for (int i=0; i<avResults.length; i++) {
      boolean found = false;
      AspectValue prefAV = null;

      for (int j=0; j<prefs.length; j++) {
  	prefAV = prefs[j].getScoringFunction().getBest().getAspectValue();
	if (prefAV.getAspectType() == avResults[i].getAspectType()) {
	  found = true;
	  break;
	}
      }
      if (!found) {
	diffs[diffCount++] = avResults[i];
      } else if (prefAV.getValue() != avResults[i].getValue()) {
	diffs[diffCount++] = avResults[i];
      }
    }
    
    AspectValue[] returnDiff = new AspectValue[diffCount];
    for (int i=0; i<diffCount; i++) {
      returnDiff[i] = diffs[i];
    }

    return returnDiff;
  }

  /** Utility method to help conversion of old code to new usage
   **/
  public static AspectValue[] convertToAVA(int[] types, double[] values) {
    int l = types.length;
    AspectValue[] ava = new AspectValue[l];
    for (int i=0; i<l; i++) {
      ava[i] = AspectValue.newAspectValue(types[i], values[i]);
    }
    return ava;
  }

  private void readObject(ObjectInputStream ois)
    throws IOException, ClassNotFoundException
  {
    ois.defaultReadObject();
    clearMemos();
  }
}

