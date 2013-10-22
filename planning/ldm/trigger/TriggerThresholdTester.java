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


package org.cougaar.planning.ldm.trigger;

/**
 * Abstract Threshold trigger tester to fire if a given
 * computed parameter value exceeds a given threshold. 
 * Comparison sense of greater than/less than is settable.
 */

public abstract class TriggerThresholdTester implements TriggerTester {
  
  private double my_threshold;
  private boolean my_fire_if_exceeds;

  // Constructor : save threshold and fire_if_exceeds flag
  TriggerThresholdTester(double threshold, boolean fire_if_exceeds) 
  { 
    my_threshold = threshold; 
    my_fire_if_exceeds = fire_if_exceeds; 
  }

  // Abstract method to compute threshold value
  public abstract double ComputeValue(Object[] objects);

  // Tester Test function : Compare compare computed value with threshold
  public boolean Test(Object[] objects) { 
    double value = ComputeValue(objects);
    if (my_fire_if_exceeds) 
      return value > my_threshold;
    else
      return value < my_threshold;
  }


  

}


