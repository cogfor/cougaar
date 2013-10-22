/*
 * <copyright>
 * 
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.ldm;

import java.util.*;

/**
 * Defines the set of objects than a subscribers is interested in.
 */
public class Subscription {

  public Subscription() {
    setAddedList(new Vector());
    setChangedList(new Vector());
    setRemovedList(new Vector());
    setMemberList(new Vector());
  }
  private org.cougaar.microedition.ldm.Subscriber subscriber;
  private org.cougaar.microedition.util.UnaryPredicate predicate;
  private java.util.Vector addedList;
  private java.util.Vector changedList;
  private java.util.Vector removedList;
  private java.util.Vector memberList;
  private boolean seeOwnChanges = true;

  protected void clearLists() {
    addedList.removeAllElements();
    changedList.removeAllElements();
    removedList.removeAllElements();
  }

  public org.cougaar.microedition.ldm.Subscriber getSubscriber() {
    return subscriber;
  }

  public void setSubscriber(org.cougaar.microedition.ldm.Subscriber newSubscriber) {
    subscriber = newSubscriber;
  }

  public void setPredicate(org.cougaar.microedition.util.UnaryPredicate newPredicate) {
    predicate = newPredicate;
  }

  public org.cougaar.microedition.util.UnaryPredicate getPredicate() {
    return predicate;
  }

  protected void setAddedList(java.util.Vector newAddedList) {
    addedList = newAddedList;
  }

  /**
   * Get the list of objects matching the predicate
   * added to the blackboard since the last execute().
   */
  public java.util.Vector getAddedList() {
    return addedList;
  }

  protected void setChangedList(java.util.Vector newChangedList) {
    changedList = newChangedList;
  }

  /**
   * Get the list of objects matching the predicate
   * changed on the blackboard since the last execute().
   */
  public java.util.Vector getChangedList() {
    return changedList;
  }

  protected void setRemovedList(java.util.Vector newRemovedList) {
    removedList = newRemovedList;
  }

  /**
   * Get the list of objects matching the predicate
   * removed from the blackboard since the last execute().
   */
  public java.util.Vector getRemovedList() {
    return removedList;
  }

  protected void setMemberList(java.util.Vector newMemberList) {
    memberList = newMemberList;
  }

  /**
   * Get the list of objects that currently match the subscription predicate.
   */
  public java.util.Vector getMemberList() {
    return memberList;
  }

  /**
   * Returns true if any of the delta lists (added, changed, removed) have
   * objects on them.
   */
  public boolean hasChanged() {
    return ((addedList.size()   != 0) ||
            (changedList.size() != 0) ||
            (removedList.size() != 0));
  }

  /**
   * Controls whether changes to the objects on this subscription are
   * seen by the plugin that publishes them.  The default is that plugins
   * DO see their own changes.
   * @param newSeeMyOwnChanges if false, the plugin doing publishChange() operations
   *                           will not be notified (woken up) for the change
   *                           on this subscription.
   */
  public void setSeeOwnChanges(boolean newSeeOwnChanges) {
    seeOwnChanges = newSeeOwnChanges;
  }
  /**
   * @return true if the subscriber plugin will see its own changes on this subscription.
   */
  public boolean isSeeOwnChanges() {
    return seeOwnChanges;
  }

}
