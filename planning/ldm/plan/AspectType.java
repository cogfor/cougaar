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

import java.util.HashMap;

/** Constant names for different 'score' dimensions in which to report
 * allocation consequences.
 **/
public interface AspectType {
  
  /** Undefined value for an aspect type **/
  int UNDEFINED = -1;

  /** Start time of given Task **/
  int START_TIME = 0;
    
  /** End time of given Task **/
  int END_TIME = 1;
  
  /** Duration time of the Task **/
  int DURATION = 2;
    
  /** Cost (in $) of allocating given Task **/
  int COST = 3;
    
  /** Probability of loss of assets associated with allocation **/
  int DANGER = 4;
    
  /** Probability of failure of the Mission **/
  int RISK = 5;
    
  /** Quantities associated with allocation (number of elements sourced, e.g.) **/
  int QUANTITY = 6;
  
  /** For repetitive tasks - specify the amount of time(milliseconds) 
   * in between deliveries 
   **/
  int INTERVAL = 7;
  
  /**For repetitive tasks the total sum quantity of item for the time span **/
  int TOTAL_QUANTITY = 8;
  
  /** For repetitive tasks the total number of shipments requested across 
   * the time span.
   * This should be used in association with the interval aspect.
   **/
  int TOTAL_SHIPMENTS = 9;

  /**   **/
  int CUSTOMER_SATISFACTION = 10;
  
  /** Used to represent an Asset/Quantity relationship
   * @see org.cougaar.planning.ldm.plan.TypedQuantityAspectValue
   **/
  int TYPED_QUANTITY = 11;         
  
  /** The extent to which a task has been satisfactorily completed **/
  int READINESS = 12;

  int _LAST_ASPECT = READINESS;
  int _ASPECT_COUNT = _LAST_ASPECT+1;

  int[] _STANDARD_ASPECTS = {0,1,2,3,4,5,6,7,8,9,10,11,12};
  
  /** Commonly-used aspect types: START_TIME, END_TIME, COST, QUANTITY **/
  int[] CommonAspects = { START_TIME, END_TIME, COST, QUANTITY };

  /** Time Aspects: START_TIME, END_TIME **/
  int[] TimeAspects = { START_TIME, END_TIME };

  // extended AspectTypes that are NOT handled by default
  // AllocationResultAggregators or AllocationResultDistributors
  
  /** The point of debarkation of a task **/
  int POD = 13;         

  /** The time at which a task should arrive at the POD **/
  int POD_DATE = 14;

  /** The number of core-defined aspect types **/
  int N_CORE_ASPECTS = 15;

  String[] ASPECT_STRINGS = {
    "START_TIME",
    "END_TIME",
    "DURATION", 
    "COST",
    "DANGER",
    "RISK",
    "QUANTITY",
    "INTERVAL",
    "TOTAL_QUANTITY",
    "TOTAL_SHIPMENTS",
    "CUSTOMER_SATISFACTION",
    "TYPED_QUANTITY",
    "READINESS",
    "POD",
    "POD_DATE"
  };

  interface Factory {
    /** the key of the AspectType **/
    int getKey();
    /** The name of the AspectType **/
    String getName();
    /** The factory to use for creating AspectValues of the AspectType. **/
    AspectValue newAspectValue(Object o);
  }
 
  /** Undefined - Illegal to use **/
  Factory Undefined = new Factory () { 
      public int getKey() { return -1; }
      public String getName() { return "Undefined"; }
      public AspectValue newAspectValue(Object o) { throw new IllegalArgumentException("Cannot make Undefined AspectValues"); }
    };

