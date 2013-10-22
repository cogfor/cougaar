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

public class DS2406
{
  static Vector   msv = new Vector();         // msv = my switch vector
  static Object   slock = new Object();       // slock = switch lock

  static int      msvsize;
  static int      timeout;
  boolean debugging = false;

  public DS2406()
  {
    timeout = 20;
    try
    {
      DSPortAdapter   pa = new TINIExternalAdapter();
      File            f = new File("DS2406.txt");
      if (f.exists())
      {
        // Read 1-Wire Net Addresses and switch identifiers.
        /*
         * NOTE: The OneWire switches are read in order, and the
         *       corresponding index numbers are assigned in this
         *       same order.  Since this code uses index numbers
         *       to perform operations on switches, changing the
         *       order of the switches in the DS2406.txt file
         *       changes the operation of the code!!!
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
            msv.addElement(new MySwitchContainer(pa, addr, sa));
          }
          catch (OneWireIOException e)
          {
            System.out.println(e);
          }
        }
        br.close();
      }
      else   // identifier file does not exist, read from 1-wire bus
      {
        try {
          File newDS2406;
          newDS2406 = new File("newDS2406.txt");
          BufferedWriter bw = new BufferedWriter(new FileWriter(newDS2406));
          pa.targetFamily(0x12);
          for (Enumeration e = pa.getAlliButtons();e.hasMoreElements(); )
          {
            iButtonContainer ibc = (iButtonContainer)e.nextElement();
            byte                addr[] = ibc.getAddress();
            String              sn = ibc.getAddressAsString();
            msv.addElement(new MySwitchContainer(pa, addr, sn));
            bw.write(sn,0,sn.length());
            bw.newLine();
          }
          bw.flush();
          bw.close();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
        catch (OneWireIOException e) {
          System.out.println();
        }
      }
      msvsize = msv.size();
      if (msvsize > 0)
      {
        int     i = 0;
        boolean level = true;
        for (Enumeration e = msv.elements(); e.hasMoreElements(); )
        {
          MySwitchContainer msc = (MySwitchContainer)e.nextElement();
          try
          {
            synchronized (slock)
            {
              msc.readSwitchState(true);
            }
            i++;
          }
          catch (Throwable t)
          {
            i += 1;
          }
        }
      }
      else
      {
        System.out.println("No Switches");
      }
    }
    catch (Exception e)
    {
      System.out.println(e);
    }
  }


  // test code
/*  public void main(String args[])
  {
    try
    {
      System.out.println("Starting....");
      MyDS2406 DalSemiSwitches = new MyDS2406();
      System.out.println("\nSwitches Initialized...");

      System.out.println("Available switches are:");
      for (int k=0; k<msvsize; k++) {
        System.out.println("  " + DalSemiSwitches.readSwitchName(k));
      }

      System.out.println("Status of Switches:");
      for (int k=0; k<msvsize; k++) {
        DalSemiSwitches.reportSwitch(k);
      }

      // Set switch 0 high (not conducting to ground)
      DalSemiSwitches.setSwitch(0, true);
      // Set switch 0 low (conducting to ground)
      DalSemiSwitches.setSwitch(0, false);

      System.out.println("\nDone...");

    }
    catch (Throwable t)
    {
      System.out.println(t);
    }
  }
*/

  public void setSwitch(int switchindex, boolean level)
  {
    try
    {
      if (switchindex > -1)
      {
        if (switchindex < msvsize)
        {
          MySwitchContainer   msc = (MySwitchContainer)msv.elementAt(switchindex);
          synchronized (slock)
          {
            msc.setSwitchState(level);
          }
        }
      }
    }
    catch (Throwable t)
    {
    }
  }

  /*
   * This method reports the state and level of switch number
   * i to standard output.  If switch number i does not exist,
   * this method does nothing.  HARDWARE NOTE: state is the
   * opposite of voltage level, but an inverter IC is used on
   * the outputs.
   */
  void reportSwitch(int i)
  {
    if ((i > -1) && (i < msvsize))
    {
      MySwitchContainer msc = (MySwitchContainer)msv.elementAt(i);

      try
      {
        boolean state;
        boolean level;
        String serno;
        String swname;

        msc.readSwitchState(true);
        state = msc.getChannelAState();
        level = msc.getChannelALevel();
        serno = msc.getAddressAsString();
        swname = msc.getSwitchName();
        if (debugging) {System.out.println("Switch index #: " + i + ", " + swname + ", resides at address " + serno + ".");}
        if (debugging) {System.out.println("STATE = " + state + ", LEVEL = " + level + ".");}
      }
      catch (Exception e)
      {
      }
    }
  }

   /*
   * This method returns the name of the indicated DS2406 switch.
   */
  String readSwitchName(int i)
  {
    String devname = " ";
    if ((i > -1) && (i < msvsize))
    {
      MySwitchContainer msc = (MySwitchContainer)msv.elementAt(i);

      try
      {
        msc.readSwitchState(true);
        devname = msc.getSwitchName();
      }
      catch (Exception e)
      {
      }
    }
    return devname;
  }

 class MySwitchContainer extends iButtonContainer12
  {
    String  namea;
    boolean lastastate, lastalevel;
    public String getSwitchName()
    {
      return(namea);
    }
    public MySwitchContainer(DSPortAdapter pa, byte[] addr,
                   String sa) throws iButtonException
    {
      namea = sa;
      setupContainer(pa, addr);
      synchronized (slock)
      {
        readSwitchState(true);
        lastastate = getChannelAState();
        lastalevel = !getChannelALevel();
      }
    }
  }
}

