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

package org.cougaar.core.agent.service.alarm;

/** 
 * A Timer for real time ("system", or "wall clock" time).
 */
public class RealTimer extends Timer {
  public RealTimer() {}

  @Override
protected String getName() {
    return "RealTimer";
  }

  @Override
protected void report(Alarm alarm) {
    long now = currentTimeMillis();
    long at = alarm.getExpirationTime();
    if ((at+EPSILON)<now) {
      // if we're more then epsilon late, we'll warn
      if (log.isInfoEnabled()) {
        log.info("Alarm "+alarm+" is "+(now-at)+"ms late");
      }
    }
    super.report(alarm);
  }

  /* /////////////////////////////////////////////////////// 

  // point test 

  public static void main(String args[]) {
    // create a timer
    Timer timer = new RealTimer();
    timer.start();

    System.err.println("currentTimeMillis() = "+timer.currentTimeMillis());
    // test running advance
    timer.addAlarm(timer.createTestAlarm(5*1000));// 5 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec (again)
    timer.addAlarm(timer.createTestAlarm(20*1000));
    timer.addAlarm(timer.createTestAlarm(30*1000));
    timer.addAlarm(timer.createTestAlarm(40*1000));
    timer.addAlarm(timer.createTestAlarm(60*60*1000)); // 60 min
    timer.sleep(120*1000);      // wait 10 seconds      
    System.exit(0);
  }

  public void sleep(long millis) {
    try {
      synchronized(this) {
        this.wait(millis);
      }
    } catch (InterruptedException ie) {}
  }
    

  Alarm createTestAlarm(long delta) {
    return new TestAlarm(delta);
  }
  private class TestAlarm implements Alarm {
    long exp;
    public TestAlarm(long delta) { this.exp = currentTimeMillis()+delta; }
    public long getExpirationTime() {return exp;}
    public void expire() { System.err.println("Alarm "+exp+" expired.");}
    public String toString() { return "<"+exp+">";}
    public boolean cancel() {}  // doesn't support cancel
  }
  */
}
