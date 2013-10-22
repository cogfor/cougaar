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

import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.io.*;
import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.plugin.*;

/**
 */
public class TiniMach5CouplingPlugin extends PluginAdapter {

  UnaryPredicate getPosResourcePred() {
    UnaryPredicate resourcePred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof TiniMach5PositionResource) {
          return true;
        }
        return false;
      }
    };
    return resourcePred;
  }

  UnaryPredicate getLocResourcePred() {
    UnaryPredicate resourcePred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof TiniMach5LocomotionResource) {
          return true;
        }
        return false;
      }
    };
    return resourcePred;
  }

  private Subscription pos_resourceSub;
  private Subscription loc_resourceSub;
  private TiniMach5LocomotionResource locresource = null;
  private TiniMach5PositionResource posresource = null;

  public void setupSubscriptions() {

    System.out.println("TiniMach5CouplingPlugin::setupSubscriptions v2");

    pos_resourceSub = subscribe(getPosResourcePred());
    loc_resourceSub = subscribe(getLocResourcePred());

  }


  public void execute() {

    System.out.println("TiniMach5CouplingPlugin.execute()");

    Enumeration enm = pos_resourceSub.getAddedList().elements();
    if (enm.hasMoreElements())
    {
      posresource = (TiniMach5PositionResource)enm.nextElement();
    }

    enm = loc_resourceSub.getAddedList().elements();
    if (enm.hasMoreElements())
    {
      locresource = (TiniMach5LocomotionResource)enm.nextElement();
    }

    if(posresource != null && locresource != null)
    {
      System.out.println("TiniMach5CouplingPlugin Assigning resource");

      posresource.setLocomotionResource(locresource);
    }
  }
}