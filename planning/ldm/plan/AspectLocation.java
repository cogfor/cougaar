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


/**
 * An AspectValue with a location instead of a value.
 *
 */
 
public class AspectLocation extends TypedAspectValue {
  private Location loc_value;

  protected AspectLocation(int type, Location new_loc_value) {
    super(type);
    this.loc_value = new_loc_value;
  }

  private static boolean hack_warnedUser; // FIXME big hack!

  public static AspectValue create(int type, Object o) {
    if (o instanceof Number && ((Number)o).doubleValue() == 0.0) {
      if (!hack_warnedUser) {
        // this bug can easily occur in the thousands, so we
        // only make a fuss this once
        hack_warnedUser = true;
        org.cougaar.util.log.LoggerFactory.getInstance().createLogger(AspectLocation.class).warn(
            "BUG 2509: create("+type+", "+o+") with non-location type "+
            (o==null?"null":(o.getClass().getName()+": "+o))+
            "!  This will be the *only* warning!", 
            new RuntimeException("Trace"));
      }
      // bogus!
      o = new Location(){};
    }
    if (o instanceof Location) {
      return new AspectLocation(type, (Location) o);
    } else {
      throw new IllegalArgumentException(
          "Cannot construct an AspectLocation from "+
          (o==null?"null":(o.getClass().getName()+": "+o)));
    }
  }

  public final double doubleValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }
  public final long longValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }
  public final float floatValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }
  public final int intValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }

  /** The location associated with the AspectValue.
   * @note locationValue is the preferred method.
    */
  public final Location getLocationValue() { return loc_value;}

  /** The location associated with the AspectValue. */
  public final Location locationValue() { return loc_value;}

  public int hashCode() {
    return getType()+loc_value.hashCode();
  }

  public String toString() {
    return loc_value+"["+getType()+"]";
  }

  public boolean nearlyEquals(Object o) {
    return
      (o instanceof AspectValue &&
       this.equals((AspectValue) o));
  }

  public boolean equals(AspectValue v) {
    if (v instanceof AspectLocation) {
      AspectLocation loc_v = (AspectLocation)v;
      return (loc_v.getAspectType() == getType() &&
              loc_v.getLocationValue() == getLocationValue());
    } else {
      return false;
    }
  }
}
