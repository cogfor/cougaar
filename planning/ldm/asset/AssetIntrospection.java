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

package org.cougaar.planning.ldm.asset;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.util.PropertyNameValue;

public class AssetIntrospection {

  public static Vector fetchAllProperties(Asset asset) {
    Vector propertyNameValues = new Vector(10);

    try {
      // get all properties of asset
      BeanInfo info = Introspector.getBeanInfo(asset.getClass());
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (int i = 0 ; i < pds.length; i++) {
	PropertyDescriptor pd = pds[i];
	Method readMethod = pd.getReadMethod();
	try {
	  Object result = readMethod.invoke(asset, null);
	  if (result != null) {
	    //	    System.out.println("Property: " + pd.getName() +
	    //			       " Value: " + result.toString() +
	    //			       " Class: " + result.getClass().toString());
	    propertyNameValues.add(new PropertyNameValue(pd.getName(), result));
	  }
	} catch (Exception e) {
	  System.out.println("Asset introspection invocation exception: " + e.toString());
	}
      }
      
      // get all dynamic properties of asset
      Enumeration dynamicProperties = asset.getOtherProperties();
      while (dynamicProperties.hasMoreElements()) {
	Object dynamicProperty = dynamicProperties.nextElement();
	//	System.out.println("Adding dynamic property: " + 
	//                         dynamicProperty.toString() +
	//			   " Value: " + dynamicProperty.toString() +
	//			   " Class: " + dynamicProperty.getClass().toString());
	propertyNameValues.add(new PropertyNameValue(prettyName(dynamicProperty.getClass().toString()), 
						     dynamicProperty));
      }

    } catch (Exception e) {
      System.out.println("Asset introspection exception: " + e.toString());
    }
    return propertyNameValues;
  }

  // Return the last field of a fully qualified name.
  // If the input string contains an "@" then it's assumed
  // that the fully qualified name preceeds it.

  private static String prettyName(String s) {
    int i = s.indexOf("@");
    if (i != -1)
      s = s.substring(0, i);
    return (s.substring(s.lastIndexOf(".")+1));
  }

}