  /** Start time of given Task **/
  Factory StartTime = new Factory () { 
      public int getKey() { return START_TIME; }
      public String getName() { return "START_TIME"; }
      public AspectValue newAspectValue(Object o) { return TimeAspectValue.create(START_TIME,o); }
    };
  /** End time of given Task **/
  Factory EndTime = new Factory () { 
      public int getKey() { return END_TIME; }
      public String getName() { return "END_TIME"; }
      public AspectValue newAspectValue(Object o) { return TimeAspectValue.create(END_TIME,o); }
    };
  /** (requested) Duration of a task **/
  Factory Duration = new Factory () { 
      public int getKey() { return DURATION; }
      public String getName() { return "DURATION"; }
      public AspectValue newAspectValue(Object o) { return TimeAspectValue.create(DURATION,o); }
    };
  /** Cost (in $) of allocating given Task **/
  Factory Cost = new Factory () { 
      public int getKey() { return COST; }
      public String getName() { return "COST"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(COST,o); }
    };
  /** Probability of loss of assets associated with allocation **/
  Factory Danger = new Factory () { 
      public int getKey() { return DANGER; }
      public String getName() { return "DANGER"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(DANGER,o); }
    };
  /** Probability of failure of the Mission **/
  Factory Risk = new Factory () { 
      public int getKey() { return RISK; }
      public String getName() { return "RISK"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(RISK,o); }
    };
  /** Quantities associated with allocation (number of elements sourced, e.g.) **/
  Factory Quantity = new Factory () { 
      public int getKey() { return QUANTITY; }
      public String getName() { return "QUANTITY"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(QUANTITY,o); }
    };
  /** For repetitive tasks - specify the amount of time(milliseconds) 
   * in between deliveries 
   **/
  Factory Interval = new Factory () { 
      public int getKey() { return INTERVAL; }
      public String getName() { return "INTERVAL"; }
      public AspectValue newAspectValue(Object o) { return TimeAspectValue.create(INTERVAL,o); }
    };
  /**For repetitive tasks the total sum quantity of item for the time span **/
  Factory TotalQuantity = new Factory () { 
      public int getKey() { return TOTAL_QUANTITY; }
      public String getName() { return "TOTAL_QUANTITY"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(TOTAL_QUANTITY,o); }
    };
  /** For repetitive tasks the total number of shipments requested across 
   * the time span.
   * This should be used in association with the interval aspect.
   **/
  Factory TotalShipments = new Factory () { 
      public int getKey() { return TOTAL_SHIPMENTS; }
      public String getName() { return "TOTAL_SHIPMENTS"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(TOTAL_SHIPMENTS,o); }
    };
  /**   **/
  Factory CustomerSatisfaction = new Factory () { 
      public int getKey() { return CUSTOMER_SATISFACTION; }
      public String getName() { return "CUSTOMER_SATISFACTION"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(CUSTOMER_SATISFACTION,o); }
    };
  /** Used to represent an Asset/Quantity relationship
   * @see org.cougaar.planning.ldm.plan.TypedQuantityAspectValue
   **/
  Factory TypedQuantity = new Factory () { 
      public int getKey() { return TYPED_QUANTITY; }
      public String getName() { return "TYPED_QUANTITY"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(TYPED_QUANTITY,o); }
    };
  /** The extent to which a task has been satisfactorily completed **/
  Factory Readiness = new Factory () { 
      public int getKey() { return READINESS; }
      public String getName() { return "READINESS"; }
      public AspectValue newAspectValue(Object o) { return FloatAspectValue.create(READINESS,o); }
    };
    
  /** The point of debarkation of a task **/
  Factory Pod = new Factory () { 
      public int getKey() { return POD; }
      public String getName() { return "POD"; }
      public AspectValue newAspectValue(Object o) { return AspectLocation.create(POD,o); }
    };
  /** The time at which a task should arrive at the POD **/
  Factory PodDate = new Factory () { 
      public int getKey() { return POD_DATE; }
      public String getName() { return "POD_DATE"; }
      public AspectValue newAspectValue(Object o) { return TimeAspectValue.create(POD_DATE,o); }
    };
    
  /** Array of the "standard" AspectValue Factories.  This should match the other
   * arrays in this class.
   **/
  Factory[] AspectFactories = {StartTime,
                               EndTime,
                               Duration,
                               Cost,
                               Danger,
                               Risk,
                               Quantity,
                               Interval,
                               TotalQuantity,
                               TotalShipments,
                               CustomerSatisfaction,
                               TypedQuantity,
                               Readiness,
                               Pod,
                               PodDate};


  /** Simple class for registering AspectValue Factories **/
  class Registry {
    private HashMap table = new HashMap(64);

    public Registry(Factory[] fs) {
      for (int i=0; i<fs.length; i++) {
        Factory f = fs[i];
        assert (f.getKey() == i);
        registerFactory(f);
      }
    }

    public void registerFactory(Factory f) {
      String s = f.getName();
      Integer i = new Integer(f.getKey());
      synchronized (table) {
        if (table.get(s) != null ||
            table.get(i) != null) {
          throw new IllegalArgumentException("Cannot register AspectType "+f+" as "+s+"("+i+")");
        }
        table.put(s,f);
        table.put(i,f);
      }
    }

    public Factory get(String name) {
      synchronized (table) {
        return (Factory) table.get(name);
      }
    }
    public Factory get(Integer key) {
      {
        int i = key.intValue();
        if (i >= 0 && i < AspectFactories.length) {
          return AspectFactories[i];
        }
      }
      synchronized (table) {
        return (Factory) table.get(key);
      }
    }
    public Factory get(int key) {
      if (key >= 0 && key < AspectFactories.length) {
        return AspectFactories[key];
      }
      synchronized (table) {
        return (Factory) table.get(new Integer(key));
      }
    }
  }

  /** Registry of AspectValue factories **/
  Registry registry = new Registry(AspectFactories);
}
