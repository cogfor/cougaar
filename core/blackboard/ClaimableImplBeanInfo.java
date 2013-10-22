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

package org.cougaar.core.blackboard;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * A BeanInfo property description for {@link ClaimableImpl}.
 * <p> 
 * A property descriptor contains:<pre>
 * attribute name, bean class, read method name, write method name
 * </pre> 
 * All other beaninfo is defaulted.
 */
public class ClaimableImplBeanInfo extends SimpleBeanInfo {

  // return appropriate properties from Task.java interface
  @Override
public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pd = new PropertyDescriptor[2];
    try {
      Class claimableClass = 
        Class.forName("org.cougaar.core.blackboard.ClaimableImpl");
      pd[0] = new PropertyDescriptor("claimed",
                                     claimableClass,
                                     "isClaimed",
                                     null);
      pd[1] = new PropertyDescriptor("claimerClassName",
                                     claimableClass,
                                     "getClaimClassName",
                                     null);

      PropertyDescriptor[] additionalPDs = 
        Introspector.getBeanInfo(
            claimableClass.getSuperclass()).getPropertyDescriptors();
      PropertyDescriptor[] finalPDs = 
        new PropertyDescriptor[additionalPDs.length + pd.length];
      System.arraycopy(
          pd, 0, finalPDs, 0, pd.length);
      System.arraycopy(
          additionalPDs, 0, finalPDs, pd.length, additionalPDs.length);
      return finalPDs;
    } catch (Exception e) {
      System.out.println("Exception:" + e);
    }
    return null;
  }

}
