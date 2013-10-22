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
package org.cougaar.microedition.demo.ugs;

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
public class UGSDemoUI {
  static long SLEEP_TIME=1000;
//  static long SLEEP_TIME=500;
  boolean packFrame = false;
  UGSDemoUIFrame frame;
  String robotUrl;
  String ugsUrl;

  RobotDataImport robotDataImport=null;
  UGSDataImport ugsDataImport=null;
  TracksDataImport tracksDataImport;
  RoadsDataImport roadsDataImport;
  /**
   * Construct the application and display the main window.
   */
  public UGSDemoUI(RobotDataImport rdi, UGSDataImport udi) {
    initializeSystemProperties();
    robotDataImport=rdi;
    ugsDataImport=udi;
    tracksDataImport=new TracksDataImport();
    roadsDataImport=new RoadsDataImport();
    robotUrl=System.getProperty("robotDataUrl");
    System.out.println("Getting robot data from url: "+robotUrl);
    ugsUrl=System.getProperty("ugsDataUrl");
    System.out.println("Getting ugs data from url: "+ugsUrl);
    frame = new UGSDemoUIFrame(this);
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
          if (roadsDataImport!=null) {
            synchronized (roadsProxies) {
              roadsDataImport.updateProxies(getRoadsDataURL(), roadsProxies);
            }
          }
    frame.setVisible(true);
  }

  /**
   * @return URL to use to get robot data
   */
  String getRobotDataURL() { return robotUrl; }
  String getUGSDataURL() { return ugsUrl; }
  String getTracksDataURL() { return "file://tracks.txt"; }
  String getRoadsDataURL() { return "file://roads.txt"; }

//  private static Vector robotProxies=new Vector();
  private static Vector robotProxies=new Vector();
  private static Vector ugsProxies=new Vector();
  private static Vector tracksProxies=new Vector();
  private static Vector roadsProxies=new Vector();

  /**
   * Return an iterator of robot proxies.
   */
  public static Iterator getRobotInfo() {
    return robotProxies.iterator();
  }

  public static Iterator getUGSInfo() {
    return ugsProxies.iterator();
  }

  public static Iterator getTracksInfo() {
    return tracksProxies.iterator();
  }

  public static Iterator getRoadsInfo() {
    return roadsProxies.iterator();
  }

  boolean bearingsDisplay=true;
  public void toggleBearingsDisplay() {
    bearingsDisplay=!bearingsDisplay;
    for (Iterator uit=getUGSInfo(); uit.hasNext(); ) {
      ((UGSProxy)uit.next()).setBearingsDisplay(bearingsDisplay);
    }
  }

  boolean tracksDisplay=false;
  public void toggleTracksDisplay() {
    tracksDisplay=!tracksDisplay;
    for (Iterator uit=getTracksInfo(); uit.hasNext(); ) {
      ((TracksProxy)uit.next()).setTrackDisplay(tracksDisplay);
    }
  }

  RobotUpdateThread robotUpdateThread;


  //void addRobotInfo(RobotProxy rp) { robotProxies.add(rp); }

  /**
   * Start the demo.
   */
  public void startUpdates() {
    if (robotUpdateThread !=null) {
      robotUpdateThread.finish();
    }
    robotUpdateThread=new RobotUpdateThread();
    robotUpdateThread.start();
//    setDemoActive(true);
  }

  /**
   * Halt the demo.
   */
  public void stopUpdates() {
    if (robotUpdateThread!=null) {
      robotUpdateThread.finish();
      robotUpdateThread=null;
    }
//    setDemoActive(false);
  }

  public void stopSensors() {
    setDemoActive(false);
  }

