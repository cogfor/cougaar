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
package org.cougaar.microedition.plugin;

import java.util.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;

/**
 * Base class for all PlugIns.  Implements subscription handling.
 */
public abstract class PluginAdapter implements Plugin {

    // private container of subscriptions
    private Vector m_subscriptions = new Vector();
    
  public PluginAdapter() {
  }

    // PlugIn interface methods
    
  /**
   * Called on agent startup to initialize plugin.
   */
  public abstract void setupSubscriptions();
  /**
   * Called when there is a change in the plugin's subscriptions.
   */
  public abstract void execute();

    /**
     * Called to get the list of subscriptions for this plugin
     */
    public Enumeration getSubscriptions()
    {
	return m_subscriptions.elements();
    }
    
  /**
   * Set the distributor associated with this plugin.
   */
  public void setDistributor(org.cougaar.microedition.ldm.Distributor newDistributor) {
    distributor = newDistributor;
  }

  /**
   * Get the distributor associated with this plugin.
   */
  public org.cougaar.microedition.ldm.Distributor getDistributor() {
    return distributor;
  }
  protected org.cougaar.microedition.ldm.Distributor distributor;


  public void setParameters(Hashtable t) {
    attrtable = t;
  }

  public Hashtable getParameters() {
    return attrtable;
  }
  private Hashtable attrtable = null;

  /**
   * Add a new object to the blackboard.
   */
  protected boolean publishAdd(Object o) {
    return distributor.publishAdd(o);
  }

  /**
   * Advertise a change to on object on the blackboard.
   */
  protected boolean publishChange(Object o) {
    return distributor.publishChange(o);
  }

  /**
   * Delete an object from the blackboard.
   */
  protected boolean publishRemove(Object o) {
    return distributor.publishRemove(o);
  }

  /**
   * Claim the transaction.  All blackboard changes must happen
   * within an transaction.
   */
  protected void openTransaction() {
    distributor.openTransaction(Thread.currentThread());
  }

  /**
   * Release the transaction lock.
   */
  protected void closeTransaction() {
    distributor.closeTransaction(Thread.currentThread(), this);
  }

  /**
   * Create a subscription.  Objects matching the UnaryPredicate
   * will be maintained on the subscription list.
   */
  protected Subscription subscribe(UnaryPredicate pred) {
    Subscription subscription = new Subscription();
    Subscriber subscriber = new Subscriber();

    subscription.setPredicate(pred);
    subscription.setSubscriber(subscriber);

    subscriber.setDistributor(getDistributor());
    subscriber.setPlugin(this);
    subscriber.setSubscription(subscription);

    getDistributor().addSubscriber(subscriber);

    m_subscriptions.addElement(subscription);
    
    return subscription;
  }

  /**
   * Convenience method.  Finds a prepositional phrase with preposition "prep"
   * in the MicroTask.  Returns null if not found.
   */
  static protected MicroPrepositionalPhrase findPreposition(MicroTask mt, String prep) {
    MicroPrepositionalPhrase ret = null;
    Vector prepositions = mt.getPrepositionalPhrases();
    if (prepositions != null) {
      Enumeration enm = prepositions.elements();
      while (enm.hasMoreElements()) {
        MicroPrepositionalPhrase mpp = (MicroPrepositionalPhrase)enm.nextElement();
        if (mpp.getPreposition().equals(prep)) {
          ret = mpp;
          break;
        }
      }
    }
    return ret;
  }

  /**
   * @return A newly-created unique identifier.
   */
  protected String makeUID() {
    return getDistributor().makeUID();
  }

  protected String getNodeName() {
    return getDistributor().getNodeName();
  }

  private boolean is_debugging = false;
  private boolean is_debugging_set = false;
  /**
   * Returns true if a parameter "debug" is equal to "true"
   */
  protected boolean isDebugging() {
    if (is_debugging_set) return is_debugging;

    is_debugging_set = true;
    if (getParameters() != null) {
      String debug = (String)getParameters().get("debug");
      if (debug != null) {
        is_debugging = debug.equals("true");
      }
    }
    return is_debugging;
  }
}
