/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
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
/** Immutable implementation of Latitude.
 **/

// should be machine generated
package org.cougaar.planning.ldm.measure;

public final class Latitude extends AbstractMeasure
{
  private static final double upperBound = 90.0;
  private static final double lowerBound = -90.0;

  //no real conversions for now
  private static final Conversion DEGREES_TO_DEGREES = new Conversion() {
    public double convert(double from) { return from; }};

  // basic unit is Degrees
  private double theValue;

  // private constructor
  private Latitude(double v) throws ValueRangeException {
    if ( inBounds(v) ) {
      theValue = v;
    } else {
      throw new ValueRangeException ("Latitude expects the double to be between -90.0 and +90.0.");
    }

  }

  public Latitude(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("degrees"))
      theValue=n;
    else
      throw new UnknownUnitException();
  }

  public int getCommonUnit() { return 0; }
  public int getMaxUnit() { return 0; }
  public String getUnitName(int i) {
    if (i ==0) return "degrees";
    else throw new IllegalArgumentException();
  }

  // TypeNamed factory methods
  public static Latitude newLatitude(double v) {
    return new Latitude(v);
  }
  public static Latitude newLatitude(String s) {
    return new Latitude((Double.valueOf(s).doubleValue()));
  }


  // Index Typed factory methods
  // ***No real conversions for now
  private static final Conversion convFactor[]={
    //conversions to base units
    DEGREES_TO_DEGREES,
    //RADIANS_TO_DEGREES,
    // conversions from base units
    DEGREES_TO_DEGREES
    //DEGREES_TO_RADIANS
  };
  // indexes into factor array
  public static int DEGREES = 0;
  //public static int RADIANS = 1;
  private static int MAXUNIT = 0;

  // TypeNamed factory methods
  public static Latitude newDegrees(double v) {
    return new Latitude(v);
  }
  public static Latitude newDegrees(String s) {
    return new Latitude((Double.valueOf(s).doubleValue()));
  }

  // Index Typed factory methods
  public static Latitude newLatitude(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Latitude(convFactor[unit].convert(v));
    else
      throw new UnknownUnitException();
  }

  public static Latitude newLatitude(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Latitude(convFactor[unit].convert(Double.valueOf(s).doubleValue()));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static AbstractMeasure newMeasure(String s, int unit) {
    return newLatitude(s, unit);
  }
  public static AbstractMeasure newMeasure(double v, int unit) {
    return newLatitude(v, unit);
  }

  // Unit-based Reader methods
  public double getDegrees() {
    return (theValue);
  }
  //public double getRADIANS() {
  //return (DEGREES_TO_RADIANS.convert(theValue));
  //}

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return convFactor[MAXUNIT+1+unit].convert(theValue);
    else
      throw new UnknownUnitException();
  }

  public static Conversion getConversion(final int from, final int to) {
    if (from >= 0 && from <= MAXUNIT &&
      to >= 0 && to <= MAXUNIT ) {
      return new Conversion() {
        public double convert(double value) {
          return convFactor[MAXUNIT+1+to].convert(convFactor[from].convert(value));
        }
      };
    } else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Latitude &&
      theValue == ((Latitude) o).getDegrees());
  }
  public String toString() {
    return Double.toString(theValue) + "o";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  private boolean inBounds( double v ) {
    boolean ok;
    if ( (v <= upperBound) && (v >= lowerBound) ) {
      ok = true;
    } else {
      ok = false;
    }
    return ok;
  }

  /**
   * TODO : fill in
   * @param other
   * @return Measure
   */
  public Measure add(Measure other) {
    return null;
  }

  /**
   * TODO : fill in
   * @param other
   * @return Measure
   */
  public Measure subtract(Measure other) {
    return null;
  }

  public Measure negate() {
    return null;
  }
  public Measure scale(double scale) {
    return null;
  }

  public Measure floor(int unit) {
    return null;
  }
  public Measure valueOf(double value) {
    return null;
  }

  public Measure valueOf(double value, int unit) {
    return null;
  }

  public int getNativeUnit() {
    return 0;  
  }

  public double getNativeValue() {
    return getValue(getNativeUnit());
  }

  public Duration divide(Rate rate) {
    return null;
  }
} // end Latitude
