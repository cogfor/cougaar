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

import java.io.*;
import java.net.*;
import java.util.*;
import com.ibutton.utils.*;
import com.ibutton.adapter.*;
import com.ibutton.container.*;
import com.ibutton.*;
import com.dalsemi.tininet.http.*;

public class DS2450
{
  static Vector   madv = new Vector();         // madv = my a/d vector
  static Object   adlock = new Object();       // adlock = a/d lock

  static int      madvsize;
  static int      timeout;
  boolean debugging =  false;

  /**
   * Create an object for reading and setting the DS2450 device at
   * the given address.
   * @param name An arbitrary name for the device
   * @param address The address of the device
   */
  public DS2450(String name, String address) {
    timeout=20;
    try {
      DSPortAdapter   pa = new TINIExternalAdapter();

          byte addr[] = Address.toByteArray(address);
          try
          {
            madv.addElement(new MyADContainer(pa, addr, name));
          }
          catch (OneWireIOException e)
          {
            System.out.println("Caught OneWireIOException: "+e);
          }

          madvsize = madv.size();

          initDevices();


    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println("Caught ex: "+ex);
    }
  }

  /**
   * Create objects for reading and setting the attached DS2450 devices.
   */
    public DS2450() {
    timeout = 20;
    // initialization
    if (debugging) System.out.println("in DS2450()");

    DSPortAdapter   adapter = new TINIExternalAdapter();
    try {
      adapter.beginExclusive(true);
      if (debugging) System.out.println("in DS2450() in exclusive region");
      adapter.targetFamily(0x20);
      iButtonContainer aD;

      if (debugging) System.out.println("in DS2450() b4 for");
      for (aD = adapter.getFirstiButton(); aD!=null;
           aD = adapter.getNextiButton() ) {
        if (debugging) System.out.println("in DS2450() in for");
        byte                addr[] =  aD.getAddress();
        String              sn = aD.getAddressAsString();
        if (debugging) System.out.println("in DS2450() for addr_as_str: "+sn+"-- 1-WB");
        madv.addElement(new MyADContainer(adapter, addr, sn));
      }
      if (debugging) System.out.println("in DS2450() out of for");


    } catch (Exception e) {
      System.out.println("Exception: " + e);
    } catch (Throwable e) {
      System.out.println("Throwable: " + e);
    } finally {
      adapter.endExclusive();
      if (debugging) System.out.println("in DS2450() out of exclusive region");
    }

    madvsize = madv.size();

    initDevices();

    if (debugging) System.out.println("in DS2450() at end and madvsize is "+madvsize);
  }

  /**
   * Initialize the devices which have been added to the container.
   */
  private void initDevices() {
    if (madvsize > 0)
      {
        int     i = 0;
        boolean level = true;
        byte[]  adstate;
        double[] advoltages;
        // default A/D setup
        double adrange = 5.12;       // volts
        double adresolution = 0.01;  // volts
        int alarmtype = 1;           // 1 = high
        double adalarmvalue = 3.0;   // volts
        boolean alarmenable = false; // disabled
        boolean outputenable = true;  // enabled
        boolean outputstate = false;    // false = logic 0

        for (Enumeration e = madv.elements(); e.hasMoreElements(); )
        {
          MyADContainer madc = (MyADContainer)e.nextElement();
          try
          {
             if (debugging) System.out.println("in DS2450() try madc "+madc);
            synchronized (adlock)
            {
             if (debugging) System.out.println("in DS2450() sync  ");
              adstate = madc.readDevice();
             if (debugging) System.out.println("in DS2450() sync adstat "+adstate);
              for (int adchan=0; adchan < madc.getNumberADChannels(); adchan++)
              {
                 madc.setADRange(adchan, adrange, adstate);
                 madc.setADResolution(adchan, adresolution, adstate);
                 madc.setADAlarm(adchan, alarmtype, adalarmvalue, adstate);
                 madc.setADAlarmEnable(adchan, alarmtype, alarmenable, adstate);
                 madc.setOutput(adchan, outputenable, outputstate, adstate);
              }

              madc.writeDevice(adstate);
            }
            i++;
          }
          catch (Throwable t)
          {
      System.out.println("Throwabel: " + t);
            i += 1;
          }
        }
      }
      else
      {
        System.out.println("in DS2450() -- No A/D Converters");
      }

  }

