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
/** Immutable implementation of TimeRate.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class TimeRate extends AbstractMeasure
  implements Externalizable, Derivative, Rate {
  // the value is stored as seconds/second
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public TimeRate() {}

  // private constructor
  private TimeRate(double v) {
    theValue = v;
  }

  /** @param unit One of the constant units of TimeRate **/
  public TimeRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v/getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Duration
   *  @param unit2 One of the constant units of Duration
   **/
  public TimeRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Duration.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      theValue = v*Duration.getConvFactor(unit1)/Duration.getConvFactor(unit2);
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Duration to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public TimeRate(Duration num, Duration den) {
    theValue = num.getValue(0)/den.getValue(0);
  }

  /** takes strings of the form "Number unit" **/
  public TimeRate(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("secondspersecond")) 
      theValue=n/(1.0d/1.0d);
    else if (u.equals("secondsperminute")) 
      theValue=n/(1.0d/(1.0d/60));
    else if (u.equals("secondsperhour")) 
      theValue=n/(1.0d/(1.0d/3600));
    else if (u.equals("secondsperday")) 
      theValue=n/(1.0d/(1.0d/86400));
    else if (u.equals("secondsperweek")) 
      theValue=n/(1.0d/(1.0d/604800));
    else if (u.equals("secondspermillisecond")) 
      theValue=n/(1.0d/1000);
    else if (u.equals("secondsperkilosecond")) 
      theValue=n/(1.0d/(1.0d/1000));
    else if (u.equals("secondspermonth")) 
      theValue=n/(1.0d/(1.0d/2629743.8));
    else if (u.equals("secondsperyear")) 
      theValue=n/(1.0d/(1.0d/31556926));
    else if (u.equals("secondsperfortnight")) 
      theValue=n/(1.0d/(1.0d/1209600));
    else if (u.equals("minutespersecond")) 
      theValue=n/((1.0d/60)/1.0d);
    else if (u.equals("minutesperminute")) 
      theValue=n/((1.0d/60)/(1.0d/60));
    else if (u.equals("minutesperhour")) 
      theValue=n/((1.0d/60)/(1.0d/3600));
    else if (u.equals("minutesperday")) 
      theValue=n/((1.0d/60)/(1.0d/86400));
    else if (u.equals("minutesperweek")) 
      theValue=n/((1.0d/60)/(1.0d/604800));
    else if (u.equals("minutespermillisecond")) 
      theValue=n/((1.0d/60)/1000);
    else if (u.equals("minutesperkilosecond")) 
      theValue=n/((1.0d/60)/(1.0d/1000));
    else if (u.equals("minutespermonth")) 
      theValue=n/((1.0d/60)/(1.0d/2629743.8));
    else if (u.equals("minutesperyear")) 
      theValue=n/((1.0d/60)/(1.0d/31556926));
    else if (u.equals("minutesperfortnight")) 
      theValue=n/((1.0d/60)/(1.0d/1209600));
    else if (u.equals("hourspersecond")) 
      theValue=n/((1.0d/3600)/1.0d);
    else if (u.equals("hoursperminute")) 
      theValue=n/((1.0d/3600)/(1.0d/60));
    else if (u.equals("hoursperhour")) 
      theValue=n/((1.0d/3600)/(1.0d/3600));
    else if (u.equals("hoursperday")) 
      theValue=n/((1.0d/3600)/(1.0d/86400));
    else if (u.equals("hoursperweek")) 
      theValue=n/((1.0d/3600)/(1.0d/604800));
    else if (u.equals("hourspermillisecond")) 
      theValue=n/((1.0d/3600)/1000);
    else if (u.equals("hoursperkilosecond")) 
      theValue=n/((1.0d/3600)/(1.0d/1000));
    else if (u.equals("hourspermonth")) 
      theValue=n/((1.0d/3600)/(1.0d/2629743.8));
    else if (u.equals("hoursperyear")) 
      theValue=n/((1.0d/3600)/(1.0d/31556926));
    else if (u.equals("hoursperfortnight")) 
      theValue=n/((1.0d/3600)/(1.0d/1209600));
    else if (u.equals("dayspersecond")) 
      theValue=n/((1.0d/86400)/1.0d);
    else if (u.equals("daysperminute")) 
      theValue=n/((1.0d/86400)/(1.0d/60));
    else if (u.equals("daysperhour")) 
      theValue=n/((1.0d/86400)/(1.0d/3600));
    else if (u.equals("daysperday")) 
      theValue=n/((1.0d/86400)/(1.0d/86400));
    else if (u.equals("daysperweek")) 
      theValue=n/((1.0d/86400)/(1.0d/604800));
    else if (u.equals("dayspermillisecond")) 
      theValue=n/((1.0d/86400)/1000);
    else if (u.equals("daysperkilosecond")) 
      theValue=n/((1.0d/86400)/(1.0d/1000));
    else if (u.equals("dayspermonth")) 
      theValue=n/((1.0d/86400)/(1.0d/2629743.8));
    else if (u.equals("daysperyear")) 
      theValue=n/((1.0d/86400)/(1.0d/31556926));
    else if (u.equals("daysperfortnight")) 
      theValue=n/((1.0d/86400)/(1.0d/1209600));
    else if (u.equals("weekspersecond")) 
      theValue=n/((1.0d/604800)/1.0d);
    else if (u.equals("weeksperminute")) 
      theValue=n/((1.0d/604800)/(1.0d/60));
    else if (u.equals("weeksperhour")) 
      theValue=n/((1.0d/604800)/(1.0d/3600));
    else if (u.equals("weeksperday")) 
      theValue=n/((1.0d/604800)/(1.0d/86400));
    else if (u.equals("weeksperweek")) 
      theValue=n/((1.0d/604800)/(1.0d/604800));
    else if (u.equals("weekspermillisecond")) 
      theValue=n/((1.0d/604800)/1000);
    else if (u.equals("weeksperkilosecond")) 
      theValue=n/((1.0d/604800)/(1.0d/1000));
    else if (u.equals("weekspermonth")) 
      theValue=n/((1.0d/604800)/(1.0d/2629743.8));
    else if (u.equals("weeksperyear")) 
      theValue=n/((1.0d/604800)/(1.0d/31556926));
    else if (u.equals("weeksperfortnight")) 
      theValue=n/((1.0d/604800)/(1.0d/1209600));
    else if (u.equals("millisecondspersecond")) 
      theValue=n/(1000/1.0d);
    else if (u.equals("millisecondsperminute")) 
      theValue=n/(1000/(1.0d/60));
    else if (u.equals("millisecondsperhour")) 
      theValue=n/(1000/(1.0d/3600));
    else if (u.equals("millisecondsperday")) 
      theValue=n/(1000/(1.0d/86400));
    else if (u.equals("millisecondsperweek")) 
      theValue=n/(1000/(1.0d/604800));
    else if (u.equals("millisecondspermillisecond")) 
      theValue=n/(1000/1000);
    else if (u.equals("millisecondsperkilosecond")) 
      theValue=n/(1000/(1.0d/1000));
    else if (u.equals("millisecondspermonth")) 
      theValue=n/(1000/(1.0d/2629743.8));
    else if (u.equals("millisecondsperyear")) 
      theValue=n/(1000/(1.0d/31556926));
    else if (u.equals("millisecondsperfortnight")) 
      theValue=n/(1000/(1.0d/1209600));
    else if (u.equals("kilosecondspersecond")) 
      theValue=n/((1.0d/1000)/1.0d);
    else if (u.equals("kilosecondsperminute")) 
      theValue=n/((1.0d/1000)/(1.0d/60));
    else if (u.equals("kilosecondsperhour")) 
      theValue=n/((1.0d/1000)/(1.0d/3600));
    else if (u.equals("kilosecondsperday")) 
      theValue=n/((1.0d/1000)/(1.0d/86400));
    else if (u.equals("kilosecondsperweek")) 
      theValue=n/((1.0d/1000)/(1.0d/604800));
    else if (u.equals("kilosecondspermillisecond")) 
      theValue=n/((1.0d/1000)/1000);
    else if (u.equals("kilosecondsperkilosecond")) 
      theValue=n/((1.0d/1000)/(1.0d/1000));
    else if (u.equals("kilosecondspermonth")) 
      theValue=n/((1.0d/1000)/(1.0d/2629743.8));
    else if (u.equals("kilosecondsperyear")) 
      theValue=n/((1.0d/1000)/(1.0d/31556926));
    else if (u.equals("kilosecondsperfortnight")) 
      theValue=n/((1.0d/1000)/(1.0d/1209600));
    else if (u.equals("monthspersecond")) 
      theValue=n/((1.0d/2629743.8)/1.0d);
    else if (u.equals("monthsperminute")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/60));
    else if (u.equals("monthsperhour")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/3600));
    else if (u.equals("monthsperday")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/86400));
    else if (u.equals("monthsperweek")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/604800));
    else if (u.equals("monthspermillisecond")) 
      theValue=n/((1.0d/2629743.8)/1000);
    else if (u.equals("monthsperkilosecond")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/1000));
    else if (u.equals("monthspermonth")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/2629743.8));
    else if (u.equals("monthsperyear")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/31556926));
    else if (u.equals("monthsperfortnight")) 
      theValue=n/((1.0d/2629743.8)/(1.0d/1209600));
    else if (u.equals("yearspersecond")) 
      theValue=n/((1.0d/31556926)/1.0d);
    else if (u.equals("yearsperminute")) 
      theValue=n/((1.0d/31556926)/(1.0d/60));
    else if (u.equals("yearsperhour")) 
      theValue=n/((1.0d/31556926)/(1.0d/3600));
    else if (u.equals("yearsperday")) 
      theValue=n/((1.0d/31556926)/(1.0d/86400));
    else if (u.equals("yearsperweek")) 
      theValue=n/((1.0d/31556926)/(1.0d/604800));
    else if (u.equals("yearspermillisecond")) 
      theValue=n/((1.0d/31556926)/1000);
    else if (u.equals("yearsperkilosecond")) 
      theValue=n/((1.0d/31556926)/(1.0d/1000));
    else if (u.equals("yearspermonth")) 
      theValue=n/((1.0d/31556926)/(1.0d/2629743.8));
    else if (u.equals("yearsperyear")) 
      theValue=n/((1.0d/31556926)/(1.0d/31556926));
    else if (u.equals("yearsperfortnight")) 
      theValue=n/((1.0d/31556926)/(1.0d/1209600));
    else if (u.equals("fortnightspersecond")) 
      theValue=n/((1.0d/1209600)/1.0d);
    else if (u.equals("fortnightsperminute")) 
      theValue=n/((1.0d/1209600)/(1.0d/60));
    else if (u.equals("fortnightsperhour")) 
      theValue=n/((1.0d/1209600)/(1.0d/3600));
    else if (u.equals("fortnightsperday")) 
      theValue=n/((1.0d/1209600)/(1.0d/86400));
    else if (u.equals("fortnightsperweek")) 
      theValue=n/((1.0d/1209600)/(1.0d/604800));
    else if (u.equals("fortnightspermillisecond")) 
      theValue=n/((1.0d/1209600)/1000);
    else if (u.equals("fortnightsperkilosecond")) 
      theValue=n/((1.0d/1209600)/(1.0d/1000));
    else if (u.equals("fortnightspermonth")) 
      theValue=n/((1.0d/1209600)/(1.0d/2629743.8));
    else if (u.equals("fortnightsperyear")) 
      theValue=n/((1.0d/1209600)/(1.0d/31556926));
    else if (u.equals("fortnightsperfortnight")) 
      theValue=n/((1.0d/1209600)/(1.0d/1209600));
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final TimeRate newSecondsPerSecond(double v) {
    return new TimeRate(v*(1.0d/(1.0d/1.0d)));
  }
  public static final TimeRate newSecondsPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1.0d)));
  }
  public static final TimeRate newSecondsPerMinute(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final TimeRate newSecondsPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final TimeRate newSecondsPerHour(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final TimeRate newSecondsPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final TimeRate newSecondsPerDay(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final TimeRate newSecondsPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final TimeRate newSecondsPerWeek(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final TimeRate newSecondsPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final TimeRate newSecondsPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/(1.0d/1000)));
  }
  public static final TimeRate newSecondsPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1000)));
  }
  public static final TimeRate newSecondsPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final TimeRate newSecondsPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final TimeRate newSecondsPerMonth(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final TimeRate newSecondsPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final TimeRate newSecondsPerYear(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final TimeRate newSecondsPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final TimeRate newSecondsPerFortnight(double v) {
    return new TimeRate(v*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final TimeRate newSecondsPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final TimeRate newMinutesPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/1.0d)));
  }
  public static final TimeRate newMinutesPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/1.0d)));
  }
  public static final TimeRate newMinutesPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/60))));
  }
  public static final TimeRate newMinutesPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/60))));
  }
  public static final TimeRate newMinutesPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/3600))));
  }
  public static final TimeRate newMinutesPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/3600))));
  }
  public static final TimeRate newMinutesPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/86400))));
  }
  public static final TimeRate newMinutesPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/86400))));
  }
  public static final TimeRate newMinutesPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/604800))));
  }
  public static final TimeRate newMinutesPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/604800))));
  }
  public static final TimeRate newMinutesPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/1000)));
  }
  public static final TimeRate newMinutesPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/1000)));
  }
  public static final TimeRate newMinutesPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/1000))));
  }
  public static final TimeRate newMinutesPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/1000))));
  }
  public static final TimeRate newMinutesPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/2629743.8))));
  }
  public static final TimeRate newMinutesPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/2629743.8))));
  }
  public static final TimeRate newMinutesPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/31556926))));
  }
  public static final TimeRate newMinutesPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/31556926))));
  }
  public static final TimeRate newMinutesPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/60)/(1.0d/1209600))));
  }
  public static final TimeRate newMinutesPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/60)/(1.0d/1209600))));
  }
  public static final TimeRate newHoursPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/1.0d)));
  }
  public static final TimeRate newHoursPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/1.0d)));
  }
  public static final TimeRate newHoursPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/60))));
  }
  public static final TimeRate newHoursPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/60))));
  }
  public static final TimeRate newHoursPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/3600))));
  }
  public static final TimeRate newHoursPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/3600))));
  }
  public static final TimeRate newHoursPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/86400))));
  }
  public static final TimeRate newHoursPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/86400))));
  }
  public static final TimeRate newHoursPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/604800))));
  }
  public static final TimeRate newHoursPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/604800))));
  }
  public static final TimeRate newHoursPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/1000)));
  }
  public static final TimeRate newHoursPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/1000)));
  }
  public static final TimeRate newHoursPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/1000))));
  }
  public static final TimeRate newHoursPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/1000))));
  }
  public static final TimeRate newHoursPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/2629743.8))));
  }
  public static final TimeRate newHoursPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/2629743.8))));
  }
  public static final TimeRate newHoursPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/31556926))));
  }
  public static final TimeRate newHoursPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/31556926))));
  }
  public static final TimeRate newHoursPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/3600)/(1.0d/1209600))));
  }
  public static final TimeRate newHoursPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3600)/(1.0d/1209600))));
  }
  public static final TimeRate newDaysPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/1.0d)));
  }
  public static final TimeRate newDaysPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/1.0d)));
  }
  public static final TimeRate newDaysPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/60))));
  }
  public static final TimeRate newDaysPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/60))));
  }
  public static final TimeRate newDaysPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/3600))));
  }
  public static final TimeRate newDaysPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/3600))));
  }
  public static final TimeRate newDaysPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/86400))));
  }
  public static final TimeRate newDaysPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/86400))));
  }
  public static final TimeRate newDaysPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/604800))));
  }
  public static final TimeRate newDaysPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/604800))));
  }
  public static final TimeRate newDaysPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/1000)));
  }
  public static final TimeRate newDaysPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/1000)));
  }
  public static final TimeRate newDaysPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/1000))));
  }
  public static final TimeRate newDaysPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/1000))));
  }
  public static final TimeRate newDaysPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/2629743.8))));
  }
  public static final TimeRate newDaysPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/2629743.8))));
  }
  public static final TimeRate newDaysPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/31556926))));
  }
  public static final TimeRate newDaysPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/31556926))));
  }
  public static final TimeRate newDaysPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/86400)/(1.0d/1209600))));
  }
  public static final TimeRate newDaysPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/86400)/(1.0d/1209600))));
  }
  public static final TimeRate newWeeksPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/1.0d)));
  }
  public static final TimeRate newWeeksPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/1.0d)));
  }
  public static final TimeRate newWeeksPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/60))));
  }
  public static final TimeRate newWeeksPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/60))));
  }
  public static final TimeRate newWeeksPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/3600))));
  }
  public static final TimeRate newWeeksPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/3600))));
  }
  public static final TimeRate newWeeksPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/86400))));
  }
  public static final TimeRate newWeeksPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/86400))));
  }
  public static final TimeRate newWeeksPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/604800))));
  }
  public static final TimeRate newWeeksPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/604800))));
  }
  public static final TimeRate newWeeksPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/1000)));
  }
  public static final TimeRate newWeeksPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/1000)));
  }
  public static final TimeRate newWeeksPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/1000))));
  }
  public static final TimeRate newWeeksPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/1000))));
  }
  public static final TimeRate newWeeksPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/2629743.8))));
  }
  public static final TimeRate newWeeksPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/2629743.8))));
  }
  public static final TimeRate newWeeksPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/31556926))));
  }
  public static final TimeRate newWeeksPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/31556926))));
  }
  public static final TimeRate newWeeksPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/604800)/(1.0d/1209600))));
  }
  public static final TimeRate newWeeksPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/604800)/(1.0d/1209600))));
  }
  public static final TimeRate newMillisecondsPerSecond(double v) {
    return new TimeRate(v*(1.0d/(1000/1.0d)));
  }
  public static final TimeRate newMillisecondsPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/1.0d)));
  }
  public static final TimeRate newMillisecondsPerMinute(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/60))));
  }
  public static final TimeRate newMillisecondsPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/60))));
  }
  public static final TimeRate newMillisecondsPerHour(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/3600))));
  }
  public static final TimeRate newMillisecondsPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/3600))));
  }
  public static final TimeRate newMillisecondsPerDay(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/86400))));
  }
  public static final TimeRate newMillisecondsPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/86400))));
  }
  public static final TimeRate newMillisecondsPerWeek(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/604800))));
  }
  public static final TimeRate newMillisecondsPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/604800))));
  }
  public static final TimeRate newMillisecondsPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/(1000/1000)));
  }
  public static final TimeRate newMillisecondsPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/1000)));
  }
  public static final TimeRate newMillisecondsPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/1000))));
  }
  public static final TimeRate newMillisecondsPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/1000))));
  }
  public static final TimeRate newMillisecondsPerMonth(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/2629743.8))));
  }
  public static final TimeRate newMillisecondsPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/2629743.8))));
  }
  public static final TimeRate newMillisecondsPerYear(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/31556926))));
  }
  public static final TimeRate newMillisecondsPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/31556926))));
  }
  public static final TimeRate newMillisecondsPerFortnight(double v) {
    return new TimeRate(v*(1.0d/(1000/(1.0d/1209600))));
  }
  public static final TimeRate newMillisecondsPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/1209600))));
  }
  public static final TimeRate newKilosecondsPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/1.0d)));
  }
  public static final TimeRate newKilosecondsPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/1.0d)));
  }
  public static final TimeRate newKilosecondsPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/60))));
  }
  public static final TimeRate newKilosecondsPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/60))));
  }
  public static final TimeRate newKilosecondsPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/3600))));
  }
  public static final TimeRate newKilosecondsPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/3600))));
  }
  public static final TimeRate newKilosecondsPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/86400))));
  }
  public static final TimeRate newKilosecondsPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/86400))));
  }
  public static final TimeRate newKilosecondsPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/604800))));
  }
  public static final TimeRate newKilosecondsPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/604800))));
  }
  public static final TimeRate newKilosecondsPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/1000)));
  }
  public static final TimeRate newKilosecondsPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/1000)));
  }
  public static final TimeRate newKilosecondsPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/1000))));
  }
  public static final TimeRate newKilosecondsPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/1000))));
  }
  public static final TimeRate newKilosecondsPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/2629743.8))));
  }
  public static final TimeRate newKilosecondsPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/2629743.8))));
  }
  public static final TimeRate newKilosecondsPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/31556926))));
  }
  public static final TimeRate newKilosecondsPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/31556926))));
  }
  public static final TimeRate newKilosecondsPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1000)/(1.0d/1209600))));
  }
  public static final TimeRate newKilosecondsPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/1209600))));
  }
  public static final TimeRate newMonthsPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/1.0d)));
  }
  public static final TimeRate newMonthsPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/1.0d)));
  }
  public static final TimeRate newMonthsPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/60))));
  }
  public static final TimeRate newMonthsPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/60))));
  }
  public static final TimeRate newMonthsPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/3600))));
  }
  public static final TimeRate newMonthsPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/3600))));
  }
  public static final TimeRate newMonthsPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/86400))));
  }
  public static final TimeRate newMonthsPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/86400))));
  }
  public static final TimeRate newMonthsPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/604800))));
  }
  public static final TimeRate newMonthsPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/604800))));
  }
  public static final TimeRate newMonthsPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/1000)));
  }
  public static final TimeRate newMonthsPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/1000)));
  }
  public static final TimeRate newMonthsPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/1000))));
  }
  public static final TimeRate newMonthsPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/1000))));
  }
  public static final TimeRate newMonthsPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/2629743.8))));
  }
  public static final TimeRate newMonthsPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/2629743.8))));
  }
  public static final TimeRate newMonthsPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/31556926))));
  }
  public static final TimeRate newMonthsPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/31556926))));
  }
  public static final TimeRate newMonthsPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/2629743.8)/(1.0d/1209600))));
  }
  public static final TimeRate newMonthsPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/2629743.8)/(1.0d/1209600))));
  }
  public static final TimeRate newYearsPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/1.0d)));
  }
  public static final TimeRate newYearsPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/1.0d)));
  }
  public static final TimeRate newYearsPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/60))));
  }
  public static final TimeRate newYearsPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/60))));
  }
  public static final TimeRate newYearsPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/3600))));
  }
  public static final TimeRate newYearsPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/3600))));
  }
  public static final TimeRate newYearsPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/86400))));
  }
  public static final TimeRate newYearsPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/86400))));
  }
  public static final TimeRate newYearsPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/604800))));
  }
  public static final TimeRate newYearsPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/604800))));
  }
  public static final TimeRate newYearsPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/1000)));
  }
  public static final TimeRate newYearsPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/1000)));
  }
  public static final TimeRate newYearsPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/1000))));
  }
  public static final TimeRate newYearsPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/1000))));
  }
  public static final TimeRate newYearsPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/2629743.8))));
  }
  public static final TimeRate newYearsPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/2629743.8))));
  }
  public static final TimeRate newYearsPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/31556926))));
  }
  public static final TimeRate newYearsPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/31556926))));
  }
  public static final TimeRate newYearsPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/31556926)/(1.0d/1209600))));
  }
  public static final TimeRate newYearsPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/31556926)/(1.0d/1209600))));
  }
  public static final TimeRate newFortnightsPerSecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/1.0d)));
  }
  public static final TimeRate newFortnightsPerSecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/1.0d)));
  }
  public static final TimeRate newFortnightsPerMinute(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/60))));
  }
  public static final TimeRate newFortnightsPerMinute(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/60))));
  }
  public static final TimeRate newFortnightsPerHour(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/3600))));
  }
  public static final TimeRate newFortnightsPerHour(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/3600))));
  }
  public static final TimeRate newFortnightsPerDay(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/86400))));
  }
  public static final TimeRate newFortnightsPerDay(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/86400))));
  }
  public static final TimeRate newFortnightsPerWeek(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/604800))));
  }
  public static final TimeRate newFortnightsPerWeek(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/604800))));
  }
  public static final TimeRate newFortnightsPerMillisecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/1000)));
  }
  public static final TimeRate newFortnightsPerMillisecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/1000)));
  }
  public static final TimeRate newFortnightsPerKilosecond(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/1000))));
  }
  public static final TimeRate newFortnightsPerKilosecond(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/1000))));
  }
  public static final TimeRate newFortnightsPerMonth(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/2629743.8))));
  }
  public static final TimeRate newFortnightsPerMonth(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/2629743.8))));
  }
  public static final TimeRate newFortnightsPerYear(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/31556926))));
  }
  public static final TimeRate newFortnightsPerYear(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/31556926))));
  }
  public static final TimeRate newFortnightsPerFortnight(double v) {
    return new TimeRate(v*(1.0d/((1.0d/1209600)/(1.0d/1209600))));
  }
  public static final TimeRate newFortnightsPerFortnight(String s) {
    return new TimeRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1209600)/(1.0d/1209600))));
  }


  public int getCommonUnit() {
    return 23;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "seconds/second",
    "seconds/minute",
    "seconds/hour",
    "seconds/day",
    "seconds/week",
    "seconds/millisecond",
    "seconds/kilosecond",
    "seconds/month",
    "seconds/year",
    "seconds/fortnight",
    "minutes/second",
    "minutes/minute",
    "minutes/hour",
    "minutes/day",
    "minutes/week",
    "minutes/millisecond",
    "minutes/kilosecond",
    "minutes/month",
    "minutes/year",
    "minutes/fortnight",
    "hours/second",
    "hours/minute",
    "hours/hour",
    "hours/day",
    "hours/week",
    "hours/millisecond",
    "hours/kilosecond",
    "hours/month",
    "hours/year",
    "hours/fortnight",
    "days/second",
    "days/minute",
    "days/hour",
    "days/day",
    "days/week",
    "days/millisecond",
    "days/kilosecond",
    "days/month",
    "days/year",
    "days/fortnight",
    "weeks/second",
    "weeks/minute",
    "weeks/hour",
    "weeks/day",
    "weeks/week",
    "weeks/millisecond",
    "weeks/kilosecond",
    "weeks/month",
    "weeks/year",
    "weeks/fortnight",
    "milliseconds/second",
    "milliseconds/minute",
    "milliseconds/hour",
    "milliseconds/day",
    "milliseconds/week",
    "milliseconds/millisecond",
    "milliseconds/kilosecond",
    "milliseconds/month",
    "milliseconds/year",
    "milliseconds/fortnight",
    "kiloseconds/second",
    "kiloseconds/minute",
    "kiloseconds/hour",
    "kiloseconds/day",
    "kiloseconds/week",
    "kiloseconds/millisecond",
    "kiloseconds/kilosecond",
    "kiloseconds/month",
    "kiloseconds/year",
    "kiloseconds/fortnight",
    "months/second",
    "months/minute",
    "months/hour",
    "months/day",
    "months/week",
    "months/millisecond",
    "months/kilosecond",
    "months/month",
    "months/year",
    "months/fortnight",
    "years/second",
    "years/minute",
    "years/hour",
    "years/day",
    "years/week",
    "years/millisecond",
    "years/kilosecond",
    "years/month",
    "years/year",
    "years/fortnight",
    "fortnights/second",
    "fortnights/minute",
    "fortnights/hour",
    "fortnights/day",
    "fortnights/week",
    "fortnights/millisecond",
    "fortnights/kilosecond",
    "fortnights/month",
    "fortnights/year",
    "fortnights/fortnight",
  };

  /** @param unit One of the constant units of TimeRate **/
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
    ((1.0d/60)/1.0d),
    ((1.0d/60)/(1.0d/60)),
    ((1.0d/60)/(1.0d/3600)),
    ((1.0d/60)/(1.0d/86400)),
    ((1.0d/60)/(1.0d/604800)),
    ((1.0d/60)/1000),
    ((1.0d/60)/(1.0d/1000)),
    ((1.0d/60)/(1.0d/2629743.8)),
    ((1.0d/60)/(1.0d/31556926)),
    ((1.0d/60)/(1.0d/1209600)),
    ((1.0d/3600)/1.0d),
    ((1.0d/3600)/(1.0d/60)),
    ((1.0d/3600)/(1.0d/3600)),
    ((1.0d/3600)/(1.0d/86400)),
    ((1.0d/3600)/(1.0d/604800)),
    ((1.0d/3600)/1000),
    ((1.0d/3600)/(1.0d/1000)),
    ((1.0d/3600)/(1.0d/2629743.8)),
    ((1.0d/3600)/(1.0d/31556926)),
    ((1.0d/3600)/(1.0d/1209600)),
    ((1.0d/86400)/1.0d),
    ((1.0d/86400)/(1.0d/60)),
    ((1.0d/86400)/(1.0d/3600)),
    ((1.0d/86400)/(1.0d/86400)),
    ((1.0d/86400)/(1.0d/604800)),
    ((1.0d/86400)/1000),
    ((1.0d/86400)/(1.0d/1000)),
    ((1.0d/86400)/(1.0d/2629743.8)),
    ((1.0d/86400)/(1.0d/31556926)),
    ((1.0d/86400)/(1.0d/1209600)),
    ((1.0d/604800)/1.0d),
    ((1.0d/604800)/(1.0d/60)),
    ((1.0d/604800)/(1.0d/3600)),
    ((1.0d/604800)/(1.0d/86400)),
    ((1.0d/604800)/(1.0d/604800)),
    ((1.0d/604800)/1000),
    ((1.0d/604800)/(1.0d/1000)),
    ((1.0d/604800)/(1.0d/2629743.8)),
    ((1.0d/604800)/(1.0d/31556926)),
    ((1.0d/604800)/(1.0d/1209600)),
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
    ((1.0d/2629743.8)/1.0d),
    ((1.0d/2629743.8)/(1.0d/60)),
    ((1.0d/2629743.8)/(1.0d/3600)),
    ((1.0d/2629743.8)/(1.0d/86400)),
    ((1.0d/2629743.8)/(1.0d/604800)),
    ((1.0d/2629743.8)/1000),
    ((1.0d/2629743.8)/(1.0d/1000)),
    ((1.0d/2629743.8)/(1.0d/2629743.8)),
    ((1.0d/2629743.8)/(1.0d/31556926)),
    ((1.0d/2629743.8)/(1.0d/1209600)),
    ((1.0d/31556926)/1.0d),
    ((1.0d/31556926)/(1.0d/60)),
    ((1.0d/31556926)/(1.0d/3600)),
    ((1.0d/31556926)/(1.0d/86400)),
    ((1.0d/31556926)/(1.0d/604800)),
    ((1.0d/31556926)/1000),
    ((1.0d/31556926)/(1.0d/1000)),
    ((1.0d/31556926)/(1.0d/2629743.8)),
    ((1.0d/31556926)/(1.0d/31556926)),
    ((1.0d/31556926)/(1.0d/1209600)),
    ((1.0d/1209600)/1.0d),
    ((1.0d/1209600)/(1.0d/60)),
    ((1.0d/1209600)/(1.0d/3600)),
    ((1.0d/1209600)/(1.0d/86400)),
    ((1.0d/1209600)/(1.0d/604800)),
    ((1.0d/1209600)/1000),
    ((1.0d/1209600)/(1.0d/1000)),
    ((1.0d/1209600)/(1.0d/2629743.8)),
    ((1.0d/1209600)/(1.0d/31556926)),
    ((1.0d/1209600)/(1.0d/1209600)),
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int SECONDS_PER_SECOND = 0;
  public static final int SECONDS_PER_MINUTE = 1;
  public static final int SECONDS_PER_HOUR = 2;
  public static final int SECONDS_PER_DAY = 3;
  public static final int SECONDS_PER_WEEK = 4;
  public static final int SECONDS_PER_MILLISECOND = 5;
  public static final int SECONDS_PER_KILOSECOND = 6;
  public static final int SECONDS_PER_MONTH = 7;
  public static final int SECONDS_PER_YEAR = 8;
  public static final int SECONDS_PER_FORTNIGHT = 9;
  public static final int MINUTES_PER_SECOND = 10;
  public static final int MINUTES_PER_MINUTE = 11;
  public static final int MINUTES_PER_HOUR = 12;
  public static final int MINUTES_PER_DAY = 13;
  public static final int MINUTES_PER_WEEK = 14;
  public static final int MINUTES_PER_MILLISECOND = 15;
  public static final int MINUTES_PER_KILOSECOND = 16;
  public static final int MINUTES_PER_MONTH = 17;
  public static final int MINUTES_PER_YEAR = 18;
  public static final int MINUTES_PER_FORTNIGHT = 19;
  public static final int HOURS_PER_SECOND = 20;
  public static final int HOURS_PER_MINUTE = 21;
  public static final int HOURS_PER_HOUR = 22;
  public static final int HOURS_PER_DAY = 23;
  public static final int HOURS_PER_WEEK = 24;
  public static final int HOURS_PER_MILLISECOND = 25;
  public static final int HOURS_PER_KILOSECOND = 26;
  public static final int HOURS_PER_MONTH = 27;
  public static final int HOURS_PER_YEAR = 28;
  public static final int HOURS_PER_FORTNIGHT = 29;
  public static final int DAYS_PER_SECOND = 30;
  public static final int DAYS_PER_MINUTE = 31;
  public static final int DAYS_PER_HOUR = 32;
  public static final int DAYS_PER_DAY = 33;
  public static final int DAYS_PER_WEEK = 34;
  public static final int DAYS_PER_MILLISECOND = 35;
  public static final int DAYS_PER_KILOSECOND = 36;
  public static final int DAYS_PER_MONTH = 37;
  public static final int DAYS_PER_YEAR = 38;
  public static final int DAYS_PER_FORTNIGHT = 39;
  public static final int WEEKS_PER_SECOND = 40;
  public static final int WEEKS_PER_MINUTE = 41;
  public static final int WEEKS_PER_HOUR = 42;
  public static final int WEEKS_PER_DAY = 43;
  public static final int WEEKS_PER_WEEK = 44;
  public static final int WEEKS_PER_MILLISECOND = 45;
  public static final int WEEKS_PER_KILOSECOND = 46;
  public static final int WEEKS_PER_MONTH = 47;
  public static final int WEEKS_PER_YEAR = 48;
  public static final int WEEKS_PER_FORTNIGHT = 49;
  public static final int MILLISECONDS_PER_SECOND = 50;
  public static final int MILLISECONDS_PER_MINUTE = 51;
  public static final int MILLISECONDS_PER_HOUR = 52;
  public static final int MILLISECONDS_PER_DAY = 53;
  public static final int MILLISECONDS_PER_WEEK = 54;
  public static final int MILLISECONDS_PER_MILLISECOND = 55;
  public static final int MILLISECONDS_PER_KILOSECOND = 56;
  public static final int MILLISECONDS_PER_MONTH = 57;
  public static final int MILLISECONDS_PER_YEAR = 58;
  public static final int MILLISECONDS_PER_FORTNIGHT = 59;
  public static final int KILOSECONDS_PER_SECOND = 60;
  public static final int KILOSECONDS_PER_MINUTE = 61;
  public static final int KILOSECONDS_PER_HOUR = 62;
  public static final int KILOSECONDS_PER_DAY = 63;
  public static final int KILOSECONDS_PER_WEEK = 64;
  public static final int KILOSECONDS_PER_MILLISECOND = 65;
  public static final int KILOSECONDS_PER_KILOSECOND = 66;
  public static final int KILOSECONDS_PER_MONTH = 67;
  public static final int KILOSECONDS_PER_YEAR = 68;
  public static final int KILOSECONDS_PER_FORTNIGHT = 69;
  public static final int MONTHS_PER_SECOND = 70;
  public static final int MONTHS_PER_MINUTE = 71;
  public static final int MONTHS_PER_HOUR = 72;
  public static final int MONTHS_PER_DAY = 73;
  public static final int MONTHS_PER_WEEK = 74;
  public static final int MONTHS_PER_MILLISECOND = 75;
  public static final int MONTHS_PER_KILOSECOND = 76;
  public static final int MONTHS_PER_MONTH = 77;
  public static final int MONTHS_PER_YEAR = 78;
  public static final int MONTHS_PER_FORTNIGHT = 79;
  public static final int YEARS_PER_SECOND = 80;
  public static final int YEARS_PER_MINUTE = 81;
  public static final int YEARS_PER_HOUR = 82;
  public static final int YEARS_PER_DAY = 83;
  public static final int YEARS_PER_WEEK = 84;
  public static final int YEARS_PER_MILLISECOND = 85;
  public static final int YEARS_PER_KILOSECOND = 86;
  public static final int YEARS_PER_MONTH = 87;
  public static final int YEARS_PER_YEAR = 88;
  public static final int YEARS_PER_FORTNIGHT = 89;
  public static final int FORTNIGHTS_PER_SECOND = 90;
  public static final int FORTNIGHTS_PER_MINUTE = 91;
  public static final int FORTNIGHTS_PER_HOUR = 92;
  public static final int FORTNIGHTS_PER_DAY = 93;
  public static final int FORTNIGHTS_PER_WEEK = 94;
  public static final int FORTNIGHTS_PER_MILLISECOND = 95;
  public static final int FORTNIGHTS_PER_KILOSECOND = 96;
  public static final int FORTNIGHTS_PER_MONTH = 97;
  public static final int FORTNIGHTS_PER_YEAR = 98;
  public static final int FORTNIGHTS_PER_FORTNIGHT = 99;
  static final int MAXUNIT = 99;

  // Index Typed factory methods
  /** @param unit One of the constant units of TimeRate **/
  public static final TimeRate newTimeRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new TimeRate(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit One of the constant units of TimeRate **/
  public static final TimeRate newTimeRate(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new TimeRate((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Index Typed factory methods
  /** @param unit1 One of the constant units of Duration
   *  @param unit2 One of the constant units of Duration
   **/
  public static final TimeRate newTimeRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Duration.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new TimeRate(v*Duration.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Duration to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public static final TimeRate newTimeRate(Duration num, Duration den) {
    return new TimeRate(num.getValue(0)/den.getValue(0));
  }

  /** @param unit1 One of the constant units of Duration
   *  @param unit2 One of the constant units of Duration
   **/
  public static final TimeRate newTimeRate(String s, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Duration.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new TimeRate((Double.valueOf(s).doubleValue())*Duration.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newTimeRate(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newTimeRate(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof TimeRate)) throw new IllegalArgumentException();
    return new TimeRate(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof TimeRate)) throw new IllegalArgumentException();
    return new TimeRate(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new TimeRate(theValue*scale,0);
  }

  public final Measure negate() {
    return newTimeRate(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newTimeRate(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new TimeRate(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new TimeRate(value, unit);
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
      throw new IllegalArgumentException("Expecting a TimeRate" + 
      ", got a " + toRate.getCanonicalNumerator().getClass() + "/" + toRate.getCanonicalDenominator().getClass());
    }
    return theValue/toRate.getNativeValue();
  }

  // Unit-based Reader methods
  public double getSecondsPerSecond() {
    return (theValue*(1.0d/1.0d));
  }
  public double getSecondsPerMinute() {
    return (theValue*(1.0d/(1.0d/60)));
  }
  public double getSecondsPerHour() {
    return (theValue*(1.0d/(1.0d/3600)));
  }
  public double getSecondsPerDay() {
    return (theValue*(1.0d/(1.0d/86400)));
  }
  public double getSecondsPerWeek() {
    return (theValue*(1.0d/(1.0d/604800)));
  }
  public double getSecondsPerMillisecond() {
    return (theValue*(1.0d/1000));
  }
  public double getSecondsPerKilosecond() {
    return (theValue*(1.0d/(1.0d/1000)));
  }
  public double getSecondsPerMonth() {
    return (theValue*(1.0d/(1.0d/2629743.8)));
  }
  public double getSecondsPerYear() {
    return (theValue*(1.0d/(1.0d/31556926)));
  }
  public double getSecondsPerFortnight() {
    return (theValue*(1.0d/(1.0d/1209600)));
  }
  public double getMinutesPerSecond() {
    return (theValue*((1.0d/60)/1.0d));
  }
  public double getMinutesPerMinute() {
    return (theValue*((1.0d/60)/(1.0d/60)));
  }
  public double getMinutesPerHour() {
    return (theValue*((1.0d/60)/(1.0d/3600)));
  }
  public double getMinutesPerDay() {
    return (theValue*((1.0d/60)/(1.0d/86400)));
  }
  public double getMinutesPerWeek() {
    return (theValue*((1.0d/60)/(1.0d/604800)));
  }
  public double getMinutesPerMillisecond() {
    return (theValue*((1.0d/60)/1000));
  }
  public double getMinutesPerKilosecond() {
    return (theValue*((1.0d/60)/(1.0d/1000)));
  }
  public double getMinutesPerMonth() {
    return (theValue*((1.0d/60)/(1.0d/2629743.8)));
  }
  public double getMinutesPerYear() {
    return (theValue*((1.0d/60)/(1.0d/31556926)));
  }
  public double getMinutesPerFortnight() {
    return (theValue*((1.0d/60)/(1.0d/1209600)));
  }
  public double getHoursPerSecond() {
    return (theValue*((1.0d/3600)/1.0d));
  }
  public double getHoursPerMinute() {
    return (theValue*((1.0d/3600)/(1.0d/60)));
  }
  public double getHoursPerHour() {
    return (theValue*((1.0d/3600)/(1.0d/3600)));
  }
  public double getHoursPerDay() {
    return (theValue*((1.0d/3600)/(1.0d/86400)));
  }
  public double getHoursPerWeek() {
    return (theValue*((1.0d/3600)/(1.0d/604800)));
  }
  public double getHoursPerMillisecond() {
    return (theValue*((1.0d/3600)/1000));
  }
  public double getHoursPerKilosecond() {
    return (theValue*((1.0d/3600)/(1.0d/1000)));
  }
  public double getHoursPerMonth() {
    return (theValue*((1.0d/3600)/(1.0d/2629743.8)));
  }
  public double getHoursPerYear() {
    return (theValue*((1.0d/3600)/(1.0d/31556926)));
  }
  public double getHoursPerFortnight() {
    return (theValue*((1.0d/3600)/(1.0d/1209600)));
  }
  public double getDaysPerSecond() {
    return (theValue*((1.0d/86400)/1.0d));
  }
  public double getDaysPerMinute() {
    return (theValue*((1.0d/86400)/(1.0d/60)));
  }
  public double getDaysPerHour() {
    return (theValue*((1.0d/86400)/(1.0d/3600)));
  }
  public double getDaysPerDay() {
    return (theValue*((1.0d/86400)/(1.0d/86400)));
  }
  public double getDaysPerWeek() {
    return (theValue*((1.0d/86400)/(1.0d/604800)));
  }
  public double getDaysPerMillisecond() {
    return (theValue*((1.0d/86400)/1000));
  }
  public double getDaysPerKilosecond() {
    return (theValue*((1.0d/86400)/(1.0d/1000)));
  }
  public double getDaysPerMonth() {
    return (theValue*((1.0d/86400)/(1.0d/2629743.8)));
  }
  public double getDaysPerYear() {
    return (theValue*((1.0d/86400)/(1.0d/31556926)));
  }
  public double getDaysPerFortnight() {
    return (theValue*((1.0d/86400)/(1.0d/1209600)));
  }
  public double getWeeksPerSecond() {
    return (theValue*((1.0d/604800)/1.0d));
  }
  public double getWeeksPerMinute() {
    return (theValue*((1.0d/604800)/(1.0d/60)));
  }
  public double getWeeksPerHour() {
    return (theValue*((1.0d/604800)/(1.0d/3600)));
  }
  public double getWeeksPerDay() {
    return (theValue*((1.0d/604800)/(1.0d/86400)));
  }
  public double getWeeksPerWeek() {
    return (theValue*((1.0d/604800)/(1.0d/604800)));
  }
  public double getWeeksPerMillisecond() {
    return (theValue*((1.0d/604800)/1000));
  }
  public double getWeeksPerKilosecond() {
    return (theValue*((1.0d/604800)/(1.0d/1000)));
  }
  public double getWeeksPerMonth() {
    return (theValue*((1.0d/604800)/(1.0d/2629743.8)));
  }
  public double getWeeksPerYear() {
    return (theValue*((1.0d/604800)/(1.0d/31556926)));
  }
  public double getWeeksPerFortnight() {
    return (theValue*((1.0d/604800)/(1.0d/1209600)));
  }
  public double getMillisecondsPerSecond() {
    return (theValue*(1000/1.0d));
  }
  public double getMillisecondsPerMinute() {
    return (theValue*(1000/(1.0d/60)));
  }
  public double getMillisecondsPerHour() {
    return (theValue*(1000/(1.0d/3600)));
  }
  public double getMillisecondsPerDay() {
    return (theValue*(1000/(1.0d/86400)));
  }
  public double getMillisecondsPerWeek() {
    return (theValue*(1000/(1.0d/604800)));
  }
  public double getMillisecondsPerMillisecond() {
    return (theValue*(1000/1000));
  }
  public double getMillisecondsPerKilosecond() {
    return (theValue*(1000/(1.0d/1000)));
  }
  public double getMillisecondsPerMonth() {
    return (theValue*(1000/(1.0d/2629743.8)));
  }
  public double getMillisecondsPerYear() {
    return (theValue*(1000/(1.0d/31556926)));
  }
  public double getMillisecondsPerFortnight() {
    return (theValue*(1000/(1.0d/1209600)));
  }
  public double getKilosecondsPerSecond() {
    return (theValue*((1.0d/1000)/1.0d));
  }
  public double getKilosecondsPerMinute() {
    return (theValue*((1.0d/1000)/(1.0d/60)));
  }
  public double getKilosecondsPerHour() {
    return (theValue*((1.0d/1000)/(1.0d/3600)));
  }
  public double getKilosecondsPerDay() {
    return (theValue*((1.0d/1000)/(1.0d/86400)));
  }
  public double getKilosecondsPerWeek() {
    return (theValue*((1.0d/1000)/(1.0d/604800)));
  }
  public double getKilosecondsPerMillisecond() {
    return (theValue*((1.0d/1000)/1000));
  }
  public double getKilosecondsPerKilosecond() {
    return (theValue*((1.0d/1000)/(1.0d/1000)));
  }
  public double getKilosecondsPerMonth() {
    return (theValue*((1.0d/1000)/(1.0d/2629743.8)));
  }
  public double getKilosecondsPerYear() {
    return (theValue*((1.0d/1000)/(1.0d/31556926)));
  }
  public double getKilosecondsPerFortnight() {
    return (theValue*((1.0d/1000)/(1.0d/1209600)));
  }
  public double getMonthsPerSecond() {
    return (theValue*((1.0d/2629743.8)/1.0d));
  }
  public double getMonthsPerMinute() {
    return (theValue*((1.0d/2629743.8)/(1.0d/60)));
  }
  public double getMonthsPerHour() {
    return (theValue*((1.0d/2629743.8)/(1.0d/3600)));
  }
  public double getMonthsPerDay() {
    return (theValue*((1.0d/2629743.8)/(1.0d/86400)));
  }
  public double getMonthsPerWeek() {
    return (theValue*((1.0d/2629743.8)/(1.0d/604800)));
  }
  public double getMonthsPerMillisecond() {
    return (theValue*((1.0d/2629743.8)/1000));
  }
  public double getMonthsPerKilosecond() {
    return (theValue*((1.0d/2629743.8)/(1.0d/1000)));
  }
  public double getMonthsPerMonth() {
    return (theValue*((1.0d/2629743.8)/(1.0d/2629743.8)));
  }
  public double getMonthsPerYear() {
    return (theValue*((1.0d/2629743.8)/(1.0d/31556926)));
  }
  public double getMonthsPerFortnight() {
    return (theValue*((1.0d/2629743.8)/(1.0d/1209600)));
  }
  public double getYearsPerSecond() {
    return (theValue*((1.0d/31556926)/1.0d));
  }
  public double getYearsPerMinute() {
    return (theValue*((1.0d/31556926)/(1.0d/60)));
  }
  public double getYearsPerHour() {
    return (theValue*((1.0d/31556926)/(1.0d/3600)));
  }
  public double getYearsPerDay() {
    return (theValue*((1.0d/31556926)/(1.0d/86400)));
  }
  public double getYearsPerWeek() {
    return (theValue*((1.0d/31556926)/(1.0d/604800)));
  }
  public double getYearsPerMillisecond() {
    return (theValue*((1.0d/31556926)/1000));
  }
  public double getYearsPerKilosecond() {
    return (theValue*((1.0d/31556926)/(1.0d/1000)));
  }
  public double getYearsPerMonth() {
    return (theValue*((1.0d/31556926)/(1.0d/2629743.8)));
  }
  public double getYearsPerYear() {
    return (theValue*((1.0d/31556926)/(1.0d/31556926)));
  }
  public double getYearsPerFortnight() {
    return (theValue*((1.0d/31556926)/(1.0d/1209600)));
  }
  public double getFortnightsPerSecond() {
    return (theValue*((1.0d/1209600)/1.0d));
  }
  public double getFortnightsPerMinute() {
    return (theValue*((1.0d/1209600)/(1.0d/60)));
  }
  public double getFortnightsPerHour() {
    return (theValue*((1.0d/1209600)/(1.0d/3600)));
  }
  public double getFortnightsPerDay() {
    return (theValue*((1.0d/1209600)/(1.0d/86400)));
  }
  public double getFortnightsPerWeek() {
    return (theValue*((1.0d/1209600)/(1.0d/604800)));
  }
  public double getFortnightsPerMillisecond() {
    return (theValue*((1.0d/1209600)/1000));
  }
  public double getFortnightsPerKilosecond() {
    return (theValue*((1.0d/1209600)/(1.0d/1000)));
  }
  public double getFortnightsPerMonth() {
    return (theValue*((1.0d/1209600)/(1.0d/2629743.8)));
  }
  public double getFortnightsPerYear() {
    return (theValue*((1.0d/1209600)/(1.0d/31556926)));
  }
  public double getFortnightsPerFortnight() {
    return (theValue*((1.0d/1209600)/(1.0d/1209600)));
  }

  /** @param unit One of the constant units of TimeRate **/
  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Duration
   *  @param unit2 One of the constant units of Duration
   **/
  public double getValue(int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Duration.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return (theValue*Duration.getConvFactor(unit2)/Duration.getConvFactor(unit1));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof TimeRate &&
             theValue == ((TimeRate) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "s/s";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // Derivative
  public final Class getNumeratorClass() { return Duration.class; }
  public final Class getDenominatorClass() { return Duration.class; }

  private final static Duration can_num = new Duration(0.0,0);
  public final Measure getCanonicalNumerator() { return can_num; }
  private final static Duration can_den = new Duration(0.0,0);
  public final Measure getCanonicalDenominator() { return can_den; }
  public final Measure computeNumerator(Measure den) {
    if (!(den instanceof Duration)) throw new IllegalArgumentException();
    return new Duration(theValue*den.getValue(0),0);
  }
  public final Measure computeDenominator(Measure num) {
    if (!(num instanceof Duration)) throw new IllegalArgumentException();
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
