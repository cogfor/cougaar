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
package org.cougaar.planning.ldm.measure;
import java.lang.reflect.Constructor;

/** A generic Scalar per Duration class that can be interpreted in a 
 * variety of ways depending on context.
 **/

public class Capacity extends AbstractMeasure {
  private Scalar quantity;
  private Duration period;

  /** Construct a capacity object which represents
   * a Scalar Measure (e.g. Volume, Distance, etc) divided by a
   * Duration (time).  Example: Miles per hour, hours per day, gallons per second.
   * May also be used
   **/
  public Capacity(Scalar quantity, Duration period) {
    this.quantity=quantity;
    this.period = period;
  }

  /** String constructor.  Allowed syntax is:
   * "Scalarmeasure=ValueUnits Duration=ValueUnits"
   * Example: "Count=20units Duration=1weeks"
   * For instantaneous: "Count=20units"
   **/
  public Capacity(String s) {
    int i = s.indexOf(' ');
    String qs;
    String ps;
    if (i == -1) {
      qs = s;
      ps = null;
    } else {
      qs = s.substring(0,i);
      ps = s.substring(i+1);
    }

    try {
      Class[] sa = new Class[1];
      sa[0] = String.class;

      int qse = qs.indexOf('=');
      String qsn = qs.substring(0,qse);
      Class qsc = Class.forName("org.cougaar.planning.ldm.measure."+qsn);
      Constructor qscc = qsc.getConstructor(sa);
      Object[] qargs = new Object[1];
      qargs[0] = qs.substring(qse+1);
      quantity = (Scalar) qscc.newInstance(qargs);

      if (ps != null) {
        int pse = ps.indexOf('=');
        String psn = ps.substring(0,pse);
        Class psc = Class.forName("org.cougaar.planning.ldm.measure."+psn);
        Constructor pscc = psc.getConstructor(sa);
        Object[] pargs = new Object[1];
        pargs[0] = ps.substring(pse+1);
        period = (Duration) pscc.newInstance(pargs);
      } else {
        period = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Exception: "+e);
    }
  }

  /** @return an illegal value, since Capacity is a tuple **/
  public int getCommonUnit() { return -1; }
  public int getMaxUnit() { return -1; }
  /** @return null, since Capacity is a tuple **/
  public String getUnitName(int i) { return null; }

  public double getValue(int unit) { throw new IllegalArgumentException(); }

  /** Construct an "instantaneous" capacity object - e.g. 
   * the capactity of "holding 10000 Gallons" or "having 48 Het-equivalents".
   **/
  public Capacity(Scalar quantity) {
    this.quantity=quantity;
    this.period = null;
  }

  /** @return the numerator of the Capacity **/
  public Scalar getQuantity() { return quantity; }

  /** @return the denominator of the Capacty **/
  public Duration getPeriod() { return period; }

  /** is this a representation of instantaneous capacity? **/
  public boolean isInstantaneous() { return period==null; }

  /** compute the figure that the capacity represents. E.g.
   * Capacity cap = new Capacity(new Volume("10 liters"), new Duration("1 seconds"));
   * double rate = cap.getCapacity(Volume.GALLONS, Duration.HOURS);
   *
   * Note that to get the quantityUnits correct, you need to know
   * what the concrete type of the quantity is.
   *
   * @exception UnknownUnitException if a bad unit in either argument.
   * @exception DivideByZero if duration is 0.
   * @exception NullPointerException if duration is null.
   **/
  public double getRate(int quantityUnits, int periodUnits) {
    if (isInstantaneous())
      throw new IllegalArgumentException("Cannot compute rate of an instantaneous Capacity");
    return quantity.getValue(quantityUnits) /
      period.getValue(periodUnits);
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

  /**
   * TODO : fill in
   * @param other
   * @return Measure
   */
  public Measure multiply(Measure other) {
    return null;
  }

  /**
   * TODO : fill in
   * @param other
   * @return Measure
   */
  public Duration divide(Rate other) {
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

  public String toString() {
    if (isInstantaneous()) {
      return "Capacity of "+quantity;
    } else {
      return quantity+" per "+period;
    }
  }

  public boolean equals(Object o) {
    if (o instanceof Capacity) {
      Capacity oc = (Capacity) o;
      Duration ocp = oc.getPeriod();
      if ((period == null) != (ocp == null)) return false;
      if (period == null)
        return quantity.equals(oc.getQuantity());
      else
        return (getRate(0,0) == oc.getRate(0,0));
    } else
      return false;
  }

  public int hashCode() {
    int qh = quantity.hashCode();
    if (period != null)
      qh = (qh<<2) + period.hashCode();
    return qh;
  }

}