  /**
   * Create objects for reading and setting the attached DS2450 devices
   * which are identified within the given file (or in "DS2450.txt" if the
   * given file is null).
   */
  public DS2450(File addressFile)
  {
    timeout = 20;
    // initialization
    if (debugging) System.out.println("in DS2450(File)");
    try
    {
      DSPortAdapter   pa = new TINIExternalAdapter();

      //File            f = new File("DS2450.txt");
      File f=addressFile;
      if (f==null) {
        f = new File("DS2450.txt");
      }
      if (f.exists())
      {
        if (debugging) System.out.println("in DS2450() and f.exists()");
        // Read 1-Wire Net Addresses and A/D identifiers.
        /*
         * NOTE: The OneWire A/Ds are read in order, and the
         *       corresponding index numbers are assigned in this
         *       same order.  Since this code uses index numbers
         *       to perform operations on switches, changing the
         *       order of the A/Ds in the DS2450.txt file
         *       changes the operation of the code.
         */
        BufferedReader  br =
                  new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String          s = null;
        while ((s = br.readLine()) != null)
        {
          int     k1 = s.indexOf("=");
          String  sn = s.substring(0, k1).trim();
          String  sa = s.substring(k1 + 1).trim();
          byte addr[] = Address.toByteArray(sn);
          try
          {
            madv.addElement(new MyADContainer(pa, addr, sa));
          }
          catch (OneWireIOException e)
          {
      System.out.println("Exception: " + e);
            System.out.println(e);
          }
        }
        br.close();
      }
      else   // identifier file does not exist, read from 1-wire bus
      {
              if (debugging) System.out.println("in DS2450() and NOT f.exists() -- 1-WB");
        try {
          File newDS2450;
          newDS2450 = new File("newDS2450.txt");
          BufferedWriter bw = new BufferedWriter(new FileWriter(newDS2450));
          pa.targetFamily(0x20);

          Enumeration e;
              if (debugging) System.out.println("in DS2450() b4 for -- 1-WB");
          e= pa.getAlliButtons();
              if (debugging) System.out.println("in DS2450() af e= pa.getAlliButtons() -- 1-WB");
          while(e.hasMoreElements() )
 //         for (Enumeration e = pa.getAlliButtons();e.hasMoreElements(); )
          {
              if (debugging) System.out.println("in DS2450() in for -- 1-WB");
            iButtonContainer ibc = (iButtonContainer)e.nextElement();

            byte                addr[] = ibc.getAddress();
            String              sn = ibc.getAddressAsString();
              if (debugging) System.out.println("in DS2450() for addr_as_str: "+sn+"-- 1-WB");
            madv.addElement(new MyADContainer(pa, addr, sn));
            bw.write(sn,0,sn.length());
            bw.newLine();
          }
              if (debugging) System.out.println("in DS2450() af for 1-- 1-WB");
          bw.flush();
          bw.close();
              if (debugging) System.out.println("in DS2450() af for 2-- 1-WB");
        }
        catch (IOException e) {
      System.out.println("Exception: " + e);
          e.printStackTrace();
        }
        catch (OneWireIOException e) {
      System.out.println("Exception: " + e);
          System.out.println();
        }
      }

      madvsize = madv.size();
              if (debugging) System.out.println("in DS2450() madvsize "+madvsize);

      if (madvsize > 0)
      {
        int     i = 0;
        boolean level = true;
        byte[]  adstate;
        double[] advoltages;
        // default A/D setup
        double adrange = 5.12;       // volts
        double adresolution = 0.01;  // volts
        int alarmtype = 1;           // 1 = high
        double adalarmvalue = 3.0;   // volts
        boolean alarmenable = false; // disabled
        boolean outputenable = true;  // enabled
        boolean outputstate = false;    // false = logic 0

        for (Enumeration e = madv.elements(); e.hasMoreElements(); )
        {
          MyADContainer madc = (MyADContainer)e.nextElement();
          try
          {
             if (debugging) System.out.println("in DS2450() try madc "+madc);
            synchronized (adlock)
            {
             if (debugging) System.out.println("in DS2450() sync  ");
              adstate = madc.readDevice();
             if (debugging) System.out.println("in DS2450() sync adstat "+adstate);
              for (int adchan=0; adchan < madc.getNumberADChannels(); adchan++)
              {
                 madc.setADRange(adchan, adrange, adstate);
                 madc.setADResolution(adchan, adresolution, adstate);
                 madc.setADAlarm(adchan, alarmtype, adalarmvalue, adstate);
                 madc.setADAlarmEnable(adchan, alarmtype, alarmenable, adstate);
                 madc.setOutput(adchan, outputenable, outputstate, adstate);
              }

              madc.writeDevice(adstate);
            }
            i++;
          }
          catch (Throwable t)
          {
      System.out.println("Throwabkle: " + t);
            i += 1;
          }
        }
      }
      else
      {
        System.out.println("in DS2450() -- No A/D Converters");
      }
    }
    catch (Exception e)
    {
      System.out.println("in DS2450() -- caught exception: "+e);
    }
    catch (Throwable t)
    {
      System.out.println("in DS2450() -- caught throwable: "+t);
    } finally {
      if (debugging) System.out.println("in DS2450() -- at end of method in finally.");
    }

  }


