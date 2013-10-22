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
import org.cougaar.microedition.shared.Constants;

/**
 * This Plugin tests the Mach5 robot control.  It sends it forward a little, then rotates it.
 * @see TiniMach5LocomotionResource
 */
public class TiniMach5TesterPlugin extends PluginAdapter {

  UnaryPredicate getPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof TiniMach5LocomotionResource;
      }
    };
    return myPred;
  }

  private Subscription botSub;

  public void setupSubscriptions() {
    System.out.println("TiniMach5TesterPlugin::setupSubscriptions");
    botSub = subscribe(getPred());
  }

  public void execute() {
    Enumeration enm = botSub.getAddedList().elements();
    while (enm.hasMoreElements()) {
      try {
        TiniMach5LocomotionResource bot = (TiniMach5LocomotionResource)enm.nextElement();
        System.out.println("TiniMach5TesterPlugin::Got added "+bot);

	bot.setSpeed(100);

	while(true)
	{
	  //basically marches in a square reporting coordinate and heading at each
	  //corner. Increase sleep time after forward command to increase dimension

          bot.forward();
          Thread.sleep(2000);
          bot.stop();
          Thread.sleep(1000);

          bot.rotate(bot.CLOCKWISE, 90, false);
          Thread.sleep(1000);
	}
      }

      catch (Exception ex)
      {
        System.out.println("Exception: "+ex);
      }

    }
  }
}
