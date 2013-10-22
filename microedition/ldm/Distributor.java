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

import org.cougaar.microedition.io.*;
import org.cougaar.microedition.node.*;
import org.cougaar.microedition.plugin.*;

/**
 * The Distributor registers Plugin subscriptions, executing Plugin based
 * on changes to their subscriptions.
 */
public class Distributor {

  private Vector addedList = new Vector();
  private Vector changedList = new Vector();
  private Vector removedList = new Vector();

  private Vector runnablePlugins = new Vector();
  private Vector allSubscribers = new Vector();

  private Vector allObjects = new Vector();

  private Semaphore sem = new Semaphore();

  private Node node;

  /**
   * @param name the agent name to be accessed by plugins.
   */
  public Distributor(Node node) {
    this.node = node;
  }

  /**
   * @return the name of this agent.
   */
  public String getNodeName() {
    return node.getNodeName();
  }

  private long UIDcount = 1;
  /**
   * @return A newly-created unique identifier.
   */
  public synchronized String makeUID() {
    return getNodeName() + '/' + UIDcount++;
  }

  private Thread owner = null;

  /**
   * Begin modifications to the blackboard.
   * @param subscriber The object that will hold the "lock" on this transaction.
   */
  public synchronized void openTransaction(Thread thread) {
    //System.out.println("OPEN TRANSACTION : " +thread.getName() +" owner= "+owner);
    if (thread == owner)
      throw new RuntimeException("Attempt to re-open transaction");
    while (owner != null) {
      try {
        //System.out.println(thread.getName() +" waiting for "+owner.getName()+ " to closeTransaction, if wedged, problems");
        wait();
	//System.out.println(thread.getName() +" fell out of wait");
      } catch (InterruptedException ie) {}
    }
    owner = thread;
    addedList.removeAllElements();
    changedList.removeAllElements();
    removedList.removeAllElements();
  }

  /**
   * Commit (finish) modifications to the blackboard.  Delta lists are updated
   * for all subscribers.
   * @param thread The thread that currently holds the "lock" on this transaction.
   * @exception RuntimeException if the thread parameter does not equal the last
   * thread given to openTransaction.
   */

  public void closeTransaction(Thread thread) {
    closeTransaction(thread, null);
  }

  /**
   * Commit (finish) modifications to the blackboard.
   * @param thread The thread that currently holds the "lock" on this transaction.
   * @param publisher The plugin that is closing the transaction.  (can be null)
   * @exception RuntimeException if the thread parameter does not equal the last
   * thread given to openTransaction.
   */
  public synchronized void closeTransaction(Thread thread, Plugin publisher) {
  //System.out.println("CLOSE TRANSACTION thread: " +thread.getName() +" owner" +owner.getName());
  if (owner == null || thread != owner )
    throw new RuntimeException("Attempt to close unopen transaction");

  // process added list
  for (Enumeration objects = addedList.elements();  objects.hasMoreElements();) {
    Object o = objects.nextElement();
    Vector subs = getSubscribers(o);
    // update subscribers
    for (Enumeration subsenum = subs.elements(); subsenum.hasMoreElements();) {
      Subscriber s = (Subscriber)subsenum.nextElement();
      Vector list = s.getSubscription().getAddedList();
      if (!list.contains(o))
        list.addElement(o);
      list = s.getSubscription().getMemberList();
      if (!list.contains(o))
        list.addElement(o);
      if (!runnablePlugins.contains(s.getPlugin()))
        runnablePlugins.addElement(s.getPlugin());
    }
    // Update the master blackboard
    if (!allObjects.contains(o))
      allObjects.addElement(o);
  }

  // process changed list
  for (Enumeration objects = changedList.elements();  objects.hasMoreElements();) {
    Object o = objects.nextElement();
    Vector subs = getSubscribers(o);
    // update subscribers
    for (Enumeration subsenum = subs.elements(); subsenum.hasMoreElements();) {
      Subscriber s = (Subscriber)subsenum.nextElement();
      if ((!s.getSubscription().isSeeOwnChanges()) && (s.getPlugin() == publisher))
        continue;
      Vector list = s.getSubscription().getChangedList();
      if (!list.contains(o))
        list.addElement(o);
      if (!runnablePlugins.contains(s.getPlugin()))
        runnablePlugins.addElement(s.getPlugin());
    }
  }

  // process removed list
  for (Enumeration objects = removedList.elements();  objects.hasMoreElements();) {
    Object o = objects.nextElement();
    Vector subs = getSubscribers(o);
    // update subscribers
    for (Enumeration subsenum = subs.elements(); subsenum.hasMoreElements();) {
      Subscriber s = (Subscriber)subsenum.nextElement();
      Vector list = s.getSubscription().getRemovedList();
      if (!list.contains(o))
        list.addElement(o);
      list = s.getSubscription().getMemberList();
      list.removeElement(o);
      if (!runnablePlugins.contains(s.getPlugin()))
        runnablePlugins.addElement(s.getPlugin());
    }
    // Update the master blackboard
    allObjects.removeElement(o);
  }

  owner = null;
  //System.out.println("CLOSE TRANSACTION: NOTIFY()");
  notifyAll();
  distribute();

  }

