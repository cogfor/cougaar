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

import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.VariableEvaluator;

public abstract class MetricsCondition implements Condition {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
public static final java.lang.String METRICS_PREFIX = "metrics:";
  public static final java.lang.String METRICS_DOUBLE_PREFIX = METRICS_PREFIX + "double:";
  public static final java.lang.String METRICS_STRING_PREFIX = METRICS_PREFIX + "string:";
  public static final java.lang.String METRICS_INTEGER_PREFIX = METRICS_PREFIX + "int:";
  public static final java.lang.String METRICS_LONG_PREFIX = METRICS_PREFIX + "long:";
  public static final java.lang.String METRICS_BOOLEAN_PREFIX = METRICS_PREFIX + "boolean:";

  private static final OMCRangeList ALL_BOOLEAN_RANGE_LIST =
    new OMCRangeList(new int[] {0, 1});

  private static final java.lang.Integer ONE = new java.lang.Integer(1);
  private static final java.lang.Integer ZERO = new java.lang.Integer(0);

  public static MetricsCondition create(java.lang.String name,
                                        MetricsService metricsService,
                                        VariableEvaluator variableEvaluator)
  {
    if (name.startsWith(METRICS_DOUBLE_PREFIX)) {
      return new Double(name, metricsService, variableEvaluator);
    }
    if (name.startsWith(METRICS_STRING_PREFIX)) {
      return new String(name, metricsService, variableEvaluator);
    }
    if (name.startsWith(METRICS_INTEGER_PREFIX)) {
      return new Integer(name, metricsService, variableEvaluator);
    }
    if (name.startsWith(METRICS_BOOLEAN_PREFIX)) {
      return new Boolean(name, metricsService, variableEvaluator);
    }
    throw new IllegalArgumentException("Unknown MetricsCondition type: " + name);
  }

  private java.lang.String name;
  private OMCRangeList allowedValues;
  private java.lang.String metricsPath;
  private MetricsService metricsService;
  private VariableEvaluator variableEvaluator;

  protected MetricsCondition(java.lang.String name,
                             MetricsService metricsService,
                             VariableEvaluator variableEvaluator,
                             OMCRangeList allowedValues,
                             java.lang.String prefix)
  {
    this.metricsService = metricsService;
    this.variableEvaluator= variableEvaluator;
    this.name = name;
    this.allowedValues = allowedValues;
    metricsPath = name.substring(prefix.length());
    Metric testMetric = getMetric();
    if (testMetric == null) {
      throw new IllegalArgumentException("Metric has no value:" + metricsPath);
    }
  }

  @Override
public java.lang.String toString() {
    return "MetricsCondition(" + getName() + ")";
  }

  protected Metric getMetric() {
    return metricsService.getValue(metricsPath, variableEvaluator);
  }

  public java.lang.String getName() {
    return name;
  }

  public OMCRangeList getAllowedValues() {
    return allowedValues;
  }

  public abstract Comparable getValue();

  public static class Double extends MetricsCondition {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public Double(java.lang.String name,
                  MetricsService metricsService,
                  VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_DOUBLE_RANGE_LIST, METRICS_DOUBLE_PREFIX);
    }

    protected Double(java.lang.String name,
                     MetricsService metricsService,
                     VariableEvaluator variableEvaluator,
                     java.lang.String prefix)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_DOUBLE_RANGE_LIST, prefix);
    }

    @Override
   public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.Double) return (java.lang.Double) rawValue;
      return new java.lang.Double(metric.doubleValue());
    }
  }

  public static class Default extends Double {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public Default(java.lang.String name,
                   MetricsService metricsService,
                   VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator, METRICS_DOUBLE_PREFIX);
    }
  }

  public static class Integer extends MetricsCondition {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public Integer(java.lang.String name,
                   MetricsService metricsService,
                   VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_INTEGER_RANGE_LIST, METRICS_INTEGER_PREFIX);
    }

    @Override
   public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.Integer) return (java.lang.Integer) rawValue;
      return new java.lang.Integer(metric.intValue());
    }
  }

  public static class Long extends MetricsCondition {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public Long(java.lang.String name,
                MetricsService metricsService,
                VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_LONG_RANGE_LIST, METRICS_LONG_PREFIX);
    }

    @Override
   public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.Long) return (java.lang.Long) rawValue;
      return new java.lang.Long(metric.longValue());
    }
  }

  public static class String extends MetricsCondition {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public String(java.lang.String name,
                  MetricsService metricsService,
                  VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_STRING_RANGE_LIST, METRICS_STRING_PREFIX);
    }

    @Override
   public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.String) return (java.lang.String) rawValue;
      return metric.stringValue();
    }
  }

  public static class Boolean extends MetricsCondition {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public Boolean(java.lang.String name,
                   MetricsService metricsService,
                   VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            ALL_BOOLEAN_RANGE_LIST, METRICS_BOOLEAN_PREFIX);
    }
    
    @Override
   public Comparable getValue() {
      Metric metric = getMetric();
      if (metric.booleanValue()) {
        return ONE;
      } else {
        return ZERO;
      }
    }
  }
}
