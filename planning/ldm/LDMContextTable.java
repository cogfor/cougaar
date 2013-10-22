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

package org.cougaar.planning.ldm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.mts.MessageAddress;

/**
 * <b>PRIVATE</b> registry of (agent -to- LDMServesPlugin) mapping,
 * for Asset prototype serialization.
 * <p>
 * Asset deserialization can use the ClusterContext to figure out
 * the address of the agent being [de]serialized.  Beyond that, an
 * Asset needs to deserialize its prototype and bind to the agent's
 * LDM.  This class allows the Asset to find the appropriate
 * LDMServesPlugin.
 * <p>
 * See <b>bug 1576</b> and <b>bug 1659</b> for future refactoring
 * plans.  This implementation is a temporary hack!
 * <p>
 * The only valid clients:
 *
 * @see org.cougaar.planning.ldm.asset.Asset
 * @see org.cougaar.planning.ldm.PlanningDomain
 */
public final class LDMContextTable {
  
  private static final Map table = new HashMap();

  /** @see org.cougaar.planning.ldm.PlanningDomain */
  static void setLDM(MessageAddress agentAddr, LDMServesPlugin ldm) {
    synchronized (table) {
      Object o = table.get(agentAddr);
      if (o instanceof LDMServesPlugin.Delegator) {
        LDMServesPlugin.Delegator delegator = (LDMServesPlugin.Delegator) o;
        synchronized (delegator) { /*prevent anyone else from mutating it while we're cutting over*/
          delegator.setLDM(ldm);
          HashMap oc = delegator.flushTemporaryPrototypeCache();
          if (oc != null) {
            for (Iterator i = oc.entrySet().iterator(); i.hasNext(); ) {
              Map.Entry entry = (Map.Entry) i.next();
              ldm.cachePrototype((String) entry.getKey(), (Asset) entry.getValue());
            }
          }
        }
      }
      table.put(agentAddr, ldm);
    }
  }

  /** @see org.cougaar.planning.ldm.asset.Asset */
  public static LDMServesPlugin getLDM(MessageAddress agentAddr) {
    synchronized (table) {
      LDMServesPlugin result = (LDMServesPlugin) table.get(agentAddr);
      if (result == null) {
        result = new LDMServesPlugin.Delegator();
        table.put(agentAddr, result);
      }
      return result;
    }
  }

}
