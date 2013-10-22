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

import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.shared.Constants;
import java.util.*;
import java.io.*;
import java.lang.*;
import javax.comm.*;

//this class keeps track of where the robot was in terms of
//relative coordinates the last time the command state changed

interface Mach5Command
{
    static String [] cmdstring = { "STOP", "GO FORWARD", "GO BACKWARD","ROTATE","READSTATE" };
    static int STOP = 0;
    static int GOFORWARD = 1;
    static int GOBACKWARD = 2;
    static int ROTATE = 3;
    static int READSTATE = 4;
}

/*

This structure keeps track of where the Mach 5 is in a relative coordinate frame.
It assumes that the relative coordinate frame is defined by the robots position and
orientation in a absolute coordinate frame at start up. For example, the robotbase
will start at Xrel = Yrel = 0 and ThetaRel = 90.0

*/
class Mach5RelativePositionData
{
  public Mach5RelativePositionData()
  {
    leftwheelpos = 0;
    rightwheelpos = 0;
    Xrel = Yrel = 0.0;
    Thetarel = Math.PI/2.0;
    specifiedrotation = 0.0;
    lastcommand = Mach5Command.STOP;
  }

  public long leftwheelpos; //Mach 5 wheel position in mm
  public long rightwheelpos;
  public double Xrel; // X position (mm) as measured from start point
  public double Yrel; //Y position (mm) as measured from start point
  public double Thetarel; //orientation measure from X axis, radians
  public double specifiedrotation; //set when ordered to rotate
  public int lastcommand; //the last command issued when updated
}

/**
 *  CougaarME Resource for controlling a MACH5 robot base from a TINI board.
 *  Parameters are:
 *  port=(serial0 or serial1)
 *    If you use serial 0, you must do a downserver -s to kill the console server
 *    If you use serial 1, you must move the jumper JP4 on the STEP board to the "RS232" position
 *  debug=
 *    If you define debug, you get loads of console text output
 *
 *  The cable for the TINI <--> Mach5 should be null modem, specifically:
 *  TINI   Mach5
 *   1  ---  1
 *   2  ---  3
 *   3  ---  2
 *   7  ---  7
 */
public class TiniMach5LocomotionResource extends ControllerResource
{

  /**
   * Constructor.  Sets name default.
   */
  public TiniMach5LocomotionResource(){}

  private String portName = "serial1";
  private boolean debug = false;
  private Mach5RelativePositionData laststatechange = new Mach5RelativePositionData();
  static final double TICKSPERDEGREE = 2.25; //experimentally determined
  public static final int CLOCKWISE = 0;
  public static final int COUNTER_CLOCKWISE = 1;

  private long advancedistance = 2000; // millimeters
  private long rotationdegrees = 0; // one meter
  private boolean isUnderControl = false;
  private static long [] wheelpos = {0, 0};
  private static long [] endposition = {0, 0};
  private static long [] lastreading = {0, 0};
  private final static long TOLERANCE = 100; //mm

  private long speed = 1000; // mm per second
  private long spinspeed = 250; // mm per second
  private long maxaccel = 250; // mm per second per second

  /**
   *  The only paramater is "port" which defaults to "serial1"
   */
  public void setParameters(Hashtable params)
  {
    setName("TiniMach5LocomotionResource");

    if (params != null)
    {
      if (params.get("port") != null)
      {
        portName = (String)params.get("port");
      }
      debug = (params.get("debug") != null);
    }

    startMonitorThread();
  }

  public long getSpeed()
  {
    return (long)speed;
  }

  public void setSpeed(long newSpeed)
  {
    speed = newSpeed;
  }

