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

package org.cougaar.planning.ldm;

import java.util.Collection;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;

/**
 * A provider of Asset Properties to the LDM.
 * @see org.cougaar.planning.ldm.LDMPluginServesLDM
 **/
public interface LatePropertyProvider extends LDMPluginServesLDM {
  /** Allows the LatePropertyProvider to specify the
   * classes of PropertyGroup for which it can provide values. <p>
   *
   * The return value is a collection of PropertyGroup interfaces handled
   * by the provider.
   * The provider will only be called by the LDM for property groups
   * which have been specified by this method. <p>
   *
   * A returned value of null is equivalent to returning a collection
   * of all property group classes, so the provider will be invoked
   * on any and all pg classes.  An important caveat is that all providers
   * which explicitly specify a PG will be invoked before any providers
   * which return a null here. <p>
   *
   * This method will be called exactly once during plugin loading.
   **/
  Collection getPropertyGroupsProvided();
  
  /** 
   * Called by the LDM to request that the property group specified
   * by the pg parameter should be filled in for the specified asset.
   * The PG instance created should both be returned and actually
   * set in the asset. <P>
   *
   * The time parameter allows specification of the time point of
   * interest.  The time may be specified as an actual Cougaar time, or
   * as LdmServesClient.UNSPECIFIED_TIME. <P>
   *
   * A LatePropertyProvider may, at its option, fill in other property
   * groups at the same time or decline to set or provide any value by
   * returning null. <P>
   * 
   * The first appropriate LatePropertyProvider to return a non-null
   * value will prevent any other possibly appropriate ones from being
   * executed. <p>
   *
   * It is important to note that this method could be called from multiple
   * threads at the same time, so the plugin code must be reentrant and
   * probably thread-aware.
   **/
  PropertyGroup fillPropertyGroup(Asset anAsset, Class pg, long time);
}
