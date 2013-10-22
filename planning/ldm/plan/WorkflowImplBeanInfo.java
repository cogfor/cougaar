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

import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
   Override the default property descriptors.
   A property descriptor contains:
   attribute name, bean class, read method name, write method name
   All other beaninfo is defaulted.
   This defines appropriate properties from the Workflow INTERFACE,
   but is actually used to introspect on the Workflow IMPLEMENTATION.
*/

public class WorkflowImplBeanInfo extends SimpleBeanInfo {

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pd = new PropertyDescriptor[5];
    int i = 0;
    try {
      Class workflowClass = Class.forName("org.cougaar.planning.ldm.plan.WorkflowImpl");
      pd[i++] = new PropertyDescriptor("parentTask",
				       workflowClass,
				       "getParentTaskID",
				       null);
      pd[i++] = new IndexedPropertyDescriptor("tasks",
					      workflowClass,
					      "getTaskIDs", null,
					      "getTaskID", null);
      pd[i++] = new PropertyDescriptor("constraints",
				       workflowClass,
				       "getConstraintsAsArray", 
				       null);
      pd[i++] = new PropertyDescriptor("allocationResult",
				       workflowClass,
				       "getAllocationResult",
				       null);
      pd[i++] = new PropertyDescriptor("constraintViolation",
				       workflowClass,
				       "constraintViolation",
				       null);
      //      pd[i++] = new IndexedPropertyDescriptor("violatedConstraints",
      //					      workflowClass,
      //					      "getViolatedConstraintsAsArray", null,
      //					      "getViolatedConstraintFromArray", null);

      PropertyDescriptor[] additionalPDs = Introspector.getBeanInfo(workflowClass.getSuperclass()).getPropertyDescriptors();
      PropertyDescriptor[] finalPDs = new PropertyDescriptor[additionalPDs.length + pd.length];
      System.arraycopy(pd, 0, finalPDs, 0, pd.length);
      System.arraycopy(additionalPDs, 0, finalPDs, pd.length, additionalPDs.length);
      //      for (i = 0; i < finalPDs.length; i++)
      //	System.out.println("WorkflowImplBeanInfo:" + finalPDs[i].getName());
      return finalPDs;
    } catch (Exception e) {
      System.out.println("Exception:" + e);
    }
    return null;
  }

}
