/*
 * <copyright>
 * 
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.tini;

import java.util.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.plugin.*;
import org.cougaar.microedition.shared.*;

import com.dalsemi.system.*;

/**
 * Plugin to control a motor controller chip using two spare ports on the TINI.
 * Pin 12 of the TINI must be connected to the IN1 port of the motor controller.
 * Pin 8  of the TINI must be connected to the IN2 port of the motor controller.
 * This Plugin responds to tasks with verb "Rotate" and preposition "motorName"
 * where motorName matches the "motorName" parameter given to this instance.
 * It looks for a preposition "Direction" with indirect object one of "Forward"
 * "Backward" or "Brake".
 */
public class TiniMotorPlugin extends PluginAdapter {

  UnaryPredicate getPred(final String myMotor) {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroTask) {
          MicroTask mt = (MicroTask)o;
          if (mt.getVerb().equals("Rotate")) {
            MicroPrepositionalPhrase mpp = findPreposition(mt, "motorName");
            return (mpp != null) && mpp.getIndirectObject().equals(myMotor);
          }
        }
        return false;
      }
    };
    return myPred;
  }
  private int sleepTime = 5000;
  private Subscription sub;
  private boolean iShouldDie = false;
  private String myMotor;
  private BitPort in1; // TX1 (12) Input #1
  private BitPort in2; // CTX (8) Input #2
  private boolean debugging;

  public void setupSubscriptions() {
    debugging = isDebugging();
    Hashtable params = getParameters();
    if (debugging) System.out.println("TiniMotorPlugin: setupSubscriptions "+params);

    myMotor = (String)params.get("motorName");
    sub = subscribe(getPred(myMotor));
    in1 = new BitPort(BitPort.Port5Bit3); // TX1 (12) Input #1
    in2 = new BitPort(BitPort.Port5Bit0); // CTX (8) Input #2
    setControlBits(0, 0);
  }

  private synchronized void allocate(MicroTask mt, boolean success) {
    if (debugging) System.out.println("TiniMotorController: ALLOCATE: "+success);
    long [] values = {0}; /* in thousandths */
    int [] aspects = {0};

    // make an allocation result.
    MicroAllocation ma = new MicroAllocation(null, mt);
    MicroAllocationResult mar = new MicroAllocationResult();
    ma.setReportedResult(mar);
    mar.setAspects(aspects);
    mar.setValues(values);
    mar.setSuccess(success);
    mt.setAllocation(ma);
    publishChange(mt);
  }

  public void execute() {
    if (debugging) System.out.println("MotorPlugin: execute");
    Enumeration enm = sub.getAddedList().elements();
    while (enm.hasMoreElements()) {
      MicroTask mt = (MicroTask)enm.nextElement();
      startMotor(mt);
    }

    // could be a direction change?
    enm = sub.getChangedList().elements();
    while (enm.hasMoreElements()) {
      MicroTask mt = (MicroTask)enm.nextElement();
      startMotor(mt);
    }

    Enumeration edel = sub.getRemovedList().elements();
    if (edel.hasMoreElements()) {
      MicroTask mt = (MicroTask)edel.nextElement();
      stopMotor(mt);
    }
  }

  /**
   * Coast.....
   */
  private void stopMotor(MicroTask mt) {
    if (debugging) System.out.println("TiniMotorController: stopMotor()");
    setControlBits(0, 0);
  }

  private void startMotor(MicroTask mt) {
  /**
   * Could spawn a thread to handle this ... nah!
   */
//    Thread t = new Thread(new Monitor(mt));
//    t.start();

    MicroPrepositionalPhrase mpp = findPreposition(mt, "Direction");
    if (debugging) System.out.println("TiniMotorController: startMotor: "+mpp.getIndirectObject());
    if (mpp != null) {
      String direction = mpp.getIndirectObject();
      if (direction.equals("Forward")) {
        if (setControlBits(1, 0))
          allocate(mt, true);
      } else if (direction.equals("Backward")) {
        if (setControlBits(0, 1))
          allocate(mt, true);
      } else if (direction.equals("Brake")) {
        if (setControlBits(1, 1))
          allocate(mt, true);
      } else {
        System.err.println("Unknown motor direction: "+direction);
        allocate(mt, false);
      }
    } else { // no preposition
      System.err.println("TiniMotorPlugin: No \"Direction\" preposition found.");
      allocate(mt, false);
    }

  }

  private int currentIn1State, currentIn2State;
  /**
   * Returns true if the state of the controller was changed.
   */
  private boolean setControlBits(int control1, int control2) {
    // Don't set bits if they are already set right
    if ((currentIn1State == control1) && (currentIn2State == control2)) {
      return false;
    }
    if (debugging) System.out.println("Setting control bits: "+control1+":"+control2);
    if (control1 == 0)
      in1.clear();
    else
      in1.set();
    if (control2 == 0)
      in2.clear();
    else
      in2.set();
    currentIn1State = control1;
    currentIn2State = control2;
    return true;
  }

//  class Monitor implements Runnable {
//    MicroTask task;
//    public Monitor(MicroTask mt) {
//      task = mt;
//    }
//    public void run() {
//      while (true) {
//        allocate(task);
//        try {Thread.sleep(sleepTime);} catch (InterruptedException ie){}
//        if (iShouldDie)
//          break;
//      }
//    }
//  }
}