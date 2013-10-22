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

import java.io.Serializable;

/**
 * Implementation of Preference.
 **/
 
public class PreferenceImpl
  implements Preference, AspectType, Cloneable, Serializable
{
  private int aspect;
  private ScoringFunction scorefun;
  private float theweight;
   
  // Default constructor
   
  public PreferenceImpl()
  {
    super();
  }
   
  /** Simple Constructor 
   * @param aspecttype
   * @param scoringfunction
   * @see org.cougaar.planning.ldm.plan.AspectValue
   */
  public PreferenceImpl(int aspecttype, ScoringFunction scoringfunction) {
    super();
    aspect = aspecttype;
    scorefun = scoringfunction;
    theweight = (float)1.0;
  }
   
  /** Constructor that takes aspect type, scoring function and weight.
   * @param aspecttype
   * @param scoringfunction
   * @param weight
   * @see org.cougaar.planning.ldm.plan.AspectValue
   */
  public PreferenceImpl(int aspecttype, ScoringFunction scoringfunction, double weight) {
    super();
    aspect = aspecttype;
    scorefun = scoringfunction;
    this.theweight = (float)weight;
  }

  public Object clone() {
    ScoringFunction scoringFunction = (ScoringFunction) getScoringFunction();
    return new PreferenceImpl(getAspectType(),
                              (ScoringFunction) scoringFunction.clone(),
                              getWeight());
  }
     
  //Preference interface implementations
   
  /** @return int  The AspectType that this preference represents
   * @see org.cougaar.planning.ldm.plan.AspectType
   */
  public final int getAspectType() {
    return aspect;
  }
   
  /** @return ScoringFunction
   * @see org.cougaar.planning.ldm.plan.ScoringFunction
   */
  public final ScoringFunction getScoringFunction() {
    return scorefun;
  }
   
  /** A Weighting of this preference from 0.0-1.0, 1.0 being high and
   * 0.0 being low.
   * @return double The weight
   */
  public final float getWeight() {
    return theweight;
  }
   
  public boolean equals(Object o) {
    if (o instanceof PreferenceImpl) {
      PreferenceImpl p = (PreferenceImpl) o;
      return aspect==p.getAspectType() &&
        theweight==p.getWeight() &&
        scorefun.equals(p.getScoringFunction());
    } else
      return false;
  }

  public int hashCode() {
    return aspect+((int)theweight*1000)+scorefun.hashCode();
  }
  public String toString() {
    return "<Preference "+aspect+" "+scorefun+" ("+theweight+")>";
  }
}
