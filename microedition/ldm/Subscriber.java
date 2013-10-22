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

/**
 * A Subscriber is an entity that has a single subscription to the blackboard
 */ 
public class Subscriber {

  public Subscriber() {
  }
  private org.cougaar.microedition.ldm.Distributor distributor;
  private org.cougaar.microedition.plugin.Plugin plugin;
  private org.cougaar.microedition.ldm.Subscription subscription;

  public org.cougaar.microedition.ldm.Distributor getDistributor() {
    return distributor;
  }

  public void setDistributor(org.cougaar.microedition.ldm.Distributor newDistributor) {
    distributor = newDistributor;
  }

  public void setPlugin(org.cougaar.microedition.plugin.Plugin newPlugin) {
    plugin = newPlugin;
  }

  public org.cougaar.microedition.plugin.Plugin getPlugin() {
    return plugin;
  }

  public void setSubscription(org.cougaar.microedition.ldm.Subscription newSubscription) {
    subscription = newSubscription;
  }

  public org.cougaar.microedition.ldm.Subscription getSubscription() {
    return subscription;
  }

  public void execute() {
    getPlugin().execute();
  }

 
} 
