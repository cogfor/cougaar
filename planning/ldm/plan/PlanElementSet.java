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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.KeyedSet;

/**
 * PlanElementSet is a custom container which maintains a hashtable-like
 * association between pe.task.key and pe object.  The supports the single
 * most time-consuming operation in logplan lookups.
 **/

public class PlanElementSet
  extends KeyedSet
{
  protected Object getKey(Object o) {
    if (o instanceof PlanElement) {
      Task task = ((PlanElement)o).getTask();
      if (task == null) {
        throw new IllegalArgumentException("Invalid PlanElement (no task) added to a PlanElementSet: "+o);
      }
      return ((UniqueObject) task).getUID();
    } else {
      return null;
    }
  }

  // special methods for PlanElement searches

  public PlanElement findPlanElement(Task task) {
    UID sk = ((UniqueObject) task).getUID();
    return (PlanElement) inner.get(sk);
  }

  public PlanElement findPlanElement(UID key) {
    return (PlanElement) inner.get(key);
  }
}
