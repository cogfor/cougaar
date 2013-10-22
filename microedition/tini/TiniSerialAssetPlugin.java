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

import javax.comm.*;
import java.io.*;
import java.net.*;
import com.dalsemi.tininet.*;

import org.cougaar.microedition.plugin.*;

public class TiniSerialAssetPlugin extends PluginAdapter {

  public void setupSubscriptions() {
    Thread t = new Thread(new ReadSerial());
    t.start();
  }

  public void execute() {}

  class ReadSerial implements Runnable {

    public void run() {
      try {
        com.dalsemi.system.TINIOS.enableSerialPort1();
        CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier("serial1");
        SerialPort sp = (SerialPort)portId.open("testApp", 0);
        sp.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        /* this helps TINI service heavy data better.  These numbers can be optimized to
            a specific application.  The use of a receive threshold is very useful when receiving
            large amounts of data */
        sp.enableReceiveThreshold(1024);
        sp.enableReceiveTimeout(1000);

        // Get our input stream for the serial port
        InputStream in = sp.getInputStream();

        // total number of bytes read.
        int bytesRead = 0;
        int num = 0;

        // Array to be used for data read.
        byte[] data = new byte[1024];

        while (true) {
          // read from the serial port.
          num = in.read(data);
          // if we got data then write it out.
          if (num > 0) {
           String strdata = new String(data, 0, num);
           System.out.print("\nNNNN:"+num+":\n"+strdata+"ZZZZ\n");
          }
        }
      }
      catch(Exception e) {
         System.out.println("GOT AN EXCEPTION");
         System.out.println(e.toString());
         com.dalsemi.system.Debug.debugDump("Exception");
         com.dalsemi.system.Debug.debugDump(e.toString());
      }
    }
  }
}