  /**
   * Add an object to the blackboard.
   * @return true
   */
  public boolean publishAdd(Object o) {
    if (!addedList.contains(o))
      addedList.addElement(o);
    return true;
  }

  /**
   * Advertise a change to an object that already exists on the blackboard.
   * @return true
   */
  public boolean publishChange(Object o) {
    if (!changedList.contains(o))
      changedList.addElement(o);
    return true;
  }

  /**
   * Remove an object from the blackboard.
   * @return true
   */
  public boolean publishRemove(Object o) {
    if (!removedList.contains(o))
      removedList.addElement(o);
    return true;
  }

  /**
   * Add a subscriber to be notified of changes to the blackboard.
   */
  public boolean addSubscriber(Subscriber s) {
    if (!allSubscribers.contains(s))
      allSubscribers.addElement(s);
    for (Enumeration objects = allObjects.elements();  objects.hasMoreElements();) {
      Object o = objects.nextElement();
      if (s.getSubscription().getPredicate().execute(o)) {
        s.getSubscription().getMemberList().addElement(o);
        s.getSubscription().getAddedList().addElement(o);
        if (!runnablePlugins.contains(s.getPlugin()))
          runnablePlugins.addElement(s.getPlugin());
      }
    }
    return true;
  }

  /**
   * Remove a subscriber.  It will no longer be notified of changes to the blackboard.
   */
  public boolean removeSubscriber(Subscriber s) {
    allSubscribers.removeElement(s);
    return true;
  }

  private Vector getSubscribers(Object o) {
    Vector ret = new Vector();
    Enumeration subs = allSubscribers.elements();
    while (subs.hasMoreElements()) {
      Subscriber s = (Subscriber)subs.nextElement();
      if (s.getSubscription().getPredicate().execute(o))
        ret.addElement(s);
    }
    return ret;
  }

  private MessageTransport messageTransport = null;

  public MessageTransport getMessageTransport() {
    return messageTransport;
  }
  public void setMessageTransport(MessageTransport mt) {
    messageTransport = mt;
  }

  /**
   *  Check for subscribers who have something to do.
   */
  public void distribute() {
    sem.put();
  }

  /**
   * Pause until a subscriber has something to do.
   */
  public void waitForSomeWork() {
    if (runnablePlugins.size() == 0)
      sem.take();
  }

  /**
   * Manage PlugIn subscriptions and executions
   */
  public void cycle() {

    for (;;) {
      // execute PlugIns
      try {

	while (runnablePlugins.size() > 0) {
	  Plugin runme = (Plugin)runnablePlugins.elementAt(0);
	  runnablePlugins.removeElementAt(0);
	  openTransaction(Thread.currentThread());
	  try {
	      //System.out.println("EXECUTE : " +runme.getPlugin().getClass());
	      runme.execute();
	      //System.out.println("DONE EXECUTE : " +runme.getPlugin().getClass());
	  }
	  catch (Throwable e) {
	    System.out.println("Exception thrown from plugin: " +e);
	  }
	  // clear out lists of all subscription for this plugin
	  for (Enumeration subs = runme.getSubscriptions();
	       subs.hasMoreElements();)
	      ((Subscription)subs.nextElement()).clearLists();
	  
	  // collect changed subscriptions
	  closeTransaction(Thread.currentThread(), runme);
	}
	waitForSomeWork();

      }
      catch (Exception ex) {
	System.out.println("Exception while processing plugins: " +ex);
	ex.printStackTrace();
      }
    }
  }
}

class Semaphore {

  int count = 1;
  public Semaphore() {
  }

  public synchronized void take() {
    while (count == 0) {
      try { wait(); } catch (InterruptedException ie) {
        System.out.println("interrupted");
      }
    }
    count = 0;
  }

  public synchronized void put() {
    count = 1;
    notify();
  }

}
