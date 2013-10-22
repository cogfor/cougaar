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

import java.net.*;
import java.io.*;
import java.util.*;


/**
 * Imports data from an URL and provides access to the data via an iterator.
 */
public class RobotDataImport {
  public void updateProxies(String urlString, Vector proxies) {
    RobotDataImportImpl rdii=new RobotDataImportImpl(urlString);
    rdii.updateProxies(proxies);
  }

    // main method is just a test driver
    public static void main(String[] args) {
      Vector proxies=new Vector();
      RobotDataImport rdi=new RobotDataImport();
      rdi.updateProxies("file:/data/robot/input.txt", proxies);
      for (Iterator pit=proxies.iterator(); pit.hasNext(); ) {
        System.out.println("Proxy: "+((RobotProxy)pit.next()));
      }
    }

}

class RobotDataImportImpl {

  public RobotDataImportImpl(String urlString) {
    try {
      URL url=new URL(urlString);
      URLConnection urlConnection=url.openConnection();
      BufferedReader br = new BufferedReader(
        new InputStreamReader(urlConnection.getInputStream()));


      String line;
      for (line=br.readLine(); line!=null; line=br.readLine()) {
        processLine(line);
      }
      if (ht.containsKey(ID)) {
        boolean error=saveCurrentItem();
        if (error) {
          System.err.println("Invalid item at end of file"+(lineCount-1));
          System.err.println("  Item contents ["+ht+"]");
        }
      }
      br.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  /**
   * Get robot proxy for the specified robot.
   */
  RobotProxy getRobotInfo(String id, Vector robotProxies) {
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
  void addRobotInfo(RobotProxy rp, Vector robotProxies) {
      robotProxies.add(rp);
  }

  /**
   * Update current robot state information.
   */
  public void updateRobot(RobotData rd, Vector robotProxies) {
      Double d;
      String id=rd.getId();
      RobotProxy rp=getRobotInfo(id, robotProxies);
      if (rp==null) {
        rp = new RobotProxy(id, rd.getLat(), rd.getLon(), rd.isLightOn(), rd.getPictureAvailable());
        //System.out.println("RD new : "+id+", "+rd.getLat()+", "+rd.getLon()+", l:"+rd.isLightOn()+", "+rd.getPictureAvailable());
        d=rd.getHeading();
        if (d!=null) { rp.setHeading(d.doubleValue()); }
        d=rd.getBearing();
        if (d!=null) { rp.setBearing(d.doubleValue()); }
        addRobotInfo(rp, robotProxies);
      } else {
        //System.out.println("RD up : "+id+", "+rd.getLat()+", "+rd.getLon()+", l:"+rd.isLightOn()+", "+rd.getPictureAvailable());
        rp.update(rd.getLat(), rd.getLon(), rd.isLightOn(), rd.getPictureAvailable());
        d=rd.getHeading();
        if (d!=null) { rp.setHeading(d.doubleValue()); }
        d=rd.getBearing();
        if (d!=null) { rp.setBearing(d.doubleValue()); }
        else { rp.clearBearing(); }
      }
    }

  public void updateProxies(Vector robotProxies) {
//          RobotDataImport rdi=new RobotDataImport(getUrl());
          Iterator it=iterator();

          // Use each record to update the system
          while (it.hasNext()) {
            updateRobot((RobotData)it.next(), robotProxies);
          }
  }

    Hashtable ht=new Hashtable();  // holds a partial record during processing
    final String ID="ID";
    final String LAT="LAT";
    final String LON="LON";
    final String LIGHTON="LIGHTON";
    final String PICTURE="PICTURE";
    final String HEADING="HEADING";
    final String BEARING="BEARING";
    Vector records=new Vector(); // list of records read in so far

    Iterator iterator() { return records.iterator(); }

    int lineCount=0;

    void processLine(String line) {
      try {
        lineCount++;
        // System.out.println("processing line: ["+line+"]");

        line.trim();
        if (line.startsWith("#") || line.length()==0) {
          return;
        }

        StringTokenizer st=new StringTokenizer(line);
        if (st.hasMoreTokens()) {
          String tok1=st.nextToken();
          String tok2=null;
          if (st.hasMoreTokens()) {
            tok2=st.nextToken();
          }

          // System.out.println("key ["+tok1+"] value: ["+tok2+"]");


          if (tok1.equalsIgnoreCase(ID)) {
            if (ht.containsKey(ID)) {
              boolean error=saveCurrentItem();
              if (error) {
                System.err.println("Invalid item ends on line "+(lineCount-1));
                System.err.println("  Item contents ["+ht+"]");
              }
            }
            ht.clear();
            if (tok2.substring(0,4).equalsIgnoreCase("UIC/")) {
              tok2=tok2.substring(4);
            }
            ht.put(ID, tok2);
            // processID(st);
          } else if (!validKey(tok1)) {
            System.err.println("Invalid key ["+tok1+"] on line "+lineCount);
          } else if (!validValue(tok1, tok2)) {
            System.err.println("Invalid value ["+ tok2+"] for key ["+tok1+"] on line "+lineCount);
          } else if (duplicateKey(tok1)) {
            System.err.println("Duplicate key ["+tok1+"] on line "+lineCount);
            System.err.println("  in record ["+ht+"]");
          } else {
            ht.put(tok1.toUpperCase(), tok2);
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    private boolean duplicateKey(String key) {
      boolean ret=false;
      if (ht.containsKey(key)) {
        ret=true;
      }
      return ret;
    }

    private boolean validKey(String key) {
      return key.toUpperCase().equals(ID)
        || key.toUpperCase().equals(LAT)
        || key.toUpperCase().equals(LON)
        || key.toUpperCase().equals(LIGHTON)
        || key.toUpperCase().equals(PICTURE)
        || key.toUpperCase().equals(HEADING)
        || key.toUpperCase().equals(BEARING);
    }

    private boolean validValue(String key, String value) {
      boolean ret=false;
      if (key.toUpperCase().equals(LIGHTON)
        || key.toUpperCase().equals(PICTURE)) {
          // everything including null is a valid boolean
          // anything but the word true is false
          ret= true;
      } else if (key.toUpperCase().equals(LAT)
        || key.toUpperCase().equals(LON)
        || key.toUpperCase().equals(HEADING)
        || key.toUpperCase().equals(BEARING)) {
          ret=isValidDouble(value);
      } else if (key.toUpperCase().equals(ID)) {
          ret=value.length()>0;
      }
      return ret;
    }

    private boolean isValidDouble(String value) {
      boolean ret=false;
      try {
        double d=Double.parseDouble(value);
        ret=true;
      } catch (Exception ex) {
        ret=false;
      }
      return ret;
    }

    /**
     * @return true indicates error
     */
    static long countSaved=0;
    void heartbeat() {
      System.out.print("+");
      if (++countSaved%30 == 0) System.out.println("("+countSaved+")");
      else System.out.flush();
    }
    private boolean saveCurrentItem() {
      // System.out.println("Saving item: "+ht);
      heartbeat();
      double lat, lon, bearing=0, heading=0;
      String id;
      boolean picture=false, lighton=false;
      String tmpstr;

      try {
        id=(String)ht.get(ID);
        lat=Double.parseDouble((String)(ht.get(LAT)));
        lon=Double.parseDouble((String)(ht.get(LON)));

        RobotData rd=new RobotData(id, lat, lon, (String)ht.get(HEADING),
          (String)ht.get(PICTURE), (String)ht.get(LIGHTON), (String)ht.get(BEARING));
        records.add(rd);
        //System.out.println("saved item id: "+rd.getId()+" l: "+rd.isLightOn());
      } catch (Exception ex) {
        System.out.println("Error: Invalid item: "+ht);
        ex.printStackTrace();
      }

      return false;
    }

    class RobotData {
      private double lat, lon;
      private Double bearing, heading;
      private String id;
      private boolean picture, lighton;
      RobotData(String id, double lat, double lon, String head, String pic, String light, String bear) {
        this.id=id;
        this.lat=lat;
        this.lon=lon;
        if (head!=null) {
          try {
            heading=new Double(head);
          } catch (Exception ex) {
          }
        }
        picture=Boolean.valueOf(pic).booleanValue();
        lighton=Boolean.valueOf(light).booleanValue();
        if (bear!=null) {
          try {
            bearing=new Double(bear);
          } catch (Exception ex) {
          }
        }
      }
      double getLat() {return lat; }
      double getLon() { return lon; }
      String getId() { return id; }
      void setPictureAvailable(boolean val) { picture=val; }
      boolean getPictureAvailable() { return picture; }
      void setLightOn(boolean val) { lighton=val; }
      boolean isLightOn() { return lighton; }
      void setHeading(double val) { heading=new Double(val); }
      Double getHeading() { return heading; }
      void setBearing(double val) { bearing=new Double(val); }
      Double getBearing() { return bearing; }
    }

}