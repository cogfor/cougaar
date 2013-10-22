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
package org.cougaar.microedition.tini;

import java.util.*;
import java.io.*;
import javax.comm.*;

import org.cougaar.microedition.asset.LocationResource;
import org.cougaar.microedition.asset.ControllerResource;
import org.cougaar.microedition.shared.Constants;

/**
 * Title:        Differential GPS Resource
 * Description:  This class polls the RT-Star differential GPS receiver
 *               over a serial interface and maintains a private database
 *               of position properties.  Accessor methods are available to
 *               provide read-only access to latitude, longitude, altitude,
 *               GPS date/timestamp, number of sattelite vehicles used in
 *               the GPS solution, groundspeed, heading, velocities (N,E,V),
 *               and overall health.  The data are extracted from DGPS
 *               message 20 (See the BAEC All-Star User's manual for the
 *               message format).
 *                    - one paramater is "port" which defaults to "serial1".
 *                    - one parameter is "debug" which defaults to false.
 * @author       ww/jdg/dav
 * @version      1.0
 */

/**
 * Special Note: One must be careful when converting change in Lat/Lon into
 *               distance!  For latitude, it is a simple scalar conversion,
 *               but for Longitude....  see below.
 *
 * Convert Change In Lat/Lon into Feet
 *
 * Latitude - 1 minute of change in latitude is equal to 1 nautical mile, or
 * 6076.115486 feet. There are 60 minutes to one degree, so therefore there
 * are 364,560 feet to one degree change of latitude, or about 69 miles.
 *
 * Longitude - 1 minute of change in longitude equals 1 nautical mile times
 * cosine of latitude.
 *
 * Example - going straight west 30 minutes from the Lowe's store in Ames, IA
 * (located exactly at 42 degrees N lat) =
 *
 *       (30 * 6076.115...) * cos(42) = 135,460 ft, or 25.65 miles
 *
 * (somewhere near Boone/Greene county line, as if you cared.)
 */

public class TiniGPSLocationControllerResource extends ControllerResource implements LocationResource {

  private String portName = "serial1";
  private boolean debug = false;

  //private Date dgpsDate = new Date();
  private boolean dgpsHealthy=false;
  private int dgpsSatellites = 0;
  private double dgpsHeading = 0.0;
  private double dgpsLatitude = 38.0;
  private double dgpsLongitude = -77.0;
  private double dgpsAltitude = 0.0;
  private int dgpsHour = 1;
  private int dgpsMinute = 0;
  private int dgpsDay = 1;
  private int dgpsMonth = 1;
  private int dgpsYear = 1980;
  private double dgpsSeconds = 0.0;
  private float dgpsGroundSpeed, dgpsVelocityNorth, dgpsVelocityEast,
                dgpsVelocityVertical ;

  private double computedheading = 0.0;
  private double computedspeed = 0.0;
  private static final double DGPSVELACCURACY = 0.035; //meters per sec
  private static final double MINVELOCITY = DGPSVELACCURACY; //meters per second
  private static final double MAXVELOCITY = 10.0;
  private double MAXHFOM = 1.0; //meters 95% accuracy

  private static final int MINSATS = 4;
  private static final int MAXSATS = 12;

  public TiniGPSLocationControllerResource() {}

  // accessor methods for DGPS properties

  public double getHeading() { // returns true bearing, in degrees
    return computedheading;        // (zero is true North)
  }

  public void adjustHeading(double adjustmentdeg)
  {
    computedheading += adjustmentdeg;
    if(computedheading > 360.0) computedheading -= 360.0;
    if(computedheading < 0.0) computedheading += 360.0;
  }

  public double getAltitude() { // returns altitude, in meters above SL
    return dgpsAltitude;
  }

  public double getLongitude() { // returns Longitude, in degrees, where
    return dgpsLongitude;        // negative is Westward from Greenwich
  }

  public double getLatitude() { // returns Latitude, in degrees, where
    return dgpsLatitude;        // negative is South of the Equator
  }

  public int getNumSVs() { // returns the number of Satellite Vehicles
    return dgpsSatellites;   // used from the last DGPS message 20
  }

