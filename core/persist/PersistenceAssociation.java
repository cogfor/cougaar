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

package org.cougaar.core.persist;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * This class establishes an association between an object that has
 * been persisted and a reference number. It also captures the state
 * of the object.
 */
public class PersistenceAssociation extends WeakReference {

  /**
   * The id assigned to the object. This id is used to replace the
   * object when its actual value is not significant.
   */
  private PersistenceReference referenceId;

  private PersistenceIdentity clientId;

  /**
   * Records if the object has not yet been removed from the plan.
   * Used to manage the lifecycle of IdentityTable entries.
   */
  private static final int NEW      = 0;
  private static final int ACTIVE   = 1;
  private static final int INACTIVE = 2;
  private int active = NEW;

  /**
   * Temporarily used to mark objects needing to be persisted or that
   * have been rehydrated
   */
  private boolean marked = false;

  /**
   * The hashcode of the object. For efficiency (see IdentityTable).
   */
  int hash;

  /**
   * Chain of associations in IdentityTable. Links together all the
   * entries in a hashtable bucket.
   */
  PersistenceAssociation next;

  PersistenceAssociation(Object object, int id, ReferenceQueue refQ) {
    this(object, new PersistenceReference(id), refQ);
  }

  PersistenceAssociation(Object object, PersistenceReference id, ReferenceQueue refQ) {
    super(object, refQ);
    if (id == null) throw new IllegalArgumentException("Null PersistenceReference");
    if (object == null) throw new IllegalArgumentException("Null Object");
    referenceId = id;
    hash = System.identityHashCode(object); // Get this now before the object disappears
  }

  public Object getObject() {
    return get();
  }

  public PersistenceReference getReferenceId() {
    return referenceId;
  }

  public PersistenceIdentity getClientId() {
    return clientId;
  }

  public void setClientId(PersistenceIdentity newClientId) {
    clientId = newClientId;
  }

  /**
   * A mark used for various purposes. During a persist operation, the
   * mark serves to identify associations that are being written to
   * this persistence delta. During rehydration, the mark is used to
   * identify associations that were restored from a particular delta.
   */
  public void setMarked(boolean newMarked) {
    marked = newMarked;
  }

  public boolean isMarked() {
    return marked;
  }

  public void setActive() {
    if (active == NEW) active = ACTIVE;
  }

  public void setActive(int newActive) {
    if (newActive > active) active = newActive;
  }

  public void setInactive() {
    active = INACTIVE;
  }

  public boolean isActive() {
    return active == ACTIVE;
  }

  public int getActive() {
    return active;
  }

  public boolean isNew() {
    return active == NEW;
  }

  public boolean isInactive() {
    return active == INACTIVE;
  }

  @Override
public String toString() {
    String activity;
    switch (active) {
    default:       activity = " 0"; break;
    case ACTIVE:   activity = " +"; break;
    case INACTIVE: activity = " -"; break;
    }
    return PersistenceServiceComponent.hc(getObject()) + activity + " @ " + referenceId;
  }
}
