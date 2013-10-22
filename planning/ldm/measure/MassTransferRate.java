/*
 * <copyright>
 *  
 *  Copyright 1997-2012 Raytheon BBN Technologies
 *  under partial sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

/* @generated Wed Jun 06 07:55:05 EDT 2012 from measures.def - DO NOT HAND EDIT */
/** Immutable implementation of MassTransferRate.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class MassTransferRate extends AbstractMeasure
  implements Externalizable, Derivative, Rate {
  // the value is stored as grams/second
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public MassTransferRate() {}

  // private constructor
  private MassTransferRate(double v) {
    theValue = v;
  }

  /** @param unit One of the constant units of MassTransferRate **/
  public MassTransferRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v/getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Mass
   *  @param unit2 One of the constant units of Duration
   **/
  public MassTransferRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Mass.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      theValue = v*Mass.getConvFactor(unit1)/Duration.getConvFactor(unit2);
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Mass to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public MassTransferRate(Mass num, Duration den) {
    theValue = num.getValue(0)/den.getValue(0);
  }

  /** takes strings of the form "Number unit" **/
  public MassTransferRate(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("gramspersecond")) 
      theValue=n/(1.0d/1.0d);
    else if (u.equals("gramsperminute")) 
      theValue=n/(1.0d/(1.0d/60));
    else if (u.equals("gramsperhour")) 
      theValue=n/(1.0d/(1.0d/3600));
    else if (u.equals("gramsperday")) 
      theValue=n/(1.0d/(1.0d/86400));
    else if (u.equals("gramsperweek")) 
      theValue=n/(1.0d/(1.0d/604800));
    else if (u.equals("gramspermillisecond")) 
      theValue=n/(1.0d/1000);
    else if (u.equals("gramsperkilosecond")) 
      theValue=n/(1.0d/(1.0d/1000));
    else if (u.equals("gramspermonth")) 
      theValue=n/(1.0d/(1.0d/2629743.8));
    else if (u.equals("gramsperyear")) 
      theValue=n/(1.0d/(1.0d/31556926));
    else if (u.equals("gramsperfortnight")) 
      theValue=n/(1.0d/(1.0d/1209600));
    else if (u.equals("kilogramspersecond")) 
      theValue=n/((1.0d/1000)/1.0d);
    else if (u.equals("kilogramsperminute")) 
      theValue=n/((1.0d/1000)/(1.0d/60));
    else if (u.equals("kilogramsperhour")) 
      theValue=n/((1.0d/1000)/(1.0d/3600));
    else if (u.equals("kilogramsperday")) 
      theValue=n/((1.0d/1000)/(1.0d/86400));
    else if (u.equals("kilogramsperweek")) 
      theValue=n/((1.0d/1000)/(1.0d/604800));
    else if (u.equals("kilogramspermillisecond")) 
      theValue=n/((1.0d/1000)/1000);
    else if (u.equals("kilogramsperkilosecond")) 
      theValue=n/((1.0d/1000)/(1.0d/1000));
    else if (u.equals("kilogramspermonth")) 
      theValue=n/((1.0d/1000)/(1.0d/2629743.8));
    else if (u.equals("kilogramsperyear")) 
      theValue=n/((1.0d/1000)/(1.0d/31556926));
    else if (u.equals("kilogramsperfortnight")) 
      theValue=n/((1.0d/1000)/(1.0d/1209600));
    else if (u.equals("ouncespersecond")) 
      theValue=n/(0.035273962/1.0d);
    else if (u.equals("ouncesperminute")) 
      theValue=n/(0.035273962/(1.0d/60));
    else if (u.equals("ouncesperhour")) 
      theValue=n/(0.035273962/(1.0d/3600));
    else if (u.equals("ouncesperday")) 
      theValue=n/(0.035273962/(1.0d/86400));
    else if (u.equals("ouncesperweek")) 
      theValue=n/(0.035273962/(1.0d/604800));
    else if (u.equals("ouncespermillisecond")) 
      theValue=n/(0.035273962/1000);
    else if (u.equals("ouncesperkilosecond")) 
      theValue=n/(0.035273962/(1.0d/1000));
    else if (u.equals("ouncespermonth")) 
      theValue=n/(0.035273962/(1.0d/2629743.8));
    else if (u.equals("ouncesperyear")) 
      theValue=n/(0.035273962/(1.0d/31556926));
    else if (u.equals("ouncesperfortnight")) 
      theValue=n/(0.035273962/(1.0d/1209600));
    else if (u.equals("poundspersecond")) 
      theValue=n/(0.0022046226/1.0d);
    else if (u.equals("poundsperminute")) 
      theValue=n/(0.0022046226/(1.0d/60));
    else if (u.equals("poundsperhour")) 
      theValue=n/(0.0022046226/(1.0d/3600));
    else if (u.equals("poundsperday")) 
      theValue=n/(0.0022046226/(1.0d/86400));
    else if (u.equals("poundsperweek")) 
      theValue=n/(0.0022046226/(1.0d/604800));
    else if (u.equals("poundspermillisecond")) 
      theValue=n/(0.0022046226/1000);
    else if (u.equals("poundsperkilosecond")) 
      theValue=n/(0.0022046226/(1.0d/1000));
    else if (u.equals("poundspermonth")) 
      theValue=n/(0.0022046226/(1.0d/2629743.8));
    else if (u.equals("poundsperyear")) 
      theValue=n/(0.0022046226/(1.0d/31556926));
    else if (u.equals("poundsperfortnight")) 
      theValue=n/(0.0022046226/(1.0d/1209600));
    else if (u.equals("tonspersecond")) 
      theValue=n/((1.0d/907184.74)/1.0d);
    else if (u.equals("tonsperminute")) 
      theValue=n/((1.0d/907184.74)/(1.0d/60));
    else if (u.equals("tonsperhour")) 
      theValue=n/((1.0d/907184.74)/(1.0d/3600));
    else if (u.equals("tonsperday")) 
      theValue=n/((1.0d/907184.74)/(1.0d/86400));
    else if (u.equals("tonsperweek")) 
      theValue=n/((1.0d/907184.74)/(1.0d/604800));
    else if (u.equals("tonspermillisecond")) 
      theValue=n/((1.0d/907184.74)/1000);
    else if (u.equals("tonsperkilosecond")) 
      theValue=n/((1.0d/907184.74)/(1.0d/1000));
    else if (u.equals("tonspermonth")) 
      theValue=n/((1.0d/907184.74)/(1.0d/2629743.8));
    else if (u.equals("tonsperyear")) 
      theValue=n/((1.0d/907184.74)/(1.0d/31556926));
    else if (u.equals("tonsperfortnight")) 
      theValue=n/((1.0d/907184.74)/(1.0d/1209600));
    else if (u.equals("shorttonspersecond")) 
      theValue=n/((1.0d/907184.74)/1.0d);
    else if (u.equals("shorttonsperminute")) 
      theValue=n/((1.0d/907184.74)/(1.0d/60));
    else if (u.equals("shorttonsperhour")) 
      theValue=n/((1.0d/907184.74)/(1.0d/3600));
    else if (u.equals("shorttonsperday")) 
      theValue=n/((1.0d/907184.74)/(1.0d/86400));
    else if (u.equals("shorttonsperweek")) 
      theValue=n/((1.0d/907184.74)/(1.0d/604800));
    else if (u.equals("shorttonspermillisecond")) 
      theValue=n/((1.0d/907184.74)/1000);
    else if (u.equals("shorttonsperkilosecond")) 
      theValue=n/((1.0d/907184.74)/(1.0d/1000));
    else if (u.equals("shorttonspermonth")) 
      theValue=n/((1.0d/907184.74)/(1.0d/2629743.8));
    else if (u.equals("shorttonsperyear")) 
      theValue=n/((1.0d/907184.74)/(1.0d/31556926));
    else if (u.equals("shorttonsperfortnight")) 
      theValue=n/((1.0d/907184.74)/(1.0d/1209600));
    else if (u.equals("longtonspersecond")) 
      theValue=n/((1.0d/1016046.9)/1.0d);
    else if (u.equals("longtonsperminute")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/60));
    else if (u.equals("longtonsperhour")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/3600));
    else if (u.equals("longtonsperday")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/86400));
    else if (u.equals("longtonsperweek")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/604800));
    else if (u.equals("longtonspermillisecond")) 
      theValue=n/((1.0d/1016046.9)/1000);
    else if (u.equals("longtonsperkilosecond")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/1000));
    else if (u.equals("longtonspermonth")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/2629743.8));
    else if (u.equals("longtonsperyear")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/31556926));
    else if (u.equals("longtonsperfortnight")) 
      theValue=n/((1.0d/1016046.9)/(1.0d/1209600));
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final MassTransferRate newGramsPerSecond(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/1.0d)));
  }
  public static final MassTransferRate newGramsPerSecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1.0d)));
  }
  public static final MassTransferRate newGramsPerMinute(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final MassTransferRate newGramsPerMinute(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final MassTransferRate newGramsPerHour(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final MassTransferRate newGramsPerHour(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final MassTransferRate newGramsPerDay(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final MassTransferRate newGramsPerDay(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final MassTransferRate newGramsPerWeek(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final MassTransferRate newGramsPerWeek(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final MassTransferRate newGramsPerMillisecond(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/1000)));
  }
  public static final MassTransferRate newGramsPerMillisecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1000)));
  }
  public static final MassTransferRate newGramsPerKilosecond(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final MassTransferRate newGramsPerKilosecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final MassTransferRate newGramsPerMonth(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newGramsPerMonth(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newGramsPerYear(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final MassTransferRate newGramsPerYear(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final MassTransferRate newGramsPerFortnight(double v) {
    return new MassTransferRate(v*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final MassTransferRate newGramsPerFortnight(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final MassTransferRate newKilogramsPerSecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/1.0d)));
  }
  public static final MassTransferRate newKilogramsPerSecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/1.0d)));
  }
  public static final MassTransferRate newKilogramsPerMinute(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/60))));
  }
  public static final MassTransferRate newKilogramsPerMinute(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/60))));
  }
  public static final MassTransferRate newKilogramsPerHour(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/3600))));
  }
  public static final MassTransferRate newKilogramsPerHour(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/3600))));
  }
  public static final MassTransferRate newKilogramsPerDay(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/86400))));
  }
  public static final MassTransferRate newKilogramsPerDay(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/86400))));
  }
  public static final MassTransferRate newKilogramsPerWeek(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/604800))));
  }
  public static final MassTransferRate newKilogramsPerWeek(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/604800))));
  }
  public static final MassTransferRate newKilogramsPerMillisecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/1000)));
  }
  public static final MassTransferRate newKilogramsPerMillisecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/1000)));
  }
  public static final MassTransferRate newKilogramsPerKilosecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/1000))));
  }
  public static final MassTransferRate newKilogramsPerKilosecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/1000))));
  }
  public static final MassTransferRate newKilogramsPerMonth(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newKilogramsPerMonth(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newKilogramsPerYear(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/31556926))));
  }
  public static final MassTransferRate newKilogramsPerYear(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/31556926))));
  }
  public static final MassTransferRate newKilogramsPerFortnight(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1000)/(1.0d/1209600))));
  }
  public static final MassTransferRate newKilogramsPerFortnight(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/1209600))));
  }
  public static final MassTransferRate newOuncesPerSecond(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/1.0d)));
  }
  public static final MassTransferRate newOuncesPerSecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/1.0d)));
  }
  public static final MassTransferRate newOuncesPerMinute(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/60))));
  }
  public static final MassTransferRate newOuncesPerMinute(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/60))));
  }
  public static final MassTransferRate newOuncesPerHour(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/3600))));
  }
  public static final MassTransferRate newOuncesPerHour(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/3600))));
  }
  public static final MassTransferRate newOuncesPerDay(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/86400))));
  }
  public static final MassTransferRate newOuncesPerDay(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/86400))));
  }
  public static final MassTransferRate newOuncesPerWeek(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/604800))));
  }
  public static final MassTransferRate newOuncesPerWeek(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/604800))));
  }
  public static final MassTransferRate newOuncesPerMillisecond(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/1000)));
  }
  public static final MassTransferRate newOuncesPerMillisecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/1000)));
  }
  public static final MassTransferRate newOuncesPerKilosecond(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/1000))));
  }
  public static final MassTransferRate newOuncesPerKilosecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/1000))));
  }
  public static final MassTransferRate newOuncesPerMonth(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newOuncesPerMonth(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newOuncesPerYear(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/31556926))));
  }
  public static final MassTransferRate newOuncesPerYear(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/31556926))));
  }
  public static final MassTransferRate newOuncesPerFortnight(double v) {
    return new MassTransferRate(v*(1.0d/(0.035273962/(1.0d/1209600))));
  }
  public static final MassTransferRate newOuncesPerFortnight(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.035273962/(1.0d/1209600))));
  }
  public static final MassTransferRate newPoundsPerSecond(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/1.0d)));
  }
  public static final MassTransferRate newPoundsPerSecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/1.0d)));
  }
  public static final MassTransferRate newPoundsPerMinute(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/60))));
  }
  public static final MassTransferRate newPoundsPerMinute(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/60))));
  }
  public static final MassTransferRate newPoundsPerHour(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/3600))));
  }
  public static final MassTransferRate newPoundsPerHour(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/3600))));
  }
  public static final MassTransferRate newPoundsPerDay(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/86400))));
  }
  public static final MassTransferRate newPoundsPerDay(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/86400))));
  }
  public static final MassTransferRate newPoundsPerWeek(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/604800))));
  }
  public static final MassTransferRate newPoundsPerWeek(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/604800))));
  }
  public static final MassTransferRate newPoundsPerMillisecond(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/1000)));
  }
  public static final MassTransferRate newPoundsPerMillisecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/1000)));
  }
  public static final MassTransferRate newPoundsPerKilosecond(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/1000))));
  }
  public static final MassTransferRate newPoundsPerKilosecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/1000))));
  }
  public static final MassTransferRate newPoundsPerMonth(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newPoundsPerMonth(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newPoundsPerYear(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/31556926))));
  }
  public static final MassTransferRate newPoundsPerYear(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/31556926))));
  }
  public static final MassTransferRate newPoundsPerFortnight(double v) {
    return new MassTransferRate(v*(1.0d/(0.0022046226/(1.0d/1209600))));
  }
  public static final MassTransferRate newPoundsPerFortnight(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/(0.0022046226/(1.0d/1209600))));
  }
  public static final MassTransferRate newTonsPerSecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/1.0d)));
  }
  public static final MassTransferRate newTonsPerSecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/1.0d)));
  }
  public static final MassTransferRate newTonsPerMinute(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/60))));
  }
  public static final MassTransferRate newTonsPerMinute(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/60))));
  }
  public static final MassTransferRate newTonsPerHour(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/3600))));
  }
  public static final MassTransferRate newTonsPerHour(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/3600))));
  }
  public static final MassTransferRate newTonsPerDay(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/86400))));
  }
  public static final MassTransferRate newTonsPerDay(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/86400))));
  }
  public static final MassTransferRate newTonsPerWeek(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/604800))));
  }
  public static final MassTransferRate newTonsPerWeek(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/604800))));
  }
  public static final MassTransferRate newTonsPerMillisecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/1000)));
  }
  public static final MassTransferRate newTonsPerMillisecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/1000)));
  }
  public static final MassTransferRate newTonsPerKilosecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/1000))));
  }
  public static final MassTransferRate newTonsPerKilosecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/1000))));
  }
  public static final MassTransferRate newTonsPerMonth(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newTonsPerMonth(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newTonsPerYear(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/31556926))));
  }
  public static final MassTransferRate newTonsPerYear(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/31556926))));
  }
  public static final MassTransferRate newTonsPerFortnight(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/1209600))));
  }
  public static final MassTransferRate newTonsPerFortnight(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/1209600))));
  }
  public static final MassTransferRate newShortTonsPerSecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/1.0d)));
  }
  public static final MassTransferRate newShortTonsPerSecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/1.0d)));
  }
  public static final MassTransferRate newShortTonsPerMinute(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/60))));
  }
  public static final MassTransferRate newShortTonsPerMinute(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/60))));
  }
  public static final MassTransferRate newShortTonsPerHour(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/3600))));
  }
  public static final MassTransferRate newShortTonsPerHour(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/3600))));
  }
  public static final MassTransferRate newShortTonsPerDay(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/86400))));
  }
  public static final MassTransferRate newShortTonsPerDay(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/86400))));
  }
  public static final MassTransferRate newShortTonsPerWeek(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/604800))));
  }
  public static final MassTransferRate newShortTonsPerWeek(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/604800))));
  }
  public static final MassTransferRate newShortTonsPerMillisecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/1000)));
  }
  public static final MassTransferRate newShortTonsPerMillisecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/1000)));
  }
  public static final MassTransferRate newShortTonsPerKilosecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/1000))));
  }
  public static final MassTransferRate newShortTonsPerKilosecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/1000))));
  }
  public static final MassTransferRate newShortTonsPerMonth(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newShortTonsPerMonth(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newShortTonsPerYear(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/31556926))));
  }
  public static final MassTransferRate newShortTonsPerYear(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/31556926))));
  }
  public static final MassTransferRate newShortTonsPerFortnight(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/907184.74)/(1.0d/1209600))));
  }
  public static final MassTransferRate newShortTonsPerFortnight(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/907184.74)/(1.0d/1209600))));
  }
  public static final MassTransferRate newLongTonsPerSecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/1.0d)));
  }
  public static final MassTransferRate newLongTonsPerSecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/1.0d)));
  }
  public static final MassTransferRate newLongTonsPerMinute(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/60))));
  }
  public static final MassTransferRate newLongTonsPerMinute(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/60))));
  }
  public static final MassTransferRate newLongTonsPerHour(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/3600))));
  }
  public static final MassTransferRate newLongTonsPerHour(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/3600))));
  }
  public static final MassTransferRate newLongTonsPerDay(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/86400))));
  }
  public static final MassTransferRate newLongTonsPerDay(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/86400))));
  }
  public static final MassTransferRate newLongTonsPerWeek(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/604800))));
  }
  public static final MassTransferRate newLongTonsPerWeek(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/604800))));
  }
  public static final MassTransferRate newLongTonsPerMillisecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/1000)));
  }
  public static final MassTransferRate newLongTonsPerMillisecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/1000)));
  }
  public static final MassTransferRate newLongTonsPerKilosecond(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/1000))));
  }
  public static final MassTransferRate newLongTonsPerKilosecond(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/1000))));
  }
  public static final MassTransferRate newLongTonsPerMonth(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newLongTonsPerMonth(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/2629743.8))));
  }
  public static final MassTransferRate newLongTonsPerYear(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/31556926))));
  }
  public static final MassTransferRate newLongTonsPerYear(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/31556926))));
  }
  public static final MassTransferRate newLongTonsPerFortnight(double v) {
    return new MassTransferRate(v*(1.0d/((1.0d/1016046.9)/(1.0d/1209600))));
  }
  public static final MassTransferRate newLongTonsPerFortnight(String s) {
    return new MassTransferRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1016046.9)/(1.0d/1209600))));
  }


  public int getCommonUnit() {
    return 30;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "grams/second",
    "grams/minute",
    "grams/hour",
    "grams/day",
    "grams/week",
    "grams/millisecond",
    "grams/kilosecond",
    "grams/month",
    "grams/year",
    "grams/fortnight",
    "kilograms/second",
    "kilograms/minute",
    "kilograms/hour",
    "kilograms/day",
    "kilograms/week",
    "kilograms/millisecond",
    "kilograms/kilosecond",
    "kilograms/month",
    "kilograms/year",
    "kilograms/fortnight",
    "ounces/second",
    "ounces/minute",
    "ounces/hour",
    "ounces/day",
    "ounces/week",
    "ounces/millisecond",
    "ounces/kilosecond",
    "ounces/month",
    "ounces/year",
    "ounces/fortnight",
    "pounds/second",
    "pounds/minute",
    "pounds/hour",
    "pounds/day",
    "pounds/week",
    "pounds/millisecond",
    "pounds/kilosecond",
    "pounds/month",
    "pounds/year",
    "pounds/fortnight",
    "tons/second",
    "tons/minute",
    "tons/hour",
    "tons/day",
    "tons/week",
    "tons/millisecond",
    "tons/kilosecond",
    "tons/month",
    "tons/year",
    "tons/fortnight",
    "short_tons/second",
    "short_tons/minute",
    "short_tons/hour",
    "short_tons/day",
    "short_tons/week",
    "short_tons/millisecond",
    "short_tons/kilosecond",
    "short_tons/month",
    "short_tons/year",
    "short_tons/fortnight",
    "long_tons/second",
    "long_tons/minute",
    "long_tons/hour",
    "long_tons/day",
    "long_tons/week",
    "long_tons/millisecond",
    "long_tons/kilosecond",
    "long_tons/month",
    "long_tons/year",
    "long_tons/fortnight",
  };

  /** @param unit One of the constant units of MassTransferRate **/
  public final String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    (1.0d/1.0d),
    (1.0d/(1.0d/60)),
    (1.0d/(1.0d/3600)),
    (1.0d/(1.0d/86400)),
    (1.0d/(1.0d/604800)),
    (1.0d/1000),
    (1.0d/(1.0d/1000)),
    (1.0d/(1.0d/2629743.8)),
    (1.0d/(1.0d/31556926)),
    (1.0d/(1.0d/1209600)),
    ((1.0d/1000)/1.0d),
    ((1.0d/1000)/(1.0d/60)),
    ((1.0d/1000)/(1.0d/3600)),
    ((1.0d/1000)/(1.0d/86400)),
    ((1.0d/1000)/(1.0d/604800)),
    ((1.0d/1000)/1000),
    ((1.0d/1000)/(1.0d/1000)),
    ((1.0d/1000)/(1.0d/2629743.8)),
    ((1.0d/1000)/(1.0d/31556926)),
    ((1.0d/1000)/(1.0d/1209600)),
    (0.035273962/1.0d),
    (0.035273962/(1.0d/60)),
    (0.035273962/(1.0d/3600)),
    (0.035273962/(1.0d/86400)),
    (0.035273962/(1.0d/604800)),
    (0.035273962/1000),
    (0.035273962/(1.0d/1000)),
    (0.035273962/(1.0d/2629743.8)),
    (0.035273962/(1.0d/31556926)),
    (0.035273962/(1.0d/1209600)),
    (0.0022046226/1.0d),
    (0.0022046226/(1.0d/60)),
    (0.0022046226/(1.0d/3600)),
    (0.0022046226/(1.0d/86400)),
    (0.0022046226/(1.0d/604800)),
    (0.0022046226/1000),
    (0.0022046226/(1.0d/1000)),
    (0.0022046226/(1.0d/2629743.8)),
    (0.0022046226/(1.0d/31556926)),
    (0.0022046226/(1.0d/1209600)),
    ((1.0d/907184.74)/1.0d),
    ((1.0d/907184.74)/(1.0d/60)),
    ((1.0d/907184.74)/(1.0d/3600)),
    ((1.0d/907184.74)/(1.0d/86400)),
    ((1.0d/907184.74)/(1.0d/604800)),
    ((1.0d/907184.74)/1000),
    ((1.0d/907184.74)/(1.0d/1000)),
    ((1.0d/907184.74)/(1.0d/2629743.8)),
    ((1.0d/907184.74)/(1.0d/31556926)),
    ((1.0d/907184.74)/(1.0d/1209600)),
    ((1.0d/907184.74)/1.0d),
    ((1.0d/907184.74)/(1.0d/60)),
    ((1.0d/907184.74)/(1.0d/3600)),
    ((1.0d/907184.74)/(1.0d/86400)),
    ((1.0d/907184.74)/(1.0d/604800)),
    ((1.0d/907184.74)/1000),
    ((1.0d/907184.74)/(1.0d/1000)),
    ((1.0d/907184.74)/(1.0d/2629743.8)),
    ((1.0d/907184.74)/(1.0d/31556926)),
    ((1.0d/907184.74)/(1.0d/1209600)),
    ((1.0d/1016046.9)/1.0d),
    ((1.0d/1016046.9)/(1.0d/60)),
    ((1.0d/1016046.9)/(1.0d/3600)),
    ((1.0d/1016046.9)/(1.0d/86400)),
    ((1.0d/1016046.9)/(1.0d/604800)),
    ((1.0d/1016046.9)/1000),
    ((1.0d/1016046.9)/(1.0d/1000)),
    ((1.0d/1016046.9)/(1.0d/2629743.8)),
    ((1.0d/1016046.9)/(1.0d/31556926)),
    ((1.0d/1016046.9)/(1.0d/1209600)),
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int GRAMS_PER_SECOND = 0;
  public static final int GRAMS_PER_MINUTE = 1;
  public static final int GRAMS_PER_HOUR = 2;
  public static final int GRAMS_PER_DAY = 3;
  public static final int GRAMS_PER_WEEK = 4;
  public static final int GRAMS_PER_MILLISECOND = 5;
  public static final int GRAMS_PER_KILOSECOND = 6;
  public static final int GRAMS_PER_MONTH = 7;
  public static final int GRAMS_PER_YEAR = 8;
  public static final int GRAMS_PER_FORTNIGHT = 9;
  public static final int KILOGRAMS_PER_SECOND = 10;
  public static final int KILOGRAMS_PER_MINUTE = 11;
  public static final int KILOGRAMS_PER_HOUR = 12;
  public static final int KILOGRAMS_PER_DAY = 13;
  public static final int KILOGRAMS_PER_WEEK = 14;
  public static final int KILOGRAMS_PER_MILLISECOND = 15;
  public static final int KILOGRAMS_PER_KILOSECOND = 16;
  public static final int KILOGRAMS_PER_MONTH = 17;
  public static final int KILOGRAMS_PER_YEAR = 18;
  public static final int KILOGRAMS_PER_FORTNIGHT = 19;
  public static final int OUNCES_PER_SECOND = 20;
  public static final int OUNCES_PER_MINUTE = 21;
  public static final int OUNCES_PER_HOUR = 22;
  public static final int OUNCES_PER_DAY = 23;
  public static final int OUNCES_PER_WEEK = 24;
  public static final int OUNCES_PER_MILLISECOND = 25;
  public static final int OUNCES_PER_KILOSECOND = 26;
  public static final int OUNCES_PER_MONTH = 27;
  public static final int OUNCES_PER_YEAR = 28;
  public static final int OUNCES_PER_FORTNIGHT = 29;
  public static final int POUNDS_PER_SECOND = 30;
  public static final int POUNDS_PER_MINUTE = 31;
  public static final int POUNDS_PER_HOUR = 32;
  public static final int POUNDS_PER_DAY = 33;
  public static final int POUNDS_PER_WEEK = 34;
  public static final int POUNDS_PER_MILLISECOND = 35;
  public static final int POUNDS_PER_KILOSECOND = 36;
  public static final int POUNDS_PER_MONTH = 37;
  public static final int POUNDS_PER_YEAR = 38;
  public static final int POUNDS_PER_FORTNIGHT = 39;
  public static final int TONS_PER_SECOND = 40;
  public static final int TONS_PER_MINUTE = 41;
  public static final int TONS_PER_HOUR = 42;
  public static final int TONS_PER_DAY = 43;
  public static final int TONS_PER_WEEK = 44;
  public static final int TONS_PER_MILLISECOND = 45;
  public static final int TONS_PER_KILOSECOND = 46;
  public static final int TONS_PER_MONTH = 47;
  public static final int TONS_PER_YEAR = 48;
  public static final int TONS_PER_FORTNIGHT = 49;
  public static final int SHORT_TONS_PER_SECOND = 50;
  public static final int SHORT_TONS_PER_MINUTE = 51;
  public static final int SHORT_TONS_PER_HOUR = 52;
  public static final int SHORT_TONS_PER_DAY = 53;
  public static final int SHORT_TONS_PER_WEEK = 54;
  public static final int SHORT_TONS_PER_MILLISECOND = 55;
  public static final int SHORT_TONS_PER_KILOSECOND = 56;
  public static final int SHORT_TONS_PER_MONTH = 57;
  public static final int SHORT_TONS_PER_YEAR = 58;
  public static final int SHORT_TONS_PER_FORTNIGHT = 59;
  public static final int LONG_TONS_PER_SECOND = 60;
  public static final int LONG_TONS_PER_MINUTE = 61;
  public static final int LONG_TONS_PER_HOUR = 62;
  public static final int LONG_TONS_PER_DAY = 63;
  public static final int LONG_TONS_PER_WEEK = 64;
  public static final int LONG_TONS_PER_MILLISECOND = 65;
  public static final int LONG_TONS_PER_KILOSECOND = 66;
  public static final int LONG_TONS_PER_MONTH = 67;
  public static final int LONG_TONS_PER_YEAR = 68;
  public static final int LONG_TONS_PER_FORTNIGHT = 69;
  static final int MAXUNIT = 69;

  // Index Typed factory methods
  /** @param unit One of the constant units of MassTransferRate **/
  public static final MassTransferRate newMassTransferRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new MassTransferRate(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit One of the constant units of MassTransferRate **/
  public static final MassTransferRate newMassTransferRate(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new MassTransferRate((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Index Typed factory methods
  /** @param unit1 One of the constant units of Mass
   *  @param unit2 One of the constant units of Duration
   **/
  public static final MassTransferRate newMassTransferRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Mass.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new MassTransferRate(v*Mass.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Mass to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public static final MassTransferRate newMassTransferRate(Mass num, Duration den) {
    return new MassTransferRate(num.getValue(0)/den.getValue(0));
  }

  /** @param unit1 One of the constant units of Mass
   *  @param unit2 One of the constant units of Duration
   **/
  public static final MassTransferRate newMassTransferRate(String s, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Mass.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new MassTransferRate((Double.valueOf(s).doubleValue())*Mass.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newMassTransferRate(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newMassTransferRate(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof MassTransferRate)) throw new IllegalArgumentException();
    return new MassTransferRate(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof MassTransferRate)) throw new IllegalArgumentException();
    return new MassTransferRate(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new MassTransferRate(theValue*scale,0);
  }

  public final Measure negate() {
    return newMassTransferRate(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newMassTransferRate(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new MassTransferRate(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new MassTransferRate(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    throw new IllegalArgumentException("Call divideRate instead to divide one Rate by another.");
  }

  public final double divideRate(Rate toRate) {
    if (toRate.getCanonicalNumerator().getClass() !=  getCanonicalNumerator().getClass() ||
    toRate.getCanonicalDenominator().getClass() !=  getCanonicalDenominator().getClass()) {
      throw new IllegalArgumentException("Expecting a MassTransferRate" + 
      ", got a " + toRate.getCanonicalNumerator().getClass() + "/" + toRate.getCanonicalDenominator().getClass());
    }
    return theValue/toRate.getNativeValue();
  }

  // Unit-based Reader methods
  public double getGramsPerSecond() {
    return (theValue*(1.0d/1.0d));
  }
  public double getGramsPerMinute() {
    return (theValue*(1.0d/(1.0d/60)));
  }
  public double getGramsPerHour() {
    return (theValue*(1.0d/(1.0d/3600)));
  }
  public double getGramsPerDay() {
    return (theValue*(1.0d/(1.0d/86400)));
  }
  public double getGramsPerWeek() {
    return (theValue*(1.0d/(1.0d/604800)));
  }
  public double getGramsPerMillisecond() {
    return (theValue*(1.0d/1000));
  }
  public double getGramsPerKilosecond() {
    return (theValue*(1.0d/(1.0d/1000)));
  }
  public double getGramsPerMonth() {
    return (theValue*(1.0d/(1.0d/2629743.8)));
  }
  public double getGramsPerYear() {
    return (theValue*(1.0d/(1.0d/31556926)));
  }
  public double getGramsPerFortnight() {
    return (theValue*(1.0d/(1.0d/1209600)));
  }
  public double getKilogramsPerSecond() {
    return (theValue*((1.0d/1000)/1.0d));
  }
  public double getKilogramsPerMinute() {
    return (theValue*((1.0d/1000)/(1.0d/60)));
  }
  public double getKilogramsPerHour() {
    return (theValue*((1.0d/1000)/(1.0d/3600)));
  }
  public double getKilogramsPerDay() {
    return (theValue*((1.0d/1000)/(1.0d/86400)));
  }
  public double getKilogramsPerWeek() {
    return (theValue*((1.0d/1000)/(1.0d/604800)));
  }
  public double getKilogramsPerMillisecond() {
    return (theValue*((1.0d/1000)/1000));
  }
  public double getKilogramsPerKilosecond() {
    return (theValue*((1.0d/1000)/(1.0d/1000)));
  }
  public double getKilogramsPerMonth() {
    return (theValue*((1.0d/1000)/(1.0d/2629743.8)));
  }
  public double getKilogramsPerYear() {
    return (theValue*((1.0d/1000)/(1.0d/31556926)));
  }
  public double getKilogramsPerFortnight() {
    return (theValue*((1.0d/1000)/(1.0d/1209600)));
  }
  public double getOuncesPerSecond() {
    return (theValue*(0.035273962/1.0d));
  }
  public double getOuncesPerMinute() {
    return (theValue*(0.035273962/(1.0d/60)));
  }
  public double getOuncesPerHour() {
    return (theValue*(0.035273962/(1.0d/3600)));
  }
  public double getOuncesPerDay() {
    return (theValue*(0.035273962/(1.0d/86400)));
  }
  public double getOuncesPerWeek() {
    return (theValue*(0.035273962/(1.0d/604800)));
  }
  public double getOuncesPerMillisecond() {
    return (theValue*(0.035273962/1000));
  }
  public double getOuncesPerKilosecond() {
    return (theValue*(0.035273962/(1.0d/1000)));
  }
  public double getOuncesPerMonth() {
    return (theValue*(0.035273962/(1.0d/2629743.8)));
  }
  public double getOuncesPerYear() {
    return (theValue*(0.035273962/(1.0d/31556926)));
  }
  public double getOuncesPerFortnight() {
    return (theValue*(0.035273962/(1.0d/1209600)));
  }
  public double getPoundsPerSecond() {
    return (theValue*(0.0022046226/1.0d));
  }
  public double getPoundsPerMinute() {
    return (theValue*(0.0022046226/(1.0d/60)));
  }
  public double getPoundsPerHour() {
    return (theValue*(0.0022046226/(1.0d/3600)));
  }
  public double getPoundsPerDay() {
    return (theValue*(0.0022046226/(1.0d/86400)));
  }
  public double getPoundsPerWeek() {
    return (theValue*(0.0022046226/(1.0d/604800)));
  }
  public double getPoundsPerMillisecond() {
    return (theValue*(0.0022046226/1000));
  }
  public double getPoundsPerKilosecond() {
    return (theValue*(0.0022046226/(1.0d/1000)));
  }
  public double getPoundsPerMonth() {
    return (theValue*(0.0022046226/(1.0d/2629743.8)));
  }
  public double getPoundsPerYear() {
    return (theValue*(0.0022046226/(1.0d/31556926)));
  }
  public double getPoundsPerFortnight() {
    return (theValue*(0.0022046226/(1.0d/1209600)));
  }
  public double getTonsPerSecond() {
    return (theValue*((1.0d/907184.74)/1.0d));
  }
  public double getTonsPerMinute() {
    return (theValue*((1.0d/907184.74)/(1.0d/60)));
  }
  public double getTonsPerHour() {
    return (theValue*((1.0d/907184.74)/(1.0d/3600)));
  }
  public double getTonsPerDay() {
    return (theValue*((1.0d/907184.74)/(1.0d/86400)));
  }
  public double getTonsPerWeek() {
    return (theValue*((1.0d/907184.74)/(1.0d/604800)));
  }
  public double getTonsPerMillisecond() {
    return (theValue*((1.0d/907184.74)/1000));
  }
  public double getTonsPerKilosecond() {
    return (theValue*((1.0d/907184.74)/(1.0d/1000)));
  }
  public double getTonsPerMonth() {
    return (theValue*((1.0d/907184.74)/(1.0d/2629743.8)));
  }
  public double getTonsPerYear() {
    return (theValue*((1.0d/907184.74)/(1.0d/31556926)));
  }
  public double getTonsPerFortnight() {
    return (theValue*((1.0d/907184.74)/(1.0d/1209600)));
  }
  public double getShortTonsPerSecond() {
    return (theValue*((1.0d/907184.74)/1.0d));
  }
  public double getShortTonsPerMinute() {
    return (theValue*((1.0d/907184.74)/(1.0d/60)));
  }
  public double getShortTonsPerHour() {
    return (theValue*((1.0d/907184.74)/(1.0d/3600)));
  }
  public double getShortTonsPerDay() {
    return (theValue*((1.0d/907184.74)/(1.0d/86400)));
  }
  public double getShortTonsPerWeek() {
    return (theValue*((1.0d/907184.74)/(1.0d/604800)));
  }
  public double getShortTonsPerMillisecond() {
    return (theValue*((1.0d/907184.74)/1000));
  }
  public double getShortTonsPerKilosecond() {
    return (theValue*((1.0d/907184.74)/(1.0d/1000)));
  }
  public double getShortTonsPerMonth() {
    return (theValue*((1.0d/907184.74)/(1.0d/2629743.8)));
  }
  public double getShortTonsPerYear() {
    return (theValue*((1.0d/907184.74)/(1.0d/31556926)));
  }
  public double getShortTonsPerFortnight() {
    return (theValue*((1.0d/907184.74)/(1.0d/1209600)));
  }
  public double getLongTonsPerSecond() {
    return (theValue*((1.0d/1016046.9)/1.0d));
  }
  public double getLongTonsPerMinute() {
    return (theValue*((1.0d/1016046.9)/(1.0d/60)));
  }
  public double getLongTonsPerHour() {
    return (theValue*((1.0d/1016046.9)/(1.0d/3600)));
  }
  public double getLongTonsPerDay() {
    return (theValue*((1.0d/1016046.9)/(1.0d/86400)));
  }
  public double getLongTonsPerWeek() {
    return (theValue*((1.0d/1016046.9)/(1.0d/604800)));
  }
  public double getLongTonsPerMillisecond() {
    return (theValue*((1.0d/1016046.9)/1000));
  }
  public double getLongTonsPerKilosecond() {
    return (theValue*((1.0d/1016046.9)/(1.0d/1000)));
  }
  public double getLongTonsPerMonth() {
    return (theValue*((1.0d/1016046.9)/(1.0d/2629743.8)));
  }
  public double getLongTonsPerYear() {
    return (theValue*((1.0d/1016046.9)/(1.0d/31556926)));
  }
  public double getLongTonsPerFortnight() {
    return (theValue*((1.0d/1016046.9)/(1.0d/1209600)));
  }

  /** @param unit One of the constant units of MassTransferRate **/
  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Mass
   *  @param unit2 One of the constant units of Duration
   **/
  public double getValue(int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Mass.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return (theValue*Duration.getConvFactor(unit2)/Mass.getConvFactor(unit1));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof MassTransferRate &&
             theValue == ((MassTransferRate) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "g/s";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // Derivative
  public final Class getNumeratorClass() { return Mass.class; }
  public final Class getDenominatorClass() { return Duration.class; }

  private final static Mass can_num = new Mass(0.0,0);
  public final Measure getCanonicalNumerator() { return can_num; }
  private final static Duration can_den = new Duration(0.0,0);
  public final Measure getCanonicalDenominator() { return can_den; }
  public final Measure computeNumerator(Measure den) {
    if (!(den instanceof Duration)) throw new IllegalArgumentException();
    return new Mass(theValue*den.getValue(0),0);
  }
  public final Measure computeDenominator(Measure num) {
    if (!(num instanceof Mass)) throw new IllegalArgumentException();
    return new Duration(num.getValue(0)/theValue,0);
  }

  // serialization
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(theValue);
  }
  public void readExternal(ObjectInput in) throws IOException {
    theValue = in.readDouble();
  }
}
