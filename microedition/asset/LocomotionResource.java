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

package org.cougaar.microedition.asset;

/**
 * This resource controls a two-wheeled robot base.  Note that the speed
 * can only be set while the robot is stopped.
 */
public abstract class LocomotionResource extends ResourceAdapter {

  /**
   * @return the speed setting (in mm/sec)
   */
  public abstract long getSpeed();
  /**
   * @param newSpeed the speed (in mm/sec) that the robot will move forward or backward
   */
  public abstract void setSpeed(long newSpeed);

  public static final int CLOCKWISE = 0;
  public static final int COUNTER_CLOCKWISE = 1;
  /**
   * Rotate one tick in the given direction.
   * @param direction one of CLOCKWISE or COUNTER_CLOCKWISE
   * @param degrees the number of degrees to rotate in that direction
   */
  public abstract long[] rotate(int direction, long degrees);
  /**
   * Move forward at the requested speed
   * @see setSpeed(long)
   */
  public abstract long[] forward();
  /**
   * Move in reverse at the requested speed
   * @see setSpeed(long)
   */
  public abstract long[] backward();
  /**
   * Whoa!
   */
  public abstract long[] stop();
}
