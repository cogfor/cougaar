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
/** Immutable implementation of Duration.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Duration extends Scalar implements Externalizable {
  // Conversion factor constants
  public static final double SECONDS_PER_MINUTES = 60;
  public static final double MINUTES_PER_SECONDS = (1.0d/60);
  public static final double SECONDS_PER_HOURS = 3600;
  public static final double HOURS_PER_SECONDS = (1.0d/3600);
  public static final double SECONDS_PER_DAYS = 86400;
  public static final double DAYS_PER_SECONDS = (1.0d/86400);
  public static final double SECONDS_PER_WEEKS = 604800;
  public static final double WEEKS_PER_SECONDS = (1.0d/604800);
  public static final double SECONDS_PER_MILLISECONDS = (1.0d/1000);
  public static final double MILLISECONDS_PER_SECONDS = 1000;
  public static final double SECONDS_PER_KILOSECONDS = 1000;
  public static final double KILOSECONDS_PER_SECONDS = (1.0d/1000);
  public static final double SECONDS_PER_MONTHS = 2629743.8;
  public static final double MONTHS_PER_SECONDS = (1.0d/2629743.8);
  public static final double SECONDS_PER_YEARS = 31556926;
  public static final double YEARS_PER_SECONDS = (1.0d/31556926);
  public static final double SECONDS_PER_FORTNIGHTS = 1209600;
  public static final double FORTNIGHTS_PER_SECONDS = (1.0d/1209600);

  // the value is stored as seconds
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Duration() {}

  // private constructor
  private Duration(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Duration(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Duration(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("seconds")) 
      theValue=n;
    else if (u.equals("minutes")) 
      theValue=n*SECONDS_PER_MINUTES;
    else if (u.equals("hours")) 
      theValue=n*SECONDS_PER_HOURS;
    else if (u.equals("days")) 
      theValue=n*SECONDS_PER_DAYS;
    else if (u.equals("weeks")) 
      theValue=n*SECONDS_PER_WEEKS;
    else if (u.equals("milliseconds")) 
      theValue=n*SECONDS_PER_MILLISECONDS;
    else if (u.equals("kiloseconds")) 
      theValue=n*SECONDS_PER_KILOSECONDS;
    else if (u.equals("months")) 
      theValue=n*SECONDS_PER_MONTHS;
    else if (u.equals("years")) 
      theValue=n*SECONDS_PER_YEARS;
    else if (u.equals("fortnights")) 
      theValue=n*SECONDS_PER_FORTNIGHTS;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Duration newSeconds(double v) {
    return new Duration(v);
  }
  public static final Duration newSeconds(String s) {
    return new Duration((Double.valueOf(s).doubleValue()));
  }
  public static final Duration newMinutes(double v) {
    return new Duration(v*SECONDS_PER_MINUTES);
  }
  public static final Duration newMinutes(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_MINUTES);
  }
  public static final Duration newHours(double v) {
    return new Duration(v*SECONDS_PER_HOURS);
  }
  public static final Duration newHours(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_HOURS);
  }
  public static final Duration newDays(double v) {
    return new Duration(v*SECONDS_PER_DAYS);
  }
  public static final Duration newDays(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_DAYS);
  }
  public static final Duration newWeeks(double v) {
    return new Duration(v*SECONDS_PER_WEEKS);
  }
  public static final Duration newWeeks(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_WEEKS);
  }
  public static final Duration newMilliseconds(double v) {
    return new Duration(v*SECONDS_PER_MILLISECONDS);
  }
  public static final Duration newMilliseconds(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_MILLISECONDS);
  }
  public static final Duration newKiloseconds(double v) {
    return new Duration(v*SECONDS_PER_KILOSECONDS);
  }
  public static final Duration newKiloseconds(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_KILOSECONDS);
  }
  public static final Duration newMonths(double v) {
    return new Duration(v*SECONDS_PER_MONTHS);
  }
  public static final Duration newMonths(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_MONTHS);
  }
  public static final Duration newYears(double v) {
    return new Duration(v*SECONDS_PER_YEARS);
  }
  public static final Duration newYears(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_YEARS);
  }
  public static final Duration newFortnights(double v) {
    return new Duration(v*SECONDS_PER_FORTNIGHTS);
  }
  public static final Duration newFortnights(String s) {
    return new Duration((Double.valueOf(s).doubleValue())*SECONDS_PER_FORTNIGHTS);
  }


  public int getCommonUnit() {
    return HOURS;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "seconds",
    "minutes",
    "hours",
    "days",
    "weeks",
    "milliseconds",
    "kiloseconds",
    "months",
    "years",
    "fortnights",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    SECONDS_PER_MINUTES,
    SECONDS_PER_HOURS,
    SECONDS_PER_DAYS,
    SECONDS_PER_WEEKS,
    SECONDS_PER_MILLISECONDS,
    SECONDS_PER_KILOSECONDS,
    SECONDS_PER_MONTHS,
    SECONDS_PER_YEARS,
    SECONDS_PER_FORTNIGHTS,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int SECONDS = 0;
  public static final int MINUTES = 1;
  public static final int HOURS = 2;
  public static final int DAYS = 3;
  public static final int WEEKS = 4;
  public static final int MILLISECONDS = 5;
  public static final int KILOSECONDS = 6;
  public static final int MONTHS = 7;
  public static final int YEARS = 8;
  public static final int FORTNIGHTS = 9;
  public static final int MAXUNIT = 9;

  // Index Typed factory methods
  public static final Duration newDuration(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Duration(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Duration newDuration(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Duration((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newDuration(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newDuration(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Duration)) throw new IllegalArgumentException();
    return new Duration(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Duration)) throw new IllegalArgumentException();
    return new Duration(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Duration(theValue*scale,0);
  }

  public final Measure negate() {
    return newDuration(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newDuration(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Duration(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Duration(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Duration)) {
      throw new IllegalArgumentException("Expecting a Duration/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getSeconds() {
    return (theValue);
  }
  public double getMinutes() {
    return (theValue/SECONDS_PER_MINUTES);
  }
  public double getHours() {
    return (theValue/SECONDS_PER_HOURS);
  }
  public double getDays() {
    return (theValue/SECONDS_PER_DAYS);
  }
  public double getWeeks() {
    return (theValue/SECONDS_PER_WEEKS);
  }
  public double getMilliseconds() {
    return (theValue/SECONDS_PER_MILLISECONDS);
  }
  public double getKiloseconds() {
    return (theValue/SECONDS_PER_KILOSECONDS);
  }
  public double getMonths() {
    return (theValue/SECONDS_PER_MONTHS);
  }
  public double getYears() {
    return (theValue/SECONDS_PER_YEARS);
  }
  public double getFortnights() {
    return (theValue/SECONDS_PER_FORTNIGHTS);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Duration &&
             theValue == ((Duration) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "s";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // serialization
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(theValue);
  }
  public void readExternal(ObjectInput in) throws IOException {
    theValue = in.readDouble();
  }
}
