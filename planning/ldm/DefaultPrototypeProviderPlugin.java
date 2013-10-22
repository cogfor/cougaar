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

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.service.PrototypeRegistryService;

/** Serve some default prototypes to the system.
 * At this point, this only serves stupid prototypes for
 * (temporary) backward compatability.
 *
 * At start, loads some low-level basics into the registry.
 * On demand, serve a few more.
 **/

public class DefaultPrototypeProviderPlugin 
  extends ComponentPlugin
  implements PrototypeProvider 
{
  
  PlanningFactory ldmf = null;  
  DomainService domainService = null;
  PrototypeRegistryService protregService = null;
  UIDService uidService = null;
  UID anID;
  
  public DefaultPrototypeProviderPlugin() {}
  
  protected void setupSubscriptions() {
  
    // get the domain service  	
    if (ldmf == null) {
      domainService = (DomainService) getServiceBroker().getService(
          this, DomainService.class, null);
      
      // get the registry service  	
      protregService = (PrototypeRegistryService) getServiceBroker().getService(
          this, PrototypeRegistryService.class, null);
      
      // get the UIDService   	
      uidService = (UIDService) getServiceBroker().getService(
          this, UIDService.class, null);
    }
    //use the services
    ldmf = (PlanningFactory) domainService.getFactory("planning");
    //anID = uidService.nextUID();
    
    preloadPrototypes();
  }
  
  // no subscriptions, so we'll never actually be run.
  protected void execute() {}
  
  
  public Asset getPrototype(String typename, Class hint) {
    // I was going to handle OPlan here, but OPlan isn't an Asset!
    try {
      // try some dynamic prototypes (for backward compatibility)
      if ("Solenoid".equals(typename)) {
        return makeProto(typename, "Consumable");
      } 
      else if ("M1A1 Tank".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("OTHER/Passenger".equals(typename)) {
        return makeProto(typename, "Person");
      }
      else if ("NSN/1520011069519".equals(typename)) {
        return makeProto(typename, "RotaryWingAircraftWeapon");
      }
      else if ("NSN/2350010871095".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("NSN/2350001226826".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("NSN/2350010318851".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("UTC/SupportOrg".equals(typename)) {
        return makeProto(typename, "SupportOrganization");
      }
      else if ("UTC/SupplyOrg".equals(typename)) {
        return makeProto(typename, "SupplyOrganization");
      }
      else if ("UTC/CombatOrg".equals(typename)) {
        return makeProto(typename, "CombatOrganization");
      }
      else if ("UTC/CommandOrg".equals(typename)) {
        return makeProto(typename, "CommandOrganization");
      }
      else if ("Organization".equals(typename)) {
        return makeProto(typename, "Organization");
      }
      
      
    } catch (Exception e) {
      // cannot really throw any of these exceptions.
    }
    return null;
  }
		
  
  private void submitAbstract(String name) {
    try {
      Asset proto = makeProto(name, "AbstractAsset");
      NewTypeIdentificationPG tip = (NewTypeIdentificationPG)proto.getTypeIdentificationPG();
      tip.setTypeIdentification(name);
      tip.setNomenclature(name);
      protregService.cachePrototype(name, proto);
      
      // here test out UIDService 
      anID = uidService.nextUID();
      //System.out.println(anID);
      //     System.out.println("agent id = " + uidService.getMessageAddress());
    } catch (Exception e) {
      // cannot really throw any of these exceptions.
    }
  }
  
  
  /*
   * modified makeProto to make prototypes with under the 'root' domain  and
   * register prototypes via the PrototypeRegistry 
   * make a prototype and an instance asset,  then test property groups 
   */
  private Asset makeProto(String typeid, String cl) {
    
    // create a prototype - register with registry service
    Asset proto = ldmf.createPrototype(cl, typeid);
  
    protregService.cachePrototype(typeid, proto);
    //* getLDM().cachePrototype(typeid, proto);
    System.out.println("making prototype: " + cl);
    return proto;
  }
  
  private void preloadPrototypes() {
    submitAbstract("Subordinates");
    submitAbstract("Ammunition");
    submitAbstract("SpareParts");
    submitAbstract("Consumable");

    submitAbstract("Repairable");
    submitAbstract("StrategicTransportation");
    submitAbstract("GSMaintenance");
    submitAbstract("DSMaintenance");
    submitAbstract("TheaterTransportation");
  }

}
