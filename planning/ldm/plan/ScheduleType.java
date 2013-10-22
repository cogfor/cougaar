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

/** Constant names for types of Schedules
 **/
public interface ScheduleType {
  String ASSIGNED_RELATIONSHIP = "Assigned_Relationship";
  String ASSIGNED_AVAILABILITY = "Assigned_Availability";
  String OTHER = "Other";
  String RELATIONSHIP = "Relationship";
  String ROLE = "Role";
  
  /** @deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleType.TOTAL_CAPACITY
   **/
  String TOTAL_CAPACITY = "Total_Capacity";

  /** @deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleType.ALLOCATED_CAPACITY
   **/
  String ALLOCATED_CAPACITY = "Allocated_Capacity";

  /** @deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleType.AVAILABLE_CAPACITY
   **/
  String AVAILABLE_CAPACITY = "Available_Capacity";

  /** @deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleType.TOTAL_INVENTORY
   **/
  String TOTAL_INVENTORY = "Total_Inventory";

  /** @deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleType.ACTUAL_CAPACITY
   **/
  String ACTUAL_CAPACITY = "Actual_Capacity";

  /** @deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleType.LABOR
   **/
  String LABOR = "Labor";
}
