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


/** Base (abstract) implementation of all Measure classes.
 * @see Measure for specification.
 **/

public abstract class AbstractMeasure implements Measure {
  
  public static AbstractMeasure newMeasure(String s, int unit) {
    throw new UnknownUnitException();
  }

  /** given a string like "100 meters", find the index of the 'm' 
   * in the units.  Will search for the first of either the char 
   * after a space or the first letter found.
   * if a likely spot is not found, return -1.
   **/
  protected final static int indexOfType(String s) {
    int l = s.length();
    for (int i = 0; i < l; i++) {
      char c = s.charAt(i);
      if (c == ' ') return i+1;
      if (Character.isLetter(c)) return i;
    }
    return -1;
  }

  public Measure min(Measure other) {
    return (compareTo(other) <= 0 ? this : other);
  }

  public Measure max(Measure other) {
    return (compareTo(other) >= 0 ? this : other);
  }

  public Measure apply(UnaryOperator op) {
    return op.apply(this);
  }

  public Measure apply(BinaryOperator op, Measure other) {
    return op.apply(this, other);
  }

  public int compareTo(Object o) {
    double da = getNativeValue();
    double db;
    if (o == null) {
      db = 0.0;
    } else {
      if (o.getClass() != getClass()) {
        throw new IllegalArgumentException(
            "Incompatible types:\n  "+this+"\n  "+o);
      }
      db = ((Measure) o).getNativeValue();
    }
    return (da < db ? -1 : da > db ? 1 : 0);
  }
}
