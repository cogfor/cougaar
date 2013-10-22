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

package org.cougaar.planning.servlet;

import java.beans.BeanInfo;
import java.beans.Beans;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.cougaar.core.util.PropertyNameValue;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetIntrospection;
import org.cougaar.planning.ldm.asset.LockedPG;
import org.cougaar.planning.ldm.asset.LockedPGSchedule;
import org.cougaar.planning.ldm.measure.AbstractMeasure;
import org.cougaar.planning.ldm.measure.Capacity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Create and return xml for first class log plan objects.
 * <p>
 * Element name is extracted from object class, by taking the
 * last field of the object class, and dropping a trailing "Impl",
 * if it exists.
 */

public class XMLize {

  /** Maximum search depth -- Integer.MAX_VALUE means unlimited. **/
  public static final int DEFAULT_UID_DEPTH = 3;
  public static Class numberClass;
  public static Class booleanClass;

  static {
      try {
	  numberClass  = Class.forName ("java.lang.Number");
	  booleanClass = Class.forName ("java.lang.Boolean");
      } catch (ClassNotFoundException cnfe) {}
  }

  public static Element getPlanObjectXML(
      Object obj, Document doc) {
    return getPlanObjectXML(obj, doc, DEFAULT_UID_DEPTH);
  }

  public static Element getPlanObjectXML(
      Object obj, Document doc, int searchDepth) {
    String className;
    if (Asset.class.isInstance(obj)) {
      className = "Asset";
    } else {
      className = obj.getClass().getName();
      int i = className.lastIndexOf(".");
      if (i >= 0) {
        className = className.substring(i+1);
      }
      i = className.lastIndexOf("Impl");
      if (i >= 0) {
        className = className.substring(0, i);
      }
    }
    Element root;

    // fix for bug where inner classes would cause XMLize to throw
    // exception
    try {
      className = className.replaceAll ("\\$", "_");

      root = doc.createElement(className);
    } catch (Exception e) {
      System.err.println("Got exception " + e + " trying to create element '" +
                         className +"'");
      root = doc.createElement ("Invalid_Name");
    }

    Set seenObjs = new HashSet();
    addNodes(doc, obj, root, seenObjs, searchDepth);
    return root;
  }

  /*
    Handle the case in which an object's class is a private or protected
    implementation class which implements one or more public interfaces or
    abstract classes.
    First check for abstract classes extended by, or interfaces implemented by,
    the class or its superclasses.
    (Note that this stops when it finds the first abstract class or
    interfaces in the class chain.)
    Then, introspect on the abstract class or all the interfaces
    found (note that these are assumed to be public)
    and invoke read property methods
    on the object cast to that class or those interfaces.
  */

  private static Vector specialIntrospection(Object obj) {
    //System.out.println(
    //  "Performing special introspection for:" + obj);
    Class objClass = obj.getClass();
    Class[] interfaces;
    while (true) {
      if (objClass == null) {
        return null;
      }
      if (Modifier.isAbstract(objClass.getModifiers())) {
        interfaces = new Class[1];
        interfaces[0] = objClass;
        break;
      }
      interfaces = objClass.getInterfaces();
      if (interfaces.length != 0) {
        break;
      }
      objClass = objClass.getSuperclass();
    }
    Vector propertyNameValues = new Vector(10);
    for (int i = 0; i < interfaces.length; i++) {
      //      System.out.println("Interface:" + interfaces[i].toString());
      try {
        BeanInfo info = Introspector.getBeanInfo(interfaces[i]);
        PropertyDescriptor[] properties = info.getPropertyDescriptors();
        Vector tmp = getPropertyNamesAndValues(properties, 
                           Beans.getInstanceOf(obj, interfaces[i]));
        if (tmp != null)
          for (int j = 0; j < tmp.size(); j++) 
            propertyNameValues.addElement(tmp.elementAt(j));
      } catch (IntrospectionException e) {
        System.err.println(
          "Exception generating XML for plan object:" + e.getMessage());
      }
    }
    // for debugging
    //    for (int i = 0; i < propertyNameValues.size(); i++) {
    //      PropertyNameValue p = (PropertyNameValue)(propertyNameValues.elementAt(i));
    //      System.out.println("Property Name: " + p.name + " Property Value: " + p.value);
    //    }
    return propertyNameValues;
  }

