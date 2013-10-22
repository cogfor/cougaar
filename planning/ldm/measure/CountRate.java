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
/** Immutable implementation of CountRate.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class CountRate extends AbstractMeasure
  implements Externalizable, Derivative, Rate {
  // the value is stored as units/second
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public CountRate() {}

  // private constructor
  private CountRate(double v) {
    theValue = v;
  }

  /** @param unit One of the constant units of CountRate **/
  public CountRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v/getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Count
   *  @param unit2 One of the constant units of Duration
   **/
  public CountRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Count.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      theValue = v*Count.getConvFactor(unit1)/Duration.getConvFactor(unit2);
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Count to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public CountRate(Count num, Duration den) {
    theValue = num.getValue(0)/den.getValue(0);
  }

  /** takes strings of the form "Number unit" **/
  public CountRate(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("unitspersecond")) 
      theValue=n/(1.0d/1.0d);
    else if (u.equals("unitsperminute")) 
      theValue=n/(1.0d/(1.0d/60));
    else if (u.equals("unitsperhour")) 
      theValue=n/(1.0d/(1.0d/3600));
    else if (u.equals("unitsperday")) 
      theValue=n/(1.0d/(1.0d/86400));
    else if (u.equals("unitsperweek")) 
      theValue=n/(1.0d/(1.0d/604800));
    else if (u.equals("unitspermillisecond")) 
      theValue=n/(1.0d/1000);
    else if (u.equals("unitsperkilosecond")) 
      theValue=n/(1.0d/(1.0d/1000));
    else if (u.equals("unitspermonth")) 
      theValue=n/(1.0d/(1.0d/2629743.8));
    else if (u.equals("unitsperyear")) 
      theValue=n/(1.0d/(1.0d/31556926));
    else if (u.equals("unitsperfortnight")) 
      theValue=n/(1.0d/(1.0d/1209600));
    else if (u.equals("eachespersecond")) 
      theValue=n/(1.0/1.0d);
    else if (u.equals("eachesperminute")) 
      theValue=n/(1.0/(1.0d/60));
    else if (u.equals("eachesperhour")) 
      theValue=n/(1.0/(1.0d/3600));
    else if (u.equals("eachesperday")) 
      theValue=n/(1.0/(1.0d/86400));
    else if (u.equals("eachesperweek")) 
      theValue=n/(1.0/(1.0d/604800));
    else if (u.equals("eachespermillisecond")) 
      theValue=n/(1.0/1000);
    else if (u.equals("eachesperkilosecond")) 
      theValue=n/(1.0/(1.0d/1000));
    else if (u.equals("eachespermonth")) 
      theValue=n/(1.0/(1.0d/2629743.8));
    else if (u.equals("eachesperyear")) 
      theValue=n/(1.0/(1.0d/31556926));
    else if (u.equals("eachesperfortnight")) 
      theValue=n/(1.0/(1.0d/1209600));
    else if (u.equals("dozenpersecond")) 
      theValue=n/((1.0d/12)/1.0d);
    else if (u.equals("dozenperminute")) 
      theValue=n/((1.0d/12)/(1.0d/60));
    else if (u.equals("dozenperhour")) 
      theValue=n/((1.0d/12)/(1.0d/3600));
    else if (u.equals("dozenperday")) 
      theValue=n/((1.0d/12)/(1.0d/86400));
    else if (u.equals("dozenperweek")) 
      theValue=n/((1.0d/12)/(1.0d/604800));
    else if (u.equals("dozenpermillisecond")) 
      theValue=n/((1.0d/12)/1000);
    else if (u.equals("dozenperkilosecond")) 
      theValue=n/((1.0d/12)/(1.0d/1000));
    else if (u.equals("dozenpermonth")) 
      theValue=n/((1.0d/12)/(1.0d/2629743.8));
    else if (u.equals("dozenperyear")) 
      theValue=n/((1.0d/12)/(1.0d/31556926));
    else if (u.equals("dozenperfortnight")) 
      theValue=n/((1.0d/12)/(1.0d/1209600));
    else if (u.equals("hundredpersecond")) 
      theValue=n/((1.0d/100)/1.0d);
    else if (u.equals("hundredperminute")) 
      theValue=n/((1.0d/100)/(1.0d/60));
    else if (u.equals("hundredperhour")) 
      theValue=n/((1.0d/100)/(1.0d/3600));
    else if (u.equals("hundredperday")) 
      theValue=n/((1.0d/100)/(1.0d/86400));
    else if (u.equals("hundredperweek")) 
      theValue=n/((1.0d/100)/(1.0d/604800));
    else if (u.equals("hundredpermillisecond")) 
      theValue=n/((1.0d/100)/1000);
    else if (u.equals("hundredperkilosecond")) 
      theValue=n/((1.0d/100)/(1.0d/1000));
    else if (u.equals("hundredpermonth")) 
      theValue=n/((1.0d/100)/(1.0d/2629743.8));
    else if (u.equals("hundredperyear")) 
      theValue=n/((1.0d/100)/(1.0d/31556926));
    else if (u.equals("hundredperfortnight")) 
      theValue=n/((1.0d/100)/(1.0d/1209600));
    else if (u.equals("grosspersecond")) 
      theValue=n/((1.0d/144)/1.0d);
    else if (u.equals("grossperminute")) 
      theValue=n/((1.0d/144)/(1.0d/60));
    else if (u.equals("grossperhour")) 
      theValue=n/((1.0d/144)/(1.0d/3600));
    else if (u.equals("grossperday")) 
      theValue=n/((1.0d/144)/(1.0d/86400));
    else if (u.equals("grossperweek")) 
      theValue=n/((1.0d/144)/(1.0d/604800));
    else if (u.equals("grosspermillisecond")) 
      theValue=n/((1.0d/144)/1000);
    else if (u.equals("grossperkilosecond")) 
      theValue=n/((1.0d/144)/(1.0d/1000));
    else if (u.equals("grosspermonth")) 
      theValue=n/((1.0d/144)/(1.0d/2629743.8));
    else if (u.equals("grossperyear")) 
      theValue=n/((1.0d/144)/(1.0d/31556926));
    else if (u.equals("grossperfortnight")) 
      theValue=n/((1.0d/144)/(1.0d/1209600));
    else if (u.equals("millionpersecond")) 
      theValue=n/((1.0d/1000000)/1.0d);
    else if (u.equals("millionperminute")) 
      theValue=n/((1.0d/1000000)/(1.0d/60));
    else if (u.equals("millionperhour")) 
      theValue=n/((1.0d/1000000)/(1.0d/3600));
    else if (u.equals("millionperday")) 
      theValue=n/((1.0d/1000000)/(1.0d/86400));
    else if (u.equals("millionperweek")) 
      theValue=n/((1.0d/1000000)/(1.0d/604800));
    else if (u.equals("millionpermillisecond")) 
      theValue=n/((1.0d/1000000)/1000);
    else if (u.equals("millionperkilosecond")) 
      theValue=n/((1.0d/1000000)/(1.0d/1000));
    else if (u.equals("millionpermonth")) 
      theValue=n/((1.0d/1000000)/(1.0d/2629743.8));
    else if (u.equals("millionperyear")) 
      theValue=n/((1.0d/1000000)/(1.0d/31556926));
    else if (u.equals("millionperfortnight")) 
      theValue=n/((1.0d/1000000)/(1.0d/1209600));
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final CountRate newUnitsPerSecond(double v) {
    return new CountRate(v*(1.0d/(1.0d/1.0d)));
  }
  public static final CountRate newUnitsPerSecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1.0d)));
  }
  public static final CountRate newUnitsPerMinute(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final CountRate newUnitsPerMinute(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final CountRate newUnitsPerHour(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final CountRate newUnitsPerHour(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final CountRate newUnitsPerDay(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final CountRate newUnitsPerDay(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final CountRate newUnitsPerWeek(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final CountRate newUnitsPerWeek(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final CountRate newUnitsPerMillisecond(double v) {
    return new CountRate(v*(1.0d/(1.0d/1000)));
  }
  public static final CountRate newUnitsPerMillisecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1000)));
  }
  public static final CountRate newUnitsPerKilosecond(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final CountRate newUnitsPerKilosecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final CountRate newUnitsPerMonth(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final CountRate newUnitsPerMonth(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final CountRate newUnitsPerYear(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final CountRate newUnitsPerYear(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final CountRate newUnitsPerFortnight(double v) {
    return new CountRate(v*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final CountRate newUnitsPerFortnight(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final CountRate newEachesPerSecond(double v) {
    return new CountRate(v*(1.0d/(1.0/1.0d)));
  }
  public static final CountRate newEachesPerSecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/1.0d)));
  }
  public static final CountRate newEachesPerMinute(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/60))));
  }
  public static final CountRate newEachesPerMinute(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/60))));
  }
  public static final CountRate newEachesPerHour(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/3600))));
  }
  public static final CountRate newEachesPerHour(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/3600))));
  }
  public static final CountRate newEachesPerDay(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/86400))));
  }
  public static final CountRate newEachesPerDay(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/86400))));
  }
  public static final CountRate newEachesPerWeek(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/604800))));
  }
  public static final CountRate newEachesPerWeek(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/604800))));
  }
  public static final CountRate newEachesPerMillisecond(double v) {
    return new CountRate(v*(1.0d/(1.0/1000)));
  }
  public static final CountRate newEachesPerMillisecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/1000)));
  }
  public static final CountRate newEachesPerKilosecond(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/1000))));
  }
  public static final CountRate newEachesPerKilosecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/1000))));
  }
  public static final CountRate newEachesPerMonth(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/2629743.8))));
  }
  public static final CountRate newEachesPerMonth(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/2629743.8))));
  }
  public static final CountRate newEachesPerYear(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/31556926))));
  }
  public static final CountRate newEachesPerYear(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/31556926))));
  }
  public static final CountRate newEachesPerFortnight(double v) {
    return new CountRate(v*(1.0d/(1.0/(1.0d/1209600))));
  }
  public static final CountRate newEachesPerFortnight(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0/(1.0d/1209600))));
  }
  public static final CountRate newDozenPerSecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/1.0d)));
  }
  public static final CountRate newDozenPerSecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/1.0d)));
  }
  public static final CountRate newDozenPerMinute(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/60))));
  }
  public static final CountRate newDozenPerMinute(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/60))));
  }
  public static final CountRate newDozenPerHour(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/3600))));
  }
  public static final CountRate newDozenPerHour(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/3600))));
  }
  public static final CountRate newDozenPerDay(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/86400))));
  }
  public static final CountRate newDozenPerDay(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/86400))));
  }
  public static final CountRate newDozenPerWeek(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/604800))));
  }
  public static final CountRate newDozenPerWeek(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/604800))));
  }
  public static final CountRate newDozenPerMillisecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/1000)));
  }
  public static final CountRate newDozenPerMillisecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/1000)));
  }
  public static final CountRate newDozenPerKilosecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/1000))));
  }
  public static final CountRate newDozenPerKilosecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/1000))));
  }
  public static final CountRate newDozenPerMonth(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/2629743.8))));
  }
  public static final CountRate newDozenPerMonth(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/2629743.8))));
  }
  public static final CountRate newDozenPerYear(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/31556926))));
  }
  public static final CountRate newDozenPerYear(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/31556926))));
  }
  public static final CountRate newDozenPerFortnight(double v) {
    return new CountRate(v*(1.0d/((1.0d/12)/(1.0d/1209600))));
  }
  public static final CountRate newDozenPerFortnight(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/12)/(1.0d/1209600))));
  }
  public static final CountRate newHundredPerSecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/1.0d)));
  }
  public static final CountRate newHundredPerSecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/1.0d)));
  }
  public static final CountRate newHundredPerMinute(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/60))));
  }
  public static final CountRate newHundredPerMinute(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/60))));
  }
  public static final CountRate newHundredPerHour(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/3600))));
  }
  public static final CountRate newHundredPerHour(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/3600))));
  }
  public static final CountRate newHundredPerDay(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/86400))));
  }
  public static final CountRate newHundredPerDay(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/86400))));
  }
  public static final CountRate newHundredPerWeek(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/604800))));
  }
  public static final CountRate newHundredPerWeek(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/604800))));
  }
  public static final CountRate newHundredPerMillisecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/1000)));
  }
  public static final CountRate newHundredPerMillisecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/1000)));
  }
  public static final CountRate newHundredPerKilosecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/1000))));
  }
  public static final CountRate newHundredPerKilosecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/1000))));
  }
  public static final CountRate newHundredPerMonth(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/2629743.8))));
  }
  public static final CountRate newHundredPerMonth(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/2629743.8))));
  }
  public static final CountRate newHundredPerYear(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/31556926))));
  }
  public static final CountRate newHundredPerYear(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/31556926))));
  }
  public static final CountRate newHundredPerFortnight(double v) {
    return new CountRate(v*(1.0d/((1.0d/100)/(1.0d/1209600))));
  }
  public static final CountRate newHundredPerFortnight(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/100)/(1.0d/1209600))));
  }
  public static final CountRate newGrossPerSecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/1.0d)));
  }
  public static final CountRate newGrossPerSecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/1.0d)));
  }
  public static final CountRate newGrossPerMinute(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/60))));
  }
  public static final CountRate newGrossPerMinute(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/60))));
  }
  public static final CountRate newGrossPerHour(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/3600))));
  }
  public static final CountRate newGrossPerHour(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/3600))));
  }
  public static final CountRate newGrossPerDay(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/86400))));
  }
  public static final CountRate newGrossPerDay(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/86400))));
  }
  public static final CountRate newGrossPerWeek(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/604800))));
  }
  public static final CountRate newGrossPerWeek(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/604800))));
  }
  public static final CountRate newGrossPerMillisecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/1000)));
  }
  public static final CountRate newGrossPerMillisecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/1000)));
  }
  public static final CountRate newGrossPerKilosecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/1000))));
  }
  public static final CountRate newGrossPerKilosecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/1000))));
  }
  public static final CountRate newGrossPerMonth(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/2629743.8))));
  }
  public static final CountRate newGrossPerMonth(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/2629743.8))));
  }
  public static final CountRate newGrossPerYear(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/31556926))));
  }
  public static final CountRate newGrossPerYear(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/31556926))));
  }
  public static final CountRate newGrossPerFortnight(double v) {
    return new CountRate(v*(1.0d/((1.0d/144)/(1.0d/1209600))));
  }
  public static final CountRate newGrossPerFortnight(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/144)/(1.0d/1209600))));
  }
  public static final CountRate newMillionPerSecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/1.0d)));
  }
  public static final CountRate newMillionPerSecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/1.0d)));
  }
  public static final CountRate newMillionPerMinute(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/60))));
  }
  public static final CountRate newMillionPerMinute(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/60))));
  }
  public static final CountRate newMillionPerHour(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/3600))));
  }
  public static final CountRate newMillionPerHour(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/3600))));
  }
  public static final CountRate newMillionPerDay(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/86400))));
  }
  public static final CountRate newMillionPerDay(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/86400))));
  }
  public static final CountRate newMillionPerWeek(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/604800))));
  }
  public static final CountRate newMillionPerWeek(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/604800))));
  }
  public static final CountRate newMillionPerMillisecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/1000)));
  }
  public static final CountRate newMillionPerMillisecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/1000)));
  }
  public static final CountRate newMillionPerKilosecond(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/1000))));
  }
  public static final CountRate newMillionPerKilosecond(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/1000))));
  }
  public static final CountRate newMillionPerMonth(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/2629743.8))));
  }
  public static final CountRate newMillionPerMonth(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/2629743.8))));
  }
  public static final CountRate newMillionPerYear(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/31556926))));
  }
  public static final CountRate newMillionPerYear(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/31556926))));
  }
  public static final CountRate newMillionPerFortnight(double v) {
    return new CountRate(v*(1.0d/((1.0d/1000000)/(1.0d/1209600))));
  }
  public static final CountRate newMillionPerFortnight(String s) {
    return new CountRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000000)/(1.0d/1209600))));
  }


  public int getCommonUnit() {
    return 0;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "units/second",
    "units/minute",
    "units/hour",
    "units/day",
    "units/week",
    "units/millisecond",
    "units/kilosecond",
    "units/month",
    "units/year",
    "units/fortnight",
    "eaches/second",
    "eaches/minute",
    "eaches/hour",
    "eaches/day",
    "eaches/week",
    "eaches/millisecond",
    "eaches/kilosecond",
    "eaches/month",
    "eaches/year",
    "eaches/fortnight",
    "dozen/second",
    "dozen/minute",
    "dozen/hour",
    "dozen/day",
    "dozen/week",
    "dozen/millisecond",
    "dozen/kilosecond",
    "dozen/month",
    "dozen/year",
    "dozen/fortnight",
    "hundred/second",
    "hundred/minute",
    "hundred/hour",
    "hundred/day",
    "hundred/week",
    "hundred/millisecond",
    "hundred/kilosecond",
    "hundred/month",
    "hundred/year",
    "hundred/fortnight",
    "gross/second",
    "gross/minute",
    "gross/hour",
    "gross/day",
    "gross/week",
    "gross/millisecond",
    "gross/kilosecond",
    "gross/month",
    "gross/year",
    "gross/fortnight",
    "million/second",
    "million/minute",
    "million/hour",
    "million/day",
    "million/week",
    "million/millisecond",
    "million/kilosecond",
    "million/month",
    "million/year",
    "million/fortnight",
  };

  /** @param unit One of the constant units of CountRate **/
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
    (1.0/1.0d),
    (1.0/(1.0d/60)),
    (1.0/(1.0d/3600)),
    (1.0/(1.0d/86400)),
    (1.0/(1.0d/604800)),
    (1.0/1000),
    (1.0/(1.0d/1000)),
    (1.0/(1.0d/2629743.8)),
    (1.0/(1.0d/31556926)),
    (1.0/(1.0d/1209600)),
    ((1.0d/12)/1.0d),
    ((1.0d/12)/(1.0d/60)),
    ((1.0d/12)/(1.0d/3600)),
    ((1.0d/12)/(1.0d/86400)),
    ((1.0d/12)/(1.0d/604800)),
    ((1.0d/12)/1000),
    ((1.0d/12)/(1.0d/1000)),
    ((1.0d/12)/(1.0d/2629743.8)),
    ((1.0d/12)/(1.0d/31556926)),
    ((1.0d/12)/(1.0d/1209600)),
    ((1.0d/100)/1.0d),
    ((1.0d/100)/(1.0d/60)),
    ((1.0d/100)/(1.0d/3600)),
    ((1.0d/100)/(1.0d/86400)),
    ((1.0d/100)/(1.0d/604800)),
    ((1.0d/100)/1000),
    ((1.0d/100)/(1.0d/1000)),
    ((1.0d/100)/(1.0d/2629743.8)),
    ((1.0d/100)/(1.0d/31556926)),
    ((1.0d/100)/(1.0d/1209600)),
    ((1.0d/144)/1.0d),
    ((1.0d/144)/(1.0d/60)),
    ((1.0d/144)/(1.0d/3600)),
    ((1.0d/144)/(1.0d/86400)),
    ((1.0d/144)/(1.0d/604800)),
    ((1.0d/144)/1000),
    ((1.0d/144)/(1.0d/1000)),
    ((1.0d/144)/(1.0d/2629743.8)),
    ((1.0d/144)/(1.0d/31556926)),
    ((1.0d/144)/(1.0d/1209600)),
    ((1.0d/1000000)/1.0d),
    ((1.0d/1000000)/(1.0d/60)),
    ((1.0d/1000000)/(1.0d/3600)),
    ((1.0d/1000000)/(1.0d/86400)),
    ((1.0d/1000000)/(1.0d/604800)),
    ((1.0d/1000000)/1000),
    ((1.0d/1000000)/(1.0d/1000)),
    ((1.0d/1000000)/(1.0d/2629743.8)),
    ((1.0d/1000000)/(1.0d/31556926)),
    ((1.0d/1000000)/(1.0d/1209600)),
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int UNITS_PER_SECOND = 0;
  public static final int UNITS_PER_MINUTE = 1;
  public static final int UNITS_PER_HOUR = 2;
  public static final int UNITS_PER_DAY = 3;
  public static final int UNITS_PER_WEEK = 4;
  public static final int UNITS_PER_MILLISECOND = 5;
  public static final int UNITS_PER_KILOSECOND = 6;
  public static final int UNITS_PER_MONTH = 7;
  public static final int UNITS_PER_YEAR = 8;
  public static final int UNITS_PER_FORTNIGHT = 9;
  public static final int EACHES_PER_SECOND = 10;
  public static final int EACHES_PER_MINUTE = 11;
  public static final int EACHES_PER_HOUR = 12;
  public static final int EACHES_PER_DAY = 13;
  public static final int EACHES_PER_WEEK = 14;
  public static final int EACHES_PER_MILLISECOND = 15;
  public static final int EACHES_PER_KILOSECOND = 16;
  public static final int EACHES_PER_MONTH = 17;
  public static final int EACHES_PER_YEAR = 18;
  public static final int EACHES_PER_FORTNIGHT = 19;
  public static final int DOZEN_PER_SECOND = 20;
  public static final int DOZEN_PER_MINUTE = 21;
  public static final int DOZEN_PER_HOUR = 22;
  public static final int DOZEN_PER_DAY = 23;
  public static final int DOZEN_PER_WEEK = 24;
  public static final int DOZEN_PER_MILLISECOND = 25;
  public static final int DOZEN_PER_KILOSECOND = 26;
  public static final int DOZEN_PER_MONTH = 27;
  public static final int DOZEN_PER_YEAR = 28;
  public static final int DOZEN_PER_FORTNIGHT = 29;
  public static final int HUNDRED_PER_SECOND = 30;
  public static final int HUNDRED_PER_MINUTE = 31;
  public static final int HUNDRED_PER_HOUR = 32;
  public static final int HUNDRED_PER_DAY = 33;
  public static final int HUNDRED_PER_WEEK = 34;
  public static final int HUNDRED_PER_MILLISECOND = 35;
  public static final int HUNDRED_PER_KILOSECOND = 36;
  public static final int HUNDRED_PER_MONTH = 37;
  public static final int HUNDRED_PER_YEAR = 38;
  public static final int HUNDRED_PER_FORTNIGHT = 39;
  public static final int GROSS_PER_SECOND = 40;
  public static final int GROSS_PER_MINUTE = 41;
  public static final int GROSS_PER_HOUR = 42;
  public static final int GROSS_PER_DAY = 43;
  public static final int GROSS_PER_WEEK = 44;
  public static final int GROSS_PER_MILLISECOND = 45;
  public static final int GROSS_PER_KILOSECOND = 46;
  public static final int GROSS_PER_MONTH = 47;
  public static final int GROSS_PER_YEAR = 48;
  public static final int GROSS_PER_FORTNIGHT = 49;
  public static final int MILLION_PER_SECOND = 50;
  public static final int MILLION_PER_MINUTE = 51;
  public static final int MILLION_PER_HOUR = 52;
  public static final int MILLION_PER_DAY = 53;
  public static final int MILLION_PER_WEEK = 54;
  public static final int MILLION_PER_MILLISECOND = 55;
  public static final int MILLION_PER_KILOSECOND = 56;
  public static final int MILLION_PER_MONTH = 57;
  public static final int MILLION_PER_YEAR = 58;
  public static final int MILLION_PER_FORTNIGHT = 59;
  static final int MAXUNIT = 59;

  // Index Typed factory methods
  /** @param unit One of the constant units of CountRate **/
  public static final CountRate newCountRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new CountRate(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit One of the constant units of CountRate **/
  public static final CountRate newCountRate(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new CountRate((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Index Typed factory methods
  /** @param unit1 One of the constant units of Count
   *  @param unit2 One of the constant units of Duration
   **/
  public static final CountRate newCountRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Count.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new CountRate(v*Count.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Count to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public static final CountRate newCountRate(Count num, Duration den) {
    return new CountRate(num.getValue(0)/den.getValue(0));
  }

  /** @param unit1 One of the constant units of Count
   *  @param unit2 One of the constant units of Duration
   **/
  public static final CountRate newCountRate(String s, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Count.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new CountRate((Double.valueOf(s).doubleValue())*Count.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newCountRate(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newCountRate(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof CountRate)) throw new IllegalArgumentException();
    return new CountRate(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof CountRate)) throw new IllegalArgumentException();
    return new CountRate(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new CountRate(theValue*scale,0);
  }

  public final Measure negate() {
    return newCountRate(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newCountRate(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new CountRate(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new CountRate(value, unit);
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
      throw new IllegalArgumentException("Expecting a CountRate" + 
      ", got a " + toRate.getCanonicalNumerator().getClass() + "/" + toRate.getCanonicalDenominator().getClass());
    }
    return theValue/toRate.getNativeValue();
  }

  // Unit-based Reader methods
  public double getUnitsPerSecond() {
    return (theValue*(1.0d/1.0d));
  }
  public double getUnitsPerMinute() {
    return (theValue*(1.0d/(1.0d/60)));
  }
  public double getUnitsPerHour() {
    return (theValue*(1.0d/(1.0d/3600)));
  }
  public double getUnitsPerDay() {
    return (theValue*(1.0d/(1.0d/86400)));
  }
  public double getUnitsPerWeek() {
    return (theValue*(1.0d/(1.0d/604800)));
  }
  public double getUnitsPerMillisecond() {
    return (theValue*(1.0d/1000));
  }
  public double getUnitsPerKilosecond() {
    return (theValue*(1.0d/(1.0d/1000)));
  }
  public double getUnitsPerMonth() {
    return (theValue*(1.0d/(1.0d/2629743.8)));
  }
  public double getUnitsPerYear() {
    return (theValue*(1.0d/(1.0d/31556926)));
  }
  public double getUnitsPerFortnight() {
    return (theValue*(1.0d/(1.0d/1209600)));
  }
  public double getEachesPerSecond() {
    return (theValue*(1.0/1.0d));
  }
  public double getEachesPerMinute() {
    return (theValue*(1.0/(1.0d/60)));
  }
  public double getEachesPerHour() {
    return (theValue*(1.0/(1.0d/3600)));
  }
  public double getEachesPerDay() {
    return (theValue*(1.0/(1.0d/86400)));
  }
  public double getEachesPerWeek() {
    return (theValue*(1.0/(1.0d/604800)));
  }
  public double getEachesPerMillisecond() {
    return (theValue*(1.0/1000));
  }
  public double getEachesPerKilosecond() {
    return (theValue*(1.0/(1.0d/1000)));
  }
  public double getEachesPerMonth() {
    return (theValue*(1.0/(1.0d/2629743.8)));
  }
  public double getEachesPerYear() {
    return (theValue*(1.0/(1.0d/31556926)));
  }
  public double getEachesPerFortnight() {
    return (theValue*(1.0/(1.0d/1209600)));
  }
  public double getDozenPerSecond() {
    return (theValue*((1.0d/12)/1.0d));
  }
  public double getDozenPerMinute() {
    return (theValue*((1.0d/12)/(1.0d/60)));
  }
  public double getDozenPerHour() {
    return (theValue*((1.0d/12)/(1.0d/3600)));
  }
  public double getDozenPerDay() {
    return (theValue*((1.0d/12)/(1.0d/86400)));
  }
  public double getDozenPerWeek() {
    return (theValue*((1.0d/12)/(1.0d/604800)));
  }
  public double getDozenPerMillisecond() {
    return (theValue*((1.0d/12)/1000));
  }
  public double getDozenPerKilosecond() {
    return (theValue*((1.0d/12)/(1.0d/1000)));
  }
  public double getDozenPerMonth() {
    return (theValue*((1.0d/12)/(1.0d/2629743.8)));
  }
  public double getDozenPerYear() {
    return (theValue*((1.0d/12)/(1.0d/31556926)));
  }
  public double getDozenPerFortnight() {
    return (theValue*((1.0d/12)/(1.0d/1209600)));
  }
  public double getHundredPerSecond() {
    return (theValue*((1.0d/100)/1.0d));
  }
  public double getHundredPerMinute() {
    return (theValue*((1.0d/100)/(1.0d/60)));
  }
  public double getHundredPerHour() {
    return (theValue*((1.0d/100)/(1.0d/3600)));
  }
  public double getHundredPerDay() {
    return (theValue*((1.0d/100)/(1.0d/86400)));
  }
  public double getHundredPerWeek() {
    return (theValue*((1.0d/100)/(1.0d/604800)));
  }
  public double getHundredPerMillisecond() {
    return (theValue*((1.0d/100)/1000));
  }
  public double getHundredPerKilosecond() {
    return (theValue*((1.0d/100)/(1.0d/1000)));
  }
  public double getHundredPerMonth() {
    return (theValue*((1.0d/100)/(1.0d/2629743.8)));
  }
  public double getHundredPerYear() {
    return (theValue*((1.0d/100)/(1.0d/31556926)));
  }
  public double getHundredPerFortnight() {
    return (theValue*((1.0d/100)/(1.0d/1209600)));
  }
  public double getGrossPerSecond() {
    return (theValue*((1.0d/144)/1.0d));
  }
  public double getGrossPerMinute() {
    return (theValue*((1.0d/144)/(1.0d/60)));
  }
  public double getGrossPerHour() {
    return (theValue*((1.0d/144)/(1.0d/3600)));
  }
  public double getGrossPerDay() {
    return (theValue*((1.0d/144)/(1.0d/86400)));
  }
  public double getGrossPerWeek() {
    return (theValue*((1.0d/144)/(1.0d/604800)));
  }
  public double getGrossPerMillisecond() {
    return (theValue*((1.0d/144)/1000));
  }
  public double getGrossPerKilosecond() {
    return (theValue*((1.0d/144)/(1.0d/1000)));
  }
  public double getGrossPerMonth() {
    return (theValue*((1.0d/144)/(1.0d/2629743.8)));
  }
  public double getGrossPerYear() {
    return (theValue*((1.0d/144)/(1.0d/31556926)));
  }
  public double getGrossPerFortnight() {
    return (theValue*((1.0d/144)/(1.0d/1209600)));
  }
  public double getMillionPerSecond() {
    return (theValue*((1.0d/1000000)/1.0d));
  }
  public double getMillionPerMinute() {
    return (theValue*((1.0d/1000000)/(1.0d/60)));
  }
  public double getMillionPerHour() {
    return (theValue*((1.0d/1000000)/(1.0d/3600)));
  }
  public double getMillionPerDay() {
    return (theValue*((1.0d/1000000)/(1.0d/86400)));
  }
  public double getMillionPerWeek() {
    return (theValue*((1.0d/1000000)/(1.0d/604800)));
  }
  public double getMillionPerMillisecond() {
    return (theValue*((1.0d/1000000)/1000));
  }
  public double getMillionPerKilosecond() {
    return (theValue*((1.0d/1000000)/(1.0d/1000)));
  }
  public double getMillionPerMonth() {
    return (theValue*((1.0d/1000000)/(1.0d/2629743.8)));
  }
  public double getMillionPerYear() {
    return (theValue*((1.0d/1000000)/(1.0d/31556926)));
  }
  public double getMillionPerFortnight() {
    return (theValue*((1.0d/1000000)/(1.0d/1209600)));
  }

  /** @param unit One of the constant units of CountRate **/
  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Count
   *  @param unit2 One of the constant units of Duration
   **/
  public double getValue(int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Count.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return (theValue*Duration.getConvFactor(unit2)/Count.getConvFactor(unit1));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof CountRate &&
             theValue == ((CountRate) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "units/s";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // Derivative
  public final Class getNumeratorClass() { return Count.class; }
  public final Class getDenominatorClass() { return Duration.class; }

  private final static Count can_num = new Count(0.0,0);
  public final Measure getCanonicalNumerator() { return can_num; }
  private final static Duration can_den = new Duration(0.0,0);
  public final Measure getCanonicalDenominator() { return can_den; }
  public final Measure computeNumerator(Measure den) {
    if (!(den instanceof Duration)) throw new IllegalArgumentException();
    return new Count(theValue*den.getValue(0),0);
  }
  public final Measure computeDenominator(Measure num) {
    if (!(num instanceof Count)) throw new IllegalArgumentException();
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
