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

import java.util.HashMap;

import org.cougaar.util.UnaryPredicate;

/**
 * A useful predicate class and generator for selecting
 * predicates by type.  Assumes that it will only be
 * called on Preferences (or it will get a ClassCastException).
 **/
 
public final class PreferencePredicate implements UnaryPredicate
{
  private final int aspect;

  private PreferencePredicate(int a) {
    aspect=a;
  }

  public boolean execute(Object o) {
    return (aspect == ((Preference)o).getAspectType());
  }

  private static final UnaryPredicate predVector[];
  private static final HashMap predTable = new HashMap(11);
  static {
    int l = AspectType._ASPECT_COUNT;
    predVector = new UnaryPredicate[l];
    for (int i = 0; i<l; i++) {
      predVector[i] = new PreferencePredicate(i);
    }
  }

  /** @return a unary predicate which returns true IFF the object
   * preference's aspect type is the same as the argument.
   **/
  public static UnaryPredicate get(int aspectType) {
    if (aspectType<0) throw new IllegalArgumentException();

    if (aspectType<=AspectType._LAST_ASPECT) {
      // handle the common ones from a pre-initialized vector
      return predVector[aspectType];
    } else {
      // hash on the aspectType for the rest.
      Integer k = new Integer(aspectType);
      UnaryPredicate p;
      if ((p = (UnaryPredicate) predTable.get(k)) != null) return p;
      synchronized (predTable) {
        //if ((p = predTable.get(k)) != null) return p;
        p = new PreferencePredicate(aspectType);
        predTable.put(k,p);
        return p;
      }
    }
  }
}