  public void  rotate(int direction, long degrees, boolean waitforcomplete)
  {
    degrees = degrees%360;
    System.out.println("TiniMach5LocomotionResource: Rotating "+degrees);
    if( degrees == 0) return;

    UpdatePositionState(Mach5Command.ROTATE);

    //sets the speed of rotation
    sendMsg("OFF\n");
    String msg = "MA "+maxaccel+"\n";
    sendMsg(msg);
    msg = "MV "+spinspeed+"\n";
    sendMsg(msg);

    int ticks = (int)((double)degrees * TICKSPERDEGREE); // experimentally determined

    switch (direction)
    {
      case CLOCKWISE :
        msg = "SPR "+ticks+" -"+ticks+"\n";
	laststatechange.specifiedrotation = -1.0*degrees;
	SetEndPosition(wheelpos[0] + ticks, wheelpos[1] - ticks);
        break;
      case COUNTER_CLOCKWISE :
        msg = "SPR -"+ticks+" "+ticks+"\n";
	laststatechange.specifiedrotation = degrees;
	SetEndPosition(wheelpos[0] - ticks, wheelpos[1] + ticks);
        break;
      default:
        throw new IllegalArgumentException("LocomotionResource.rotate must be one of CLOCKWISE or COUNTERCLOCKWISE");
    }


    sendMsg(msg);

    if(waitforcomplete)
    {
      System.out.print("Wheelbase rotating...");

     //totatl seconds = V/A + D/V;
      long msecs = (long)((spinspeed/maxaccel) + (ticks/spinspeed))*1000;
      try { Thread.sleep(msecs);}
      catch (InterruptedException ie) {}
      //System.out.println("...done");
    }
  }

  public void  translate(long millimeters, boolean waitforcomplete)
  {

    System.out.println("TiniMach5LocomotionResource: translating "+millimeters);

    UpdatePositionState(Mach5Command.GOFORWARD);

    //sets the speed of translation
    sendMsg("OFF\n");
    String msg = "MA "+maxaccel+"\n";
    sendMsg(msg);
    msg = "MV "+speed+"\n";
    sendMsg(msg);

    msg = "SPR "+millimeters+" "+millimeters+"\n";

    SetEndPosition(wheelpos[0] + millimeters, wheelpos[1] + millimeters);

    sendMsg(msg);

    //sleep enough time to complete command, otherwise we may pre-empt it.
    if(waitforcomplete)
    {
      System.out.print("Wheelbase translating...");
      long msecs = (long)((speed/maxaccel) + (millimeters/speed))*1000;
      try { Thread.sleep(msecs);}
      catch (InterruptedException ie) {}
      //System.out.println("...done");
    }
  }

  private void SetEndPosition(long pos0, long pos1)
  {
    endposition[0] = pos0;
    endposition[1] = pos1;
    lastreading[0] = pos0;
    lastreading[1] = pos1;
  }

  private boolean AtDestinationPosition()
  {
    System.out.println("Checking if at destination...");
    if(getWheelPositions() == false)
    {
      System.out.println("Something went wrong.");
      return false;
    }

    //see if there is stagnation
    if(wheelpos[0] == lastreading[0] && wheelpos[1] == lastreading[1])
    {
      System.out.println("Mach5 seems to think it is there");
      return true;
    }

    if(Math.abs(wheelpos[0] - endposition[0]) < TOLERANCE &&
       Math.abs(wheelpos[1] - endposition[1]) < TOLERANCE )
    {
      System.out.println("Destination reached!!");
      return true;
    }

    System.out.println("Mach5 not there yet... "+wheelpos[0]+":"+wheelpos[1]+" "+endposition[0]+":"+endposition[1]);
    lastreading[0] = wheelpos[0];
    lastreading[1] = wheelpos[1];

    return false;
  }

  public void stop() {
    System.out.print("Wheelbase stopped...");
    UpdatePositionState(Mach5Command.STOP);
    sendMsg("OFF\n");
  }

  public void  forward() {
    int spd = (int)speed;
    if(spd == 0)
       stop();
    else
    {
      UpdatePositionState(Mach5Command.GOFORWARD);
      sendMsg("SV "+spd+" "+spd+"\n");
    }
  }

  public void backward() {
    int spd = 0 - (int)speed;
    if(spd == 0)
      stop();
    else
    {
      UpdatePositionState(Mach5Command.GOBACKWARD);
      sendMsg("SV "+spd+" "+spd+"\n");
    }
  }

