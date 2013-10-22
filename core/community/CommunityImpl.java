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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.naming.directory.Attributes;

import org.cougaar.core.service.community.Agent;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;

/**
 * Implementation of org.cougaar.core.service.community.Community interface used
 * to define communities.
 */
public class CommunityImpl extends EntityImpl
    implements Community, java.io.Serializable, Cloneable {

  protected static final DateFormat df = new SimpleDateFormat("hh:mm:ss,SSS");
  protected Map entities = Collections.synchronizedMap(new HashMap());
  protected long lastUpdate;

  /**
   * Constructor
   * @param name Name of community
   */
  public CommunityImpl(String name) {
    super(name);
    lastUpdate = now();
  }

  /**
   * Constructor
   * @param name Name of community
   * @param attrs Initial attributes
   */
  public CommunityImpl(String name, Attributes attrs) {
    super(name, attrs);
    lastUpdate = now();
  }

  /**
   * Returns a collection containing all entities associated with this
   * community.
   * @return  Collection of Entity objects
   */
  public Collection getEntities() {
    synchronized (entities) {
      if (entities.isEmpty()) {
        return new ArrayList();
      } else {
        return new ArrayList(entities.values());
      }
    }
  }

  public void setEntities(Collection newEntities) {
    synchronized (entities) {
      entities = Collections.synchronizedMap(new HashMap());
    }
    for (Iterator it = newEntities.iterator(); it.hasNext(); ) {
      addEntity((Entity)it.next());
    }
  }

  public void setAttributes(Attributes attrs) {
    super.setAttributes(attrs);
    lastUpdate = now();
  }

  public long getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(long time) {
    lastUpdate = time;
  }

  /**
   * Returns the named Entity or null if it doesn't exist.
   * @param name  Name of entity
   * @return  Entity referenced by name
   */
  public Entity getEntity(String name) {
    return (Entity)entities.get(name);
  }

  /**
   * Returns true if community contains entity.
   * @param name Name of requested entity
   * @return true if community contains entity
   */
  public boolean hasEntity(String name) {
    return entities.containsKey(name);
  }

  /**
   * Adds an Entity to the community.
   * @param entity  Entity to add to community
   */
  public void addEntity(Entity entity) {
    if (entity != null) {
      synchronized (entities) {
        entities.put(entity.getName(), entity);
        lastUpdate = now();
      }
    }
  }

  /**
   * Removes an Entity from the community.
   * @param name  Name of entity to remove from community
   */
  public void removeEntity(String name) {
    synchronized (entities) {
      entities.remove(name);
      lastUpdate = now();
    }
  }

  /**
   * Performs search of community and returns collection of matching Entity
   * objects.
   * @param filter    JNDI style search filter
   * @param qualifier Search qualifier (e.g., AGENTS_ONLY, COMMUNITIES_ONLY, or
   *                  ALL_ENTITIES)
   * @return Set of Entity objects satisfying search filter
   */
  public Set search(String filter,
                    int qualifier) {
    Set matches = new HashSet();
    SearchStringParser parser = new SearchStringParser();
    try {
      Filter f = parser.parse(filter);
      for (Iterator it = getEntities().iterator(); it.hasNext(); ) {
        Entity entity = (Entity)it.next();
        if (entity != null && f.match(entity.getAttributes())) {
          if ((qualifier == ALL_ENTITIES) ||
              (qualifier == AGENTS_ONLY && entity instanceof Agent) ||
              (qualifier == COMMUNITIES_ONLY && entity instanceof Community)) {
            matches.add(entity);
          }
        }
      }
    } catch (Exception ex) {
      System.out.println("Exception in Community search, filter=" + filter);
      ex.printStackTrace();
    }
    return matches;
  }

  // Converts a collection of entities to a compact string representation of names
  private String entityNames(Collection members) {
    StringBuffer sb = new StringBuffer("[");
    for (Iterator it = members.iterator(); it.hasNext(); ) {
      sb.append(it.next().toString() + (it.hasNext() ? "," : ""));
    }
    return (sb.append("]").toString());
  }

  public String qualifierToString(int qualifier) {
    switch (qualifier) {
      case AGENTS_ONLY: return "AGENTS_ONLY";
      case COMMUNITIES_ONLY: return "COMMUNITIES_ONLY";
      case ALL_ENTITIES: return "ALL_ENTITIES";
    }
    return "INVALID_VALUE";
  }

  protected long now() {
    return System.currentTimeMillis();
  }

  public Object clone() {
    CommunityImpl clone =  (CommunityImpl)super.clone();
    clone.lastUpdate = lastUpdate;
    clone.entities = CommunityUtils.cloneEntities(getEntities());
    return clone;
  }

  public boolean equals(Object o) {
    if (o instanceof CommunityImpl) {
      CommunityImpl c = (CommunityImpl)o;
      return (name.equals(c.name) &&
          lastUpdate == c.lastUpdate &&
          attrs.equals(c.attrs));
    }
    return false;
  }

  /**
   * Returns an XML representation of community.
   * @return XML representation of community
   */
  public String toXml() {
    return toXml("");
  }

  /**
   * Returns an XML representation of community.
   * @param indent Blank string used to pad beginning of entry to control
   *               indentation formatting
   * @return XML representation of community
   */
  public String toXml(String indent) {
    StringBuffer sb =
        new StringBuffer(indent + "<Community name=\"" + getName() +
                         "\" timestamp=\"" + df.format(new Date(lastUpdate)) + "\" >\n");
    Attributes attrs = getAttributes();
    if (attrs != null && attrs.size() > 0)
      sb.append(attrsToString(getAttributes(), indent + "  "));
    for (Iterator it = getEntities().iterator(); it.hasNext(); ) {
      sb.append(((Entity)it.next()).toXml(indent + "  "));
    }
    sb.append(indent + "</Community>\n");
    return sb.toString();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(this.getName());
    stream.writeObject(this.getAttributes());
    stream.writeObject(getEntities());
    stream.writeLong(lastUpdate);
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    entities = Collections.synchronizedMap(new HashMap());
    setName((String)stream.readObject());
    setAttributes((Attributes)stream.readObject());
    setEntities((Collection)stream.readObject());
    lastUpdate = stream.readLong();
  }
}
