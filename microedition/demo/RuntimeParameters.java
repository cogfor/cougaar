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
package org.cougaar.microedition.demo;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.Properties;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;

public class RuntimeParameters {

    Properties props=new Properties();
    String resourceName="default.properties";
    Object parent=null;
    boolean verbose
        //=false;
        =true;

    public void setVerbose(boolean b) {
        verbose=b;
    }

    //Construct the application
    public RuntimeParameters() {
    }

    public RuntimeParameters(Object parent) {
        this.parent=parent;
    }

    public RuntimeParameters(String resourceName) {
        this.resourceName=resourceName;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    // allows you to get a number property without having doing the error
    static int getProperty(Properties p, String key, int adefault) {
      String str=p.getProperty(key);
      int rc=adefault;
      try {
        rc  = Integer.parseInt(str);
      } catch (NumberFormatException nfe) {
        rc=adefault;
      }
      return rc;
    }

    /**
     * Loads properties from a java resource.  This will load the
     * named resource identifier into the given properties instance.
     *
     * @param properties the Properties instance to receive the properties
     * @param resourceName the name of the resource to load
     * @param verbose indicates whether status messages should be printed
     */
    public boolean load()
    {
        InputStream propsIn=null;
        if (parent!=null) {
            Class kl=parent.getClass();
            System.out.println("loading properties.  File: ["
               +resourceName+"] "
               +"Package location is same as for class: ["+kl+"]");
            propsIn = kl.getResourceAsStream(resourceName);
        } else {
            System.out.println("loading props from FileInputStream("
                           +resourceName+")");
            try {
                propsIn = new FileInputStream(resourceName);
            } catch (Exception ex) {
                ex.printStackTrace();
                propsIn=null;
            }
        }

        if (propsIn == null) {
            if (verbose) {
                System.err.println("Unable to locate resources: "
                                   + resourceName);
            }
            return false;
        } else {
            try {
                props.load(propsIn);
                return true;
            } catch (java.io.IOException e) {
                if (verbose) {
                    System.err.println("Caught IOException loading: "
                                   + resourceName);
                    e.printStackTrace();
                }
                return false;
            }
        }
    }

    public void list(PrintStream ps) {
        props.list(ps);
    }
    public boolean loadProperties(URL url, boolean verbose)
    {

        try {
            InputStream propsIn = url.openStream();
            props.load(propsIn);
            return true;
        } catch (java.io.IOException e) {
            if (verbose) {
                System.err.println("Caught IOException loading resources: "
                                   + url);
            }
            return false;
        }

    }

    public void addToSystemProperties() {
      String key, value, oldval;
      for (Enumeration pne=props.propertyNames();
                  pne.hasMoreElements();) {
                key = (String)pne.nextElement();
                value = props.getProperty(key);
                oldval=System.setProperty(key, value);
          if (verbose&&oldval!=null) {
            System.err.println("Information: Replaced existing System.property.");
            System.err.println("Information:   key=["+key+"]");
            System.err.println("Information:   old value=["+oldval+"]");
            System.err.println("Information:   new value=["+value+"]");
          }
      }
    }

    static public int getSystemProperty(String key, int defaultVal) {
      String defaultValStr=null;
      int rcInt=defaultVal;

      try {
        defaultValStr = System.getProperty(key);
        rcInt=Integer.parseInt(defaultValStr);
      } catch (Exception ex) {
      }
      return rcInt;
    }

    static public int getSystemProperty(String key, int defaultVal,
          int invalidVal, boolean warnIfInvalid) {
      int rcInt=invalidVal;
      rcInt=getSystemProperty(key, invalidVal);
      if (rcInt==invalidVal) {
        rcInt=defaultVal;
        if (warnIfInvalid) {
          System.err.println("Warning: No valid "+key
            +" value specified.  Defaulting to: "+defaultVal);
        }
      }
     return rcInt;
    }

    static public String getSystemProperty(String key, String defaultVal,
      boolean warnIfNoValue) {
       String rcStr=System.getProperty(key);
       if (rcStr==null) {
        rcStr=defaultVal;
        if (warnIfNoValue) System.err.println("Warning: No valid "+key
          +" value specified.  Defaulting to: "+rcStr);
       }
       return rcStr;
    }



    static void test(String[] args) {
      RuntimeParameters myprp=new RuntimeParameters();
      System.out.println("No properties yet so the list call should give nothing...");
      myprp.list(System.out);
      System.out.println("Adding test properties...");
      myprp.setProperty("asd", "testValue_1");
      myprp.setProperty("a.s.d", "testValue_2");
      myprp.setProperty("user.name", "myNewUserName");
      System.out.println("Properties set so the list call should give pairs...");
      myprp.list(System.out);

      System.out.println("Loading from defaultProperties.ini...");
      myprp.load();
      System.out.println("Calling list...");
      myprp.list(System.out);

      System.out.println("Calling list on System.properties...");
      Properties System_properties;
      System_properties=System.getProperties();
      System_properties.list(System.out);
      System.out.println("Adding my properties to System.properties...");
      myprp.addToSystemProperties();
      System.out.println("Calling my list...");
      myprp.list(System.out);
      System.out.println("Calling list on System.properties...");
      System_properties=System.getProperties();
      System_properties.list(System.out);

      try {} finally {
          System.out.println("Done.");
      }
    }    // end-test

    //Main method
    public static
    void main(String[] args) {
        // RuntimeParameters.
        test(args);
    } // end main
}

