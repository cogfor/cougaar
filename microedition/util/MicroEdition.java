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

/**
 * This class contains one static method which returns the appropriate object given the ME type.
 */
public class MicroEdition {

 static ObjectFactory factory = null;

/**
 * This static variable stores the kvm (j2me) configuration. If null we assume java1.1.
 */
 static String kvmConfig;
 static {
   kvmConfig = System.getProperty("microedition.configuration");
   try {
     if (kvmConfig != null) {
       factory = (ObjectFactory)Class.forName("org.cougaar.microedition.kvm.KvmObjectFactory").newInstance();
     } else {
       factory = (ObjectFactory)Class.forName("org.cougaar.microedition.jvm.JvmObjectFactory").newInstance();
     } // I have no way to distinguish TINI from a java1.1  Good thing it doesn't matter now.
   } catch (Exception ex) {
     System.err.println("Error installing object factory");
     ex.printStackTrace();
   }
 }



/**
 * This constructor does nothing.
 */
  private MicroEdition() {
  }

  /**
   * This static method returns the appropriate object given the ME type.
   *
   * @param   ofType The abstract type of which to make a concrete instance.
   * @return  Object which is an appropriate instantiation of the ofType argument.
   */
  public static Object getObjectME(Class ofType) {
    return factory.getObjectME(ofType);
  }

  /**
   * This static method returns the appropriate object given the ME type.
   *
   * @param   ofType The name of the abstract type of which to make a concrete instance.
   * @return  Object which is an appropriate instantiation of the ofType argument.
   */
  public static Object getObjectME(String ofType) {
    Object ret = null;
    try {
      ret = factory.getObjectME(Class.forName(ofType));
    } catch (ClassNotFoundException cnfe) {
      System.err.println("Error getting micro object "+ofType);
    }
    return ret;
  }

}
