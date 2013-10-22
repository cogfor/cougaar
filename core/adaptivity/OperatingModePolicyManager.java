/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.PlaybookConstrainService;
import org.cougaar.util.UnaryPredicate;

/** 
 * A PolicyManager that handles OperatingModePolicies
 * For now, it listens for OperatingModePolicies and uses the 
 * PlaybookConstrainService to constrain the playbook with the OMPolicies.
 * It also sets the values of OperatingModes for non-adaptive OMPolicies.
 * In the future, it will forward InterAgentOperatingModePolicies to
 * other entities.
 */

public class OperatingModePolicyManager extends ServiceUserPluginBase {
  private PlaybookConstrainService playbookConstrainService;
  private OperatingModeService operatingModeService;

  private static final Class[] requiredServices = {
    PlaybookConstrainService.class,
    OperatingModeService.class
  };


  private static UnaryPredicate policyPredicate = 
    new UnaryPredicate() {
	/**
       * 
       */
      private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
	  if (o instanceof OperatingModePolicy) {
            if (o instanceof InterAgentOperatingModePolicy) {
              InterAgentOperatingModePolicy iaomp =
                (InterAgentOperatingModePolicy) o;
              return iaomp.appliesToThisAgent();
            }
	    return true;
	  }
	  return false;
	}
      };

  private IncrementalSubscription policySubscription;

  private class OMPredicate implements UnaryPredicate {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private HashSet omnames;

    public OMPredicate() {
      omnames = new HashSet(13);
    }

    public void addOM(String om) {
      omnames.add(om);
    }

    public void removeOM(String om) {
      omnames.remove(om);
    }

    public boolean execute(Object o) {
      if (o instanceof OperatingMode) {
	OperatingMode om = (OperatingMode)o;
	String bbOM = om.getName();
	for (Iterator it = omnames.iterator(); it.hasNext();) {
	  String storedName = (String) it.next();
	  if (bbOM.equals(storedName)) {
	    return true;
	  }
	}
      }
      return false;
    }
  }

  private IncrementalSubscription omSubscription;
  private OMPredicate omPred;

  public OperatingModePolicyManager() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
  }

  private void reallySetupSubscriptions() {

    policySubscription = blackboard.subscribe(policyPredicate);

    omPred = new OMPredicate();
    omSubscription = blackboard.subscribe(omPred);
  }

  private boolean haveServices() {
    if (playbookConstrainService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      playbookConstrainService = sb.getService(this, PlaybookConstrainService.class, null);
      operatingModeService = getServiceBroker().getService(this, OperatingModeService.class, null);
      reallySetupSubscriptions();
      return true;
    }
    return false;
  }

  @Override