  public float getGroundSpeed() { // returns ground speed, in Meters/sec
    return dgpsGroundSpeed;       //
  }

  public float getVelocityNorth() { // returns Meters/sec
    return dgpsVelocityNorth;       // (true North vector)
  }

  public float getVelocityEast() { // returns Meters/sec
    return dgpsVelocityEast;       // (wrt true North)
  }

  public float getVelocityVertical() { // returns Meters/sec
    return dgpsVelocityVertical;       //
  }

  public boolean isHealthy() { // healthy if last DGPS message was good (ie,
    return dgpsHealthy;        // the number of Satellite Vehicles >3, and the
  }                            // year was valid)

  public Date getDate() {
  /**
   * On-demand parse the time data from the last message into a date
   */
    Calendar dgpsCal = Calendar.getInstance();
    dgpsCal.clear();
    dgpsCal.set(dgpsYear,(dgpsMonth-1),dgpsDay,dgpsHour,dgpsMinute,(int)dgpsSeconds);
    return dgpsCal.getTime();
  }

  public void getValues(long [] values)
  {
    values[0] = (long)(scalingFactor*getLatitude());
    values[1] = (long)(scalingFactor*getLongitude());
    values[2] = (long)(scalingFactor*getHeading());
    Date d = getDate();
    values[3] = d.getTime();
  }

  public void getValueAspects(int [] aspects)
  {
    aspects[0] = Constants.Aspects.LATITUDE;
    aspects[1] = Constants.Aspects.LONGITUDE;
    aspects[2] = Constants.Aspects.HEADING;
    aspects[3] = Constants.Aspects.TIME;
  }

  public int getNumberAspects()
  {
    return 4;
  }

  public void setChan(int c) {}
  public void setUnits(String u) {}
  public boolean conditionChanged() {return true;}

  private boolean isundercontrol = false;

  public void startControl()
  {
    isundercontrol = true;
  }

  public void stopControl()
  {
    isundercontrol = false;
  }

  public boolean isUnderControl()
  {
    return isundercontrol;
  }