 /*
  // test code
  public void main(String[] args)
  {
    double adresult = 0.0;
    boolean alarmresult = false;
    boolean debugging = false;
    try
    {
      int adindex = 0;  // first DS2450 on 1-wire bus = 0, second = 1,...
      int adchan = 0;   // channel
      double adrange = 5.12;  // A/D voltage range
      double adresolution = 0.01;  // A/D resolution
      int alarmtype = 1;  // If using alarms, 1 = high alarm, 0 = low alarm
      double alarmtrigger = 3.0;  // alarm threshold value
      boolean alarmenable = true;
      boolean adoutputenable = true;  // true if using A/D channel as output pin
      boolean adoutputstate = true;  // true = not conducting to ground, logic 1

      System.out.println("Starting....");
      DS2450 ADConverters = new DS2450(args);
      System.out.println("\nA/Ds Initialized.");

      adchan = 0;
      if (debugging) {
        adindex = 0;
        ADConverters.readStatus(adindex);
      }
      // turn off output enable feature
      adoutputenable = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      // set up to do a/d
      ADConverters.configureAD(adindex, adchan, adrange, adresolution);
      // set up alarm
      ADConverters.configureAlarm(adindex, adchan, alarmtype, alarmtrigger, alarmenable);

      adchan = 1;
      // set the output pin high
      adoutputenable = true;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + ", channel " + adchan + " reconfigured:");

      adchan = 2;
      // turn off output enable feature
      adoutputenable = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      // set up to do a/d
      ADConverters.configureAD(adindex, adchan, adrange, adresolution);
      // set up alarm
      ADConverters.configureAlarm(adindex, adchan, alarmtype, alarmtrigger, alarmenable);

      adchan = 3;
      // set the output high
      adoutputenable = true;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      if (debugging) {
        ADConverters.readStatus(adindex);
      }

      // perform an a/d conversion with no regard for alarms
      adchan = 0;
      System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");
      adchan = 2;
      System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");

      // drive the output pins low then high
      adchan = 1;
      adoutputstate = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      adoutputstate = true;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);

      adchan = 3;
      adoutputstate = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      adoutputstate = true;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);



      // Perform A/D and check alarm on a single channel
      adindex = 0;
      adchan = 0;
      adresult = ADConverters.readVoltage(adindex,adchan);
      alarmresult = ADConverters.readAlarm(adindex, adchan, alarmtype);

      // Perform a series of A/Ds and check channel alarms after every conversion
      int junk = 0;
      int adchanA = 0;
      int adchanB = 2;
      int adresetA = 1;
      int adresetB = 3;
      int RotationAndLimit = 0;
      double adresultA = 0.0;
      double adresultB = 0.0;
      boolean allalarmsresult[] = {false, false, false, false};

      for (int i = 0; i<10; i++) {
        // do a/d
        adresultA = ADConverters.readVoltage(RotationAndLimit,adchanA);
        adresultB = ADConverters.readVoltage(RotationAndLimit,adchanB);
        System.out.println(ADConverters.readDeviceName(RotationAndLimit) + ", Rotation A/D " + RotationAndLimit + " channel " + adchanA + " result = : " + adresultA + " volts.");
        System.out.println(ADConverters.readDeviceName(RotationAndLimit) + ", Limit A/D " + RotationAndLimit + " channel " + adchanB + " result = : " + adresultB + " volts.");
        // check alarms
        allalarmsresult = ADConverters.readAllAlarms(RotationAndLimit, alarmtype);
        System.out.println(ADConverters.readDeviceName(RotationAndLimit) + " alarms initial check result = : \n" + allalarmsresult[0] + ", "
             + allalarmsresult[1] + ", " + allalarmsresult[2] + ", " + allalarmsresult[3] + ".");
        // if alarm, notify user and reset F/F
        if (allalarmsresult[adchanA]) {
          System.out.println(ADConverters.readDeviceName(RotationAndLimit) + " alarm event occured on channel " + adchanA + ".");
          // clear the associated F/F
          adoutputstate = false;
          ADConverters.configureADOutput(RotationAndLimit, adresetA, adoutputenable, adoutputstate);
          adoutputstate = true;
          ADConverters.configureADOutput(RotationAndLimit, adresetA, adoutputenable, adoutputstate);
        }
        if (allalarmsresult[adchanB]) {
          System.out.println(ADConverters.readDeviceName(RotationAndLimit) + " Limit alarm event occured on channel " + adchanB + ".");
          // clear the associated F/F
          adoutputstate = false;
          ADConverters.configureADOutput(RotationAndLimit, adresetB, adoutputenable, adoutputstate);
          adoutputstate = true;
          ADConverters.configureADOutput(RotationAndLimit, adresetB, adoutputenable, adoutputstate);
        }
      }
      System.out.println("\nDone...");
    }
    catch (Throwable t)
    {
      System.out.println(t);
    }
  }
*/


