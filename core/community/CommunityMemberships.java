/*
 * <copyright>
 *
 *  Copyright 2001-2004 Mobile Intelligence Corp
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
package org.cougaar.community;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.service.community.Entity;

/**
 * Identifies the entities that an agent has added (via a join request) to
 * all its parent communities.  This map is
 * compared to Community objects received from the various community
 * managers sending Community data to this agent.  If a mismatch is detected
 * the agent will attempt to rectify by sending an join or leave request
 * to manager.  A mismatch is likely the result of a community manager
 * termination/restart.
 */
public class CommunityMemberships implements Serializable {

  private Map parentCommunities = new HashMap();
  private transient Set listeners = Collections.synchronizedSet(new HashSet());

  /**
   * Add an entity to community.
   * @param communityName Name of parent community
   * @param entity Entity added by monitoring agent
   */
  public synchronized void add(String communityName, Entity entity) {
    Map entities = (Map)parentCommunities.get(communityName);
    if (entities == null) {
      entities = new HashMap();
      parentCommunities.put(communityName, entities);
    }
    entities.put(entity.getName(), entity);
    fireListeners();
  }

  /**
   * Get names of all parent communities.
   * @return Set of community names.
   */
  public synchronized Set listCommunities() {
    return parentCommunities.keySet();
  }

  /**
   * Check for existence of community.
   * @param communityName Name of community
   * @return true if community exists
   */
  public synchronized boolean contains(String communityName) {
    return parentCommunities.containsKey(communityName);
  }

  /**
   * Check for existence of entity in community.
   * @param communityName Name of community
   * @param entityName Name of entity
   * @return true if community exists and has entry for specified entity
   */
  public synchronized boolean contains(String communityName, String entityName) {
    Map entities = (Map)parentCommunities.get(communityName);
    return (entities != null && entities.containsKey(entityName));
  }

  /**
   * Remove community.
   * @param communityName Name of community to remove
   */
  public synchronized void remove(String communityName) {
    if (parentCommunities.containsKey(communityName)) {
      parentCommunities.remove(communityName);
      fireListeners();
    }
  }

  /**
   * Remove entity from community.  If community entry is empty after removal
   * of entity, the community is also removed.
   * @param communityName Name of parent community
   * @param entityName Name of entity to remove
   */
  public synchronized void remove(String communityName, String entityName) {
    Map entities = (Map)parentCommunities.get(communityName);
    if (entities != null && entities.containsKey(entityName)) {
      entities.remove(entityName);
      if (entities.isEmpty()) parentCommunities.remove(communityName);
      fireListeners();
    }
  }

  /**
   * Returns a Collection of Entity objects associated with named Community.
   * @param communityName Name of community
   * @return Collection of Entity objects associated with named community
   */
  public synchronized Collection getEntities(String communityName) {
    Map entities = (Map)parentCommunities.get(communityName);
    if (entities != null) {
      return new ArrayList(entities.values());
    } else {
      return Collections.EMPTY_SET;
    }
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Iterator it = parentCommunities.entrySet().iterator(); it.hasNext();) {
      Map.Entry me = (Map.Entry)it.next();
      String communityName = (String)me.getKey();
      Map entities = (Map)me.getValue();
      String entityNames = CommunityUtils.entityNames(entities.keySet());
      sb.append(communityName + "=" + entityNames);
      if (it.hasNext()) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  public void addListener(CommunityMembershipsListener cml) {
    if (listeners == null) {
      listeners = Collections.synchronizedSet(new HashSet());
    }
    listeners.add(cml);
  }

  public void removeListener(CommunityMembershipsListener cml) {
    if (listeners != null) {
      listeners.remove(cml);
    }
  }

  protected void fireListeners() {
    if (listeners != null) {
      List l = new ArrayList(listeners);
      for (Iterator it = l.iterator(); it.hasNext(); ) {
        CommunityMembershipsListener cml = (CommunityMembershipsListener)it.next();
        cml.membershipsChanged();
      }
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    synchronized (this) {
      stream.writeObject(this.parentCommunities);
    }
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    parentCommunities = (HashMap)stream.readObject();
  }
}