  public void startSensors() {
    setDemoActive(true);
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
// Begin a replace block for factoring RDI.RobotData objects out of RDUI class
// MOve the following to robotDataImport class
//
//  /**
//   * Get robot proxy for the specified robot.
//   */
//  RobotProxy getRobotInfo(String id) {
//    RobotProxy rp, ret=null;
//    for (Iterator it=robotProxies.iterator(); it.hasNext()&& ret==null; ) {
//      rp=(RobotProxy)it.next();
//      if (rp.getId().equals(id)) {
//        ret=rp;
//      }
//    }
//    return ret;
//  }
//
//  /**
//   * Add robotProxy to the current set of robotProxies.
//   */
//  void addRobotInfo(RobotProxy rp) {
//    synchronized(robotProxies) {
//      robotProxies.add(rp);
//    }
//  }
//
//  /**
//   * Update current robot state information.
//   */
//  synchronized public boolean updateRobot(RobotDataImport.RobotData rd) {
//      Double d;
//      String id=rd.getId();
//      RobotProxy rp=getRobotInfo(id);
//      if (rp==null) {
//        rp = new RobotProxy(id, rd.getLat(), rd.getLon(), rd.isLightOn(), rd.getPictureAvailable());
//        //System.out.println("RD new : "+id+", "+rd.getLat()+", "+rd.getLon()+", l:"+rd.isLightOn()+", "+rd.getPictureAvailable());
//        d=rd.getHeading();
//        if (d!=null) { rp.setHeading(d.doubleValue()); }
//        d=rd.getBearing();
//        if (d!=null) { rp.setBearing(d.doubleValue()); }
//        addRobotInfo(rp);
//      } else {
//        //System.out.println("RD up : "+id+", "+rd.getLat()+", "+rd.getLon()+", l:"+rd.isLightOn()+", "+rd.getPictureAvailable());
//        rp.update(rd.getLat(), rd.getLon(), rd.isLightOn(), rd.getPictureAvailable());
//        d=rd.getHeading();
//        if (d!=null) { rp.setHeading(d.doubleValue()); }
//        d=rd.getBearing();
//        if (d!=null) { rp.setBearing(d.doubleValue()); }
//        else { rp.clearBearing(); }
//      }
//      return true;
//    }
// end of a replace block for factoring RDI.RobotData objects out of RDUI class

//  /**
//   * UI test stub code.
//   * Obtain updated information from robots
//   * @return true if any robot had info updated
//   */
//    synchronized public boolean updateRobots_test() {
//      RobotProxy rp;
//      // System.out.println("in update robots");
//      double heading=0;
//      ++steps;
//      for (Iterator iter=RobotDemoUI.getRobotInfo(); iter.hasNext();) {
//        rp=(RobotProxy)iter.next();
//        if (rp.getId().equalsIgnoreCase("spinner")) {
//          rp.setHeading(rp.heading+15);
//          rp.setLightOn(!rp.isLightOn());
//        } else if (rp.getId().equalsIgnoreCase("spinner2")) {
//          rp.setHeading(rp.heading-45);
//        } else {
//
//          if (!rp.getId().startsWith("stopped_")) {
//            if (steps == 2) {
//              rp.setId("stopped_"+rp.getId());
//              rp.setBearing(2);
//              steps++;
//            } else if (steps == 10) {
//              rp.setId("stopped_"+rp.getId());
//              rp.setBearing(-15);
//              steps++;
//            } else {
//              rp.stepForward(5);
//            }
//          }
//        }
//      }
//      return true;
//    }

    /**
     * Query for robot data and update display.
     */
    public void run() {
      System.out.println("Thread started "+this);

      // Run until exit(), but only poll while keepPolling is true
      while (true) {

        // Get new data from URL
        if (keepPolling) {
// Begin a replace block for factoring RDI.RobotData objects out of RDUI class
// Replace this:
//          RobotDataImport rdi=new RobotDataImport(getUrl());
//          Iterator it=rdi.iterator();
//
//          // Use each record to update the system
//          while (it.hasNext()) {
//            RobotDataImport.RobotData rd=(RobotDataImport.RobotData)it.next();
//            if (updateRobot(rd)) {
//              frame.repaint();
//            }
//          }
// with this:
//          RobotDataImport rdi=new RobotDataImport(getUrl());
//          synchronized (robotProxies) { rdi.updateProxies(robotProxies); }
//          RobotDataImport rdi=new RobotDataImport();
          if (robotDataImport!=null) {
            synchronized (robotProxies) {
              robotDataImport.updateProxies(getRobotDataURL(), robotProxies);
            }
          }
          if (ugsDataImport!=null) {
            synchronized (ugsProxies) {
              ugsDataImport.updateProxies(getUGSDataURL(), ugsProxies);
            }
          }
          if (tracksDataImport!=null) {
            synchronized (tracksProxies) {
              tracksDataImport.updateProxies(getTracksDataURL(), tracksProxies);
            }
          }
          if (roadsDataImport!=null) {
            synchronized (roadsProxies) {
              roadsDataImport.updateProxies(getRoadsDataURL(), roadsProxies);
            }
          }
          frame.repaint();
// end of a replace block for factoring RDI.RobotData objects out of RDUI class
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
  // file:alpreg.ini http://192.233.51.222:5557/$SurveillanceManager/UGSDATA.PSP
  public static void main(String[] args) {
    if (args.length>0) {
        System.setProperty("robotDataUrl", args[0]);
    } else {
        System.setProperty("robotDataUrl", "file:/data/robot/input.txt");
    }
    if (args.length>1) {
        System.setProperty("ugsDataUrl", args[1]);
    } else {
        System.setProperty("ugsDataUrl", "file:/data/ugs/input.txt");
    }

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }

//    new UGSDemoUI();
//    new UGSDemoUI(new RobotDataImport(), null);
    new UGSDemoUI(null, new UGSDataImport());
//    new UGSDemoUI(new RobotDataImport(), new UGSDataImport());
  }
}


class TracksDataImport {
  Vector proxies=new Vector();

  TracksDataImport() {
    proxies.add(makeProxy("Track1"));
    proxies.add(makeProxy("Track2"));

    String debugTrackProp=System.getProperty("debugTrack");
    System.out.println("checking debugTrack value: ["+debugTrackProp+"]");
    if (debugTrackProp!=null && debugTrackProp.equalsIgnoreCase("true")) {
     proxies.add(makeProxy("DebugGrid"));
    }
  }
  TracksProxy makeProxy(String id) {
    Vector lats=new Vector();
    Vector lons=new Vector();
//    double lat=40.15;
//    double lon=-100.18;
    double lat=40.0;
    double lon=-100.0;
    double latInc=0;
    double lonInc=0;
    int entries=4;
//    if (id.equalsIgnoreCase("Track1")) {
//      entries=2;
//      lat+=.3;
//      latInc=.116/(entries-1);
//      lon+=.02;
//      lonInc=.04/(entries-1);
//    } else if (id.equalsIgnoreCase("Track2")) {
//      entries=6;
//      lonInc=.08/(entries-1);
//      lon+=.025;
//      lat+=.7;
//      latInc=-.086/(entries-1);
    if (id.equalsIgnoreCase("Track1")) {
      entries=3;
//      entries=8;
//      latInc=.78/(entries-1);
//      lat+=.1;
//      lon-=.42;
//      lonInc=-.16/(entries-1);
      latInc=.1/(entries-1);
      lon-=.53;
      lat+=.65;
      lonInc=-.02/(entries-1);
    } else if (id.equalsIgnoreCase("Track2")) {
      entries=5;
      latInc=.06/(entries-1);
      lon-=.5;
      lat+=.7;
      lonInc=-.3/(entries-1);
    } else {
      return makeTrack3(id);
    }
    for (int idx=0; idx<entries; idx++) {
      if (id.equalsIgnoreCase("Track2")&&idx==2)
        continue;

      double latRan=0;
      double lonRan=0;
//      if (idx>0 && idx<entries-1) {
//        latRan=Math.random()*.02;
//        lonRan=Math.random()*.004;
//      }
      lats.add(new Double(lat+latInc*idx+latRan));
      lons.add(new Double(lon+lonInc*idx+lonRan));
    }
    return new TracksProxy(id, lats, lons);
  }
  private TracksProxy makeTrack3(String id) {
    Vector lats=new Vector();
    Vector lons=new Vector();

    double firstLat=40.1;
    double firstLon=-100.1;

    for (float latidx=0; latidx < .9; latidx+=.1) {
      for (float lonidx=0; lonidx < .9; lonidx+=.1) {
        System.out.println(latidx+" "+lonidx);
        System.out.println("("+(firstLat+latidx)+", "+(firstLon-lonidx)+")");
        lats.add(new Double(firstLat+latidx));
        lons.add(new Double(firstLon-lonidx));
      }
    }
    return new TracksProxy(id, lats, lons);
  }

  void updateProxies(String url, Vector theProxies) {
    if (theProxies.isEmpty()) {
      theProxies.addAll(proxies);
    }
  }
}


class RoadsDataImport {
  Vector proxies=new Vector();

  RoadsDataImport() {
//    proxies.add(makeProxy("Road1"));
//    proxies.add(makeProxy("Road2"));
//    proxies.add(makeProxy("Road3"));
//    proxies.add(makeProxy("Road4"));
    proxies.add(makeProxy("Road1b"));
    proxies.add(makeProxy("Road2b"));
    proxies.add(makeProxy("Road3b"));
    proxies.add(makeProxy("Road4b"));
  }
  RoadsProxy makeProxy(String id) {
    Vector lats=new Vector();
    Vector lons=new Vector();
    double lat=40.15;
    double lon=-100.18;
    double latInc=0;
    double lonInc=0;
    double latoff1=0, latoff2=0;
    double lonoff1=0, lonoff2=0;
    int entries=4;
    if (id.equalsIgnoreCase("Road1")) {
      entries=4*3;
      latInc=.116/(entries/3-1)*2;
      lonInc=.04/(entries/3-1)*2;
      lat+=.3-(latInc*entries/3);
      lon+=.02-(lonInc*entries/3);
    } else if (id.equalsIgnoreCase("Road2")) {
      entries=6*3;
      lonInc=.08/(entries/3-1);
//      latInc=-.086/(entries/3-1);
      latInc=-.078/(entries/3-1);
      lon+=.025-(lonInc*entries/3);
      lat+=.7-(latInc*entries/3);
    } else if (id.equalsIgnoreCase("Road3")) {
      entries=6*3;
      latInc=-.086/(entries/3-1)*8;
      lonInc=.03/(entries/3-1);
      lon+=.055-(lonInc*entries/3);
      lat+=.7-(latInc*entries/3);
    } else if (id.equalsIgnoreCase("Road4")) {
      entries=6*3;
      latInc=-.086/(entries/3-1);
      lonInc=.08/(entries/3-1);
      lon+=.025-(lonInc*entries/3);
      lat+=.2-(latInc*entries/3);
    } else if (id.equalsIgnoreCase("Road1b")) {
      entries=2;
      latInc=1;
      lonInc=-.2;
      lon=-100.4;
      lat=40;
    } else if (id.equalsIgnoreCase("Road2b")) {
      entries=2;
      latInc=.6;
      lonInc=1;
      lon=-101;
      lat=40.3;
    } else if (id.equalsIgnoreCase("Road3b")) {
      entries=2;
      latInc=-.2;
      lonInc=1;
      lon=-101;
      lat=40.4;
    } else if (id.equalsIgnoreCase("Road4b")) {
      entries=2;
      latInc=-.2;
      lonInc=1;
      lon=-101;
      lat=40.8;
    } else {
      entries=6*3;
      latInc=-.086/(entries/3-1);
      lonInc=.08/(entries/3-1);
      lon+=.025-(lonInc*entries/3);
      lat+=.9-(latInc*entries/3);
    }
    for (int idx=0; idx<entries; idx++) {

      double latRan=0;
      double lonRan=0;
//      if (idx<entries-1) {
//        latRan=Math.random()*.02;
//        lonRan=Math.random()*.004;
//      }
      lats.add(new Double(lat+latInc*idx+latRan));
      lons.add(new Double(lon+lonInc*idx+lonRan));
    }
    return new RoadsProxy(id, lats, lons);
  }
  void updateProxies(String url, Vector theProxies) {
    if (theProxies.isEmpty()) {
      theProxies.addAll(proxies);
    }
  }
}