    /**
     * Get the property names and values (returned in a vector)
     * from the given property descriptors and for the given object.
     *
     * Removes redundant properties from measure objects.
     */

  private static Vector getPropertyNamesAndValues(
      PropertyDescriptor[] properties, Object obj) {
    Vector pv = new Vector();

    // IGNORE JAVA.SQL.DATE CLASS
    if (java.sql.Date.class.isInstance(obj)) {
      return pv;
    }

    if ((obj instanceof AbstractMeasure) &&
        (!(obj instanceof Capacity))) {
      // Special case code. Capacity is a duple of Measures so there's no common
      // unit to pull out.
      properties = 
        prunePropertiesFromMeasure ((AbstractMeasure)obj, properties);
    }

    for (int i = 0; i < properties.length; i++) {
      PropertyDescriptor pd = properties[i];
      Method rm = pd.getReadMethod();
      if (rm == null) {
        continue;
      }

      // invoke the read method for each property
      Object childObject = getReadResult (obj, rm);
      if (childObject == null) {
        continue;
      }
      
      // add property name and value to vectorarray
      String name = pd.getName();
      if (pd.getPropertyType().isArray()) {
        int length = Array.getLength(childObject);
        for (int j = 0; j < length; j++) {
          Object value = Array.get(childObject, j);
          if (value == null) {
            value = "null";
          } else if (value.getClass().isPrimitive()) {
            value = String.valueOf(value);
          }
          pv.add(new PropertyNameValue(name, value));
        }
      } else {
	  if (isPrimitive(childObject.getClass()))
	      childObject = String.valueOf(childObject);

	  pv.add(new PropertyNameValue(name, childObject));
      }
    }

    Collections.sort(pv, lessStringIgnoreCase);
    return pv;
  }

  private static final Comparator lessStringIgnoreCase = 
    new Comparator() {
        public int compare(Object first, Object second) {
          String firstName = ((PropertyNameValue)first).name;
          String secondName = ((PropertyNameValue)second).name;
          return firstName.compareToIgnoreCase(secondName);
        }
      };

    /** 
     * Invoke read method on object
     * 
     * @return Object that is the result of the read
     */
    protected static Object getReadResult (Object obj, Method rm) {
	// invoke the read method for each property
	Object childObject = null;
	try {
	    childObject = rm.invoke(obj, null);
	} catch (InvocationTargetException ie) {
	    System.err.println("Invocation target exception invoking: " + 
			       rm.getName() + 
			       " on object of class:" + 
			       obj.getClass().getName());
	    System.err.println(ie.getTargetException().getMessage());
	} catch (Exception e) {
	    System.err.println("Exception " + e.toString() + 
			       " invoking: " + rm.getName() + 
			       " on object of class:" + 
			       obj.getClass().getName());
	    System.err.println(e.getMessage());
	}
	return childObject;
    }

    /** 
     * Removes redundant measure properties.
     * Returns only the common unit measure.  
     * For example, for Distance, returns only the meters property and discards furlongs.
     *
     * (Converts underscores in common unit names.)
     * 
     * @param measure needed so can get common unit
     * @param properties initial complete set of measure properties
     * @return array containing the one property descriptor for the common unit property
     */
    protected static PropertyDescriptor[] prunePropertiesFromMeasure (AbstractMeasure measure, 
								      PropertyDescriptor [] properties) {
	
        String cu = measure.getUnitName(measure.getCommonUnit());
        if (cu == null) {
          return new PropertyDescriptor[0];
        }

	int pos = 0;
	int underIndex = -1;
	String noUnders = "";
	while ((underIndex = cu.indexOf ('_', pos)) != -1) {
	    noUnders = noUnders + cu.substring (pos, underIndex) +
		cu.substring (underIndex+1, underIndex+2).toUpperCase ();
	    pos = underIndex+2;
	}
	while ((underIndex = cu.indexOf ('/', pos)) != -1) {
	    noUnders = noUnders + cu.substring (pos, underIndex) + "Per" +
		cu.substring (underIndex+1, underIndex+2).toUpperCase ();
	    pos = underIndex+2;
	}
	noUnders = noUnders + cu.substring (pos);
	for (int i = 0; i < properties.length; i++) {
	    PropertyDescriptor pd = properties[i];

	    if (pd.getName ().equals (noUnders)) {
		return new PropertyDescriptor[]{pd};
            }
	}
	return null;
    }
	

