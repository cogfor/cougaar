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

package org.cougaar.core.blackboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * An {@link EnvelopeTuple} indicating that a collection of objects
 * have been added to the blackboard.
 */
public final class BulkEnvelopeTuple extends EnvelopeTuple {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final Collection bulk;
  @Override
public Object getObject() { return bulk; }

  public BulkEnvelopeTuple(Collection o) {
    if (o == null) throw new IllegalArgumentException("Collection is null");
    bulk = o;
  }

  @Override
public final int getAction() { return Envelope.BULK; }
  @Override
public final boolean isBulk() { return true; }
  public final Collection getCollection() { return bulk; }

  @Override
boolean applyToSubscription(Subscription s, boolean isVisible) {
    boolean changedP = false;

    if (bulk instanceof ArrayList) {
      ArrayList a = (ArrayList) bulk;
      int l = a.size();
      for (int i = 0; i < l; i++) {
        Object o = a.get(i);
        if (o == null) continue;
        changedP |=  s.conditionalAdd(o, isVisible);
      }
    } else {
      for (Iterator it = bulk.iterator(); it.hasNext(); ) {
        Object o = it.next();
        if (o == null) continue;
        changedP |=  s.conditionalAdd(o, isVisible);
      }
    }
    return changedP;
  }
}