  private Thread owner = null;

  public synchronized void UpdatePositionState(int newcommand)
  {
   if(owner != null)
   {
    System.out.println("Waiting to UpdatePositionState...");
    try {
      wait();
    }
    catch (Exception e) {
      System.out.println("UpdatePositionState: Exception on wait");
    }
    owner = Thread.currentThread();
   }

   if(getWheelPositions() == false) return;

   //compute change in wheel positioning since last command
   long diffleft = wheelpos[0] - laststatechange.leftwheelpos;
   long diffright = wheelpos[1] - laststatechange.rightwheelpos;
   double mmrange = 0.0;

   switch(laststatechange.lastcommand)
   {
    case Mach5Command.STOP:
	 break;
    case Mach5Command.GOFORWARD:
    case Mach5Command.GOBACKWARD:
	 //compute how far we have come
	 //the diff should be the same on each wheel
	 if(diffleft != diffright)
	 {
	    System.out.println("Different range indications " +diffleft +" " +diffright);
	    mmrange = (diffleft + diffright) * 0.5;
	 }
	 else
	 {
	   mmrange = diffleft;
	 }

	 laststatechange.Xrel += mmrange * TiniTrig.tinicos(laststatechange.Thetarel);
	 laststatechange.Yrel += mmrange * TiniTrig.tinisin(laststatechange.Thetarel);

         break;
    case Mach5Command.ROTATE:
	 if((diffright + diffleft) != 0)
	 {
	    System.out.println("Different rotation indications " +diffleft +" " +diffright);
	 }
	 mmrange = (diffright - diffleft)*0.5; //averages the absolute value

	 //double degreesrotated = mmrange/TICKSPERDEGREE;
	 double degreesrotated = laststatechange.specifiedrotation; //because the wheels report wrong

	 laststatechange.Thetarel += (degreesrotated*(Math.PI/180.0));
	 if(laststatechange.Thetarel >= (Math.PI*2.0)) laststatechange.Thetarel -= (Math.PI*2.0);
	 if(laststatechange.Thetarel < 0.0) laststatechange.Thetarel += (Math.PI*2.0);
    	 break;
   }

   laststatechange.lastcommand = newcommand;
   laststatechange.leftwheelpos = wheelpos[0];
   laststatechange.rightwheelpos = wheelpos[1];

   if (debug) System.out.println("Current relative state: "
			       +Mach5Command.cmdstring[laststatechange.lastcommand] +" "
			       +laststatechange.rightwheelpos +" "
			       +laststatechange.leftwheelpos +" "
			       +laststatechange.Xrel +" "
			       +laststatechange.Yrel +" "
			       +laststatechange.Thetarel*(180.0/Math.PI));

   owner = null;
   notifyAll();
  }

  public synchronized Mach5RelativePositionData ReadCurrentState()
  {
   if(owner != null)
   {
    System.out.println("Waiting to ReadCurrentState...");
    try {
      wait();
    }
    catch (Exception e) {
      System.out.println("ReadCurrentState: Exception on wait");
    }
    owner = Thread.currentThread();
   }

   Mach5RelativePositionData ret = new Mach5RelativePositionData();

   if(getWheelPositions() == false)
     return laststatechange;

   //compute change in wheel positioning since last command
   long diffleft = wheelpos[0] - laststatechange.leftwheelpos;
   long diffright = wheelpos[1] - laststatechange.rightwheelpos;
   double mmrange = 0.0;

   switch(laststatechange.lastcommand)
   {
    case Mach5Command.GOFORWARD:
    case Mach5Command.GOBACKWARD:
	 //how far are we along this leg
	 mmrange = (diffleft + diffright) * 0.5;
	 ret.Xrel = laststatechange.Xrel + mmrange * TiniTrig.tinicos(laststatechange.Thetarel);
	 ret.Yrel = laststatechange.Yrel + mmrange * TiniTrig.tinisin(laststatechange.Thetarel);
	 ret.Thetarel = laststatechange.Thetarel;
         break;
    case Mach5Command.ROTATE:
	 //include last rotation
	 ret.Xrel = laststatechange.Xrel;
	 ret.Yrel = laststatechange.Yrel;

	 //mmrange = (diffright - diffleft)*0.5; //averages the absolute value
	 //double degreesrotated = mmrange/TICKSPERDEGREE; //positive = CW
	 ret.Thetarel = laststatechange.Thetarel + (laststatechange.specifiedrotation*(Math.PI/180.0));
	 if(ret.Thetarel >= (Math.PI*2.0)) ret.Thetarel -= (Math.PI*2.0);
	 if(ret.Thetarel < 0.0) ret.Thetarel += (Math.PI*2.0);
    	 break;
   }

   owner = null;
   notifyAll();

   return ret;
  }

