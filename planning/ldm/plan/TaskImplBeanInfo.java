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
   This defines appropriate properties from the Task INTERFACE,
   but is actually used to introspect on the Task IMPLEMENTATION.
*/

public class TaskImplBeanInfo extends SimpleBeanInfo {

  // return appropriate properties from Task.java interface
  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pd = new PropertyDescriptor[10];
    int i = 0;
    try {
      Class taskClass = Class.forName("org.cougaar.planning.ldm.plan.TaskImpl");
      pd[i++] = new PropertyDescriptor("parentTask",
				       taskClass,
				       "getParentTaskID",
				       null);
      pd[i++] = new PropertyDescriptor("workflow",
				       taskClass,
				       "getWorkflow",
				       null);
      pd[i++] = new PropertyDescriptor("directObject",
				       taskClass,
				       "getDirectObject",
				       null);
      pd[i++] = new IndexedPropertyDescriptor("prepositionalPhrases",
					      taskClass,
					      "getPrepositionalPhrasesAsArray", null,
					      "getPrepositionalPhraseFromArray", null);
      pd[i++] = new PropertyDescriptor("verb",
				       taskClass,
				       "getVerbName",
				       null);
      pd[i++] = new PropertyDescriptor("planElement",
				       taskClass,
				       "getPlanElementID",
				       null);
      pd[i++] = new PropertyDescriptor("ID",
				       taskClass,
				       "getUID",
				       null);
      pd[i++] = new IndexedPropertyDescriptor("preferences",
				       taskClass,
				       "getPreferencesAsArray",null,
				       "getPreferenceFromArray",null);
      pd[i++] = new PropertyDescriptor("priority",
				       taskClass,
				       "getPriority",
				       null);
      pd[i++] = new PropertyDescriptor("context",
				       taskClass,
				       "getContext",
				       null);

      //      System.out.println("TaskImplBeanInfo getting pds for:" + taskClass.getSuperclass());
      PropertyDescriptor[] additionalPDs = Introspector.getBeanInfo(taskClass.getSuperclass()).getPropertyDescriptors();
      PropertyDescriptor[] finalPDs = new PropertyDescriptor[additionalPDs.length + pd.length];
      System.arraycopy(pd, 0, finalPDs, 0, pd.length);
      System.arraycopy(additionalPDs, 0, finalPDs, pd.length, additionalPDs.length);
      //      for (i = 0; i < finalPDs.length; i++)
      //	System.out.println("TaskImplBeanInfo:" + finalPDs[i].getName());
      return finalPDs;
    } catch (Exception e) {
      System.out.println("Exception:" + e);
    }
    return null;
  }

}