  /*
   * This method reads the A/D level on the indicated channel on the indicated
   * converter IC.
   */
  public double readVoltage(int adindex, int adchannel)
  {
    if (debugging) System.out.println("in DS2450.readVoltage("+adindex+", "+adchannel+")");
    double voltage = 0.0;
    try
    {
      if (adindex > -1 && adchannel > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);
          if (adchannel < madc.getNumberADChannels())
          {
            if (debugging) System.out.println("in DS2450.readVoltage("+adindex+", "+adchannel+") entering sync");
            synchronized (adlock)
            {
              if (debugging) System.out.println("in DS2450.readVoltage("+adindex+", "+adchannel+") in sync");
              byte[] adstate = madc.readDevice();
              madc.doADConvert(adchannel, adstate);
              voltage = madc.getADVoltage(adchannel, adstate);
              if (debugging) System.out.println("in DS2450.readVoltage("+adindex+", "+adchannel+") leaving sync");
            }
            if (debugging) System.out.println("in DS2450.readVoltage("+adindex+", "+adchannel+") out sync");
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    if (debugging) System.out.println("in DS2450.readVoltage("+adindex+", "+adchannel+") leaving method");
    return voltage;
  }

  /*
   * This method reads the A/D level on all channels on the indicated
   * converter IC.
   */
  public double[] readAllVoltages(int adindex)
  {
    double[] voltages = {0.0, 0.0, 0.0, 0.0};
    try
    {
      if (adindex > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);
          boolean[] convertChannels = {true, false, true, false};
          synchronized (adlock)
          {
            byte[] adstate = madc.readDevice();
            madc.doADConvert(convertChannels, adstate);
            voltages = madc.getADVoltage(adstate);
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return voltages;
  }



  /*
   * This method reads the alarm trigger on the indicated channel on the indicated
   * converter IC.  The alarm will only be triggered by a previous readVoltage()
   * call.  The alarm flags for all channels in the device are cleared.
   */
  public boolean readAlarm(int adindex, int adchannel, int alarmtype)
  {
    boolean triggered = false;
    try
    {
      if (adindex > -1 && adchannel > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);
          if (adchannel < madc.getNumberADChannels())
          {
            synchronized (adlock)
            {
              byte[] adstate = madc.readDevice();
              if (madc.hasADAlarmed(adchannel, alarmtype, adstate))
              {
                triggered = true;
                if (debugging) {System.out.println("A/D ALARM event has occurred.");}
                madc.writeDevice(adstate);     // clear alarm flags for all channels
              }
            }
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return triggered;
  }

  /*
   * This method reads the alarm triggers on all channels on the indicated
   * converter IC.  The alarms will only be triggered by a previous readAllVoltages()
   * call.  The alarm flags for all channels in the device are cleared when read.
   */
  public boolean[] readAllAlarms(int adindex, int alarmtype)
  {
    int numberofADchannels = 4;
    boolean triggered[] = new boolean [numberofADchannels];
    try
    {
      if (adindex > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);
          for (int i = 0; i < madc.getNumberADChannels(); i++)
          {
            triggered[i] = false;
          }
          synchronized (adlock)
          {
            byte[] adstate = madc.readDevice();
            for (int chancount = 0; chancount < madc.getNumberADChannels(); chancount++)
            {
              if (madc.hasADAlarmed(chancount, alarmtype, adstate))
              {
                triggered[chancount] = true;
                if (debugging) System.out.println("A/D ALARM event has occurred on channel " + chancount + ".");
              }
            }
            madc.writeDevice(adstate);     // clear alarm flags for all channels
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return triggered;
  }

  /*
   * This method configures A/D Output capability on the indicated channel.
   */
  public void configureADOutput(int adindex, int adchannel, boolean outputenable, boolean outputstate)
  {
    try
    {
      if (adindex > -1 && adchannel > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);
          if (adchannel < madc.getNumberADChannels())
          {
            synchronized (adlock)
            {
              byte[] adstate = madc.readDevice();
              madc.setOutput(adchannel, outputenable, outputstate, adstate);
              madc.writeDevice(adstate);
            }
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return;
  }

  /*
   * This method configures A/D conversion on the indicated channel.
   */
  public void configureAD(int adindex, int adchannel, double adrange, double adresolution)
  {
    // double adrange = 2.56 or 5.12;       // volts
    // double adresolution = 0.01;  // volts
    try
    {
      if (adindex > -1 && adchannel > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);
          if (adchannel < madc.getNumberADChannels())
          {
            synchronized (adlock)
            {
              byte[] adstate = madc.readDevice();
              madc.setADRange(adchannel, adrange, adstate);
              madc.setADResolution(adchannel, adresolution, adstate);
              madc.writeDevice(adstate);
            }
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return;
  }


  /*
   * This method configures A/D alarm on the indicated channel.  You should use
   * the configureAD method before using this method.
   */
  public void configureAlarm(int adindex, int adchannel, int alarmtype, double alarmtrigger, boolean alarmenable)
  {
    // int alarmtype = 1 (high) or 0 (low)
    // double alarmtrigger = some voltage level within adrange
    // boolean alarmenable = true (enable the alarm) or false (disable the alarm)
    try
    {
      if (adindex > -1 && adchannel > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);
          if (adchannel < madc.getNumberADChannels())
          {
            synchronized (adlock)
            {
              byte[] adstate = madc.readDevice();
              madc.setADAlarm(adchannel, alarmtype, alarmtrigger, adstate);
              madc.setADAlarmEnable(adchannel, alarmtype, alarmenable, adstate);
              madc.writeDevice(adstate);
            }
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return;
  }

  /*
   * This method reads the status of the indicated A/D converter IC and prints
   * it to standard output.
   */
  public void readStatus(int adindex)
  {
    double voltage = 0.0;
    try
    {
      if (adindex > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);

          synchronized (adlock)
          {
            byte[] adstate = madc.readDevice();
            if (debugging) System.out.println("\n" + madc.getADName());
            if (debugging) System.out.println("Number of A/D Channels supported: " + madc.getNumberADChannels() + ".");
            for (int adchan=0; adchan < madc.getNumberADChannels(); adchan++)
            {
              if (debugging) System.out.println ("Channel " + adchan + " status");
              double adrangeset = madc.getADRange(adchan, adstate);
              if (debugging) System.out.println("A/D range set at: 0 to " + adrangeset + " volts.");
              double adresolutionset = madc.getADResolution(adchan, adstate);
              if (debugging) System.out.println("A/D resolution set at: " + adresolutionset + " volts.");
              int highalarm = 1;
              int lowalarm = 0;
              double alarmvoltageset = madc.getADAlarm(adchan, highalarm, adstate);
              if (debugging) System.out.println("Alarm trigger voltage set at: " + alarmvoltageset + " volts.");
              boolean alarmenabled = madc.getADAlarmEnable(adchan, highalarm, adstate);
              if (debugging) System.out.println("Alarm enabled: " + alarmenabled + ".");
              boolean outputenabled = madc.isOutputEnabled(adchan, adstate);
              if (debugging) System.out.println("Output enabled: " + outputenabled + ".");
              boolean outputlogiclevel = madc.getOutputState(adchan, adstate);
              if (debugging) System.out.println("Output logic level (true = high/false = low): " + outputlogiclevel + ".");
              boolean powerexternal = madc.isPowerExternal(adstate);
              if (debugging) System.out.println("Power set to external: " + powerexternal + ".");

            }
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return;
  }
  /*
   * This method reads the one wire bus state.
   */
  public boolean readOutput(int adindex, int adchan)
  {
    double voltage = 0.0;
    boolean outputlogiclevel=false;
    try
    {
      if ((adindex > -1) && (adindex < madvsize))
      {
        MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);

        if (adchan > -1 && adchan < madc.getNumberADChannels())
        {
          synchronized (adlock)
          {
            byte[] adstate = madc.readDevice();
            if (debugging) System.out.println("\n" + madc.getADName());
            if (debugging) System.out.println("Number of A/D Channels supported: " + madc.getNumberADChannels() + ".");

              if (debugging) System.out.println ("Channel " + adchan + " status");
              boolean outputenabled = madc.isOutputEnabled(adchan, adstate);
              if (debugging) System.out.println("Output enabled: " + outputenabled + ".");
              outputlogiclevel = madc.getOutputState(adchan, adstate);
              if (debugging) System.out.println("Output logic level (true = high/false = low): " + outputlogiclevel + ".");


          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return outputlogiclevel;
  }

  /*
   * This method reads the name of the onewire device IC.
   */
  public String readDeviceName(int adindex)
  {
    String devname = " ";
    try
    {
      if (adindex > -1)
      {
        if (adindex < madvsize)
        {
          MyADContainer   madc = (MyADContainer)madv.elementAt(adindex);

          synchronized (adlock)
          {
            devname = madc.getADName();
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
    return devname;
  }

  class MyADContainer extends iButtonContainer20
  {
    String  namea;
    byte[] lastadstate;
    public String getADName()
    {
      return(namea);
    }
    public MyADContainer(DSPortAdapter pa, byte[] addr,
                   String sa) throws iButtonException
    {
      namea = sa;
      setupContainer(pa, addr);
      synchronized (adlock)
      {
        lastadstate = readDevice();
      }
    }
  }

  /**
   * Test driver.
   */
/*  public static void main(String[] args)
  {
    double adresult = 0.0;
    boolean alarmresult = false;
    boolean debugging = true;

    try
    {
      int adindex = 0;
      int adchan = 0;
      double adrange = 5.12;
      double adresolution = 0.01;
      int alarmtype = 1;
      double alarmtrigger = 3.0;
      boolean alarmenable = true;
      boolean adoutputenable = true;
      boolean adoutputstate = true;  // true = not conducting to ground, logic 1
if (debugging) System.out.println("RICH:  WTF?");

      if (debugging) System.out.println("Starting....");
      DS2450 ADConverters = new DS2450();
      if (debugging) System.out.println("\nA/Ds Initialized.");
      if (debugging) {
        adindex = 0;
        ADConverters.readStatus(adindex);
      }

      adindex = 0;
      adchan = 0;
      // turn off output enable feature, then set up for a/d
      adoutputenable = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      adoutputenable = true;
      ADConverters.configureAD(adindex, adchan, adrange, adresolution);
      ADConverters.configureAlarm(adindex, adchan, alarmtype, alarmtrigger, alarmenable);
      if (debugging) System.out.println("\n" + ADConverters.readDeviceName(adindex) + ", A/D " + adindex + ", channel " + adchan + " reconfigured:");

      adindex = 0;
      adchan = 1;
      // set the output high
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + ", channel " + adchan + " reconfigured:");


      adindex = 0;
      adchan = 2;
      // turn off output enable feature, then set up for a/d
      adoutputenable = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      adoutputenable = true;
      ADConverters.configureAD(adindex, adchan, adrange, adresolution);
      ADConverters.configureAlarm(adindex, adchan, alarmtype, alarmtrigger, alarmenable);
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + ", channel " + adchan + " reconfigured:");

      adindex = 0;
      adchan = 3;
      // set the output high
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + ", channel " + adchan + " reconfigured:");

      if (debugging) {
        adindex = 0;
        ADConverters.readStatus(adindex);
      }
      // do an a/d
      adindex = 0;
      adchan = 0;
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");
      adindex = 0;
      adchan = 2;
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");
//      pause();

      // clear one F/F
      adindex = 0;
      adchan = 1;
      adoutputstate = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      adoutputstate = true;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " F/F cleared.");
//      pause();
      if (debugging) {
        adindex = 0;
        ADConverters.readStatus(adindex);
      }
      // do an a/d
      adindex = 0;
      adchan = 0;
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");
      adindex = 0;
      adchan = 2;
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");


      // clear one F/F
      adindex = 0;
      adchan = 3;
      adoutputstate = false;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      adoutputstate = true;
      ADConverters.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " F/F cleared.");
      if (debugging) {
        adindex = 0;
        ADConverters.readStatus(adindex);
      }
      // do an a/d
      adindex = 0;
      adchan = 0;
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");
      adindex = 0;
      adchan = 2;
      if (debugging) System.out.println(ADConverters.readDeviceName(adindex) + ", A/D " + adindex + " channel " + adchan + " conversion result = : " + ADConverters.readVoltage(adindex,adchan) + " volts.");
      adindex = 0;



      // Using one DS2450 A/D device
      // Perform A/D and check channel alarms after every conversion
      int junk = 0;
      int adchanA = 0;
      int adchanB = 2;
      int adresetA = 1;
      int adresetB = 3;
      int RotationAndLimit = 0;
      double adresultA = 0.0;
      double adresultB = 0.0;
      boolean allalarmsresult[] = {false, false, false, false};



      if (debugging) System.out.println("\n\nAbout to start continuous operation");
      if (debugging) {
        ADConverters.readStatus(RotationAndLimit);
      }


//      for (int i = 0; i<6; i++) {
      for (int i = 0; i<2; i++) {
        // do a/d
        adresultA = ADConverters.readVoltage(RotationAndLimit,adchanA);
        adresultB = ADConverters.readVoltage(RotationAndLimit,adchanB);
        if (debugging) System.out.println(ADConverters.readDeviceName(RotationAndLimit) + ", Rotation A/D " + RotationAndLimit + " channel " + adchanA + " result = : " + adresultA + " volts.");
        if (debugging) System.out.println(ADConverters.readDeviceName(RotationAndLimit) + ", Limit A/D " + RotationAndLimit + " channel " + adchanB + " result = : " + adresultB + " volts.");
        // check alarms
        allalarmsresult = ADConverters.readAllAlarms(RotationAndLimit, alarmtype);
        if (debugging) System.out.println(ADConverters.readDeviceName(RotationAndLimit) + " alarms initial check result = : \n" + allalarmsresult[0] + ", "
             + allalarmsresult[1] + ", " + allalarmsresult[2] + ", " + allalarmsresult[3] + ".");
        // if alarm, notify user and reset F/F
        if (allalarmsresult[adchanA]) {
          if (debugging) System.out.println(ADConverters.readDeviceName(RotationAndLimit) + " Rotation alarm event occured on channel " + adchanA + ".");
          if (debugging) System.out.println("About to clear Channel " + adchanA);
          //pause();
          // clear the associated F/F
          adoutputstate = false;
          ADConverters.configureADOutput(RotationAndLimit, adresetA, adoutputenable, adoutputstate);
          adoutputstate = true;
          ADConverters.configureADOutput(RotationAndLimit, adresetA, adoutputenable, adoutputstate);
          if (debugging) System.out.println(ADConverters.readDeviceName(RotationAndLimit) + ", A/D " + RotationAndLimit + " channel " + adchanA + " F/F cleared.");
          //pause();
       }
        if (allalarmsresult[adchanB]) {
          if (debugging) System.out.println(ADConverters.readDeviceName(RotationAndLimit) + " Limit alarm event occured on channel " + adchanB + ".");
          if (debugging) System.out.println("About to clear Channel " + adchanB);
          //pause();
          // clear the associated F/F
          adoutputstate = false;
          ADConverters.configureADOutput(RotationAndLimit, adresetB, adoutputenable, adoutputstate);
          adoutputstate = true;
          ADConverters.configureADOutput(RotationAndLimit, adresetB, adoutputenable, adoutputstate);
          if (debugging) System.out.println(ADConverters.readDeviceName(RotationAndLimit) + ", A/D " + RotationAndLimit + " channel " + adchanB + " F/F cleared.");
          //pause();
        }


        if (debugging) {
          ADConverters.readStatus(RotationAndLimit);
          System.out.println("Pass # " + i + " complete...");
        }

      }


      if (debugging) System.out.println("\nDone...");
    }
    catch (Throwable t)
    {
      System.out.println("Exception: " + t);
    }
  }
*/

}
