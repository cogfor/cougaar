/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Mobile Intelligence Corp
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
package org.cougaar.core.service.community;

import java.util.Collection;
import java.util.Set;

/**
 * Defines the attributes and child entities for a community.  This is the
 * primary class used by the community service infrastructure to describe the
 * details of a single community.  Instances of this class are published
 * to the blackboards of community members and other interested agents.
 */
public interface Community extends Entity {

  // Search qualifiers
  public static final int AGENTS_ONLY = 0;
  public static final int COMMUNITIES_ONLY = 1;
  public static final int ALL_ENTITIES = 2;

  /**
   * Returns a collection containing all entities associated with this
   * community.
   * @return  Collection of Entity objects
   */
  public Collection getEntities();

  /**
   * Returns named Entity or null if it doesn't exist.
   * @param  name of requested entity
   * @return named entity
   */
  public Entity getEntity(String name);

  /**
   * Returns true if community contains entity.
   * @param  name of requested entity
   * @return true if community contains entity
   */
  public boolean hasEntity(String name);

  /**
   * Adds an Entity to the community.
   * @param entity  Entity to add to community
   */
  public void addEntity(Entity entity);

  /**
   * Removes an Entity from the community.
   * @param entityName  Name of Entity to remove from community
   */
  public void removeEntity(String entityName);

  /**
   * Performs search of community and returns collection of matching Entity
   * objects.
   * @param filter    JNDI style search filter
   * @param qualifier Search qualifier (e.g., AGENTS_ONLY, COMMUNITIES_ONLY, or
   *                  ALL_ENTITIES)
   * @return Set of Entity objects satisfying search filter
   */
  public Set search(String filter,
                    int    qualifier);

  public String qualifierToString(int qualifier);

}
