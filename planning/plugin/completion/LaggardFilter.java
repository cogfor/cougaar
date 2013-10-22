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


public class LaggardFilter {
  public static final long LAGGARD_UPDATE_INTERVAL = 15000L;
  public static final long NON_LAGGARD_UPDATE_INTERVAL = 120000L;

  private Laggard oldLaggard = null;
  private long updateTime;

  public boolean filter(Laggard newLaggard) {
    if (oldLaggard == newLaggard) return false;
    boolean result = filter(newLaggard.isLaggard(), newLaggard.getTimestamp());
    if (result) setOldLaggard(newLaggard);
    return result;
  }

  public boolean filter(boolean isLaggard, long timestamp) {
    return
      oldLaggard == null ||
      oldLaggard.isLaggard() != isLaggard ||
      timestamp > updateTime;
  }

  public void setOldLaggard(Laggard newLaggard) {
    oldLaggard = newLaggard;
    updateTime = oldLaggard.getTimestamp() +
      (oldLaggard.isLaggard()
       ? LAGGARD_UPDATE_INTERVAL
       : NON_LAGGARD_UPDATE_INTERVAL);
  }
}

