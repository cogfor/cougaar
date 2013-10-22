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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class PrepositionalPhraseImplBeanInfo extends SimpleBeanInfo {

   /**
    Override the default property descriptors.
    A property descriptor contains:
    attribute name, attribute return value, read method name, write method name.
    Property descriptors returned by this method are:
    indirectObject, Object, getIndirectObject, null
    preposition, String, getPreposition, null

    All other beaninfo is defaulted; that is,
    the Java Introspector is used to determine
    the rest of the information about this implementation.
   */

   public PropertyDescriptor[] getPropertyDescriptors() {
     PropertyDescriptor[] pd = new PropertyDescriptor[2];
     try {
       pd[0] = new PropertyDescriptor("preposition",
          Class.forName("org.cougaar.planning.ldm.plan.PrepositionalPhraseImpl"),
				      "getPreposition", null);
       pd[1] = new PropertyDescriptor("indirectObject",
          Class.forName("org.cougaar.planning.ldm.plan.PrepositionalPhraseImpl"),
				      "getIndirectObject", null);
     } catch (IntrospectionException ie) {
       System.out.println(ie);
     } catch (ClassNotFoundException ce) {
       System.out.println(ce);
     }
     return pd;
   }
}
