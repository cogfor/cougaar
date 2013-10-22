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

import javax.swing.UIManager;
import java.awt.*;
import java.util.Vector;
import java.util.Iterator;
import java.awt.event.*;
import java.net.*;
import java.io.*;

/**
 * Displays demo UI and controls demo.
 */
public class RobotDemoUI {
  static long SLEEP_TIME=500;
  boolean packFrame = false;
  RobotDemoUIFrame frame;
  String url;

  /**
   * Construct the application and display the main window.
   */
  public RobotDemoUI() {
    initializeSystemProperties();
    url=System.getProperty("robotDataUrl");
    System.out.println("Getting robot data from url: "+url);
    frame = new RobotDemoUIFrame(this);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { System.exit(0);  }
    });

    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);

    if (robotUpdateThread !=null) {
      robotUpdateThread.finish();
    }
    robotUpdateThread=new RobotUpdateThread();
    robotUpdateThread.start();
  }

  /**
   * @return URL to use to get robot data
   */
  String getUrl() { return url; }

  private static Vector robotProxies=new Vector();

  /**
   * Return an iterator of robot proxies.
   */
  public static Iterator getRobotInfo() {
    return robotProxies.iterator();
  }


  RobotUpdateThread robotUpdateThread;

  public void launchCommand()
  {
    String launchurl="";
    try
    {
      launchurl=System.getProperty("launchUrl");
      System.out.println("Launch URL = " +launchurl);
    }
    catch(Exception ex)
    {
      System.err.println("Error:  Need to set launchUrl");
      return;
    }
    BufferedReader br=null;
    try
    {
      br=new BufferedReader(
        new InputStreamReader(new URL(launchurl).openStream()));
      for (String line = br.readLine(); line!=null; line = br.readLine()) {
        System.out.println(line);
      }
    }
    catch (Exception ex)
    {
      System.err.println("");
    }
    finally
    {
      try {
        if (br!=null) {
          br.close();
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Start the demo.
   */
  public void startUpdates()
  {
    /*
    if (robotUpdateThread !=null) {
      robotUpdateThread.finish();
    }
    robotUpdateThread=new RobotUpdateThread();
    robotUpdateThread.start();
    */
    setDemoActive(true);
  }

  /**
   * Halt the demo.
   */
  public void stopUpdates()
  {
    /*
    if (robotUpdateThread!=null) {
      robotUpdateThread.finish();
    }
    */
    setDemoActive(false);
  }

  private boolean demoActive=false;
  /**
   * Return the active state of the demo (true for active).
   */
  private boolean getDemoActive() { return demoActive; }

  /**
   * Set the active state for the demo and notify the COUGAAR society that
   * it should begin.
   */
      private void setDemoActive(boolean toState) {
      String urlBase="";
      String urlSuffix="";
      try {
        urlBase=System.getProperty("startSystemUrlBase");
      }
      catch(Exception ex) {
        System.err.println("Error:  Need to set laptopUrlBase");
      }
      try {
        urlSuffix=System.getProperty("startSystemUrlSuffix");
        //if (urlSuffix==null) urlSuffix="";
      }
      catch(Exception ex) {
        System.err.println("Error:  Need to set laptopUrlSuffix");
      }
      String fullUrl=urlBase+urlSuffix+toState;
      System.out.println("Retrieving data from url: ["+fullUrl+"]");
      BufferedReader br=null;
      try {
        br=new BufferedReader(
          new InputStreamReader(new URL(fullUrl).openStream()));
        for (String line = br.readLine(); line!=null; line = br.readLine()) {
          System.out.println(line);
        }
      } catch (Exception ex) {
        System.err.println("Error:  PSP to start society indicated a failure.  Check that the PSP is configured correctly.  Url used: ["+fullUrl+"]");
        // ex.printStackTrace();
      } finally {
        try {
          if (br!=null) {
            br.close();
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    } // end hitPSP

    static int steps=0;  // just for testing stuff

  /**
   * Thread for continuously updating the display with current robot state
   * data.  Robots are polled while keepPolling flag is set.
   */
  class RobotUpdateThread extends Thread {

    /**
     * Construct object.
     */
    RobotUpdateThread() {
      synchronized  (robotProxies) {
        robotProxies.clear();
      }  // end sync
    } // end ctor

    /* ======================
  RobotProxy makeNewRobotProxy(int xloc, int yloc, double heading) {
    RobotProxy rp=new RobotProxy(xloc, yloc);
    rp.setHeading(heading);
    return rp;
  }
  =================== */

    boolean keepPolling=true;
  /**
   * Stop polling for updated robot data.
   */
  public void finish() {
    keepPolling=false;
  }

  /**
   * Get robot proxy for the specified robot.
   */
  RobotProxy getRobotInfo(String id) {
    RobotProxy rp, ret=null;
    for (Iterator it=robotProxies.iterator(); it.hasNext()&& ret==null; ) {
      rp=(RobotProxy)it.next();
      if (rp.getId().equals(id)) {
        ret=rp;
      }
    }
    return ret;
  }

  /**
   * Add robotProxy to the current set of robotProxies.
   */
  void addRobotInfo(RobotProxy rp) {
    synchronized(robotProxies) {
      robotProxies.add(rp);
    }
  }

  /**
   * Update current robot state information.
   */
  synchronized public boolean updateRobot(RobotDataImport.RobotData rd) {
      Double d;
      String id=rd.getId();
      RobotProxy rp=getRobotInfo(id);
      if (rp==null) {
        rp = new RobotProxy(id, rd.getLat(), rd.getLon(), rd.isLightOn(), rd.getPictureAvailable());
        //System.out.println("RD new : "+id+", "+rd.getLat()+", "+rd.getLon()+", l:"+rd.isLightOn()+", "+rd.getPictureAvailable());
        d=rd.getHeading();
        if (d!=null) { rp.setHeading(d.doubleValue()); }
        d=rd.getBearing();
        if (d!=null) { rp.setBearing(d.doubleValue()); }
        addRobotInfo(rp);
      } else {
        //System.out.println("RD up : "+id+", "+rd.getLat()+", "+rd.getLon()+", l:"+rd.isLightOn()+", "+rd.getPictureAvailable());
        rp.update(rd.getLat(), rd.getLon(), rd.isLightOn(), rd.getPictureAvailable());
        d=rd.getHeading();
        if (d!=null) { rp.setHeading(d.doubleValue()); }
        d=rd.getBearing();
        if (d!=null) { rp.setBearing(d.doubleValue()); }
        else { rp.clearBearing(); }
      }
      return true;
    }

  /**
   * UI test stub code.
   * Obtain updated information from robots
   * @return true if any robot had info updated
   */
    synchronized public boolean updateRobots_test() {
      RobotProxy rp;
      // System.out.println("in update robots");
      double heading=0;
      ++steps;
      for (Iterator iter=RobotDemoUI.getRobotInfo(); iter.hasNext();) {
        rp=(RobotProxy)iter.next();
        if (rp.getId().equalsIgnoreCase("spinner")) {
          rp.setHeading(rp.heading+15);
          rp.setLightOn(!rp.isLightOn());
        } else if (rp.getId().equalsIgnoreCase("spinner2")) {
          rp.setHeading(rp.heading-45);
        } else {

          if (!rp.getId().startsWith("stopped_")) {
            if (steps == 2) {
              rp.setId("stopped_"+rp.getId());
              rp.setBearing(2);
              steps++;
            } else if (steps == 10) {
              rp.setId("stopped_"+rp.getId());
              rp.setBearing(-15);
              steps++;
            } else {
              rp.stepForward(5);
            }
          }
        }
      }
      return true;
    }

    /**
     * Query for robot data and update display.
     */
    public void run() {
      System.out.println("Thread started "+this);

      // Run until exit(), but only poll while keepPolling is true
      while (true) {

        // Get new data from URL
        if (keepPolling) {
          RobotDataImport rdi=new RobotDataImport(getUrl());
          Iterator it=rdi.iterator();

          // Use each record to update the system
          while (it.hasNext()) {
            RobotDataImport.RobotData rd=(RobotDataImport.RobotData)it.next();
            if (updateRobot(rd)) {
              frame.repaint();
            }
          }
        } else {
          frame.repaint();
        }
        try {
          sleep(SLEEP_TIME);
        } catch (Exception ex) {
        }
      }
      //System.out.println("Thread done "+this);
    }
  }

  /**
   * Load application specific system properties from properties file.
   */
  private void initializeSystemProperties() {
    RuntimeParameters props=new RuntimeParameters("robotdemoui.properties");
    props.load();
    props.list(System.out);
    props.addToSystemProperties();
  }

  /**Main method*/
  public static void main(String[] args) {
    if (args.length>0) {
        System.setProperty("robotDataUrl", args[0]);
    } else {
        System.setProperty("robotDataUrl", "file:/data/robot/input.txt");
    }

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    new RobotDemoUI();
  }
}