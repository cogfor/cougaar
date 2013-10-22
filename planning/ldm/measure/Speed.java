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
/** Immutable implementation of Speed.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Speed extends AbstractMeasure
  implements Externalizable, Derivative, Rate {
  // the value is stored as meters/second
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Speed() {}

  // private constructor
  private Speed(double v) {
    theValue = v;
  }

  /** @param unit One of the constant units of Speed **/
  public Speed(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v/getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Distance
   *  @param unit2 One of the constant units of Duration
   **/
  public Speed(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Distance.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      theValue = v*Distance.getConvFactor(unit1)/Duration.getConvFactor(unit2);
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Distance to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public Speed(Distance num, Duration den) {
    theValue = num.getValue(0)/den.getValue(0);
  }

  /** takes strings of the form "Number unit" **/
  public Speed(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("meterspersecond")) 
      theValue=n/(1.0d/1.0d);
    else if (u.equals("metersperminute")) 
      theValue=n/(1.0d/(1.0d/60));
    else if (u.equals("metersperhour")) 
      theValue=n/(1.0d/(1.0d/3600));
    else if (u.equals("metersperday")) 
      theValue=n/(1.0d/(1.0d/86400));
    else if (u.equals("metersperweek")) 
      theValue=n/(1.0d/(1.0d/604800));
    else if (u.equals("meterspermillisecond")) 
      theValue=n/(1.0d/1000);
    else if (u.equals("metersperkilosecond")) 
      theValue=n/(1.0d/(1.0d/1000));
    else if (u.equals("meterspermonth")) 
      theValue=n/(1.0d/(1.0d/2629743.8));
    else if (u.equals("metersperyear")) 
      theValue=n/(1.0d/(1.0d/31556926));
    else if (u.equals("metersperfortnight")) 
      theValue=n/(1.0d/(1.0d/1209600));
    else if (u.equals("milespersecond")) 
      theValue=n/((1.0d/1609.344)/1.0d);
    else if (u.equals("milesperminute")) 
      theValue=n/((1.0d/1609.344)/(1.0d/60));
    else if (u.equals("milesperhour")) 
      theValue=n/((1.0d/1609.344)/(1.0d/3600));
    else if (u.equals("milesperday")) 
      theValue=n/((1.0d/1609.344)/(1.0d/86400));
    else if (u.equals("milesperweek")) 
      theValue=n/((1.0d/1609.344)/(1.0d/604800));
    else if (u.equals("milespermillisecond")) 
      theValue=n/((1.0d/1609.344)/1000);
    else if (u.equals("milesperkilosecond")) 
      theValue=n/((1.0d/1609.344)/(1.0d/1000));
    else if (u.equals("milespermonth")) 
      theValue=n/((1.0d/1609.344)/(1.0d/2629743.8));
    else if (u.equals("milesperyear")) 
      theValue=n/((1.0d/1609.344)/(1.0d/31556926));
    else if (u.equals("milesperfortnight")) 
      theValue=n/((1.0d/1609.344)/(1.0d/1209600));
    else if (u.equals("nauticalmilespersecond")) 
      theValue=n/((1.0d/1852.0)/1.0d);
    else if (u.equals("nauticalmilesperminute")) 
      theValue=n/((1.0d/1852.0)/(1.0d/60));
    else if (u.equals("nauticalmilesperhour")) 
      theValue=n/((1.0d/1852.0)/(1.0d/3600));
    else if (u.equals("nauticalmilesperday")) 
      theValue=n/((1.0d/1852.0)/(1.0d/86400));
    else if (u.equals("nauticalmilesperweek")) 
      theValue=n/((1.0d/1852.0)/(1.0d/604800));
    else if (u.equals("nauticalmilespermillisecond")) 
      theValue=n/((1.0d/1852.0)/1000);
    else if (u.equals("nauticalmilesperkilosecond")) 
      theValue=n/((1.0d/1852.0)/(1.0d/1000));
    else if (u.equals("nauticalmilespermonth")) 
      theValue=n/((1.0d/1852.0)/(1.0d/2629743.8));
    else if (u.equals("nauticalmilesperyear")) 
      theValue=n/((1.0d/1852.0)/(1.0d/31556926));
    else if (u.equals("nauticalmilesperfortnight")) 
      theValue=n/((1.0d/1852.0)/(1.0d/1209600));
    else if (u.equals("yardspersecond")) 
      theValue=n/((1.0d/0.9414)/1.0d);
    else if (u.equals("yardsperminute")) 
      theValue=n/((1.0d/0.9414)/(1.0d/60));
    else if (u.equals("yardsperhour")) 
      theValue=n/((1.0d/0.9414)/(1.0d/3600));
    else if (u.equals("yardsperday")) 
      theValue=n/((1.0d/0.9414)/(1.0d/86400));
    else if (u.equals("yardsperweek")) 
      theValue=n/((1.0d/0.9414)/(1.0d/604800));
    else if (u.equals("yardspermillisecond")) 
      theValue=n/((1.0d/0.9414)/1000);
    else if (u.equals("yardsperkilosecond")) 
      theValue=n/((1.0d/0.9414)/(1.0d/1000));
    else if (u.equals("yardspermonth")) 
      theValue=n/((1.0d/0.9414)/(1.0d/2629743.8));
    else if (u.equals("yardsperyear")) 
      theValue=n/((1.0d/0.9414)/(1.0d/31556926));
    else if (u.equals("yardsperfortnight")) 
      theValue=n/((1.0d/0.9414)/(1.0d/1209600));
    else if (u.equals("feetpersecond")) 
      theValue=n/((1.0d/0.3048)/1.0d);
    else if (u.equals("feetperminute")) 
      theValue=n/((1.0d/0.3048)/(1.0d/60));
    else if (u.equals("feetperhour")) 
      theValue=n/((1.0d/0.3048)/(1.0d/3600));
    else if (u.equals("feetperday")) 
      theValue=n/((1.0d/0.3048)/(1.0d/86400));
    else if (u.equals("feetperweek")) 
      theValue=n/((1.0d/0.3048)/(1.0d/604800));
    else if (u.equals("feetpermillisecond")) 
      theValue=n/((1.0d/0.3048)/1000);
    else if (u.equals("feetperkilosecond")) 
      theValue=n/((1.0d/0.3048)/(1.0d/1000));
    else if (u.equals("feetpermonth")) 
      theValue=n/((1.0d/0.3048)/(1.0d/2629743.8));
    else if (u.equals("feetperyear")) 
      theValue=n/((1.0d/0.3048)/(1.0d/31556926));
    else if (u.equals("feetperfortnight")) 
      theValue=n/((1.0d/0.3048)/(1.0d/1209600));
    else if (u.equals("inchespersecond")) 
      theValue=n/((1.0d/0.0254)/1.0d);
    else if (u.equals("inchesperminute")) 
      theValue=n/((1.0d/0.0254)/(1.0d/60));
    else if (u.equals("inchesperhour")) 
      theValue=n/((1.0d/0.0254)/(1.0d/3600));
    else if (u.equals("inchesperday")) 
      theValue=n/((1.0d/0.0254)/(1.0d/86400));
    else if (u.equals("inchesperweek")) 
      theValue=n/((1.0d/0.0254)/(1.0d/604800));
    else if (u.equals("inchespermillisecond")) 
      theValue=n/((1.0d/0.0254)/1000);
    else if (u.equals("inchesperkilosecond")) 
      theValue=n/((1.0d/0.0254)/(1.0d/1000));
    else if (u.equals("inchespermonth")) 
      theValue=n/((1.0d/0.0254)/(1.0d/2629743.8));
    else if (u.equals("inchesperyear")) 
      theValue=n/((1.0d/0.0254)/(1.0d/31556926));
    else if (u.equals("inchesperfortnight")) 
      theValue=n/((1.0d/0.0254)/(1.0d/1209600));
    else if (u.equals("kilometerspersecond")) 
      theValue=n/((1.0d/1000.0)/1.0d);
    else if (u.equals("kilometersperminute")) 
      theValue=n/((1.0d/1000.0)/(1.0d/60));
    else if (u.equals("kilometersperhour")) 
      theValue=n/((1.0d/1000.0)/(1.0d/3600));
    else if (u.equals("kilometersperday")) 
      theValue=n/((1.0d/1000.0)/(1.0d/86400));
    else if (u.equals("kilometersperweek")) 
      theValue=n/((1.0d/1000.0)/(1.0d/604800));
    else if (u.equals("kilometerspermillisecond")) 
      theValue=n/((1.0d/1000.0)/1000);
    else if (u.equals("kilometersperkilosecond")) 
      theValue=n/((1.0d/1000.0)/(1.0d/1000));
    else if (u.equals("kilometerspermonth")) 
      theValue=n/((1.0d/1000.0)/(1.0d/2629743.8));
    else if (u.equals("kilometersperyear")) 
      theValue=n/((1.0d/1000.0)/(1.0d/31556926));
    else if (u.equals("kilometersperfortnight")) 
      theValue=n/((1.0d/1000.0)/(1.0d/1209600));
    else if (u.equals("centimeterspersecond")) 
      theValue=n/(100/1.0d);
    else if (u.equals("centimetersperminute")) 
      theValue=n/(100/(1.0d/60));
    else if (u.equals("centimetersperhour")) 
      theValue=n/(100/(1.0d/3600));
    else if (u.equals("centimetersperday")) 
      theValue=n/(100/(1.0d/86400));
    else if (u.equals("centimetersperweek")) 
      theValue=n/(100/(1.0d/604800));
    else if (u.equals("centimeterspermillisecond")) 
      theValue=n/(100/1000);
    else if (u.equals("centimetersperkilosecond")) 
      theValue=n/(100/(1.0d/1000));
    else if (u.equals("centimeterspermonth")) 
      theValue=n/(100/(1.0d/2629743.8));
    else if (u.equals("centimetersperyear")) 
      theValue=n/(100/(1.0d/31556926));
    else if (u.equals("centimetersperfortnight")) 
      theValue=n/(100/(1.0d/1209600));
    else if (u.equals("millimeterspersecond")) 
      theValue=n/(1000/1.0d);
    else if (u.equals("millimetersperminute")) 
      theValue=n/(1000/(1.0d/60));
    else if (u.equals("millimetersperhour")) 
      theValue=n/(1000/(1.0d/3600));
    else if (u.equals("millimetersperday")) 
      theValue=n/(1000/(1.0d/86400));
    else if (u.equals("millimetersperweek")) 
      theValue=n/(1000/(1.0d/604800));
    else if (u.equals("millimeterspermillisecond")) 
      theValue=n/(1000/1000);
    else if (u.equals("millimetersperkilosecond")) 
      theValue=n/(1000/(1.0d/1000));
    else if (u.equals("millimeterspermonth")) 
      theValue=n/(1000/(1.0d/2629743.8));
    else if (u.equals("millimetersperyear")) 
      theValue=n/(1000/(1.0d/31556926));
    else if (u.equals("millimetersperfortnight")) 
      theValue=n/(1000/(1.0d/1209600));
    else if (u.equals("furlongspersecond")) 
      theValue=n/((1.0d/201.168)/1.0d);
    else if (u.equals("furlongsperminute")) 
      theValue=n/((1.0d/201.168)/(1.0d/60));
    else if (u.equals("furlongsperhour")) 
      theValue=n/((1.0d/201.168)/(1.0d/3600));
    else if (u.equals("furlongsperday")) 
      theValue=n/((1.0d/201.168)/(1.0d/86400));
    else if (u.equals("furlongsperweek")) 
      theValue=n/((1.0d/201.168)/(1.0d/604800));
    else if (u.equals("furlongspermillisecond")) 
      theValue=n/((1.0d/201.168)/1000);
    else if (u.equals("furlongsperkilosecond")) 
      theValue=n/((1.0d/201.168)/(1.0d/1000));
    else if (u.equals("furlongspermonth")) 
      theValue=n/((1.0d/201.168)/(1.0d/2629743.8));
    else if (u.equals("furlongsperyear")) 
      theValue=n/((1.0d/201.168)/(1.0d/31556926));
    else if (u.equals("furlongsperfortnight")) 
      theValue=n/((1.0d/201.168)/(1.0d/1209600));
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Speed newMetersPerSecond(double v) {
    return new Speed(v*(1.0d/(1.0d/1.0d)));
  }
  public static final Speed newMetersPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1.0d)));
  }
  public static final Speed newMetersPerMinute(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final Speed newMetersPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final Speed newMetersPerHour(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final Speed newMetersPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final Speed newMetersPerDay(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final Speed newMetersPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final Speed newMetersPerWeek(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final Speed newMetersPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final Speed newMetersPerMillisecond(double v) {
    return new Speed(v*(1.0d/(1.0d/1000)));
  }
  public static final Speed newMetersPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1000)));
  }
  public static final Speed newMetersPerKilosecond(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final Speed newMetersPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final Speed newMetersPerMonth(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final Speed newMetersPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final Speed newMetersPerYear(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final Speed newMetersPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final Speed newMetersPerFortnight(double v) {
    return new Speed(v*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final Speed newMetersPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final Speed newMilesPerSecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/1.0d)));
  }
  public static final Speed newMilesPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/1.0d)));
  }
  public static final Speed newMilesPerMinute(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/60))));
  }
  public static final Speed newMilesPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/60))));
  }
  public static final Speed newMilesPerHour(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/3600))));
  }
  public static final Speed newMilesPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/3600))));
  }
  public static final Speed newMilesPerDay(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/86400))));
  }
  public static final Speed newMilesPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/86400))));
  }
  public static final Speed newMilesPerWeek(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/604800))));
  }
  public static final Speed newMilesPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/604800))));
  }
  public static final Speed newMilesPerMillisecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/1000)));
  }
  public static final Speed newMilesPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/1000)));
  }
  public static final Speed newMilesPerKilosecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/1000))));
  }
  public static final Speed newMilesPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/1000))));
  }
  public static final Speed newMilesPerMonth(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/2629743.8))));
  }
  public static final Speed newMilesPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/2629743.8))));
  }
  public static final Speed newMilesPerYear(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/31556926))));
  }
  public static final Speed newMilesPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/31556926))));
  }
  public static final Speed newMilesPerFortnight(double v) {
    return new Speed(v*(1.0d/((1.0d/1609.344)/(1.0d/1209600))));
  }
  public static final Speed newMilesPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1609.344)/(1.0d/1209600))));
  }
  public static final Speed newNauticalMilesPerSecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/1.0d)));
  }
  public static final Speed newNauticalMilesPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/1.0d)));
  }
  public static final Speed newNauticalMilesPerMinute(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/60))));
  }
  public static final Speed newNauticalMilesPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/60))));
  }
  public static final Speed newNauticalMilesPerHour(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/3600))));
  }
  public static final Speed newNauticalMilesPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/3600))));
  }
  public static final Speed newNauticalMilesPerDay(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/86400))));
  }
  public static final Speed newNauticalMilesPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/86400))));
  }
  public static final Speed newNauticalMilesPerWeek(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/604800))));
  }
  public static final Speed newNauticalMilesPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/604800))));
  }
  public static final Speed newNauticalMilesPerMillisecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/1000)));
  }
  public static final Speed newNauticalMilesPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/1000)));
  }
  public static final Speed newNauticalMilesPerKilosecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/1000))));
  }
  public static final Speed newNauticalMilesPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/1000))));
  }
  public static final Speed newNauticalMilesPerMonth(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/2629743.8))));
  }
  public static final Speed newNauticalMilesPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/2629743.8))));
  }
  public static final Speed newNauticalMilesPerYear(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/31556926))));
  }
  public static final Speed newNauticalMilesPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/31556926))));
  }
  public static final Speed newNauticalMilesPerFortnight(double v) {
    return new Speed(v*(1.0d/((1.0d/1852.0)/(1.0d/1209600))));
  }
  public static final Speed newNauticalMilesPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1852.0)/(1.0d/1209600))));
  }
  public static final Speed newYardsPerSecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/1.0d)));
  }
  public static final Speed newYardsPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/1.0d)));
  }
  public static final Speed newYardsPerMinute(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/60))));
  }
  public static final Speed newYardsPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/60))));
  }
  public static final Speed newYardsPerHour(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/3600))));
  }
  public static final Speed newYardsPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/3600))));
  }
  public static final Speed newYardsPerDay(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/86400))));
  }
  public static final Speed newYardsPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/86400))));
  }
  public static final Speed newYardsPerWeek(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/604800))));
  }
  public static final Speed newYardsPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/604800))));
  }
  public static final Speed newYardsPerMillisecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/1000)));
  }
  public static final Speed newYardsPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/1000)));
  }
  public static final Speed newYardsPerKilosecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/1000))));
  }
  public static final Speed newYardsPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/1000))));
  }
  public static final Speed newYardsPerMonth(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/2629743.8))));
  }
  public static final Speed newYardsPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/2629743.8))));
  }
  public static final Speed newYardsPerYear(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/31556926))));
  }
  public static final Speed newYardsPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/31556926))));
  }
  public static final Speed newYardsPerFortnight(double v) {
    return new Speed(v*(1.0d/((1.0d/0.9414)/(1.0d/1209600))));
  }
  public static final Speed newYardsPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.9414)/(1.0d/1209600))));
  }
  public static final Speed newFeetPerSecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/1.0d)));
  }
  public static final Speed newFeetPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/1.0d)));
  }
  public static final Speed newFeetPerMinute(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/60))));
  }
  public static final Speed newFeetPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/60))));
  }
  public static final Speed newFeetPerHour(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/3600))));
  }
  public static final Speed newFeetPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/3600))));
  }
  public static final Speed newFeetPerDay(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/86400))));
  }
  public static final Speed newFeetPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/86400))));
  }
  public static final Speed newFeetPerWeek(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/604800))));
  }
  public static final Speed newFeetPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/604800))));
  }
  public static final Speed newFeetPerMillisecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/1000)));
  }
  public static final Speed newFeetPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/1000)));
  }
  public static final Speed newFeetPerKilosecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/1000))));
  }
  public static final Speed newFeetPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/1000))));
  }
  public static final Speed newFeetPerMonth(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/2629743.8))));
  }
  public static final Speed newFeetPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/2629743.8))));
  }
  public static final Speed newFeetPerYear(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/31556926))));
  }
  public static final Speed newFeetPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/31556926))));
  }
  public static final Speed newFeetPerFortnight(double v) {
    return new Speed(v*(1.0d/((1.0d/0.3048)/(1.0d/1209600))));
  }
  public static final Speed newFeetPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.3048)/(1.0d/1209600))));
  }
  public static final Speed newInchesPerSecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/1.0d)));
  }
  public static final Speed newInchesPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/1.0d)));
  }
  public static final Speed newInchesPerMinute(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/60))));
  }
  public static final Speed newInchesPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/60))));
  }
  public static final Speed newInchesPerHour(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/3600))));
  }
  public static final Speed newInchesPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/3600))));
  }
  public static final Speed newInchesPerDay(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/86400))));
  }
  public static final Speed newInchesPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/86400))));
  }
  public static final Speed newInchesPerWeek(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/604800))));
  }
  public static final Speed newInchesPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/604800))));
  }
  public static final Speed newInchesPerMillisecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/1000)));
  }
  public static final Speed newInchesPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/1000)));
  }
  public static final Speed newInchesPerKilosecond(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/1000))));
  }
  public static final Speed newInchesPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/1000))));
  }
  public static final Speed newInchesPerMonth(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/2629743.8))));
  }
  public static final Speed newInchesPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/2629743.8))));
  }
  public static final Speed newInchesPerYear(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/31556926))));
  }
  public static final Speed newInchesPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/31556926))));
  }
  public static final Speed newInchesPerFortnight(double v) {
    return new Speed(v*(1.0d/((1.0d/0.0254)/(1.0d/1209600))));
  }
  public static final Speed newInchesPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/0.0254)/(1.0d/1209600))));
  }
  public static final Speed newKilometersPerSecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/1.0d)));
  }
  public static final Speed newKilometersPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/1.0d)));
  }
  public static final Speed newKilometersPerMinute(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/60))));
  }
  public static final Speed newKilometersPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/60))));
  }
  public static final Speed newKilometersPerHour(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/3600))));
  }
  public static final Speed newKilometersPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/3600))));
  }
  public static final Speed newKilometersPerDay(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/86400))));
  }
  public static final Speed newKilometersPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/86400))));
  }
  public static final Speed newKilometersPerWeek(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/604800))));
  }
  public static final Speed newKilometersPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/604800))));
  }
  public static final Speed newKilometersPerMillisecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/1000)));
  }
  public static final Speed newKilometersPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/1000)));
  }
  public static final Speed newKilometersPerKilosecond(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/1000))));
  }
  public static final Speed newKilometersPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/1000))));
  }
  public static final Speed newKilometersPerMonth(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/2629743.8))));
  }
  public static final Speed newKilometersPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/2629743.8))));
  }
  public static final Speed newKilometersPerYear(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/31556926))));
  }
  public static final Speed newKilometersPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/31556926))));
  }
  public static final Speed newKilometersPerFortnight(double v) {
    return new Speed(v*(1.0d/((1.0d/1000.0)/(1.0d/1209600))));
  }
  public static final Speed newKilometersPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000.0)/(1.0d/1209600))));
  }
  public static final Speed newCentimetersPerSecond(double v) {
    return new Speed(v*(1.0d/(100/1.0d)));
  }
  public static final Speed newCentimetersPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/1.0d)));
  }
  public static final Speed newCentimetersPerMinute(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/60))));
  }
  public static final Speed newCentimetersPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/60))));
  }
  public static final Speed newCentimetersPerHour(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/3600))));
  }
  public static final Speed newCentimetersPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/3600))));
  }
  public static final Speed newCentimetersPerDay(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/86400))));
  }
  public static final Speed newCentimetersPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/86400))));
  }
  public static final Speed newCentimetersPerWeek(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/604800))));
  }
  public static final Speed newCentimetersPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/604800))));
  }
  public static final Speed newCentimetersPerMillisecond(double v) {
    return new Speed(v*(1.0d/(100/1000)));
  }
  public static final Speed newCentimetersPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/1000)));
  }
  public static final Speed newCentimetersPerKilosecond(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/1000))));
  }
  public static final Speed newCentimetersPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/1000))));
  }
  public static final Speed newCentimetersPerMonth(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/2629743.8))));
  }
  public static final Speed newCentimetersPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/2629743.8))));
  }
  public static final Speed newCentimetersPerYear(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/31556926))));
  }
  public static final Speed newCentimetersPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/31556926))));
  }
  public static final Speed newCentimetersPerFortnight(double v) {
    return new Speed(v*(1.0d/(100/(1.0d/1209600))));
  }
  public static final Speed newCentimetersPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(100/(1.0d/1209600))));
  }
  public static final Speed newMillimetersPerSecond(double v) {
    return new Speed(v*(1.0d/(1000/1.0d)));
  }
  public static final Speed newMillimetersPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/1.0d)));
  }
  public static final Speed newMillimetersPerMinute(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/60))));
  }
  public static final Speed newMillimetersPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/60))));
  }
  public static final Speed newMillimetersPerHour(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/3600))));
  }
  public static final Speed newMillimetersPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/3600))));
  }
  public static final Speed newMillimetersPerDay(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/86400))));
  }
  public static final Speed newMillimetersPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/86400))));
  }
  public static final Speed newMillimetersPerWeek(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/604800))));
  }
  public static final Speed newMillimetersPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/604800))));
  }
  public static final Speed newMillimetersPerMillisecond(double v) {
    return new Speed(v*(1.0d/(1000/1000)));
  }
  public static final Speed newMillimetersPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/1000)));
  }
  public static final Speed newMillimetersPerKilosecond(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/1000))));
  }
  public static final Speed newMillimetersPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/1000))));
  }
  public static final Speed newMillimetersPerMonth(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/2629743.8))));
  }
  public static final Speed newMillimetersPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/2629743.8))));
  }
  public static final Speed newMillimetersPerYear(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/31556926))));
  }
  public static final Speed newMillimetersPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/31556926))));
  }
  public static final Speed newMillimetersPerFortnight(double v) {
    return new Speed(v*(1.0d/(1000/(1.0d/1209600))));
  }
  public static final Speed newMillimetersPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/1209600))));
  }
  public static final Speed newFurlongsPerSecond(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/1.0d)));
  }
  public static final Speed newFurlongsPerSecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/1.0d)));
  }
  public static final Speed newFurlongsPerMinute(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/60))));
  }
  public static final Speed newFurlongsPerMinute(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/60))));
  }
  public static final Speed newFurlongsPerHour(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/3600))));
  }
  public static final Speed newFurlongsPerHour(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/3600))));
  }
  public static final Speed newFurlongsPerDay(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/86400))));
  }
  public static final Speed newFurlongsPerDay(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/86400))));
  }
  public static final Speed newFurlongsPerWeek(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/604800))));
  }
  public static final Speed newFurlongsPerWeek(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/604800))));
  }
  public static final Speed newFurlongsPerMillisecond(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/1000)));
  }
  public static final Speed newFurlongsPerMillisecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/1000)));
  }
  public static final Speed newFurlongsPerKilosecond(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/1000))));
  }
  public static final Speed newFurlongsPerKilosecond(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/1000))));
  }
  public static final Speed newFurlongsPerMonth(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/2629743.8))));
  }
  public static final Speed newFurlongsPerMonth(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/2629743.8))));
  }
  public static final Speed newFurlongsPerYear(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/31556926))));
  }
  public static final Speed newFurlongsPerYear(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/31556926))));
  }
  public static final Speed newFurlongsPerFortnight(double v) {
    return new Speed(v*(1.0d/((1.0d/201.168)/(1.0d/1209600))));
  }
  public static final Speed newFurlongsPerFortnight(String s) {
    return new Speed((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/201.168)/(1.0d/1209600))));
  }


  public int getCommonUnit() {
    return 12;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "meters/second",
    "meters/minute",
    "meters/hour",
    "meters/day",
    "meters/week",
    "meters/millisecond",
    "meters/kilosecond",
    "meters/month",
    "meters/year",
    "meters/fortnight",
    "miles/second",
    "miles/minute",
    "miles/hour",
    "miles/day",
    "miles/week",
    "miles/millisecond",
    "miles/kilosecond",
    "miles/month",
    "miles/year",
    "miles/fortnight",
    "nautical_miles/second",
    "nautical_miles/minute",
    "nautical_miles/hour",
    "nautical_miles/day",
    "nautical_miles/week",
    "nautical_miles/millisecond",
    "nautical_miles/kilosecond",
    "nautical_miles/month",
    "nautical_miles/year",
    "nautical_miles/fortnight",
    "yards/second",
    "yards/minute",
    "yards/hour",
    "yards/day",
    "yards/week",
    "yards/millisecond",
    "yards/kilosecond",
    "yards/month",
    "yards/year",
    "yards/fortnight",
    "feet/second",
    "feet/minute",
    "feet/hour",
    "feet/day",
    "feet/week",
    "feet/millisecond",
    "feet/kilosecond",
    "feet/month",
    "feet/year",
    "feet/fortnight",
    "inches/second",
    "inches/minute",
    "inches/hour",
    "inches/day",
    "inches/week",
    "inches/millisecond",
    "inches/kilosecond",
    "inches/month",
    "inches/year",
    "inches/fortnight",
    "kilometers/second",
    "kilometers/minute",
    "kilometers/hour",
    "kilometers/day",
    "kilometers/week",
    "kilometers/millisecond",
    "kilometers/kilosecond",
    "kilometers/month",
    "kilometers/year",
    "kilometers/fortnight",
    "centimeters/second",
    "centimeters/minute",
    "centimeters/hour",
    "centimeters/day",
    "centimeters/week",
    "centimeters/millisecond",
    "centimeters/kilosecond",
    "centimeters/month",
    "centimeters/year",
    "centimeters/fortnight",
    "millimeters/second",
    "millimeters/minute",
    "millimeters/hour",
    "millimeters/day",
    "millimeters/week",
    "millimeters/millisecond",
    "millimeters/kilosecond",
    "millimeters/month",
    "millimeters/year",
    "millimeters/fortnight",
    "furlongs/second",
    "furlongs/minute",
    "furlongs/hour",
    "furlongs/day",
    "furlongs/week",
    "furlongs/millisecond",
    "furlongs/kilosecond",
    "furlongs/month",
    "furlongs/year",
    "furlongs/fortnight",
  };

  /** @param unit One of the constant units of Speed **/
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
    ((1.0d/1609.344)/1.0d),
    ((1.0d/1609.344)/(1.0d/60)),
    ((1.0d/1609.344)/(1.0d/3600)),
    ((1.0d/1609.344)/(1.0d/86400)),
    ((1.0d/1609.344)/(1.0d/604800)),
    ((1.0d/1609.344)/1000),
    ((1.0d/1609.344)/(1.0d/1000)),
    ((1.0d/1609.344)/(1.0d/2629743.8)),
    ((1.0d/1609.344)/(1.0d/31556926)),
    ((1.0d/1609.344)/(1.0d/1209600)),
    ((1.0d/1852.0)/1.0d),
    ((1.0d/1852.0)/(1.0d/60)),
    ((1.0d/1852.0)/(1.0d/3600)),
    ((1.0d/1852.0)/(1.0d/86400)),
    ((1.0d/1852.0)/(1.0d/604800)),
    ((1.0d/1852.0)/1000),
    ((1.0d/1852.0)/(1.0d/1000)),
    ((1.0d/1852.0)/(1.0d/2629743.8)),
    ((1.0d/1852.0)/(1.0d/31556926)),
    ((1.0d/1852.0)/(1.0d/1209600)),
    ((1.0d/0.9414)/1.0d),
    ((1.0d/0.9414)/(1.0d/60)),
    ((1.0d/0.9414)/(1.0d/3600)),
    ((1.0d/0.9414)/(1.0d/86400)),
    ((1.0d/0.9414)/(1.0d/604800)),
    ((1.0d/0.9414)/1000),
    ((1.0d/0.9414)/(1.0d/1000)),
    ((1.0d/0.9414)/(1.0d/2629743.8)),
    ((1.0d/0.9414)/(1.0d/31556926)),
    ((1.0d/0.9414)/(1.0d/1209600)),
    ((1.0d/0.3048)/1.0d),
    ((1.0d/0.3048)/(1.0d/60)),
    ((1.0d/0.3048)/(1.0d/3600)),
    ((1.0d/0.3048)/(1.0d/86400)),
    ((1.0d/0.3048)/(1.0d/604800)),
    ((1.0d/0.3048)/1000),
    ((1.0d/0.3048)/(1.0d/1000)),
    ((1.0d/0.3048)/(1.0d/2629743.8)),
    ((1.0d/0.3048)/(1.0d/31556926)),
    ((1.0d/0.3048)/(1.0d/1209600)),
    ((1.0d/0.0254)/1.0d),
    ((1.0d/0.0254)/(1.0d/60)),
    ((1.0d/0.0254)/(1.0d/3600)),
    ((1.0d/0.0254)/(1.0d/86400)),
    ((1.0d/0.0254)/(1.0d/604800)),
    ((1.0d/0.0254)/1000),
    ((1.0d/0.0254)/(1.0d/1000)),
    ((1.0d/0.0254)/(1.0d/2629743.8)),
    ((1.0d/0.0254)/(1.0d/31556926)),
    ((1.0d/0.0254)/(1.0d/1209600)),
    ((1.0d/1000.0)/1.0d),
    ((1.0d/1000.0)/(1.0d/60)),
    ((1.0d/1000.0)/(1.0d/3600)),
    ((1.0d/1000.0)/(1.0d/86400)),
    ((1.0d/1000.0)/(1.0d/604800)),
    ((1.0d/1000.0)/1000),
    ((1.0d/1000.0)/(1.0d/1000)),
    ((1.0d/1000.0)/(1.0d/2629743.8)),
    ((1.0d/1000.0)/(1.0d/31556926)),
    ((1.0d/1000.0)/(1.0d/1209600)),
    (100/1.0d),
    (100/(1.0d/60)),
    (100/(1.0d/3600)),
    (100/(1.0d/86400)),
    (100/(1.0d/604800)),
    (100/1000),
    (100/(1.0d/1000)),
    (100/(1.0d/2629743.8)),
    (100/(1.0d/31556926)),
    (100/(1.0d/1209600)),
    (1000/1.0d),
    (1000/(1.0d/60)),
    (1000/(1.0d/3600)),
    (1000/(1.0d/86400)),
    (1000/(1.0d/604800)),
    (1000/1000),
    (1000/(1.0d/1000)),
    (1000/(1.0d/2629743.8)),
    (1000/(1.0d/31556926)),
    (1000/(1.0d/1209600)),
    ((1.0d/201.168)/1.0d),
    ((1.0d/201.168)/(1.0d/60)),
    ((1.0d/201.168)/(1.0d/3600)),
    ((1.0d/201.168)/(1.0d/86400)),
    ((1.0d/201.168)/(1.0d/604800)),
    ((1.0d/201.168)/1000),
    ((1.0d/201.168)/(1.0d/1000)),
    ((1.0d/201.168)/(1.0d/2629743.8)),
    ((1.0d/201.168)/(1.0d/31556926)),
    ((1.0d/201.168)/(1.0d/1209600)),
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int METERS_PER_SECOND = 0;
  public static final int METERS_PER_MINUTE = 1;
  public static final int METERS_PER_HOUR = 2;
  public static final int METERS_PER_DAY = 3;
  public static final int METERS_PER_WEEK = 4;
  public static final int METERS_PER_MILLISECOND = 5;
  public static final int METERS_PER_KILOSECOND = 6;
  public static final int METERS_PER_MONTH = 7;
  public static final int METERS_PER_YEAR = 8;
  public static final int METERS_PER_FORTNIGHT = 9;
  public static final int MILES_PER_SECOND = 10;
  public static final int MILES_PER_MINUTE = 11;
  public static final int MILES_PER_HOUR = 12;
  public static final int MILES_PER_DAY = 13;
  public static final int MILES_PER_WEEK = 14;
  public static final int MILES_PER_MILLISECOND = 15;
  public static final int MILES_PER_KILOSECOND = 16;
  public static final int MILES_PER_MONTH = 17;
  public static final int MILES_PER_YEAR = 18;
  public static final int MILES_PER_FORTNIGHT = 19;
  public static final int NAUTICAL_MILES_PER_SECOND = 20;
  public static final int NAUTICAL_MILES_PER_MINUTE = 21;
  public static final int NAUTICAL_MILES_PER_HOUR = 22;
  public static final int NAUTICAL_MILES_PER_DAY = 23;
  public static final int NAUTICAL_MILES_PER_WEEK = 24;
  public static final int NAUTICAL_MILES_PER_MILLISECOND = 25;
  public static final int NAUTICAL_MILES_PER_KILOSECOND = 26;
  public static final int NAUTICAL_MILES_PER_MONTH = 27;
  public static final int NAUTICAL_MILES_PER_YEAR = 28;
  public static final int NAUTICAL_MILES_PER_FORTNIGHT = 29;
  public static final int YARDS_PER_SECOND = 30;
  public static final int YARDS_PER_MINUTE = 31;
  public static final int YARDS_PER_HOUR = 32;
  public static final int YARDS_PER_DAY = 33;
  public static final int YARDS_PER_WEEK = 34;
  public static final int YARDS_PER_MILLISECOND = 35;
  public static final int YARDS_PER_KILOSECOND = 36;
  public static final int YARDS_PER_MONTH = 37;
  public static final int YARDS_PER_YEAR = 38;
  public static final int YARDS_PER_FORTNIGHT = 39;
  public static final int FEET_PER_SECOND = 40;
  public static final int FEET_PER_MINUTE = 41;
  public static final int FEET_PER_HOUR = 42;
  public static final int FEET_PER_DAY = 43;
  public static final int FEET_PER_WEEK = 44;
  public static final int FEET_PER_MILLISECOND = 45;
  public static final int FEET_PER_KILOSECOND = 46;
  public static final int FEET_PER_MONTH = 47;
  public static final int FEET_PER_YEAR = 48;
  public static final int FEET_PER_FORTNIGHT = 49;
  public static final int INCHES_PER_SECOND = 50;
  public static final int INCHES_PER_MINUTE = 51;
  public static final int INCHES_PER_HOUR = 52;
  public static final int INCHES_PER_DAY = 53;
  public static final int INCHES_PER_WEEK = 54;
  public static final int INCHES_PER_MILLISECOND = 55;
  public static final int INCHES_PER_KILOSECOND = 56;
  public static final int INCHES_PER_MONTH = 57;
  public static final int INCHES_PER_YEAR = 58;
  public static final int INCHES_PER_FORTNIGHT = 59;
  public static final int KILOMETERS_PER_SECOND = 60;
  public static final int KILOMETERS_PER_MINUTE = 61;
  public static final int KILOMETERS_PER_HOUR = 62;
  public static final int KILOMETERS_PER_DAY = 63;
  public static final int KILOMETERS_PER_WEEK = 64;
  public static final int KILOMETERS_PER_MILLISECOND = 65;
  public static final int KILOMETERS_PER_KILOSECOND = 66;
  public static final int KILOMETERS_PER_MONTH = 67;
  public static final int KILOMETERS_PER_YEAR = 68;
  public static final int KILOMETERS_PER_FORTNIGHT = 69;
  public static final int CENTIMETERS_PER_SECOND = 70;
  public static final int CENTIMETERS_PER_MINUTE = 71;
  public static final int CENTIMETERS_PER_HOUR = 72;
  public static final int CENTIMETERS_PER_DAY = 73;
  public static final int CENTIMETERS_PER_WEEK = 74;
  public static final int CENTIMETERS_PER_MILLISECOND = 75;
  public static final int CENTIMETERS_PER_KILOSECOND = 76;
  public static final int CENTIMETERS_PER_MONTH = 77;
  public static final int CENTIMETERS_PER_YEAR = 78;
  public static final int CENTIMETERS_PER_FORTNIGHT = 79;
  public static final int MILLIMETERS_PER_SECOND = 80;
  public static final int MILLIMETERS_PER_MINUTE = 81;
  public static final int MILLIMETERS_PER_HOUR = 82;
  public static final int MILLIMETERS_PER_DAY = 83;
  public static final int MILLIMETERS_PER_WEEK = 84;
  public static final int MILLIMETERS_PER_MILLISECOND = 85;
  public static final int MILLIMETERS_PER_KILOSECOND = 86;
  public static final int MILLIMETERS_PER_MONTH = 87;
  public static final int MILLIMETERS_PER_YEAR = 88;
  public static final int MILLIMETERS_PER_FORTNIGHT = 89;
  public static final int FURLONGS_PER_SECOND = 90;
  public static final int FURLONGS_PER_MINUTE = 91;
  public static final int FURLONGS_PER_HOUR = 92;
  public static final int FURLONGS_PER_DAY = 93;
  public static final int FURLONGS_PER_WEEK = 94;
  public static final int FURLONGS_PER_MILLISECOND = 95;
  public static final int FURLONGS_PER_KILOSECOND = 96;
  public static final int FURLONGS_PER_MONTH = 97;
  public static final int FURLONGS_PER_YEAR = 98;
  public static final int FURLONGS_PER_FORTNIGHT = 99;
  static final int MAXUNIT = 99;

  // Index Typed factory methods
  /** @param unit One of the constant units of Speed **/
  public static final Speed newSpeed(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Speed(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit One of the constant units of Speed **/
  public static final Speed newSpeed(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Speed((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Index Typed factory methods
  /** @param unit1 One of the constant units of Distance
   *  @param unit2 One of the constant units of Duration
   **/
  public static final Speed newSpeed(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Distance.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new Speed(v*Distance.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Distance to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public static final Speed newSpeed(Distance num, Duration den) {
    return new Speed(num.getValue(0)/den.getValue(0));
  }

  /** @param unit1 One of the constant units of Distance
   *  @param unit2 One of the constant units of Duration
   **/
  public static final Speed newSpeed(String s, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Distance.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new Speed((Double.valueOf(s).doubleValue())*Distance.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newSpeed(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newSpeed(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Speed)) throw new IllegalArgumentException();
    return new Speed(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Speed)) throw new IllegalArgumentException();
    return new Speed(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Speed(theValue*scale,0);
  }

  public final Measure negate() {
    return newSpeed(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newSpeed(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Speed(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Speed(value, unit);
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
      throw new IllegalArgumentException("Expecting a Speed" + 
      ", got a " + toRate.getCanonicalNumerator().getClass() + "/" + toRate.getCanonicalDenominator().getClass());
    }
    return theValue/toRate.getNativeValue();
  }

  // Unit-based Reader methods
  public double getMetersPerSecond() {
    return (theValue*(1.0d/1.0d));
  }
  public double getMetersPerMinute() {
    return (theValue*(1.0d/(1.0d/60)));
  }
  public double getMetersPerHour() {
    return (theValue*(1.0d/(1.0d/3600)));
  }
  public double getMetersPerDay() {
    return (theValue*(1.0d/(1.0d/86400)));
  }
  public double getMetersPerWeek() {
    return (theValue*(1.0d/(1.0d/604800)));
  }
  public double getMetersPerMillisecond() {
    return (theValue*(1.0d/1000));
  }
  public double getMetersPerKilosecond() {
    return (theValue*(1.0d/(1.0d/1000)));
  }
  public double getMetersPerMonth() {
    return (theValue*(1.0d/(1.0d/2629743.8)));
  }
  public double getMetersPerYear() {
    return (theValue*(1.0d/(1.0d/31556926)));
  }
  public double getMetersPerFortnight() {
    return (theValue*(1.0d/(1.0d/1209600)));
  }
  public double getMilesPerSecond() {
    return (theValue*((1.0d/1609.344)/1.0d));
  }
  public double getMilesPerMinute() {
    return (theValue*((1.0d/1609.344)/(1.0d/60)));
  }
  public double getMilesPerHour() {
    return (theValue*((1.0d/1609.344)/(1.0d/3600)));
  }
  public double getMilesPerDay() {
    return (theValue*((1.0d/1609.344)/(1.0d/86400)));
  }
  public double getMilesPerWeek() {
    return (theValue*((1.0d/1609.344)/(1.0d/604800)));
  }
  public double getMilesPerMillisecond() {
    return (theValue*((1.0d/1609.344)/1000));
  }
  public double getMilesPerKilosecond() {
    return (theValue*((1.0d/1609.344)/(1.0d/1000)));
  }
  public double getMilesPerMonth() {
    return (theValue*((1.0d/1609.344)/(1.0d/2629743.8)));
  }
  public double getMilesPerYear() {
    return (theValue*((1.0d/1609.344)/(1.0d/31556926)));
  }
  public double getMilesPerFortnight() {
    return (theValue*((1.0d/1609.344)/(1.0d/1209600)));
  }
  public double getNauticalMilesPerSecond() {
    return (theValue*((1.0d/1852.0)/1.0d));
  }
  public double getNauticalMilesPerMinute() {
    return (theValue*((1.0d/1852.0)/(1.0d/60)));
  }
  public double getNauticalMilesPerHour() {
    return (theValue*((1.0d/1852.0)/(1.0d/3600)));
  }
  public double getNauticalMilesPerDay() {
    return (theValue*((1.0d/1852.0)/(1.0d/86400)));
  }
  public double getNauticalMilesPerWeek() {
    return (theValue*((1.0d/1852.0)/(1.0d/604800)));
  }
  public double getNauticalMilesPerMillisecond() {
    return (theValue*((1.0d/1852.0)/1000));
  }
  public double getNauticalMilesPerKilosecond() {
    return (theValue*((1.0d/1852.0)/(1.0d/1000)));
  }
  public double getNauticalMilesPerMonth() {
    return (theValue*((1.0d/1852.0)/(1.0d/2629743.8)));
  }
  public double getNauticalMilesPerYear() {
    return (theValue*((1.0d/1852.0)/(1.0d/31556926)));
  }
  public double getNauticalMilesPerFortnight() {
    return (theValue*((1.0d/1852.0)/(1.0d/1209600)));
  }
  public double getYardsPerSecond() {
    return (theValue*((1.0d/0.9414)/1.0d));
  }
  public double getYardsPerMinute() {
    return (theValue*((1.0d/0.9414)/(1.0d/60)));
  }
  public double getYardsPerHour() {
    return (theValue*((1.0d/0.9414)/(1.0d/3600)));
  }
  public double getYardsPerDay() {
    return (theValue*((1.0d/0.9414)/(1.0d/86400)));
  }
  public double getYardsPerWeek() {
    return (theValue*((1.0d/0.9414)/(1.0d/604800)));
  }
  public double getYardsPerMillisecond() {
    return (theValue*((1.0d/0.9414)/1000));
  }
  public double getYardsPerKilosecond() {
    return (theValue*((1.0d/0.9414)/(1.0d/1000)));
  }
  public double getYardsPerMonth() {
    return (theValue*((1.0d/0.9414)/(1.0d/2629743.8)));
  }
  public double getYardsPerYear() {
    return (theValue*((1.0d/0.9414)/(1.0d/31556926)));
  }
  public double getYardsPerFortnight() {
    return (theValue*((1.0d/0.9414)/(1.0d/1209600)));
  }
  public double getFeetPerSecond() {
    return (theValue*((1.0d/0.3048)/1.0d));
  }
  public double getFeetPerMinute() {
    return (theValue*((1.0d/0.3048)/(1.0d/60)));
  }
  public double getFeetPerHour() {
    return (theValue*((1.0d/0.3048)/(1.0d/3600)));
  }
  public double getFeetPerDay() {
    return (theValue*((1.0d/0.3048)/(1.0d/86400)));
  }
  public double getFeetPerWeek() {
    return (theValue*((1.0d/0.3048)/(1.0d/604800)));
  }
  public double getFeetPerMillisecond() {
    return (theValue*((1.0d/0.3048)/1000));
  }
  public double getFeetPerKilosecond() {
    return (theValue*((1.0d/0.3048)/(1.0d/1000)));
  }
  public double getFeetPerMonth() {
    return (theValue*((1.0d/0.3048)/(1.0d/2629743.8)));
  }
  public double getFeetPerYear() {
    return (theValue*((1.0d/0.3048)/(1.0d/31556926)));
  }
  public double getFeetPerFortnight() {
    return (theValue*((1.0d/0.3048)/(1.0d/1209600)));
  }
  public double getInchesPerSecond() {
    return (theValue*((1.0d/0.0254)/1.0d));
  }
  public double getInchesPerMinute() {
    return (theValue*((1.0d/0.0254)/(1.0d/60)));
  }
  public double getInchesPerHour() {
    return (theValue*((1.0d/0.0254)/(1.0d/3600)));
  }
  public double getInchesPerDay() {
    return (theValue*((1.0d/0.0254)/(1.0d/86400)));
  }
  public double getInchesPerWeek() {
    return (theValue*((1.0d/0.0254)/(1.0d/604800)));
  }
  public double getInchesPerMillisecond() {
    return (theValue*((1.0d/0.0254)/1000));
  }
  public double getInchesPerKilosecond() {
    return (theValue*((1.0d/0.0254)/(1.0d/1000)));
  }
  public double getInchesPerMonth() {
    return (theValue*((1.0d/0.0254)/(1.0d/2629743.8)));
  }
  public double getInchesPerYear() {
    return (theValue*((1.0d/0.0254)/(1.0d/31556926)));
  }
  public double getInchesPerFortnight() {
    return (theValue*((1.0d/0.0254)/(1.0d/1209600)));
  }
  public double getKilometersPerSecond() {
    return (theValue*((1.0d/1000.0)/1.0d));
  }
  public double getKilometersPerMinute() {
    return (theValue*((1.0d/1000.0)/(1.0d/60)));
  }
  public double getKilometersPerHour() {
    return (theValue*((1.0d/1000.0)/(1.0d/3600)));
  }
  public double getKilometersPerDay() {
    return (theValue*((1.0d/1000.0)/(1.0d/86400)));
  }
  public double getKilometersPerWeek() {
    return (theValue*((1.0d/1000.0)/(1.0d/604800)));
  }
  public double getKilometersPerMillisecond() {
    return (theValue*((1.0d/1000.0)/1000));
  }
  public double getKilometersPerKilosecond() {
    return (theValue*((1.0d/1000.0)/(1.0d/1000)));
  }
  public double getKilometersPerMonth() {
    return (theValue*((1.0d/1000.0)/(1.0d/2629743.8)));
  }
  public double getKilometersPerYear() {
    return (theValue*((1.0d/1000.0)/(1.0d/31556926)));
  }
  public double getKilometersPerFortnight() {
    return (theValue*((1.0d/1000.0)/(1.0d/1209600)));
  }
  public double getCentimetersPerSecond() {
    return (theValue*(100/1.0d));
  }
  public double getCentimetersPerMinute() {
    return (theValue*(100/(1.0d/60)));
  }
  public double getCentimetersPerHour() {
    return (theValue*(100/(1.0d/3600)));
  }
  public double getCentimetersPerDay() {
    return (theValue*(100/(1.0d/86400)));
  }
  public double getCentimetersPerWeek() {
    return (theValue*(100/(1.0d/604800)));
  }
  public double getCentimetersPerMillisecond() {
    return (theValue*(100/1000));
  }
  public double getCentimetersPerKilosecond() {
    return (theValue*(100/(1.0d/1000)));
  }
  public double getCentimetersPerMonth() {
    return (theValue*(100/(1.0d/2629743.8)));
  }
  public double getCentimetersPerYear() {
    return (theValue*(100/(1.0d/31556926)));
  }
  public double getCentimetersPerFortnight() {
    return (theValue*(100/(1.0d/1209600)));
  }
  public double getMillimetersPerSecond() {
    return (theValue*(1000/1.0d));
  }
  public double getMillimetersPerMinute() {
    return (theValue*(1000/(1.0d/60)));
  }
  public double getMillimetersPerHour() {
    return (theValue*(1000/(1.0d/3600)));
  }
  public double getMillimetersPerDay() {
    return (theValue*(1000/(1.0d/86400)));
  }
  public double getMillimetersPerWeek() {
    return (theValue*(1000/(1.0d/604800)));
  }
  public double getMillimetersPerMillisecond() {
    return (theValue*(1000/1000));
  }
  public double getMillimetersPerKilosecond() {
    return (theValue*(1000/(1.0d/1000)));
  }
  public double getMillimetersPerMonth() {
    return (theValue*(1000/(1.0d/2629743.8)));
  }
  public double getMillimetersPerYear() {
    return (theValue*(1000/(1.0d/31556926)));
  }
  public double getMillimetersPerFortnight() {
    return (theValue*(1000/(1.0d/1209600)));
  }
  public double getFurlongsPerSecond() {
    return (theValue*((1.0d/201.168)/1.0d));
  }
  public double getFurlongsPerMinute() {
    return (theValue*((1.0d/201.168)/(1.0d/60)));
  }
  public double getFurlongsPerHour() {
    return (theValue*((1.0d/201.168)/(1.0d/3600)));
  }
  public double getFurlongsPerDay() {
    return (theValue*((1.0d/201.168)/(1.0d/86400)));
  }
  public double getFurlongsPerWeek() {
    return (theValue*((1.0d/201.168)/(1.0d/604800)));
  }
  public double getFurlongsPerMillisecond() {
    return (theValue*((1.0d/201.168)/1000));
  }
  public double getFurlongsPerKilosecond() {
    return (theValue*((1.0d/201.168)/(1.0d/1000)));
  }
  public double getFurlongsPerMonth() {
    return (theValue*((1.0d/201.168)/(1.0d/2629743.8)));
  }
  public double getFurlongsPerYear() {
    return (theValue*((1.0d/201.168)/(1.0d/31556926)));
  }
  public double getFurlongsPerFortnight() {
    return (theValue*((1.0d/201.168)/(1.0d/1209600)));
  }

  /** @param unit One of the constant units of Speed **/
  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Distance
   *  @param unit2 One of the constant units of Duration
   **/
  public double getValue(int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Distance.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return (theValue*Duration.getConvFactor(unit2)/Distance.getConvFactor(unit1));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Speed &&
             theValue == ((Speed) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "m/s";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // Derivative
  public final Class getNumeratorClass() { return Distance.class; }
  public final Class getDenominatorClass() { return Duration.class; }

  private final static Distance can_num = new Distance(0.0,0);
  public final Measure getCanonicalNumerator() { return can_num; }
  private final static Duration can_den = new Duration(0.0,0);
  public final Measure getCanonicalDenominator() { return can_den; }
  public final Measure computeNumerator(Measure den) {
    if (!(den instanceof Duration)) throw new IllegalArgumentException();
    return new Distance(theValue*den.getValue(0),0);
  }
  public final Measure computeDenominator(Measure num) {
    if (!(num instanceof Distance)) throw new IllegalArgumentException();
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