  /** 
   * <b>Recursively</b> introspect and add nodes to the XML document.
   * <p>
   * Keeps a Set of objects (as these can be circular) and stops 
   * when it tries to introspect over an object a second time.
   * <p>
   * Also keeps a depth counter, decrements for each call to addNodes, 
   * and stops when the counter is zero.  Use Integer.MAX_VALUE to 
   * indicate an unlimited search.
   */

  private static void addNodes(
      Document doc, Object obj, 
      Element parentElement, 
      Set seenObjs,
      int searchDepth) {
    if (obj == null) {
      return;
    }

    Class objectClass;

    if (LockedPG.class.isInstance(obj)) {
      objectClass = ((LockedPG) obj).getIntrospectionClass();
    } else if (LockedPGSchedule.class.isInstance(obj)) {
      objectClass = ((LockedPGSchedule) obj).getIntrospectionClass();
    } else {
      objectClass = obj.getClass();
    }


    if (((searchDepth <= 0) &&
         (obj instanceof UniqueObject)) ||
        (!(seenObjs.add(obj)))) {
      // Already seen this object or reached maximum depth. 
      // Write the UID if possible, otherwise write the "toString".
      //
      // System.out.println(
      //   "Object traversed already/max depth: " + 
      //   obj.getClass().toString() + " " + obj);
      String sID;
      UID uid;
      if ((obj instanceof UniqueObject) &&
          ((uid = (((UniqueObject)obj).getUID())) != null) &&
          ((sID = uid.toString()) != null)) {
        Element item = doc.createElement("UID");
        item.appendChild(doc.createTextNode(sID));
        parentElement.appendChild(item);
      } else {
        parentElement.appendChild(doc.createTextNode(obj.toString()));
      }
      return;
    }

    BeanInfo info = null;
    Vector propertyNameValues;

    if (Asset.class.isInstance(obj)) {
      propertyNameValues = 
        AssetIntrospection.fetchAllProperties((Asset)obj);
    } else {
      int mods = objectClass.getModifiers();
      //System.out.println("Introspecting on: " + objectClass + 
      //    " modifiers: " + Modifier.toString(mods));
      if (!Modifier.isPublic(mods)) {
        propertyNameValues = specialIntrospection(obj);
      } else {
        try {
          info = Introspector.getBeanInfo(objectClass);
        } catch (IntrospectionException e) {
          System.err.println("Exception in converting object to XML: " + 
                             e.getMessage());
        }

        propertyNameValues = 
          getPropertyNamesAndValues(info.getPropertyDescriptors(), obj);
      }
    }

    // add the nodes for the properties and values
    for (int i = 0; i < propertyNameValues.size(); i++) {
      PropertyNameValue pnv = 
        (PropertyNameValue)propertyNameValues.elementAt(i);
      Object propertyValue = pnv.value;
      String propertyName = pnv.name;
      // check if this should be a leaf
      boolean isLeaf;
      Class propertyClass = propertyValue.getClass();

      String propertyClassName = propertyClass.getName();
      isLeaf = ((propertyClassName.equals("java.lang.String")) ||
		(propertyClassName.equals("java.lang.Class"))); 

      if (isLeaf) {
        // leaf node
        Element item = doc.createElement(propertyName);
        item.appendChild(doc.createTextNode(propertyValue.toString()));
        parentElement.appendChild(item);
      } else {
        // this removes the class name following the $ for Locked classes
        int index = propertyName.indexOf('$');
        if (index > 0) {
          propertyName = propertyName.substring(0, index);
        }
        Element item = doc.createElement(propertyName);
        parentElement.appendChild(item);
        // recurse!
        addNodes(doc, propertyValue, item, seenObjs, (searchDepth-1));
      }
    }
  }

    /**
     * Includes Double, Integer, etc. and Boolean as primitive types.
     *
     * Checks to see if class is a direct descendant of Number or a 
     * Boolean.
     *
     * @return true when class is of a primitive type
     */
    protected static boolean isPrimitive (Class propertyClass) {
      if (propertyClass.isPrimitive())
	  return true;
      try {
	  Class superClass = propertyClass.getSuperclass ();
	  if (superClass.equals (numberClass))
	      return true;
	  if (propertyClass.equals (booleanClass))
	      return true;
      } catch (Exception e) {
	  System.err.println ("Exception " + e);
      }
      
      return false;
    }
}