  /**
   * Gets the positions of the wheels since startup.
   * @return the left wheel position in [0], the right in [1]
   */
  private boolean getWheelPositions()
  {
    boolean retbool = false;

    sendMsg("QP\n");
    String msg = getMsg();
    while (msg == null) {
      try { Thread.sleep(100);} catch (InterruptedException ie){}
      msg = getMsg();
    }

    try {
      //System.out.println("getWheelPosition msg = " +msg);
      org.cougaar.microedition.util.StringTokenizer toker = new org.cougaar.microedition.util.StringTokenizer(msg.trim(), " ");

      // format should be "> NNNNNN NNNNNN ....."
      int ntokens = toker.countTokens();
      if (ntokens >= 3) { //  enough tokens
        toker.nextToken();  // eat the ">"
        String left = toker.nextToken();
        String right = toker.nextToken();
        wheelpos[0] = Long.parseLong(left);
        wheelpos[1] = Long.parseLong(right);
	retbool = true;
      }
      else
      {
	System.out.println("getWheelPositions: not enough tokens!!!");
	retbool = false;
      }
    }
    catch (Exception nfe)
    { // error parsing wheel position text
      System.out.println("getWheelPositions Exception " +nfe);
      retbool = false;
    }
    return retbool;
  }

  private Vector outgoingMsgs = new Vector();
  private Vector incomingMsgs = new Vector();


  private void sendMsg(String msg) {
    synchronized (outgoingMsgs) {
      outgoingMsgs.addElement(msg);
      if (debug) System.out.println("sendMsg: outQ= "+outgoingMsgs);
      outgoingMsgs.notifyAll();
    }
  }

  private String getMsg() {
    String ret = null;
    if (incomingMsgs.size() > 0) {
      synchronized (incomingMsgs) {
        ret = (String)incomingMsgs.elementAt(0);
        incomingMsgs.removeElementAt(0);
	if (debug) System.out.println("getMsg: inQ= "+incomingMsgs);
      }
    }
    return ret;
  }

  private void startMonitorThread() {
    Thread t = new Thread(new SerialManager());
    t.start();
  }

  /**
   *  A worker thread to service the serial port
   */
  private class SerialManager implements Runnable {

    public void flushstream(InputStream input)
    {
           //clear rubbish on serial port
       byte [] junk = new byte[128];
       int totalgarbage = 0;
       int nread = 0;
       try
       {
	 nread = input.read(junk, 0, 128);
	 while(nread > 0)
	 {
	  totalgarbage += nread;
	  nread = input.read(junk, 0, 128);
	 }
       }
       catch(Exception e) {}

       System.out.println("Serial port cleared..." +totalgarbage);
    }

