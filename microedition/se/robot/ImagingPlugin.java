/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.microedition.se.robot;

import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.UnaryPredicate;
import java.util.*;
import org.cougaar.microedition.shared.Constants;
import org.cougaar.microedition.se.domain.*;

/**
  Receives GetImage tasks and responds by obtaining and publishing an image
 */
public class ImagingPlugin extends SimplePlugin
{
  // Subscription for all myVerb tasks
  private IncrementalSubscription taskSub;
  // Subscription for all ImageAssets
  private IncrementalSubscription assetSub;


/**
 */
  public void setupSubscriptions()
  {
    //System.out.println("ImagingPlugin::setupSubscriptions");

    // This predicate matches all tasks with one of myVerbs
    taskSub = (IncrementalSubscription)subscribe(new UnaryPredicate()
    {
      public boolean execute(Object o) {
        if (o instanceof Task) {
          Task t = (Task)o;
          return t.getVerb().equals(Constants.Robot.verbs[Constants.Robot.GETIMAGE]) ;
        }
        return false;
      }
    });

    assetSub = (IncrementalSubscription)subscribe(new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof MicroAgent)
        {
          MicroAgent m = (MicroAgent)o;
          String possible_roles = m.getMicroAgentPG().getCapabilities();
          StringTokenizer st = new StringTokenizer(possible_roles, ",");
          while (st.hasMoreTokens())
          {
            String a_role = st.nextToken();
            if(a_role.equals(Constants.Robot.meRoles[Constants.Robot.CAMERACONTROLLER]))
                 return true;
          }
        }
        return false;
      }
    });
  }
/**
 */
  protected void execute ()
  {

    Enumeration micros = assetSub.elements();
    if (!micros.hasMoreElements())
    {
      System.out.println("ImagingPlugin execute: no mc resources");
      return; // if no assets return
    }

    MicroAgent micro = (MicroAgent)micros.nextElement();

    Task t;

    Enumeration tasks = taskSub.elements();
    while (tasks.hasMoreElements())
    {
      t = (Task)tasks.nextElement();
      if (t.getPlanElement() != null)
        continue; // only want unallocated tasks
      //System.out.println("ImagingPlugin::allocing "+t.getVerb()+" task to micro");
      Allocation allo = makeAllocation(t, micro);
      publishAdd(allo);
    }

    Enumeration e = taskSub.getRemovedList();
    while (e.hasMoreElements()) {
      t = (Task)e.nextElement();
      //System.out.println("ImagingPlugin::got removed task with verb "+t);
    }

    e = taskSub.getChangedList();
    while (e.hasMoreElements()) {
      t = (Task)e.nextElement();
      //System.out.println("ImagingPlugin::got changed task with verb "+t.getVerb());
    }
  }

  private Allocation makeAllocation(Task t, MicroAgent micro) {
    AllocationResult estAR = null;
    Allocation allocation =
      theLDMF.createAllocation(t.getPlan(), t, micro, estAR, Role.ASSIGNED);
    return allocation;
  }
}

