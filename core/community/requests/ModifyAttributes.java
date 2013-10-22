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

package org.cougaar.community.requests;

import javax.naming.directory.ModificationItem;

import org.cougaar.core.util.UID;

/**
 * Request to modify the attributes of a Community or Entity.
 */
public class ModifyAttributes
    extends CommunityRequest implements java.io.Serializable {

  private String entityName;
  private ModificationItem mods[];

  /**
   * Modifies the attributes associated with an entity.  The modified attributes
   * are applied to the entity in the specified community unless the entity is
   * null in which case the attribute modifications are applied to the community.
   * @param communityName  Name of affected community
   * @param entityName     Name of entity, if null the attributes are applied
   *                       to the community
   * @param mods           Attribute modifications
   * @param uid            Unique identifier
   */
  public ModifyAttributes(String                    communityName,
                          String                    entityName,
                          ModificationItem[]        mods,
                          UID                       uid) {
    super(communityName, uid);
    this.entityName = entityName;
    this.mods = mods;
  }

  public String getEntityName() {
    return entityName;
  }

  public ModificationItem[] getModifications() {
    return mods;
  }

}
