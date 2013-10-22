/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.service;

/**
 * This service provides metrics for recent persistence activity,
 * such as the most recent snapshot size in bytes.
 */
public interface PersistenceMetricsService {
  interface Metric {
    long getStartTime();
    long getEndTime();
    long getSize();
    long getCpuTime();
    boolean isFull();
    Throwable getException();
    String getName();
    Class getPersistencePluginClass();
    String getPersistencePluginName();
    int getPersistencePluginParamCount();
    String getPersistencePluginParam(int i);
  }

  /**
   * Designates that averaging should include only full
   * snapshots
   */
  static final int FULL = 1;

  /**
   * Designates that averaging should include only delta
   * snapshots
   */
  static final int DELTA = 2;

  /**
   * Designates that averaging should include all
   * snapshots
   */
  static final int ALL = 3;

  static final int MAX_METRICS = 100;

  /**
   * Get all retained metrics. The maximum number retained is
   * currently a constant MAX_METRICS
   * @param which one of the constants FULL, DELTA, ALL designating
   * which kind of snapshots should be included.
   */
  Metric[] getAll(int which);

  /**
   * Get the average of all metrics ever generated including the ones
   * that have been dropped due to exceeding MAX_METRICS
   * @param which one of the constants FULL, DELTA, ALL designating
   * which kind of snapshots should be averaged.
   */
  Metric getAverage(int which);

  /**
   * Get the count of all persistence snapshots taken (the denominator
   * of the average).
   * @return the count of all persistence snapshots taken.
   * @param which one of the constants FULL, DELTA, ALL designating
   * which kind of snapshots should be counted.
   */
  int getCount(int which);
}
