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

package org.cougaar.planning.plugin.completion;

import java.io.Serializable;
import java.text.DecimalFormat;

import org.cougaar.core.mts.MessageAddress;

class Laggard implements Comparable, Serializable {
  private long timestamp = System.currentTimeMillis();
  private MessageAddress agent;
  private double blackboardCompletion;
  private double cpuConsumption;
  private boolean isLaggard;
//   private Map verbCounts = new HashMap();
  Laggard(MessageAddress me,
          double blackboardCompletion,
          double cpuConsumption,
          boolean isLaggard)
  {
    this.agent = me;
    this.blackboardCompletion= blackboardCompletion;
    this.cpuConsumption = cpuConsumption;
    this.isLaggard = isLaggard;
  }

  Laggard(MessageAddress me, Laggard oldLaggard) {
    agent = me;
    if (oldLaggard != null) {
      blackboardCompletion = oldLaggard.blackboardCompletion;
      cpuConsumption = oldLaggard.cpuConsumption;
      isLaggard = oldLaggard.isLaggard;
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  public MessageAddress getAgent() {
    return agent;
  }

  public double getBlackboardCompletion() {
    return blackboardCompletion;
  }

  public double getCPUConsumption() {
    return cpuConsumption;
  }

  public boolean isLaggard() {
    return isLaggard;
  }

//   public Map getVerbCounts() {
//     return verbCounts;
//   }

  public int compareTo(Object o) {
    if (this == o) return 0;
    Laggard that = (Laggard) o;
    if (isLaggard()) {
      if (!that.isLaggard()) return -1;
    } else {
      if (that.isLaggard()) return 1;
      return 1;
    }
    double diff = (this.blackboardCompletion - that.blackboardCompletion +
                   that.cpuConsumption - this.cpuConsumption);
    if (diff < 0.0) return -1;
    if (diff > 0.0) return 1;
    return this.agent.toString().compareTo(that.agent.toString());
  }

  private static final DecimalFormat format = new DecimalFormat("0.00");

  public String toString() {
    return
      "Laggard("
      + agent + ","
      + isLaggard + ","
      + format.format(blackboardCompletion) + ","
      + format.format(cpuConsumption) + ")@"
      + CompletionSourcePlugin.formatDate(timestamp);
  }
}
