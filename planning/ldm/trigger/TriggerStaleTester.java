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

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.cougaar.planning.ldm.plan.Allocation;

/**
 * A Trigger Tester to determine if an allocation is stale
 */

public class TriggerStaleTester implements TriggerTester {
  private transient boolean stale;

  /** 
   * Return indication if any allocation in group is stale
   */
  public boolean Test(Object[] objects) {
    // Check if any of the objects are 'stale' allocations
    // reset stale flag each time
    stale = false;
    List objectlist = Arrays.asList(objects);
    ListIterator lit = objectlist.listIterator();
    while ( lit.hasNext() ) {
      // just to be safe for now, get the object as an Object and 
      // check if its an Allocation before checking the stale flag.
      Object o = (Object)lit.next();
      if (o instanceof Allocation) {
        if ( ((Allocation)o).isStale() ) {
          stale = true;
        }
      }
    }
    //System.err.println("TriggerStaleTester returning: "+stale);
    return stale;
  }


}
