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
import com.systronix.sbx2.*;

/**
 * create a Tini Compass.
 */
public class TiniCompass extends CompassResource {

  static String[] Units = { "Degrees Magnetic"};

  public TiniCompass() {}

  //abstract method from SensorResource. A no op.
  public void setChan(int c) {}

  //abstract method from SensorResource
  public void setUnits(String u)
  {
    units = Units[0];
  }

    //These are the BCD interpretations of the 10 bits
  static final long BIT0VAL = 1;
  static final long BIT1VAL = 2;
  static final long BIT2VAL = 4;
  static final long BIT3VAL = 8;
  static final long BIT4VAL = 10;
  static final long BIT5VAL = 20;
  static final long BIT6VAL = 40;
  static final long BIT7VAL = 80;
  static final long BIT8VAL = 100;
  static final long BIT9VAL = 200;

  static final byte BIT0 = (byte)1;
  static final byte BIT1 = (byte)1<<1;
  static final byte BIT2 = (byte)1<<2;
  static final byte BIT3 = (byte)1<<3;
  static final byte BIT4 = (byte)1<<4;
  static final byte BIT5 = (byte)1<<5;
  static final byte BIT6 = (byte)1<<6;
  static final byte BIT7 = (byte)(1<<7);

  public static void printbits(byte b)
  {
    if((b & BIT0) != 0) System.out.print("1"); else System.out.print("0");
    if((b & BIT1) != 0) System.out.print("1"); else System.out.print("0");
    if((b & BIT2) != 0) System.out.print("1"); else System.out.print("0");
    if((b & BIT3) != 0) System.out.print("1"); else System.out.print("0");
    if((b & BIT4) != 0) System.out.print("1"); else System.out.print("0");
    if((b & BIT5) != 0) System.out.print("1"); else System.out.print("0");
    if((b & BIT6) != 0) System.out.print("1"); else System.out.print("0");
    if((b & BIT7) != 0) System.out.print("1"); else System.out.print("0");
    System.out.print(" ");
  }

  public long getValue()
  {

    byte b1, b2, b3;
    long longval = 0;

    b1 = Parallel.getInput(Parallel.INOUT0);
    b2 = Parallel.getInput(Parallel.INOUT1);
    b3 = Parallel.getInput(Parallel.INOUT2);

    //seems to require negation
    b1 = (byte)~b1;
    b2 = (byte)~b2;
    b3 = (byte)~b3;

    //printbits(b1);
    //printbits(b2);
    //printbits(b3);
    //System.out.println();

    //interpret compass BCD
    //assumes the INOUT1 is the first 8 bits and
    //the final 2 bits come from the first 2 bits of INOUT2

    longval = 0;
    if((b2 & BIT0) != 0) longval += BIT0VAL;
    if((b2 & BIT1) != 0) longval += BIT1VAL;
    if((b2 & BIT2) != 0) longval += BIT2VAL;
    if((b2 & BIT3) != 0) longval += BIT3VAL;
    if((b2 & BIT4) != 0) longval += BIT4VAL;
    if((b2 & BIT5) != 0) longval += BIT5VAL;
    if((b2 & BIT6) != 0) longval += BIT6VAL;
    if((b2 & BIT7) != 0) longval += BIT7VAL;

    if((b3 & BIT0) != 0) longval += BIT8VAL;
    if((b3 & BIT1) != 0) longval += BIT9VAL;

    return longval;
  }
}