public void execute() {
    if (haveServices()) {
      if (policySubscription.hasChanged()) {
	removePolicies(policySubscription.getRemovedCollection());
	changePolicies(policySubscription.getChangedCollection());
	addPolicies(policySubscription.getAddedCollection());
      }
      
      // Missing OperatingModes may have shown up
      if (omSubscription.hasChanged()) {
	checkOMSubs();
      }
    }
  }

  /**
   * Constrain the playbook with the new policies
   */
  private void addPolicies(Collection newPolicies) {
    if (logger.isInfoEnabled()) logger.info("Adding policy");
    for (Iterator it = newPolicies.iterator(); it.hasNext();) {
      OperatingModePolicy omp = (OperatingModePolicy)it.next();
      if (nonAdaptive(omp)) {
	playbookConstrainService.constrain(omp);
      }
    }
  }

  /**
   * Unconstrain the playbook with the removed policies
   */
  private void removePolicies(Collection removedPolicies)  {
    if (logger.isInfoEnabled()) logger.info("Removing policy");
    for (Iterator it = removedPolicies.iterator(); it.hasNext();) {
      playbookConstrainService.unconstrain((OperatingModePolicy)it.next());
    }
  }
  
  /**
   * Unconstrain, then reconstrain the playbook with the
   * changed policies.
   */
  private void changePolicies(Collection changedPolicies) {
    if (logger.isInfoEnabled()) logger.info("Changing policy");
    for (Iterator it = changedPolicies.iterator(); it.hasNext();) {
      OperatingModePolicy omp = (OperatingModePolicy)it.next();
      if (nonAdaptive(omp)) {
	playbookConstrainService.unconstrain(omp);
	playbookConstrainService.constrain(omp);
      }
    }
  }

  /**
   * If any missing OperatingModes have been published run through
   * all the policies again
   **/
  private void checkOMSubs() {
    if (logger.isInfoEnabled()) logger.info("checking for wanted Operating Modes");

//      for (Iterator subIt = omSubscription.getAddedCollection().iterator();
//  	 subIt.hasNext();) {
//        System.out.println("Found OM: " + subIt.next());
//      }
    
    // If anyone of the missing OMs has shown up, run through ALL of policies
    for (Iterator policyIt = policySubscription.iterator(); 
	 policyIt.hasNext();) {
      OperatingModePolicy omp = (OperatingModePolicy) policyIt.next();
      nonAdaptive(omp);
    }
  }


  /**
   * This methods sets the values of OperatingModes of NonAdaptive Policies.
   * A non adaptivie policies is defined as one that has a TRUE ifClause and
   * only single point ranges as values of the OperatingModeConstraints.
   * @return true if the policy always restricts its OMs to a single point and has a TRUE ifClause
   * 
   **/
  private boolean nonAdaptive(OperatingModePolicy policy) {
    PolicyKernel pk = policy.getPolicyKernel();
    ConstrainingClause ifClause = pk.getIfClause();
    ArrayList keepers = new ArrayList(13);

    if (ifClause.equals(ConstrainingClause.TRUE_CLAUSE)) {
      ConstraintPhrase[] cps = pk.getOperatingModeConstraints();

      for (int i=0; i<cps.length; i++) {
	OMCRangeList rl = cps[i].getAllowedValues();
	OMCRange[] ranges = rl.getAllowedValues();

	if (ranges.length > 1) {
	  return false;
	}
	if (!(ranges[0] instanceof OMCPoint)) {
	  return false;
	} 

	ConstraintOperator op = cps[i].getOperator();
  	if (op.equals(ConstraintOperator.EQUAL) ||
  	    op.equals(ConstraintOperator.ASSIGN) ||
  	    op.equals(ConstraintOperator.IN)) {
	  keepers.add(cps[i]);
  	} else {
  	  return false; 
  	}
      }
    }

    if (keepers.size() == 0) {
      return false;
    }

    for (Iterator it = keepers.iterator(); it.hasNext();) {
      ConstraintPhrase cp = (ConstraintPhrase)it.next();
      
      /* lookup and set operating mode corresponding to this phrase */
      OperatingMode om = findOM(cp.getProxyName());
      if (om != null) {
	OMCRange theValue = cp.getAllowedValues().getAllowedValues()[0];
	om.setValue(theValue.getMin());
	blackboard.publishChange(om);
	
	// Remove this om from the predicate, in case it's there.
	omPred.removeOM(cp.getProxyName());
      } else {
	// didn't find operating mode we expected
	// add it to the om predicate, and hope it shows up later
	omPred.addOM(cp.getProxyName());
      }
    }
    return true;
  }

  /**
   * Lookup an OperatingMode first using the OperatingModeService,
   * then by searching the local subscription
   **/
  private OperatingMode findOM(String omName) {
    // first try to find the om using the OMService
    OperatingMode om = operatingModeService.getOperatingModeByName(omName);
    if (om == null) {
      // Not in OMService, maybe in our subscription
      for (Iterator subIt = omSubscription.iterator(); subIt.hasNext();) {
	OperatingMode subOM = (OperatingMode) subIt.next();
	if (omName.equals(subOM.getName())) {
	  return subOM;
	}
      }
    }
    return om;
  }
}

