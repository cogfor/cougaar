/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.plugin.freeze;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.SimpleUniqueObject;

/**
 * A source-side freeze {@link Relay}.
 * <p>
 * NOTE: This is part of the older mechanism for freezing the society.  The
 * current mechanism uses FreezeServlet located on every agent in the society,
 * and depends on some external process to tell all agents to freeze.  This older
 * mechanism has not been removed so that people can continue to use a single servlet
 * to freeze the entire society, but the FreezeServlet mechanism is preferred now.
 */
public class FreezeRelaySource
  extends SimpleUniqueObject
  implements Relay.Source, NotPersistable
{
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
transient Map targets;

  FreezeRelaySource(Set targets) {
    this.targets = new HashMap();
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      Object target = i.next();
      this.targets.put(target, Collections.singleton(target));
    }
  }

  // Application Source API
  synchronized Set getUnfrozenAgents() {
    Set ret = new HashSet();
    for (Iterator i = targets.values().iterator(); i.hasNext(); ) {
      Set unfrozen = (Set) i.next();
      ret.addAll(unfrozen);
    }
    return ret;
  }

  // Relay.Source implementation
  public Set getTargets() {
    return targets.keySet();
  }
  public Object getContent() {
    return null;                // No actual content; only responses
  }
  public TargetFactory getTargetFactory() {
    return FreezeRelayFactory.getTargetFactory();
  }
  public int updateResponse(MessageAddress target, Object response) {
    synchronized (targets) {
      targets.put(target, response);
      return Relay.RESPONSE_CHANGE;
    }
  }
}
