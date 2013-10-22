/*
 * <copyright>
 * 
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.util;

import java.util.*;

/**
 * This adapter class implements the functions of an ObjectFactory.  Just override
 * addClasses to call addClass with each class specific to this JVM.
 */
public abstract class ObjectFactoryAdapter implements ObjectFactory {

  public ObjectFactoryAdapter() {
    addClasses();
  }

  protected Vector classes = new Vector();

  protected abstract void addClasses();

  protected void addClass(Class clazz) {
      classes.addElement(clazz);
  }

  protected void addClass(String className) {
    try {
      addClass(Class.forName(className));
    } catch (Exception ex) {
      System.err.println("Error initializing ObjectFactory with class "+className);
      ex.printStackTrace();
    }
  }

  public Object getObjectME(Class ofType) {
    Object ret = null;
    for (Enumeration enm = classes.elements(); enm.hasMoreElements();) {
      Class clazz = (Class) enm.nextElement();
      if (ofType.isAssignableFrom(clazz)) {
        try {
          ret = clazz.newInstance();
          break;
        } catch (Exception iae) {
          System.err.println("Error instantiating "+ofType.getName());
        }
      }
    }
    return ret;
  }


}