    public void run() {
      /*
       * Open serial port input and output streams
       */
//       InputStream input = System.in;
//       OutputStream output = System.out;

       boolean firstio = true;
       InputStream input;
       OutputStream output;

      if (debug) System.out.println("TiniMach5LocomotionResource started on "+portName);
      if (portName.equals("serial1")) {
        com.dalsemi.system.TINIOS.enableSerialPort1();
      }
      try {
        CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(portName);
        SerialPort sp = (SerialPort)portId.open("MACH5", 0);
        sp.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.enableReceiveTimeout(1000);

        input = sp.getInputStream();
        output = sp.getOutputStream();

      } catch (Exception exp) {
        System.err.println("TiniMach5LocomotionResource: Error initializing serial port '"+portName+"'");
        exp.printStackTrace();
        return;
      }

       byte [] data = new byte[128];

       forever:
       while (true) {
         /*
          * Write any pending output messages
          */
          String msg = null;
          synchronized (outgoingMsgs) {
            while (outgoingMsgs.isEmpty()) {
              try {
                System.gc();
                if (debug) System.out.println("Waiting on "+outgoingMsgs);
                outgoingMsgs.wait();
                if (debug) System.out.println("DONE Waiting on "+outgoingMsgs);
              } catch (InterruptedException ex) {}
            }
            msg = (String)outgoingMsgs.elementAt(0);
            outgoingMsgs.removeElementAt(0);
          }
          try {
            if (debug) System.out.println("SEND:"+msg);
	    if(firstio)
	    {
	      flushstream(input);
	      firstio = false;
	    }
            output.write(msg.getBytes());
            output.flush();
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }

          /*
           * Read until the terminal character is received
           */
           int dataptr = 0;
           try {
             byte ch = 0;
             while (ch != 10) { // line-feed
               int datum = input.read();
               if (datum < 0) { // read timeout
                 if (debug) System.out.println("#### Serial port timeout");
                 continue forever;
               }
               ch = (byte)datum;
               data[dataptr++] = ch;
             }
           } catch (IOException ioe) {
             ioe.printStackTrace();
           }
            if (debug) System.out.println("RECV:"+new String(data));
           // only ones I'm interested in are the responses to the "QP"
           if (msg.startsWith("QP"))
             incomingMsgs.addElement(new String(data, 0, dataptr));
       }

    }


  }

  private boolean interruptcheck()
  {
      if(Thread.interrupted())
      {
	System.out.println("TiniMach5LocomotionResource Advance Manager abandoning");
	stop();
	return true;
      }

      return false;
  }

  public void getValues(long [] values)
  {
    values[0] = 0;
  }

  public void getValueAspects(int [] aspects)
  {
    aspects[0] = 0;
  }

  public int getNumberAspects()
  {
    return 1;
  }

  public void setChan(int c) {}
  public void setUnits(String u) {}

  public boolean conditionChanged()
  {
    if(isUnderControl)
      return AtDestinationPosition();

    return false;
  }

 public boolean getSuccess()
 {
   stopControl();
   return true;
 }

  public void startControl()
  {
    isUnderControl = true;
    System.out.println("TiniMach5LocomotionResource Start Control");
    long degrees = rotationdegrees;
    if(degrees < 0)
      rotate(COUNTER_CLOCKWISE, (long)(-degrees), true);
    else
      rotate(CLOCKWISE, (long)(degrees), true);

    long translation = advancedistance;
    translate(translation, false);
  }

  public void stopControl()
  {
    stop();
    isUnderControl = false;
  }

  public boolean isUnderControl()
  {
    return isUnderControl;
  }

  public void modifyControl(String controlparameter, String controlparametervalue)
  {
    try
    {
      if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.VELOCITYPREP]))
      {
	Double temp = new Double(controlparametervalue);
	setSpeed((long)temp.doubleValue());
	System.out.println("TiniMach5LocomotionResource: speed set: " +getSpeed());
      }

      if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.TRANSLATEPREP]))
      {
	Double temp = new Double(controlparametervalue);
	advancedistance = (long)temp.doubleValue();
	System.out.println("TiniMach5LocomotionResource: translate: " +advancedistance+" mm");
      }

      if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.ROTATEPREP]))
      {
	Double temp = new Double(controlparametervalue);
	rotationdegrees = (long)temp.doubleValue();
	System.out.println("TiniMach5LocomotionResource: rotation: " +rotationdegrees +" degrees");
      }
    }
    catch (Exception ex) {}
  }
}