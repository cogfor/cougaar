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

import com.dalsemi.system.*;

/**
 * Tests the motor controller on a TINI board.  Runs forward 5 seconds, backward 5 secs,
 * brake for 5 seconds and then turns the motor off.
 */
public class TiniMotorTester  {

  private int sleepTime = 5000;
  private boolean iShouldDie = false;
  private BitPort in1; // TX1 (12) Input #1
  private BitPort in2; // CTX (8) Input #2
  private boolean debugging;

  public TiniMotorTester() {
    in1 = new BitPort(BitPort.Port5Bit3); // TX1 (12) Input #1
    in2 = new BitPort(BitPort.Port5Bit0); // CTX (8) Input #2
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

  /*
  public static void main(String [] args) {
    TiniMotorTester m = new TiniMotorTester();
    System.out.println("Forward");
    m.setControlBits(1, 0);
    try {Thread.sleep(5000);}catch (InterruptedException ie){}
    System.out.println("Backward");
    m.setControlBits(0, 1);
    try {Thread.sleep(5000);}catch (InterruptedException ie){}
    System.out.println("Brake");
    m.setControlBits(1, 1);
    try {Thread.sleep(5000);}catch (InterruptedException ie){}
    System.out.println("Off");
    m.setControlBits(0, 0);

  }
  */
}