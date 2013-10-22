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


package org.cougaar.planning.ldm.plan;

/** Special Interface to PlanElement for Assessors only.
 * In particular, only plugins which provide cougaar-external access
 * to a given asset should call these methods.  For example, the 
 * infrastructure relies on this interface to propagate allocation
 * information between agents for organizations.
 *
 * Note that while all PlanElements implement this interface,
 * PlanElement does not extend this interface, thus forcing 
 * Assessors to cast to this class. 
 *
 * In no case should a plugin cast PlanElements to any type
 * in the cougaar planning package tree.
 **/
public interface PlanElementForAssessor extends PlanElement {
  
  /** @param rcvres set the received AllocationResult object associated 
   * with this plan element.
   **/
  void setReceivedResult(AllocationResult rcvres);
  
  /**
   * @param repres set the reported AllocationResult object associated 
   * with this plan element.
   * @deprecated used setReceivedResult instead 
   **/
  void setReportedResult(AllocationResult repres);
  
}
