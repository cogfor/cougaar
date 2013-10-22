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

import org.cougaar.microedition.asset.*;
import com.ibutton.adapter.*;
import com.ibutton.container.*;
import org.cougaar.microedition.tini.DS2406;


/**
 * Asset for motor control.
 */

public class TiniMotorController extends MotorControllerResource {

  private static DS2406 controlInputs;
  private int direction = 0;  // 0 = coast, 1 = backward, 2 = forward, 3 = brake
  private boolean debugging = false;
  private static final int IN1 = 0;
  private static final int IN2 = 1;
  private static final int COAST = 0;
  private static final int BACKWARD = 1;
  private static final int FORWARD = 2;
  private static final int BRAKE = 3;

  // test code
/*
  public void main(String args[]) {
    TiniMotorController TMC = new TiniMotorController();
    TMC.setDirection(COAST);
    TMC.start();
    TMC.stop();

    TMC.setDirection(BACKWARD);
    TMC.start();
    TMC.stop();

    TMC.setDirection(FORWARD);
    TMC.start();
    TMC.stop();

    TMC.setDirection(BRAKE);
    TMC.start();
    TMC.stop();

    TMC.setDirection(FORWARD);
    TMC.turnOneIncrement();

    TMC.setDirection(BACKWARD);
    TMC.turnOneIncrement();

    if (debugging) {TMC.readHWStatus();}
  }
*/

  public TiniMotorController() {
    try
    {
      controlInputs = new DS2406();
      if (debugging) {System.out.println("\nSwitches Initialized...");}
    }
    catch (Throwable t)
    {
      System.out.println(t);
    }
  }

  public void setDirection(int newDirection) {
    if (newDirection >= COAST && newDirection <= BRAKE) {
      direction = newDirection;
    } else {
      direction = COAST;
    }
  }

  public void turnOneIncrement() {
    try {
      switch (direction) {
        case 1:
          controlInputs.setSwitch(IN2, true);
          controlInputs.setSwitch(IN1, false);
          controlInputs.setSwitch(IN1, true);
          break;
        case 2:
          controlInputs.setSwitch(IN1, true);
          controlInputs.setSwitch(IN2, false);
          controlInputs.setSwitch(IN2, true);
          break;
      }
      return;
    }
    catch (Throwable t)
    {
      System.out.println(t);
    }
  }

  public void start() {
    try {
      switch (direction) {
        case 0:
          controlInputs.setSwitch(IN1, false);
          controlInputs.setSwitch(IN2, false);
          break;
        case 1:
          controlInputs.setSwitch(IN1, false);
          controlInputs.setSwitch(IN2, true);
          break;
        case 2:
          controlInputs.setSwitch(IN1, true);
          controlInputs.setSwitch(IN2, false);
          break;
        case 3:
          controlInputs.setSwitch(IN1, true);
          controlInputs.setSwitch(IN2, true);
          break;
      }
      return;
    }
    catch (Throwable t)
    {
      System.out.println(t);
    }
  }

  public void stop() {
    try {
      controlInputs.setSwitch(IN1, true);
      controlInputs.setSwitch(IN2, true);
      return;
    }
    catch (Throwable t)
    {
      System.out.println(t);
    }
  }

  public void readHWStatus() {
    try {
      controlInputs.reportSwitch(IN1);
      controlInputs.reportSwitch(IN2);
      return;
    }
    catch (Throwable t)
    {
      System.out.println(t);
    }
  }

  public void getValues(long [] values)
  {
  }

  public void getValueAspects(int [] aspects)
  {

  }

  public int getNumberAspects() { return 0; }

  public void setChan(int c) {}
  public void setUnits(String u) {}

  public void startControl() { start(); }
  public void stopControl()  { stop(); }
  public boolean conditionChanged() { return false; }
  public boolean isUnderControl() { return false; }
  public void modifyControl(String controlparameter, String controlparametervalue) {}

}