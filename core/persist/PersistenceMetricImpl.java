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

import org.cougaar.core.service.PersistenceMetricsService;

/**
 * {@link org.cougaar.core.service.PersistenceMetricsService.Metric}
 * implementation.
 */
public class PersistenceMetricImpl implements PersistenceMetricsService.Metric {
  private String name;
  private long startTime, endTime, cpuTime, size;
  private boolean full;
  private Throwable failed;
  private PersistencePlugin plugin;
  private int count;

  PersistenceMetricImpl(String name,
                        long startTime, long endTime, long cpuTime,
                        long size, boolean full,
                        Throwable failed,
                        PersistencePlugin plugin)
  {
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
    this.cpuTime = cpuTime;
    this.size = size;
    this.full = full;
    this.failed = failed;
    this.plugin = plugin;
    this.count = 1;
  }

  PersistenceMetricImpl() {
  }

  void average(PersistenceMetricsService.Metric metric) {
    startTime += metric.getStartTime();
    endTime += metric.getEndTime();
    cpuTime += metric.getCpuTime();
    size += metric.getSize();
    count += 1;
  }

  public long getStartTime() {
    return count == 0 ? startTime : startTime / count;
  }

  public long getEndTime() {
    return count == 0 ? endTime : endTime / count;
  }

  public long getSize() {
    return count == 0 ? size : size / count;
  }

  public long getCpuTime() {
    return count == 0 ? cpuTime : cpuTime / count;
  }

  public boolean isFull() {
    return full;
  }

  public Throwable getException() {
    return failed;
  }

  public String getName() {
    return name;
  }

  public Class getPersistencePluginClass() {
    return plugin.getClass();
  }

  public String getPersistencePluginName() {
    return plugin.getName();
  }

  public int getPersistencePluginParamCount() {
    return plugin.getParamCount();
  }

  public String getPersistencePluginParam(int i) {
    return plugin.getParam(i);
  }

  public int getCount() {
    return count;
  }

  @Override
public String toString() {
    return (failed == null ? "Persisted " : "Failed ")
      + (full ? "full" : "delta")
      + name
      + ", "
      + size
      +" bytes in "
      + (endTime - startTime) + " ms"
      + ((cpuTime > 0L) ? (" using " + cpuTime) : "")
      + " ms cpu";
  }
}

