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
/** Immutable implementation of CostRate.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class CostRate extends AbstractMeasure
  implements Externalizable, Derivative, Rate {
  // the value is stored as dollars/second
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public CostRate() {}

  // private constructor
  private CostRate(double v) {
    theValue = v;
  }

  /** @param unit One of the constant units of CostRate **/
  public CostRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v/getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Cost
   *  @param unit2 One of the constant units of Duration
   **/
  public CostRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Cost.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      theValue = v*Cost.getConvFactor(unit1)/Duration.getConvFactor(unit2);
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Cost to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public CostRate(Cost num, Duration den) {
    theValue = num.getValue(0)/den.getValue(0);
  }

  /** takes strings of the form "Number unit" **/
  public CostRate(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("dollarspersecond")) 
      theValue=n/(1.0d/1.0d);
    else if (u.equals("dollarsperminute")) 
      theValue=n/(1.0d/(1.0d/60));
    else if (u.equals("dollarsperhour")) 
      theValue=n/(1.0d/(1.0d/3600));
    else if (u.equals("dollarsperday")) 
      theValue=n/(1.0d/(1.0d/86400));
    else if (u.equals("dollarsperweek")) 
      theValue=n/(1.0d/(1.0d/604800));
    else if (u.equals("dollarspermillisecond")) 
      theValue=n/(1.0d/1000);
    else if (u.equals("dollarsperkilosecond")) 
      theValue=n/(1.0d/(1.0d/1000));
    else if (u.equals("dollarspermonth")) 
      theValue=n/(1.0d/(1.0d/2629743.8));
    else if (u.equals("dollarsperyear")) 
      theValue=n/(1.0d/(1.0d/31556926));
    else if (u.equals("dollarsperfortnight")) 
      theValue=n/(1.0d/(1.0d/1209600));
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final CostRate newDollarsPerSecond(double v) {
    return new CostRate(v*(1.0d/(1.0d/1.0d)));
  }
  public static final CostRate newDollarsPerSecond(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1.0d)));
  }
  public static final CostRate newDollarsPerMinute(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final CostRate newDollarsPerMinute(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/60))));
  }
  public static final CostRate newDollarsPerHour(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final CostRate newDollarsPerHour(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/3600))));
  }
  public static final CostRate newDollarsPerDay(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final CostRate newDollarsPerDay(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/86400))));
  }
  public static final CostRate newDollarsPerWeek(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final CostRate newDollarsPerWeek(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/604800))));
  }
  public static final CostRate newDollarsPerMillisecond(double v) {
    return new CostRate(v*(1.0d/(1.0d/1000)));
  }
  public static final CostRate newDollarsPerMillisecond(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/1000)));
  }
  public static final CostRate newDollarsPerKilosecond(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final CostRate newDollarsPerKilosecond(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1000))));
  }
  public static final CostRate newDollarsPerMonth(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final CostRate newDollarsPerMonth(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/2629743.8))));
  }
  public static final CostRate newDollarsPerYear(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final CostRate newDollarsPerYear(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/31556926))));
  }
  public static final CostRate newDollarsPerFortnight(double v) {
    return new CostRate(v*(1.0d/(1.0d/(1.0d/1209600))));
  }
  public static final CostRate newDollarsPerFortnight(String s) {
    return new CostRate((Double.valueOf(s).doubleValue())*(1.0d/(1.0d/(1.0d/1209600))));
  }


  public int getCommonUnit() {
    return 0;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "dollars/second",
    "dollars/minute",
    "dollars/hour",
    "dollars/day",
    "dollars/week",
    "dollars/millisecond",
    "dollars/kilosecond",
    "dollars/month",
    "dollars/year",
    "dollars/fortnight",
  };

  /** @param unit One of the constant units of CostRate **/
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
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int DOLLARS_PER_SECOND = 0;
  public static final int DOLLARS_PER_MINUTE = 1;
  public static final int DOLLARS_PER_HOUR = 2;
  public static final int DOLLARS_PER_DAY = 3;
  public static final int DOLLARS_PER_WEEK = 4;
  public static final int DOLLARS_PER_MILLISECOND = 5;
  public static final int DOLLARS_PER_KILOSECOND = 6;
  public static final int DOLLARS_PER_MONTH = 7;
  public static final int DOLLARS_PER_YEAR = 8;
  public static final int DOLLARS_PER_FORTNIGHT = 9;
  static final int MAXUNIT = 9;

  // Index Typed factory methods
  /** @param unit One of the constant units of CostRate **/
  public static final CostRate newCostRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new CostRate(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit One of the constant units of CostRate **/
  public static final CostRate newCostRate(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new CostRate((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Index Typed factory methods
  /** @param unit1 One of the constant units of Cost
   *  @param unit2 One of the constant units of Duration
   **/
  public static final CostRate newCostRate(double v, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Cost.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new CostRate(v*Cost.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  /** @param num An instance of Cost to use as numerator
   *  @param den An instance of Durationto use as denominator
   **/
  public static final CostRate newCostRate(Cost num, Duration den) {
    return new CostRate(num.getValue(0)/den.getValue(0));
  }

  /** @param unit1 One of the constant units of Cost
   *  @param unit2 One of the constant units of Duration
   **/
  public static final CostRate newCostRate(String s, int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Cost.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return new CostRate((Double.valueOf(s).doubleValue())*Cost.getConvFactor(unit1)/Duration.getConvFactor(unit2));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newCostRate(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newCostRate(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof CostRate)) throw new IllegalArgumentException();
    return new CostRate(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof CostRate)) throw new IllegalArgumentException();
    return new CostRate(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new CostRate(theValue*scale,0);
  }

  public final Measure negate() {
    return newCostRate(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newCostRate(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new CostRate(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new CostRate(value, unit);
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
      throw new IllegalArgumentException("Expecting a CostRate" + 
      ", got a " + toRate.getCanonicalNumerator().getClass() + "/" + toRate.getCanonicalDenominator().getClass());
    }
    return theValue/toRate.getNativeValue();
  }

  // Unit-based Reader methods
  public double getDollarsPerSecond() {
    return (theValue*(1.0d/1.0d));
  }
  public double getDollarsPerMinute() {
    return (theValue*(1.0d/(1.0d/60)));
  }
  public double getDollarsPerHour() {
    return (theValue*(1.0d/(1.0d/3600)));
  }
  public double getDollarsPerDay() {
    return (theValue*(1.0d/(1.0d/86400)));
  }
  public double getDollarsPerWeek() {
    return (theValue*(1.0d/(1.0d/604800)));
  }
  public double getDollarsPerMillisecond() {
    return (theValue*(1.0d/1000));
  }
  public double getDollarsPerKilosecond() {
    return (theValue*(1.0d/(1.0d/1000)));
  }
  public double getDollarsPerMonth() {
    return (theValue*(1.0d/(1.0d/2629743.8)));
  }
  public double getDollarsPerYear() {
    return (theValue*(1.0d/(1.0d/31556926)));
  }
  public double getDollarsPerFortnight() {
    return (theValue*(1.0d/(1.0d/1209600)));
  }

  /** @param unit One of the constant units of CostRate **/
  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  /** @param unit1 One of the constant units of Cost
   *  @param unit2 One of the constant units of Duration
   **/
  public double getValue(int unit1, int unit2) {
    if (unit1 >= 0 && unit1 <= Cost.MAXUNIT &&
        unit2 >= 0 && unit2 <= Duration.MAXUNIT)
      return (theValue*Duration.getConvFactor(unit2)/Cost.getConvFactor(unit1));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof CostRate &&
             theValue == ((CostRate) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "usd/s";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // Derivative
  public final Class getNumeratorClass() { return Cost.class; }
  public final Class getDenominatorClass() { return Duration.class; }

  private final static Cost can_num = new Cost(0.0,0);
  public final Measure getCanonicalNumerator() { return can_num; }
  private final static Duration can_den = new Duration(0.0,0);
  public final Measure getCanonicalDenominator() { return can_den; }
  public final Measure computeNumerator(Measure den) {
    if (!(den instanceof Duration)) throw new IllegalArgumentException();
    return new Cost(theValue*den.getValue(0),0);
  }
  public final Measure computeDenominator(Measure num) {
    if (!(num instanceof Cost)) throw new IllegalArgumentException();
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
