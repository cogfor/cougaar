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

package org.cougaar.planning.ldm.plan;

import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.measure.Rate;

/**
 * An AspectValue represented by a rate measure
 */
 
public class AspectRate extends TypedAspectValue {
  private Rate rate_value;

  protected AspectRate(int type, Rate new_rate_value) {
    super(type);
    rate_value = new_rate_value;
  }

  private static boolean hack_warnedUser; // FIXME big hack!

  public static AspectValue create(int type, Object o) {
    if (o instanceof Number) {
      double v = ((Number)o).doubleValue();
      if (v != 0.0) {
        if (!hack_warnedUser) {
          // this bug can easily occur in the thousands, so we
          // only make a fuss this once
          hack_warnedUser = true;
          org.cougaar.util.log.LoggerFactory.getInstance().createLogger(AspectRate.class).warn(
                                                                                               "BUG 2529: create("+type+", "+o+") with non-rate type "+
                                                                                               (o==null?"null":(o.getClass().getName()+": "+o))+
                                                                                               "!  This will be the *only* warning!", 
                                                                                               new RuntimeException("Trace"));
        }
      }
      // bogus!
      o = CountRate.newUnitsPerDay(v);
    }

    if (o instanceof Rate) {
      return new AspectRate(type, (Rate) o);
    } else {
      throw new IllegalArgumentException("Cannot create an AspectRate from "+o);
    }
  }
   
  public static AspectValue create(int type, Rate r) {
    return new AspectRate(type,r);
  }

  public static AspectValue create(int type, int v) {
    return new AspectRate(type,CountRate.newUnitsPerDay((double)v));
  }
  public static AspectValue create(int type, float v) {
    return new AspectRate(type,CountRate.newUnitsPerDay((double)v));
  }
  public static AspectValue create(int type, long v) {
    return new AspectRate(type,CountRate.newUnitsPerDay((double)v));
  }
  public static AspectValue create(int type, double v) {
    return new AspectRate(type,CountRate.newUnitsPerDay(v));
  }

  /** Non-preferred alias for #create(int, Rate).
   **/
  public static final AspectRate newAspectRate(int type, Rate r) {
    return new AspectRate(type, r);
  }
   
  /** The rate value of the aspect.
   * @note the preferred accessor is #rateValue()
   */
  public final Rate getRateValue() {
    return rate_value;
  }

  public final Rate rateValue() {
    return rate_value;
  }

  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final double doubleValue() {
    return (double) rateValue().getValue(rateValue().getCommonUnit());
  }
  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final long longValue() {
    return (long) doubleValue();
  }
  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final float floatValue() {
    return (float) doubleValue();
  }
  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final int intValue() {
    return (int) doubleValue();
  }

   
  public boolean equals(Object v) {
    if (!(v instanceof AspectRate)) {
      return false;
    } 
    AspectRate rate_v = (AspectRate) v;

    return (rate_v.getAspectType() == getAspectType() &&
            rate_v.rateValue().equals(rateValue()));
  }

  public String toString() {
    return rate_value.toString()+"["+getType()+"]";
  }
}
