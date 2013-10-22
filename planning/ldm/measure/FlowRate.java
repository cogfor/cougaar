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
/** Immutable implementation of FlowRate.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class FlowRate extends AbstractMeasure
  implements Externalizable, Derivative, Rate {
  // the value is stored as liters/second
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public FlowRate() {}

  // private constructor
  private FlowRate(double v) {
    theValue = v;
  }

  /** @param unit One of the constant units of FlowRate **/
  public FlowRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v/getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Volume
   *  @param unit2 One of the constant units of Duration
   **/
  public FlowRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Volume.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      theValue = v*Volume.getConvFactor(unit1)/Duration.getConvFactor(unit2);
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Volume to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public FlowRate(Volume num, Duration den) {
    theValue = num.getValue(0)/den.getValue(0);
  }

  /** takes strings of the form "Number unit" **/
  public FlowRate(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("literspersecond")) 
      theValue=n/(1.0d/1.0d);
    else if (u.equals("litersperminute")) 
      theValue=n/(1.0d/(1.0d/60));
    else if (u.equals("litersperhour")) 
      theValue=n/(1.0d/(1.0d/3600));
    else if (u.equals("litersperday")) 
      theValue=n/(1.0d/(1.0d/86400));
    else if (u.equals("litersperweek")) 
      theValue=n/(1.0d/(1.0d/604800));
    else if (u.equals("literspermillisecond")) 
      theValue=n/(1.0d/1000);
    else if (u.equals("litersperkilosecond")) 
      theValue=n/(1.0d/(1.0d/1000));
    else if (u.equals("literspermonth")) 
      theValue=n/(1.0d/(1.0d/2629743.8));
    else if (u.equals("litersperyear")) 
      theValue=n/(1.0d/(1.0d/31556926));
    else if (u.equals("litersperfortnight")) 
      theValue=n/(1.0d/(1.0d/1209600));
    else if (u.equals("ouncespersecond")) 
      theValue=n/(33.814023/1.0d);
    else if (u.equals("ouncesperminute")) 
      theValue=n/(33.814023/(1.0d/60));
    else if (u.equals("ouncesperhour")) 
      theValue=n/(33.814023/(1.0d/3600));
    else if (u.equals("ouncesperday")) 
      theValue=n/(33.814023/(1.0d/86400));
    else if (u.equals("ouncesperweek")) 
      theValue=n/(33.814023/(1.0d/604800));
    else if (u.equals("ouncespermillisecond")) 
      theValue=n/(33.814023/1000);
    else if (u.equals("ouncesperkilosecond")) 
      theValue=n/(33.814023/(1.0d/1000));
    else if (u.equals("ouncespermonth")) 
      theValue=n/(33.814023/(1.0d/2629743.8));
    else if (u.equals("ouncesperyear")) 
      theValue=n/(33.814023/(1.0d/31556926));
    else if (u.equals("ouncesperfortnight")) 
      theValue=n/(33.814023/(1.0d/1209600));
    else if (u.equals("gallonspersecond")) 
      theValue=n/((1.0d/3.785412)/1.0d);
    else if (u.equals("gallonsperminute")) 
      theValue=n/((1.0d/3.785412)/(1.0d/60));
    else if (u.equals("gallonsperhour")) 
      theValue=n/((1.0d/3.785412)/(1.0d/3600));
    else if (u.equals("gallonsperday")) 
      theValue=n/((1.0d/3.785412)/(1.0d/86400));
    else if (u.equals("gallonsperweek")) 
      theValue=n/((1.0d/3.785412)/(1.0d/604800));
    else if (u.equals("gallonspermillisecond")) 
      theValue=n/((1.0d/3.785412)/1000);
    else if (u.equals("gallonsperkilosecond")) 
      theValue=n/((1.0d/3.785412)/(1.0d/1000));
    else if (u.equals("gallonspermonth")) 
      theValue=n/((1.0d/3.785412)/(1.0d/2629743.8));
    else if (u.equals("gallonsperyear")) 
      theValue=n/((1.0d/3.785412)/(1.0d/31556926));
    else if (u.equals("gallonsperfortnight")) 
      theValue=n/((1.0d/3.785412)/(1.0d/1209600));
    else if (u.equals("imperialgallonspersecond")) 
      theValue=n/((1.0d/4.546090)/1.0d);
    else if (u.equals("imperialgallonsperminute")) 
      theValue=n/((1.0d/4.546090)/(1.0d/60));
    else if (u.equals("imperialgallonsperhour")) 
      theValue=n/((1.0d/4.546090)/(1.0d/3600));
    else if (u.equals("imperialgallonsperday")) 
      theValue=n/((1.0d/4.546090)/(1.0d/86400));
    else if (u.equals("imperialgallonsperweek")) 
      theValue=n/((1.0d/4.546090)/(1.0d/604800));
    else if (u.equals("imperialgallonspermillisecond")) 
      theValue=n/((1.0d/4.546090)/1000);
    else if (u.equals("imperialgallonsperkilosecond")) 
      theValue=n/((1.0d/4.546090)/(1.0d/1000));
    else if (u.equals("imperialgallonspermonth")) 
      theValue=n/((1.0d/4.546090)/(1.0d/2629743.8));
    else if (u.equals("imperialgallonsperyear")) 
      theValue=n/((1.0d/4.546090)/(1.0d/31556926));
    else if (u.equals("imperialgallonsperfortnight")) 
      theValue=n/((1.0d/4.546090)/(1.0d/1209600));
    else if (u.equals("cubicfeetpersecond")) 
      theValue=n/((1.0d/28.316847)/1.0d);
    else if (u.equals("cubicfeetperminute")) 
      theValue=n/((1.0d/28.316847)/(1.0d/60));
    else if (u.equals("cubicfeetperhour")) 
      theValue=n/((1.0d/28.316847)/(1.0d/3600));
    else if (u.equals("cubicfeetperday")) 
      theValue=n/((1.0d/28.316847)/(1.0d/86400));
    else if (u.equals("cubicfeetperweek")) 
      theValue=n/((1.0d/28.316847)/(1.0d/604800));
    else if (u.equals("cubicfeetpermillisecond")) 
      theValue=n/((1.0d/28.316847)/1000);
    else if (u.equals("cubicfeetperkilosecond")) 
      theValue=n/((1.0d/28.316847)/(1.0d/1000));
    else if (u.equals("cubicfeetpermonth")) 
      theValue=n/((1.0d/28.316847)/(1.0d/2629743.8));
    else if (u.equals("cubicfeetperyear")) 
      theValue=n/((1.0d/28.316847)/(1.0d/31556926));
    else if (u.equals("cubicfeetperfortnight")) 
      theValue=n/((1.0d/28.316847)/(1.0d/1209600));
    else if (u.equals("cubicyardspersecond")) 
      theValue=n/((1.0d/764.55486)/1.0d);
    else if (u.equals("cubicyardsperminute")) 
      theValue=n/((1.0d/764.55486)/(1.0d/60));
    else if (u.equals("cubicyardsperhour")) 
      theValue=n/((1.0d/764.55486)/(1.0d/3600));
    else if (u.equals("cubicyardsperday")) 
      theValue=n/((1.0d/764.55486)/(1.0d/86400));
    else if (u.equals("cubicyardsperweek")) 
      theValue=n/((1.0d/764.55486)/(1.0d/604800));
    else if (u.equals("cubicyardspermillisecond")) 
      theValue=n/((1.0d/764.55486)/1000);
    else if (u.equals("cubicyardsperkilosecond")) 
      theValue=n/((1.0d/764.55486)/(1.0d/1000));
    else if (u.equals("cubicyardspermonth")) 
      theValue=n/((1.0d/764.55486)/(1.0d/2629743.8));
    else if (u.equals("cubicyardsperyear")) 
      theValue=n/((1.0d/764.55486)/(1.0d/31556926));
    else if (u.equals("cubicyardsperfortnight")) 
      theValue=n/((1.0d/764.55486)/(1.0d/1209600));
    else if (u.equals("mtonspersecond")) 
      theValue=n/((1.0d/1132.67388)/1.0d);
    else if (u.equals("mtonsperminute")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/60));
    else if (u.equals("mtonsperhour")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/3600));
    else if (u.equals("mtonsperday")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/86400));
    else if (u.equals("mtonsperweek")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/604800));
    else if (u.equals("mtonspermillisecond")) 
      theValue=n/((1.0d/1132.67388)/1000);
    else if (u.equals("mtonsperkilosecond")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/1000));
    else if (u.equals("mtonspermonth")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/2629743.8));
    else if (u.equals("mtonsperyear")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/31556926));
    else if (u.equals("mtonsperfortnight")) 
      theValue=n/((1.0d/1132.67388)/(1.0d/1209600));
    else if (u.equals("cubiccentimeterspersecond")) 
      theValue=n/(1000/1.0d);
    else if (u.equals("cubiccentimetersperminute")) 
      theValue=n/(1000/(1.0d/60));
    else if (u.equals("cubiccentimetersperhour")) 
      theValue=n/(1000/(1.0d/3600));
    else if (u.equals("cubiccentimetersperday")) 
      theValue=n/(1000/(1.0d/86400));
    else if (u.equals("cubiccentimetersperweek")) 
      theValue=n/(1000/(1.0d/604800));
    else if (u.equals("cubiccentimeterspermillisecond")) 
      theValue=n/(1000/1000);
    else if (u.equals("cubiccentimetersperkilosecond")) 
      theValue=n/(1000/(1.0d/1000));
    else if (u.equals("cubiccentimeterspermonth")) 
      theValue=n/(1000/(1.0d/2629743.8));
    else if (u.equals("cubiccentimetersperyear")) 
      theValue=n/(1000/(1.0d/31556926));
    else if (u.equals("cubiccentimetersperfortnight")) 
      theValue=n/(1000/(1.0d/1209600));
    else if (u.equals("cubicmeterspersecond")) 
      theValue=n/((1.0d/1000)/1.0d);
    else if (u.equals("cubicmetersperminute")) 
      theValue=n/((1.0d/1000)/(1.0d/60));
    else if (u.equals("cubicmetersperhour")) 
      theValue=n/((1.0d/1000)/(1.0d/3600));
    else if (u.equals("cubicmetersperday")) 
      theValue=n/((1.0d/1000)/(1.0d/86400));
    else if (u.equals("cubicmetersperweek")) 
      theValue=n/((1.0d/1000)/(1.0d/604800));
    else if (u.equals("cubicmeterspermillisecond")) 
      theValue=n/((1.0d/1000)/1000);
    else if (u.equals("cubicmetersperkilosecond")) 
      theValue=n/((1.0d/1000)/(1.0d/1000));
    else if (u.equals("cubicmeterspermonth")) 
      theValue=n/((1.0d/1000)/(1.0d/2629743.8));
    else if (u.equals("cubicmetersperyear")) 
      theValue=n/((1.0d/1000)/(1.0d/31556926));
    else if (u.equals("cubicmetersperfortnight")) 
      theValue=n/((1.0d/1000)/(1.0d/1209600));
    else if (u.equals("barrelspersecond")) 
      theValue=n/((1.0d/158.98729)/1.0d);
    else if (u.equals("barrelsperminute")) 
      theValue=n/((1.0d/158.98729)/(1.0d/60));
    else if (u.equals("barrelsperhour")) 
      theValue=n/((1.0d/158.98729)/(1.0d/3600));
    else if (u.equals("barrelsperday")) 
      theValue=n/((1.0d/158.98729)/(1.0d/86400));
    else if (u.equals("barrelsperweek")) 
      theValue=n/((1.0d/158.98729)/(1.0d/604800));
    else if (u.equals("barrelspermillisecond")) 
      theValue=n/((1.0d/158.98729)/1000);
    else if (u.equals("barrelsperkilosecond")) 
      theValue=n/((1.0d/158.98729)/(1.0d/1000));
    else if (u.equals("barrelspermonth")) 
      theValue=n/((1.0d/158.98729)/(1.0d/2629743.8));
    else if (u.equals("barrelsperyear")) 
      theValue=n/((1.0d/158.98729)/(1.0d/31556926));
    else if (u.equals("barrelsperfortnight")) 
      theValue=n/((1.0d/158.98729)/(1.0d/1209600));
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final FlowRate newLitersPerSecond(double v) {
    return new FlowRate(v*(1.0d/(1.0d/1.0d)));
  }
  public static final FlowRate newLitersPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1.0d)));
  }
  public static final FlowRate newLitersPerMinute(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final FlowRate newLitersPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final FlowRate newLitersPerHour(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final FlowRate newLitersPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final FlowRate newLitersPerDay(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final FlowRate newLitersPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final FlowRate newLitersPerWeek(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final FlowRate newLitersPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final FlowRate newLitersPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/(1.0d/1000)));
  }
  public static final FlowRate newLitersPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1000)));
  }
  public static final FlowRate newLitersPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final FlowRate newLitersPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final FlowRate newLitersPerMonth(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final FlowRate newLitersPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final FlowRate newLitersPerYear(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final FlowRate newLitersPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final FlowRate newLitersPerFortnight(double v) {
    return new FlowRate(v*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final FlowRate newLitersPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final FlowRate newOuncesPerSecond(double v) {
    return new FlowRate(v*(1.0d/(33.814023/1.0d)));
  }
  public static final FlowRate newOuncesPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/1.0d)));
  }
  public static final FlowRate newOuncesPerMinute(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/60))));
  }
  public static final FlowRate newOuncesPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/60))));
  }
  public static final FlowRate newOuncesPerHour(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/3600))));
  }
  public static final FlowRate newOuncesPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/3600))));
  }
  public static final FlowRate newOuncesPerDay(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/86400))));
  }
  public static final FlowRate newOuncesPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/86400))));
  }
  public static final FlowRate newOuncesPerWeek(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/604800))));
  }
  public static final FlowRate newOuncesPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/604800))));
  }
  public static final FlowRate newOuncesPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/(33.814023/1000)));
  }
  public static final FlowRate newOuncesPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/1000)));
  }
  public static final FlowRate newOuncesPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/1000))));
  }
  public static final FlowRate newOuncesPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/1000))));
  }
  public static final FlowRate newOuncesPerMonth(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/2629743.8))));
  }
  public static final FlowRate newOuncesPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/2629743.8))));
  }
  public static final FlowRate newOuncesPerYear(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/31556926))));
  }
  public static final FlowRate newOuncesPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/31556926))));
  }
  public static final FlowRate newOuncesPerFortnight(double v) {
    return new FlowRate(v*(1.0d/(33.814023/(1.0d/1209600))));
  }
  public static final FlowRate newOuncesPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(33.814023/(1.0d/1209600))));
  }
  public static final FlowRate newGallonsPerSecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/1.0d)));
  }
  public static final FlowRate newGallonsPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/1.0d)));
  }
  public static final FlowRate newGallonsPerMinute(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/60))));
  }
  public static final FlowRate newGallonsPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/60))));
  }
  public static final FlowRate newGallonsPerHour(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/3600))));
  }
  public static final FlowRate newGallonsPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/3600))));
  }
  public static final FlowRate newGallonsPerDay(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/86400))));
  }
  public static final FlowRate newGallonsPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/86400))));
  }
  public static final FlowRate newGallonsPerWeek(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/604800))));
  }
  public static final FlowRate newGallonsPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/604800))));
  }
  public static final FlowRate newGallonsPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/1000)));
  }
  public static final FlowRate newGallonsPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/1000)));
  }
  public static final FlowRate newGallonsPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/1000))));
  }
  public static final FlowRate newGallonsPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/1000))));
  }
  public static final FlowRate newGallonsPerMonth(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/2629743.8))));
  }
  public static final FlowRate newGallonsPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/2629743.8))));
  }
  public static final FlowRate newGallonsPerYear(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/31556926))));
  }
  public static final FlowRate newGallonsPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/31556926))));
  }
  public static final FlowRate newGallonsPerFortnight(double v) {
    return new FlowRate(v*(1.0d/((1.0d/3.785412)/(1.0d/1209600))));
  }
  public static final FlowRate newGallonsPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/3.785412)/(1.0d/1209600))));
  }
  public static final FlowRate newImperialGallonsPerSecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/1.0d)));
  }
  public static final FlowRate newImperialGallonsPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/1.0d)));
  }
  public static final FlowRate newImperialGallonsPerMinute(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/60))));
  }
  public static final FlowRate newImperialGallonsPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/60))));
  }
  public static final FlowRate newImperialGallonsPerHour(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/3600))));
  }
  public static final FlowRate newImperialGallonsPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/3600))));
  }
  public static final FlowRate newImperialGallonsPerDay(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/86400))));
  }
  public static final FlowRate newImperialGallonsPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/86400))));
  }
  public static final FlowRate newImperialGallonsPerWeek(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/604800))));
  }
  public static final FlowRate newImperialGallonsPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/604800))));
  }
  public static final FlowRate newImperialGallonsPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/1000)));
  }
  public static final FlowRate newImperialGallonsPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/1000)));
  }
  public static final FlowRate newImperialGallonsPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/1000))));
  }
  public static final FlowRate newImperialGallonsPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/1000))));
  }
  public static final FlowRate newImperialGallonsPerMonth(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/2629743.8))));
  }
  public static final FlowRate newImperialGallonsPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/2629743.8))));
  }
  public static final FlowRate newImperialGallonsPerYear(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/31556926))));
  }
  public static final FlowRate newImperialGallonsPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/31556926))));
  }
  public static final FlowRate newImperialGallonsPerFortnight(double v) {
    return new FlowRate(v*(1.0d/((1.0d/4.546090)/(1.0d/1209600))));
  }
  public static final FlowRate newImperialGallonsPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/4.546090)/(1.0d/1209600))));
  }
  public static final FlowRate newCubicFeetPerSecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/1.0d)));
  }
  public static final FlowRate newCubicFeetPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/1.0d)));
  }
  public static final FlowRate newCubicFeetPerMinute(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/60))));
  }
  public static final FlowRate newCubicFeetPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/60))));
  }
  public static final FlowRate newCubicFeetPerHour(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/3600))));
  }
  public static final FlowRate newCubicFeetPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/3600))));
  }
  public static final FlowRate newCubicFeetPerDay(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/86400))));
  }
  public static final FlowRate newCubicFeetPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/86400))));
  }
  public static final FlowRate newCubicFeetPerWeek(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/604800))));
  }
  public static final FlowRate newCubicFeetPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/604800))));
  }
  public static final FlowRate newCubicFeetPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/1000)));
  }
  public static final FlowRate newCubicFeetPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/1000)));
  }
  public static final FlowRate newCubicFeetPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/1000))));
  }
  public static final FlowRate newCubicFeetPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/1000))));
  }
  public static final FlowRate newCubicFeetPerMonth(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicFeetPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicFeetPerYear(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/31556926))));
  }
  public static final FlowRate newCubicFeetPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/31556926))));
  }
  public static final FlowRate newCubicFeetPerFortnight(double v) {
    return new FlowRate(v*(1.0d/((1.0d/28.316847)/(1.0d/1209600))));
  }
  public static final FlowRate newCubicFeetPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/28.316847)/(1.0d/1209600))));
  }
  public static final FlowRate newCubicYardsPerSecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/1.0d)));
  }
  public static final FlowRate newCubicYardsPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/1.0d)));
  }
  public static final FlowRate newCubicYardsPerMinute(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/60))));
  }
  public static final FlowRate newCubicYardsPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/60))));
  }
  public static final FlowRate newCubicYardsPerHour(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/3600))));
  }
  public static final FlowRate newCubicYardsPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/3600))));
  }
  public static final FlowRate newCubicYardsPerDay(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/86400))));
  }
  public static final FlowRate newCubicYardsPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/86400))));
  }
  public static final FlowRate newCubicYardsPerWeek(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/604800))));
  }
  public static final FlowRate newCubicYardsPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/604800))));
  }
  public static final FlowRate newCubicYardsPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/1000)));
  }
  public static final FlowRate newCubicYardsPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/1000)));
  }
  public static final FlowRate newCubicYardsPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/1000))));
  }
  public static final FlowRate newCubicYardsPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/1000))));
  }
  public static final FlowRate newCubicYardsPerMonth(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicYardsPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicYardsPerYear(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/31556926))));
  }
  public static final FlowRate newCubicYardsPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/31556926))));
  }
  public static final FlowRate newCubicYardsPerFortnight(double v) {
    return new FlowRate(v*(1.0d/((1.0d/764.55486)/(1.0d/1209600))));
  }
  public static final FlowRate newCubicYardsPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/764.55486)/(1.0d/1209600))));
  }
  public static final FlowRate newMtonsPerSecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/1.0d)));
  }
  public static final FlowRate newMtonsPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/1.0d)));
  }
  public static final FlowRate newMtonsPerMinute(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/60))));
  }
  public static final FlowRate newMtonsPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/60))));
  }
  public static final FlowRate newMtonsPerHour(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/3600))));
  }
  public static final FlowRate newMtonsPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/3600))));
  }
  public static final FlowRate newMtonsPerDay(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/86400))));
  }
  public static final FlowRate newMtonsPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/86400))));
  }
  public static final FlowRate newMtonsPerWeek(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/604800))));
  }
  public static final FlowRate newMtonsPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/604800))));
  }
  public static final FlowRate newMtonsPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/1000)));
  }
  public static final FlowRate newMtonsPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/1000)));
  }
  public static final FlowRate newMtonsPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/1000))));
  }
  public static final FlowRate newMtonsPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/1000))));
  }
  public static final FlowRate newMtonsPerMonth(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/2629743.8))));
  }
  public static final FlowRate newMtonsPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/2629743.8))));
  }
  public static final FlowRate newMtonsPerYear(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/31556926))));
  }
  public static final FlowRate newMtonsPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/31556926))));
  }
  public static final FlowRate newMtonsPerFortnight(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1132.67388)/(1.0d/1209600))));
  }
  public static final FlowRate newMtonsPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1132.67388)/(1.0d/1209600))));
  }
  public static final FlowRate newCubicCentimetersPerSecond(double v) {
    return new FlowRate(v*(1.0d/(1000/1.0d)));
  }
  public static final FlowRate newCubicCentimetersPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/1.0d)));
  }
  public static final FlowRate newCubicCentimetersPerMinute(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/60))));
  }
  public static final FlowRate newCubicCentimetersPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/60))));
  }
  public static final FlowRate newCubicCentimetersPerHour(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/3600))));
  }
  public static final FlowRate newCubicCentimetersPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/3600))));
  }
  public static final FlowRate newCubicCentimetersPerDay(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/86400))));
  }
  public static final FlowRate newCubicCentimetersPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/86400))));
  }
  public static final FlowRate newCubicCentimetersPerWeek(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/604800))));
  }
  public static final FlowRate newCubicCentimetersPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/604800))));
  }
  public static final FlowRate newCubicCentimetersPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/(1000/1000)));
  }
  public static final FlowRate newCubicCentimetersPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/1000)));
  }
  public static final FlowRate newCubicCentimetersPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/1000))));
  }
  public static final FlowRate newCubicCentimetersPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/1000))));
  }
  public static final FlowRate newCubicCentimetersPerMonth(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicCentimetersPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicCentimetersPerYear(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/31556926))));
  }
  public static final FlowRate newCubicCentimetersPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/31556926))));
  }
  public static final FlowRate newCubicCentimetersPerFortnight(double v) {
    return new FlowRate(v*(1.0d/(1000/(1.0d/1209600))));
  }
  public static final FlowRate newCubicCentimetersPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/(1000/(1.0d/1209600))));
  }
  public static final FlowRate newCubicMetersPerSecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/1.0d)));
  }
  public static final FlowRate newCubicMetersPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/1.0d)));
  }
  public static final FlowRate newCubicMetersPerMinute(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/60))));
  }
  public static final FlowRate newCubicMetersPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/60))));
  }
  public static final FlowRate newCubicMetersPerHour(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/3600))));
  }
  public static final FlowRate newCubicMetersPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/3600))));
  }
  public static final FlowRate newCubicMetersPerDay(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/86400))));
  }
  public static final FlowRate newCubicMetersPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/86400))));
  }
  public static final FlowRate newCubicMetersPerWeek(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/604800))));
  }
  public static final FlowRate newCubicMetersPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/604800))));
  }
  public static final FlowRate newCubicMetersPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/1000)));
  }
  public static final FlowRate newCubicMetersPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/1000)));
  }
  public static final FlowRate newCubicMetersPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/1000))));
  }
  public static final FlowRate newCubicMetersPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/1000))));
  }
  public static final FlowRate newCubicMetersPerMonth(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicMetersPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/2629743.8))));
  }
  public static final FlowRate newCubicMetersPerYear(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/31556926))));
  }
  public static final FlowRate newCubicMetersPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/31556926))));
  }
  public static final FlowRate newCubicMetersPerFortnight(double v) {
    return new FlowRate(v*(1.0d/((1.0d/1000)/(1.0d/1209600))));
  }
  public static final FlowRate newCubicMetersPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/1000)/(1.0d/1209600))));
  }
  public static final FlowRate newBarrelsPerSecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/1.0d)));
  }
  public static final FlowRate newBarrelsPerSecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/1.0d)));
  }
  public static final FlowRate newBarrelsPerMinute(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/60))));
  }
  public static final FlowRate newBarrelsPerMinute(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/60))));
  }
  public static final FlowRate newBarrelsPerHour(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/3600))));
  }
  public static final FlowRate newBarrelsPerHour(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/3600))));
  }
  public static final FlowRate newBarrelsPerDay(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/86400))));
  }
  public static final FlowRate newBarrelsPerDay(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/86400))));
  }
  public static final FlowRate newBarrelsPerWeek(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/604800))));
  }
  public static final FlowRate newBarrelsPerWeek(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/604800))));
  }
  public static final FlowRate newBarrelsPerMillisecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/1000)));
  }
  public static final FlowRate newBarrelsPerMillisecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/1000)));
  }
  public static final FlowRate newBarrelsPerKilosecond(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/1000))));
  }
  public static final FlowRate newBarrelsPerKilosecond(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/1000))));
  }
  public static final FlowRate newBarrelsPerMonth(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/2629743.8))));
  }
  public static final FlowRate newBarrelsPerMonth(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/2629743.8))));
  }
  public static final FlowRate newBarrelsPerYear(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/31556926))));
  }
  public static final FlowRate newBarrelsPerYear(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/31556926))));
  }
  public static final FlowRate newBarrelsPerFortnight(double v) {
    return new FlowRate(v*(1.0d/((1.0d/158.98729)/(1.0d/1209600))));
  }
  public static final FlowRate newBarrelsPerFortnight(String s) {
    return new FlowRate((Double.valueOf(s).doubleValue())*(1.0d/((1.0d/158.98729)/(1.0d/1209600))));
  }


  public int getCommonUnit() {
    return 23;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "liters/second",
    "liters/minute",
    "liters/hour",
    "liters/day",
    "liters/week",
    "liters/millisecond",
    "liters/kilosecond",
    "liters/month",
    "liters/year",
    "liters/fortnight",
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
    "gallons/second",
    "gallons/minute",
    "gallons/hour",
    "gallons/day",
    "gallons/week",
    "gallons/millisecond",
    "gallons/kilosecond",
    "gallons/month",
    "gallons/year",
    "gallons/fortnight",
    "imperial_gallons/second",
    "imperial_gallons/minute",
    "imperial_gallons/hour",
    "imperial_gallons/day",
    "imperial_gallons/week",
    "imperial_gallons/millisecond",
    "imperial_gallons/kilosecond",
    "imperial_gallons/month",
    "imperial_gallons/year",
    "imperial_gallons/fortnight",
    "cubic_feet/second",
    "cubic_feet/minute",
    "cubic_feet/hour",
    "cubic_feet/day",
    "cubic_feet/week",
    "cubic_feet/millisecond",
    "cubic_feet/kilosecond",
    "cubic_feet/month",
    "cubic_feet/year",
    "cubic_feet/fortnight",
    "cubic_yards/second",
    "cubic_yards/minute",
    "cubic_yards/hour",
    "cubic_yards/day",
    "cubic_yards/week",
    "cubic_yards/millisecond",
    "cubic_yards/kilosecond",
    "cubic_yards/month",
    "cubic_yards/year",
    "cubic_yards/fortnight",
    "mtons/second",
    "mtons/minute",
    "mtons/hour",
    "mtons/day",
    "mtons/week",
    "mtons/millisecond",
    "mtons/kilosecond",
    "mtons/month",
    "mtons/year",
    "mtons/fortnight",
    "cubic_centimeters/second",
    "cubic_centimeters/minute",
    "cubic_centimeters/hour",
    "cubic_centimeters/day",
    "cubic_centimeters/week",
    "cubic_centimeters/millisecond",
    "cubic_centimeters/kilosecond",
    "cubic_centimeters/month",
    "cubic_centimeters/year",
    "cubic_centimeters/fortnight",
    "cubic_meters/second",
    "cubic_meters/minute",
    "cubic_meters/hour",
    "cubic_meters/day",
    "cubic_meters/week",
    "cubic_meters/millisecond",
    "cubic_meters/kilosecond",
    "cubic_meters/month",
    "cubic_meters/year",
    "cubic_meters/fortnight",
    "barrels/second",
    "barrels/minute",
    "barrels/hour",
    "barrels/day",
    "barrels/week",
    "barrels/millisecond",
    "barrels/kilosecond",
    "barrels/month",
    "barrels/year",
    "barrels/fortnight",
  };

  /** @param unit One of the constant units of FlowRate **/
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
    (33.814023/1.0d),
    (33.814023/(1.0d/60)),
    (33.814023/(1.0d/3600)),
    (33.814023/(1.0d/86400)),
    (33.814023/(1.0d/604800)),
    (33.814023/1000),
    (33.814023/(1.0d/1000)),
    (33.814023/(1.0d/2629743.8)),
    (33.814023/(1.0d/31556926)),
    (33.814023/(1.0d/1209600)),
    ((1.0d/3.785412)/1.0d),
    ((1.0d/3.785412)/(1.0d/60)),
    ((1.0d/3.785412)/(1.0d/3600)),
    ((1.0d/3.785412)/(1.0d/86400)),
    ((1.0d/3.785412)/(1.0d/604800)),
    ((1.0d/3.785412)/1000),
    ((1.0d/3.785412)/(1.0d/1000)),
    ((1.0d/3.785412)/(1.0d/2629743.8)),
    ((1.0d/3.785412)/(1.0d/31556926)),
    ((1.0d/3.785412)/(1.0d/1209600)),
    ((1.0d/4.546090)/1.0d),
    ((1.0d/4.546090)/(1.0d/60)),
    ((1.0d/4.546090)/(1.0d/3600)),
    ((1.0d/4.546090)/(1.0d/86400)),
    ((1.0d/4.546090)/(1.0d/604800)),
    ((1.0d/4.546090)/1000),
    ((1.0d/4.546090)/(1.0d/1000)),
    ((1.0d/4.546090)/(1.0d/2629743.8)),
    ((1.0d/4.546090)/(1.0d/31556926)),
    ((1.0d/4.546090)/(1.0d/1209600)),
    ((1.0d/28.316847)/1.0d),
    ((1.0d/28.316847)/(1.0d/60)),
    ((1.0d/28.316847)/(1.0d/3600)),
    ((1.0d/28.316847)/(1.0d/86400)),
    ((1.0d/28.316847)/(1.0d/604800)),
    ((1.0d/28.316847)/1000),
    ((1.0d/28.316847)/(1.0d/1000)),
    ((1.0d/28.316847)/(1.0d/2629743.8)),
    ((1.0d/28.316847)/(1.0d/31556926)),
    ((1.0d/28.316847)/(1.0d/1209600)),
    ((1.0d/764.55486)/1.0d),
    ((1.0d/764.55486)/(1.0d/60)),
    ((1.0d/764.55486)/(1.0d/3600)),
    ((1.0d/764.55486)/(1.0d/86400)),
    ((1.0d/764.55486)/(1.0d/604800)),
    ((1.0d/764.55486)/1000),
    ((1.0d/764.55486)/(1.0d/1000)),
    ((1.0d/764.55486)/(1.0d/2629743.8)),
    ((1.0d/764.55486)/(1.0d/31556926)),
    ((1.0d/764.55486)/(1.0d/1209600)),
    ((1.0d/1132.67388)/1.0d),
    ((1.0d/1132.67388)/(1.0d/60)),
    ((1.0d/1132.67388)/(1.0d/3600)),
    ((1.0d/1132.67388)/(1.0d/86400)),
    ((1.0d/1132.67388)/(1.0d/604800)),
    ((1.0d/1132.67388)/1000),
    ((1.0d/1132.67388)/(1.0d/1000)),
    ((1.0d/1132.67388)/(1.0d/2629743.8)),
    ((1.0d/1132.67388)/(1.0d/31556926)),
    ((1.0d/1132.67388)/(1.0d/1209600)),
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
    ((1.0d/158.98729)/1.0d),
    ((1.0d/158.98729)/(1.0d/60)),
    ((1.0d/158.98729)/(1.0d/3600)),
    ((1.0d/158.98729)/(1.0d/86400)),
    ((1.0d/158.98729)/(1.0d/604800)),
    ((1.0d/158.98729)/1000),
    ((1.0d/158.98729)/(1.0d/1000)),
    ((1.0d/158.98729)/(1.0d/2629743.8)),
    ((1.0d/158.98729)/(1.0d/31556926)),
    ((1.0d/158.98729)/(1.0d/1209600)),
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int LITERS_PER_SECOND = 0;
  public static final int LITERS_PER_MINUTE = 1;
  public static final int LITERS_PER_HOUR = 2;
  public static final int LITERS_PER_DAY = 3;
  public static final int LITERS_PER_WEEK = 4;
  public static final int LITERS_PER_MILLISECOND = 5;
  public static final int LITERS_PER_KILOSECOND = 6;
  public static final int LITERS_PER_MONTH = 7;
  public static final int LITERS_PER_YEAR = 8;
  public static final int LITERS_PER_FORTNIGHT = 9;
  public static final int OUNCES_PER_SECOND = 10;
  public static final int OUNCES_PER_MINUTE = 11;
  public static final int OUNCES_PER_HOUR = 12;
  public static final int OUNCES_PER_DAY = 13;
  public static final int OUNCES_PER_WEEK = 14;
  public static final int OUNCES_PER_MILLISECOND = 15;
  public static final int OUNCES_PER_KILOSECOND = 16;
  public static final int OUNCES_PER_MONTH = 17;
  public static final int OUNCES_PER_YEAR = 18;
  public static final int OUNCES_PER_FORTNIGHT = 19;
  public static final int GALLONS_PER_SECOND = 20;
  public static final int GALLONS_PER_MINUTE = 21;
  public static final int GALLONS_PER_HOUR = 22;
  public static final int GALLONS_PER_DAY = 23;
  public static final int GALLONS_PER_WEEK = 24;
  public static final int GALLONS_PER_MILLISECOND = 25;
  public static final int GALLONS_PER_KILOSECOND = 26;
  public static final int GALLONS_PER_MONTH = 27;
  public static final int GALLONS_PER_YEAR = 28;
  public static final int GALLONS_PER_FORTNIGHT = 29;
  public static final int IMPERIAL_GALLONS_PER_SECOND = 30;
  public static final int IMPERIAL_GALLONS_PER_MINUTE = 31;
  public static final int IMPERIAL_GALLONS_PER_HOUR = 32;
  public static final int IMPERIAL_GALLONS_PER_DAY = 33;
  public static final int IMPERIAL_GALLONS_PER_WEEK = 34;
  public static final int IMPERIAL_GALLONS_PER_MILLISECOND = 35;
  public static final int IMPERIAL_GALLONS_PER_KILOSECOND = 36;
  public static final int IMPERIAL_GALLONS_PER_MONTH = 37;
  public static final int IMPERIAL_GALLONS_PER_YEAR = 38;
  public static final int IMPERIAL_GALLONS_PER_FORTNIGHT = 39;
  public static final int CUBIC_FEET_PER_SECOND = 40;
  public static final int CUBIC_FEET_PER_MINUTE = 41;
  public static final int CUBIC_FEET_PER_HOUR = 42;
  public static final int CUBIC_FEET_PER_DAY = 43;
  public static final int CUBIC_FEET_PER_WEEK = 44;
  public static final int CUBIC_FEET_PER_MILLISECOND = 45;
  public static final int CUBIC_FEET_PER_KILOSECOND = 46;
  public static final int CUBIC_FEET_PER_MONTH = 47;
  public static final int CUBIC_FEET_PER_YEAR = 48;
  public static final int CUBIC_FEET_PER_FORTNIGHT = 49;
  public static final int CUBIC_YARDS_PER_SECOND = 50;
  public static final int CUBIC_YARDS_PER_MINUTE = 51;
  public static final int CUBIC_YARDS_PER_HOUR = 52;
  public static final int CUBIC_YARDS_PER_DAY = 53;
  public static final int CUBIC_YARDS_PER_WEEK = 54;
  public static final int CUBIC_YARDS_PER_MILLISECOND = 55;
  public static final int CUBIC_YARDS_PER_KILOSECOND = 56;
  public static final int CUBIC_YARDS_PER_MONTH = 57;
  public static final int CUBIC_YARDS_PER_YEAR = 58;
  public static final int CUBIC_YARDS_PER_FORTNIGHT = 59;
  public static final int MTONS_PER_SECOND = 60;
  public static final int MTONS_PER_MINUTE = 61;
  public static final int MTONS_PER_HOUR = 62;
  public static final int MTONS_PER_DAY = 63;
  public static final int MTONS_PER_WEEK = 64;
  public static final int MTONS_PER_MILLISECOND = 65;
  public static final int MTONS_PER_KILOSECOND = 66;
  public static final int MTONS_PER_MONTH = 67;
  public static final int MTONS_PER_YEAR = 68;
  public static final int MTONS_PER_FORTNIGHT = 69;
  public static final int CUBIC_CENTIMETERS_PER_SECOND = 70;
  public static final int CUBIC_CENTIMETERS_PER_MINUTE = 71;
  public static final int CUBIC_CENTIMETERS_PER_HOUR = 72;
  public static final int CUBIC_CENTIMETERS_PER_DAY = 73;
  public static final int CUBIC_CENTIMETERS_PER_WEEK = 74;
  public static final int CUBIC_CENTIMETERS_PER_MILLISECOND = 75;
  public static final int CUBIC_CENTIMETERS_PER_KILOSECOND = 76;
  public static final int CUBIC_CENTIMETERS_PER_MONTH = 77;
  public static final int CUBIC_CENTIMETERS_PER_YEAR = 78;
  public static final int CUBIC_CENTIMETERS_PER_FORTNIGHT = 79;
  public static final int CUBIC_METERS_PER_SECOND = 80;
  public static final int CUBIC_METERS_PER_MINUTE = 81;
  public static final int CUBIC_METERS_PER_HOUR = 82;
  public static final int CUBIC_METERS_PER_DAY = 83;
  public static final int CUBIC_METERS_PER_WEEK = 84;
  public static final int CUBIC_METERS_PER_MILLISECOND = 85;
  public static final int CUBIC_METERS_PER_KILOSECOND = 86;
  public static final int CUBIC_METERS_PER_MONTH = 87;
  public static final int CUBIC_METERS_PER_YEAR = 88;
  public static final int CUBIC_METERS_PER_FORTNIGHT = 89;
  public static final int BARRELS_PER_SECOND = 90;
  public static final int BARRELS_PER_MINUTE = 91;
  public static final int BARRELS_PER_HOUR = 92;
  public static final int BARRELS_PER_DAY = 93;
  public static final int BARRELS_PER_WEEK = 94;
  public static final int BARRELS_PER_MILLISECOND = 95;
  public static final int BARRELS_PER_KILOSECOND = 96;
  public static final int BARRELS_PER_MONTH = 97;
  public static final int BARRELS_PER_YEAR = 98;
  public static final int BARRELS_PER_FORTNIGHT = 99;
  static final int MAXUNIT = 99;

  // Index Typed factory methods
  /** @param unit One of the constant units of FlowRate **/
  public static final FlowRate newFlowRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new FlowRate(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit One of the constant units of FlowRate **/
  public static final FlowRate newFlowRate(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new FlowRate((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Index Typed factory methods
  /** @param unit1 One of the constant units of Volume
   *  @param unit2 One of the constant units of Duration
   **/
  public static final FlowRate newFlowRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Volume.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new FlowRate(v*Volume.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Volume to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public static final FlowRate newFlowRate(Volume num, Duration den) {
    return new FlowRate(num.getValue(0)/den.getValue(0));
  }

  /** @param unit1 One of the constant units of Volume
   *  @param unit2 One of the constant units of Duration
   **/
  public static final FlowRate newFlowRate(String s, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Volume.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new FlowRate((Double.valueOf(s).doubleValue())*Volume.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newFlowRate(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newFlowRate(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof FlowRate)) throw new IllegalArgumentException();
    return new FlowRate(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof FlowRate)) throw new IllegalArgumentException();
    return new FlowRate(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new FlowRate(theValue*scale,0);
  }

  public final Measure negate() {
    return newFlowRate(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newFlowRate(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new FlowRate(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new FlowRate(value, unit);
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
      throw new IllegalArgumentException("Expecting a FlowRate" + 
      ", got a " + toRate.getCanonicalNumerator().getClass() + "/" + toRate.getCanonicalDenominator().getClass());
    }
    return theValue/toRate.getNativeValue();
  }

  // Unit-based Reader methods
  public double getLitersPerSecond() {
    return (theValue*(1.0d/1.0d));
  }
  public double getLitersPerMinute() {
    return (theValue*(1.0d/(1.0d/60)));
  }
  public double getLitersPerHour() {
    return (theValue*(1.0d/(1.0d/3600)));
  }
  public double getLitersPerDay() {
    return (theValue*(1.0d/(1.0d/86400)));
  }
  public double getLitersPerWeek() {
    return (theValue*(1.0d/(1.0d/604800)));
  }
  public double getLitersPerMillisecond() {
    return (theValue*(1.0d/1000));
  }
  public double getLitersPerKilosecond() {
    return (theValue*(1.0d/(1.0d/1000)));
  }
  public double getLitersPerMonth() {
    return (theValue*(1.0d/(1.0d/2629743.8)));
  }
  public double getLitersPerYear() {
    return (theValue*(1.0d/(1.0d/31556926)));
  }
  public double getLitersPerFortnight() {
    return (theValue*(1.0d/(1.0d/1209600)));
  }
  public double getOuncesPerSecond() {
    return (theValue*(33.814023/1.0d));
  }
  public double getOuncesPerMinute() {
    return (theValue*(33.814023/(1.0d/60)));
  }
  public double getOuncesPerHour() {
    return (theValue*(33.814023/(1.0d/3600)));
  }
  public double getOuncesPerDay() {
    return (theValue*(33.814023/(1.0d/86400)));
  }
  public double getOuncesPerWeek() {
    return (theValue*(33.814023/(1.0d/604800)));
  }
  public double getOuncesPerMillisecond() {
    return (theValue*(33.814023/1000));
  }
  public double getOuncesPerKilosecond() {
    return (theValue*(33.814023/(1.0d/1000)));
  }
  public double getOuncesPerMonth() {
    return (theValue*(33.814023/(1.0d/2629743.8)));
  }
  public double getOuncesPerYear() {
    return (theValue*(33.814023/(1.0d/31556926)));
  }
  public double getOuncesPerFortnight() {
    return (theValue*(33.814023/(1.0d/1209600)));
  }
  public double getGallonsPerSecond() {
    return (theValue*((1.0d/3.785412)/1.0d));
  }
  public double getGallonsPerMinute() {
    return (theValue*((1.0d/3.785412)/(1.0d/60)));
  }
  public double getGallonsPerHour() {
    return (theValue*((1.0d/3.785412)/(1.0d/3600)));
  }
  public double getGallonsPerDay() {
    return (theValue*((1.0d/3.785412)/(1.0d/86400)));
  }
  public double getGallonsPerWeek() {
    return (theValue*((1.0d/3.785412)/(1.0d/604800)));
  }
  public double getGallonsPerMillisecond() {
    return (theValue*((1.0d/3.785412)/1000));
  }
  public double getGallonsPerKilosecond() {
    return (theValue*((1.0d/3.785412)/(1.0d/1000)));
  }
  public double getGallonsPerMonth() {
    return (theValue*((1.0d/3.785412)/(1.0d/2629743.8)));
  }
  public double getGallonsPerYear() {
    return (theValue*((1.0d/3.785412)/(1.0d/31556926)));
  }
  public double getGallonsPerFortnight() {
    return (theValue*((1.0d/3.785412)/(1.0d/1209600)));
  }
  public double getImperialGallonsPerSecond() {
    return (theValue*((1.0d/4.546090)/1.0d));
  }
  public double getImperialGallonsPerMinute() {
    return (theValue*((1.0d/4.546090)/(1.0d/60)));
  }
  public double getImperialGallonsPerHour() {
    return (theValue*((1.0d/4.546090)/(1.0d/3600)));
  }
  public double getImperialGallonsPerDay() {
    return (theValue*((1.0d/4.546090)/(1.0d/86400)));
  }
  public double getImperialGallonsPerWeek() {
    return (theValue*((1.0d/4.546090)/(1.0d/604800)));
  }
  public double getImperialGallonsPerMillisecond() {
    return (theValue*((1.0d/4.546090)/1000));
  }
  public double getImperialGallonsPerKilosecond() {
    return (theValue*((1.0d/4.546090)/(1.0d/1000)));
  }
  public double getImperialGallonsPerMonth() {
    return (theValue*((1.0d/4.546090)/(1.0d/2629743.8)));
  }
  public double getImperialGallonsPerYear() {
    return (theValue*((1.0d/4.546090)/(1.0d/31556926)));
  }
  public double getImperialGallonsPerFortnight() {
    return (theValue*((1.0d/4.546090)/(1.0d/1209600)));
  }
  public double getCubicFeetPerSecond() {
    return (theValue*((1.0d/28.316847)/1.0d));
  }
  public double getCubicFeetPerMinute() {
    return (theValue*((1.0d/28.316847)/(1.0d/60)));
  }
  public double getCubicFeetPerHour() {
    return (theValue*((1.0d/28.316847)/(1.0d/3600)));
  }
  public double getCubicFeetPerDay() {
    return (theValue*((1.0d/28.316847)/(1.0d/86400)));
  }
  public double getCubicFeetPerWeek() {
    return (theValue*((1.0d/28.316847)/(1.0d/604800)));
  }
  public double getCubicFeetPerMillisecond() {
    return (theValue*((1.0d/28.316847)/1000));
  }
  public double getCubicFeetPerKilosecond() {
    return (theValue*((1.0d/28.316847)/(1.0d/1000)));
  }
  public double getCubicFeetPerMonth() {
    return (theValue*((1.0d/28.316847)/(1.0d/2629743.8)));
  }
  public double getCubicFeetPerYear() {
    return (theValue*((1.0d/28.316847)/(1.0d/31556926)));
  }
  public double getCubicFeetPerFortnight() {
    return (theValue*((1.0d/28.316847)/(1.0d/1209600)));
  }
  public double getCubicYardsPerSecond() {
    return (theValue*((1.0d/764.55486)/1.0d));
  }
  public double getCubicYardsPerMinute() {
    return (theValue*((1.0d/764.55486)/(1.0d/60)));
  }
  public double getCubicYardsPerHour() {
    return (theValue*((1.0d/764.55486)/(1.0d/3600)));
  }
  public double getCubicYardsPerDay() {
    return (theValue*((1.0d/764.55486)/(1.0d/86400)));
  }
  public double getCubicYardsPerWeek() {
    return (theValue*((1.0d/764.55486)/(1.0d/604800)));
  }
  public double getCubicYardsPerMillisecond() {
    return (theValue*((1.0d/764.55486)/1000));
  }
  public double getCubicYardsPerKilosecond() {
    return (theValue*((1.0d/764.55486)/(1.0d/1000)));
  }
  public double getCubicYardsPerMonth() {
    return (theValue*((1.0d/764.55486)/(1.0d/2629743.8)));
  }
  public double getCubicYardsPerYear() {
    return (theValue*((1.0d/764.55486)/(1.0d/31556926)));
  }
  public double getCubicYardsPerFortnight() {
    return (theValue*((1.0d/764.55486)/(1.0d/1209600)));
  }
  public double getMtonsPerSecond() {
    return (theValue*((1.0d/1132.67388)/1.0d));
  }
  public double getMtonsPerMinute() {
    return (theValue*((1.0d/1132.67388)/(1.0d/60)));
  }
  public double getMtonsPerHour() {
    return (theValue*((1.0d/1132.67388)/(1.0d/3600)));
  }
  public double getMtonsPerDay() {
    return (theValue*((1.0d/1132.67388)/(1.0d/86400)));
  }
  public double getMtonsPerWeek() {
    return (theValue*((1.0d/1132.67388)/(1.0d/604800)));
  }
  public double getMtonsPerMillisecond() {
    return (theValue*((1.0d/1132.67388)/1000));
  }
  public double getMtonsPerKilosecond() {
    return (theValue*((1.0d/1132.67388)/(1.0d/1000)));
  }
  public double getMtonsPerMonth() {
    return (theValue*((1.0d/1132.67388)/(1.0d/2629743.8)));
  }
  public double getMtonsPerYear() {
    return (theValue*((1.0d/1132.67388)/(1.0d/31556926)));
  }
  public double getMtonsPerFortnight() {
    return (theValue*((1.0d/1132.67388)/(1.0d/1209600)));
  }
  public double getCubicCentimetersPerSecond() {
    return (theValue*(1000/1.0d));
  }
  public double getCubicCentimetersPerMinute() {
    return (theValue*(1000/(1.0d/60)));
  }
  public double getCubicCentimetersPerHour() {
    return (theValue*(1000/(1.0d/3600)));
  }
  public double getCubicCentimetersPerDay() {
    return (theValue*(1000/(1.0d/86400)));
  }
  public double getCubicCentimetersPerWeek() {
    return (theValue*(1000/(1.0d/604800)));
  }
  public double getCubicCentimetersPerMillisecond() {
    return (theValue*(1000/1000));
  }
  public double getCubicCentimetersPerKilosecond() {
    return (theValue*(1000/(1.0d/1000)));
  }
  public double getCubicCentimetersPerMonth() {
    return (theValue*(1000/(1.0d/2629743.8)));
  }
  public double getCubicCentimetersPerYear() {
    return (theValue*(1000/(1.0d/31556926)));
  }
  public double getCubicCentimetersPerFortnight() {
    return (theValue*(1000/(1.0d/1209600)));
  }
  public double getCubicMetersPerSecond() {
    return (theValue*((1.0d/1000)/1.0d));
  }
  public double getCubicMetersPerMinute() {
    return (theValue*((1.0d/1000)/(1.0d/60)));
  }
  public double getCubicMetersPerHour() {
    return (theValue*((1.0d/1000)/(1.0d/3600)));
  }
  public double getCubicMetersPerDay() {
    return (theValue*((1.0d/1000)/(1.0d/86400)));
  }
  public double getCubicMetersPerWeek() {
    return (theValue*((1.0d/1000)/(1.0d/604800)));
  }
  public double getCubicMetersPerMillisecond() {
    return (theValue*((1.0d/1000)/1000));
  }
  public double getCubicMetersPerKilosecond() {
    return (theValue*((1.0d/1000)/(1.0d/1000)));
  }
  public double getCubicMetersPerMonth() {
    return (theValue*((1.0d/1000)/(1.0d/2629743.8)));
  }
  public double getCubicMetersPerYear() {
    return (theValue*((1.0d/1000)/(1.0d/31556926)));
  }
  public double getCubicMetersPerFortnight() {
    return (theValue*((1.0d/1000)/(1.0d/1209600)));
  }
  public double getBarrelsPerSecond() {
    return (theValue*((1.0d/158.98729)/1.0d));
  }
  public double getBarrelsPerMinute() {
    return (theValue*((1.0d/158.98729)/(1.0d/60)));
  }
  public double getBarrelsPerHour() {
    return (theValue*((1.0d/158.98729)/(1.0d/3600)));
  }
  public double getBarrelsPerDay() {
    return (theValue*((1.0d/158.98729)/(1.0d/86400)));
  }
  public double getBarrelsPerWeek() {
    return (theValue*((1.0d/158.98729)/(1.0d/604800)));
  }
  public double getBarrelsPerMillisecond() {
    return (theValue*((1.0d/158.98729)/1000));
  }
  public double getBarrelsPerKilosecond() {
    return (theValue*((1.0d/158.98729)/(1.0d/1000)));
  }
  public double getBarrelsPerMonth() {
    return (theValue*((1.0d/158.98729)/(1.0d/2629743.8)));
  }
  public double getBarrelsPerYear() {
    return (theValue*((1.0d/158.98729)/(1.0d/31556926)));
  }
  public double getBarrelsPerFortnight() {
    return (theValue*((1.0d/158.98729)/(1.0d/1209600)));
  }

  /** @param unit One of the constant units of FlowRate **/
  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Volume
   *  @param unit2 One of the constant units of Duration
   **/
  public double getValue(int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Volume.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return (theValue*Duration.getConvFactor(unit2)/Volume.getConvFactor(unit1));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof FlowRate &&
             theValue == ((FlowRate) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "l/s";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // Derivative
  public final Class getNumeratorClass() { return Volume.class; }
  public final Class getDenominatorClass() { return Duration.class; }

  private final static Volume can_num = new Volume(0.0,0);
  public final Measure getCanonicalNumerator() { return can_num; }
  private final static Duration can_den = new Duration(0.0,0);
  public final Measure getCanonicalDenominator() { return can_den; }
  public final Measure computeNumerator(Measure den) {
    if (!(den instanceof Duration)) throw new IllegalArgumentException();
    return new Volume(theValue*den.getValue(0),0);
  }
  public final Measure computeDenominator(Measure num) {
    if (!(num instanceof Volume)) throw new IllegalArgumentException();
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
