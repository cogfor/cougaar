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

package org.cougaar.core.persist;

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.service.PersistenceMetricsService;

/**
 * {@link PersistenceMetricsService} implementation.
 */
public class PersistenceMetricsServiceImpl implements PersistenceMetricsService {

  private PersistenceMetricImpl fullAverageMetric = new PersistenceMetricImpl();
  private PersistenceMetricImpl deltaAverageMetric = new PersistenceMetricImpl();
  private PersistenceMetricImpl allAverageMetric = new PersistenceMetricImpl();
  private int base = 0;
  private int size = 0;
  private Metric[] metrics = new Metric[MAX_METRICS];

  void addMetric(PersistenceMetricsService.Metric metric) {
    synchronized (metrics) {
      if (size >= metrics.length) {
        metrics[base++] = metric;
        if (base >= metrics.length) base = 0;
      } else {
        metrics[size++] = metric;
      }
      allAverageMetric.average(metric);
      if (metric.isFull()) {
        fullAverageMetric.average(metric);
      } else {
        deltaAverageMetric.average(metric);
      }
    }
  }

  /**
   * Get all retained metrics. The maximum number retained is
   * currently a constant MAX_METRICS
   */
  public Metric[] getAll(int which) {
    synchronized (metrics) {
      if (which == ALL) {
        Metric[] result = new Metric[size];
        System.arraycopy(metrics, base, result, 0, size - base);
        System.arraycopy(metrics, 0, result, size - base, base);
        return result;
      } else {
        List result = new ArrayList(size);
        boolean wantFull = which == FULL;
        for (int i = 0; i < size; i++) {
          Metric metric = metrics[(base + i) % metrics.length];
          if (metric.isFull() == wantFull) {
            result.add(metric);
          }
        }
        return (Metric[]) result.toArray(new Metric[result.size()]);
      }
    }
  }

  /**
   * Get the average of all metrics ever generated including the ones
   * that have been dropped due to exceeding MAX_METRICS
   */
  public Metric getAverage(int which) {
    switch (which) {
    case FULL:
      return fullAverageMetric;
    case DELTA:
      return deltaAverageMetric;
    case ALL:
      return allAverageMetric;
    }
    return null;
  }

  public int getCount(int which) {
    switch (which) {
    case FULL:
      return fullAverageMetric.getCount();
    case DELTA:
      return deltaAverageMetric.getCount();
    case ALL:
      return allAverageMetric.getCount();
    }
    return 01;
  }
}

