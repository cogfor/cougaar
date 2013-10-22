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

import java.util.EventObject;

/**
 * An event used to notify interested parties that a change
 * has occurred in a community of interest.
 * <p>
 * The event contains a reference to the community generating
 * the event.  The event also contains attributes identifying the
 * cheange type and affected entity.  Since event generators may not
 * generate separate events for each change these attributes can only
 * be assumed to reflect the most recent change.
 */
public class CommunityChangeEvent extends EventObject {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
// Defines the type of change
  public static final int ADD_COMMUNITY                = 1;
  public static final int REMOVE_COMMUNITY             = 2;
  public static final int COMMUNITY_ATTRIBUTES_CHANGED = 3;
  public static final int ADD_ENTITY                   = 4;
  public static final int REMOVE_ENTITY                = 5;
  public static final int ENTITY_ATTRIBUTES_CHANGED    = 6;

  protected Community community;
  protected int type;
  protected String whatChanged;

  /**
   *
   * @param community    Changed community
   * @param type         Type of most recent change
   * @param whatChanged  Name of entity associated with most recent change
   */
  public CommunityChangeEvent(Community community, int type, String whatChanged) {
    super(community.getName());
    this.community = community;
    this.type = type;
    this.whatChanged = whatChanged;
  }

  /**
   * Returns a reference to changed community.
   * @return  Reference to changed community
   */
  public Community getCommunity() {
    return community;
  }

  /**
   * Returns name of community generating event.
   * @return Name of changed community
   */
  public String getCommunityName() {
    return community.getName();
  }

  /**
   * Returns a code indicating the type of the most recent change.
   * @return  Change code
   */
  public int getType() {
    return type;
  }

  /**
   * Returns the name of the Entity associated with the most recent change.
   * @return  Entity name.
   */
  public String getWhatChanged() {
    return whatChanged;
  }

  /**
   * Returns a string representation of the change code.
   * @param changeType
   * @return  Change code as a string
   */
  public static String getChangeTypeAsString(int changeType) {
    switch (changeType) {
      case ADD_COMMUNITY: return "ADD_COMMUNITY";
      case REMOVE_COMMUNITY: return "REMOVE_COMMUNITY";
      case COMMUNITY_ATTRIBUTES_CHANGED: return "COMMUNITY_ATTRIBUTES_CHANGED";
      case ADD_ENTITY: return "ADD_ENTITY";
      case REMOVE_ENTITY: return "REMOVE_ENTITY";
      case ENTITY_ATTRIBUTES_CHANGED: return "ENTITY_ATTRIBUTES_CHANGED";
    }
    return "INVALID_VALUE";
  }

  /**
   * Returns a string representation of the change event.
   * @return Event as a string
   */
  @Override
public String toString() {
    String communityName = getCommunityName();
    return "CommunityChangeEvent:" +
      " community=" + (communityName == null ? "*" : communityName) +
      " type=" + getChangeTypeAsString(type) +
      " whatChanged=" + whatChanged;
  }
}
