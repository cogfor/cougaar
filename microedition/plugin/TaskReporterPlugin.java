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
package org.cougaar.microedition.plugin;

import java.io.*;
import java.util.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;

/**
 * Says when tasks are added/changed/deleted.
 */
public class TaskReporterPlugin extends PluginAdapter {

  UnaryPredicate getPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof MicroTask);
      }
    };
    return myPred;
  }

  Subscription sub;

  public void setupSubscriptions() {
    sub = subscribe(getPred());
  }

  public void execute() {
    Enumeration enm = sub.getAddedList().elements();
    while (enm.hasMoreElements()) {
      MicroTask mt = (MicroTask)enm.nextElement();
      System.out.println("TaskReporter: Added Task: "+mt.getVerb());
    }
    enm = sub.getChangedList().elements();
    while (enm.hasMoreElements()) {
      MicroTask mt = (MicroTask)enm.nextElement();
      System.out.println("TaskReporter: Changed Task: "+mt.getVerb());
    }
    enm = sub.getRemovedList().elements();
    while (enm.hasMoreElements()) {
      MicroTask mt = (MicroTask)enm.nextElement();
      System.out.println("TaskReporter: Removed Task: "+mt.getVerb());
    }

  }

}