  public void modifyControl(String controlparameter, String controlparametervalue)
  {
      if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.ROTATEPREP]))
      {
	Double temp = new Double(controlparametervalue);
	double rotationdegrees = (long)temp.doubleValue();
	System.out.println("TiniGPSLocationControllerResource: rotation: " +rotationdegrees +" degrees");
	adjustHeading(rotationdegrees);
      }
  }

  // Parameter handling
  public void setParameters(Hashtable params)
  {
    setName("TiniGPSLocationControllerResource");
    setScalingFactor((long)Constants.Geophysical.DEGTOBILLIONTHS);

    if (params != null) {
      if (params.get("port") != null) {
        portName = (String)params.get("port");
      }
      if (params.get("hfom") != null) {
        String hfomstring = (String)params.get("hfom");
	MAXHFOM = Double.valueOf(hfomstring).doubleValue();
      }
      debug = (params.get("debug") != null);
    }
    if (debug)
       System.out.println("TiniGPSLocationControllerResource:setParams:"+params);

    startMonitorThread();
  }

  private void startMonitorThread() {
    Thread t = new Thread(new SerialManager());
    t.start();
  }

  /**
   *  A worker thread to service the serial port
   */
  private short ThisYear=2001;
  /*
   * @todo : make this not hard-coded
   */
  private class SerialManager implements Runnable {
    public void run() {
      /*
       * Open serial port input stream
       */
      InputStream input = null;

      if (debug) System.out.println("TiniGPSLocationControllerResource started on "+portName);

      if (portName.equals("serial1")) {
        com.dalsemi.system.TINIOS.enableSerialPort1();
      }
      try {
        CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(portName);
        SerialPort sp = (SerialPort)portId.open("GPS", 0);
        sp.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.enableReceiveThreshold(2048);

        input = sp.getInputStream();
      } catch (Exception ex) {
        System.out.println("TiniGPSLocationControllerResource Error:"+ex);
        ex.printStackTrace();
      }

      if (debug) System.out.println("TiniGPSLocationControllerResource Starting");

      /*
       * Search for the beginning of Message #20 in the input stream
       */
      int soh = 0, msgid = 0, msgcompid = 0, mblocksize = 0;
      int nread = 0;
      byte [] mblockmessage = new byte[256];

      while(true) //continuously read the port
      {
	try
	{
	  int nskip = 0;
	  while (true) //loop until start of header is detected
	  {
	    soh = input.read();
	    if(soh == 1)
	    {
	      msgid = input.read(); //read message id and complement
	      msgcompid = input.read();

	      if(msgid == (255 - msgcompid))
		break; //start of message block detected
	    }
	    else
	    {
	      nskip++; //for debugging purposes
	    }
	  }

	  mblocksize = input.read(); //read message block size F(msg id)
/*
	  if(debug)
	  {
	    System.out.println("Bytes skipped        : " +nskip);
	    System.out.println("Start Of Header (SOH): " +soh);
	    System.out.println("Block ID             : " +msgid);
	    System.out.println("Block ID Complement  : " +msgcompid);
	    System.out.println("Message Length       : " +mblocksize);
	  }
*/
	}
	catch (Exception e)
	{
	  System.out.println("Problems reading port stream block header");
	}

	try
	{
	  //read message block
	  //offset 4 bytes for header, add 2 for checksum
	  mblockmessage[0] = (byte)soh;
	  mblockmessage[1] = (byte)msgid;
	  mblockmessage[2] = (byte)msgcompid;
	  mblockmessage[3] = (byte)mblocksize;
	  nread = input.read(mblockmessage, 4,  mblocksize + 2);
	}
	catch (Exception e)
	{
	  System.out.println("Unable to read File input stream");
	  continue;
	}

	if(nread != (mblocksize + 2))
	{
	  System.out.println("Error: unable to read message block");
	  continue;
	}

	if(msgid == 20)
	{

	   /*
	    * Extract the number of satellites and date/time from the message,
	    * and use this information to determine the health of the message.
	    */
	  byte [] dbl = new byte[8];
	  byte [] flt = new byte[4];

	  int numSatellites = mblockmessage[71];
	  int readYear = (int)unsigned(mblockmessage[16]) + (int)unsigned(mblockmessage[17])*256;

	  System.arraycopy(mblockmessage, 62, flt, 0, 4);
	  float hfom = makeFloat(flt);

	  if((numSatellites > MINSATS) && (numSatellites < MAXSATS) && (readYear==ThisYear) && (hfom < MAXHFOM))
	  {
	    dgpsHealthy = true;
	  }
	  else
	  {
	    dgpsHealthy = false;
	  }

	  if(dgpsHealthy)
	  {
	    dgpsSatellites=numSatellites;
	    dgpsYear=readYear;
	    dgpsMonth=mblockmessage[15];
	    dgpsDay=mblockmessage[14];
	    dgpsHour=mblockmessage[4];
	    dgpsMinute=mblockmessage[5];

	    System.arraycopy(mblockmessage, 6, dbl, 0, 8);
	    dgpsSeconds = makeDouble(dbl);

	    /*
	    * Extract the lat, lon, alt, speed, heading, velN, velE, and velV,
	    * and place in the private database for use by the accessor methods.
	    */
	    System.arraycopy(mblockmessage, 18, dbl, 0, 8);
	    dgpsLatitude = (180.0/Math.PI)*makeDouble(dbl);
	    System.arraycopy(mblockmessage, 26, dbl, 0, 8);
	    dgpsLongitude = (180.0/Math.PI)*makeDouble(dbl);
	    System.arraycopy(mblockmessage, 34, flt, 0, 4);
	    dgpsAltitude = (double)makeFloat(flt);
	    System.arraycopy(mblockmessage, 38, flt, 0, 4);
	    dgpsGroundSpeed = makeFloat(flt);
	    System.arraycopy(mblockmessage, 42, flt, 0, 4);
	    dgpsHeading = (180.0/Math.PI)*(double)makeFloat(flt);
	    System.arraycopy(mblockmessage, 46, flt, 0, 4);
	    dgpsVelocityNorth = makeFloat(flt);
	    System.arraycopy(mblockmessage, 50, flt, 0, 4);
	    dgpsVelocityEast = makeFloat(flt);
	    System.arraycopy(mblockmessage, 54, flt, 0, 4);
	    dgpsVelocityVertical = makeFloat(flt);

	    if(Math.abs(dgpsVelocityNorth) > MINVELOCITY && Math.abs(dgpsVelocityEast) > MINVELOCITY &&
	       Math.abs(dgpsVelocityNorth) < MAXVELOCITY && Math.abs(dgpsVelocityEast) < MAXVELOCITY)
	    {
	      //assume motion is reasonable
	     computedheading = TiniTrig.tiniatan2((double)dgpsVelocityNorth, (double)dgpsVelocityEast); //radians from -pi to pi
	     computedheading *= (180.0/Math.PI); //degrees from -180 to 180 as measured from east vector (CCW = +)
	     computedheading = 90.0 - computedheading; //true heading
	     if (computedheading < 0.0) computedheading += 360.0; //keep it positive
	    }

	    //VsinA = VelNorth (sqrt function does not appear to work)
	    //computedspeed = dgpsVelocityNorth*dgpsVelocityNorth + dgpsVelocityEast*dgpsVelocityEast;

	    double angle = TiniTrig.tiniatan2((double)dgpsVelocityNorth, (double)dgpsVelocityEast);
	    double sangle = TiniTrig.tinisin(angle);
	    if(sangle == 0.0)
	    {
	      computedspeed = Math.abs(dgpsVelocityEast);
	    }
	    else
	    {
	      computedspeed = Math.abs(dgpsVelocityNorth/sangle);
	    }

	    if (debug)
	    {
	      int isecs = (int)dgpsSeconds;
	      int iheading = (int)computedheading;
	      int mmpersec = (int)(computedspeed*1000.0);
	      //System.out.println("N:"+numSatellites+" "+dgpsHour+":"+dgpsMinute+":"+isecs+" "+mmpersec+":"+iheading);

	      //Warning!!!!!   too much printing causes problems while keepinmg up with port reads

	      System.out.println("GPS: nSat: "+numSatellites+" HFOM "+hfom);
	      System.out.println("     date "+dgpsYear+":"+dgpsMonth+":"+dgpsDay);
	      System.out.println("     time "+dgpsHour+":"+dgpsMinute+":"+dgpsSeconds);

	      Date nowdate = getDate();
	      long msecs = nowdate.getTime();
	      TimeZone tz = TimeZone.getDefault();
	      //System.out.println("Timezone: " +tz.getID());
	      //System.out.println("  UTC : "+msecs);

	      //System.out.println("     lat:"+dgpsLatitude+" long:"+dgpsLongitude+" alt:"+dgpsAltitude);
	      //System.out.println("     velN:"+dgpsVelocityNorth+" velE:"+dgpsVelocityEast);
	      //System.out.println("     computed heading:"+computedheading+" speed:"+computedspeed);
	    }
	  } // if healthy
	  else
	  {
	    if (debug) System.out.println("Unhealthy NAV message received "+numSatellites+" "+readYear+" "+hfom);
	  }
	} //if msg = 20
      } // while
    } // public void run

    private long unsigned(byte byt) {
      return (long)byt&0xff;
    }

    /*
     * Method to make a double out of an unsigned long
     */
    private double makeDouble(byte [] bits) {
      long lng = 0;
      for (int i=0; i<8; i++) {
        lng |= (unsigned(bits[i])) << (i*8);
      }
      return Double.longBitsToDouble(lng);
    }

    /*
     * Method to make a float out of an unsigned int
     */
    private float makeFloat(byte [] bits) {
      int shrt = 0;
      for (int i=0; i<4; i++) {
        shrt |= (unsigned(bits[i])) << (i*8);
      }
      return Float.intBitsToFloat(shrt);
    }
  }

  public boolean getSuccess()
  {
    return isHealthy();
  }

  private void debugfunc()
  {
    debug = true;
    startMonitorThread();
  }

/*
  public static void main(String args[])
  {
    TiniGPSLocationControllerResource tgpsres = new TiniGPSLocationControllerResource();
    tgpsres.debugfunc();
  }
  */
}