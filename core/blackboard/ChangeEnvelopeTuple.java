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

import java.util.Collection;
import java.util.List;

/**
 * An {@link EnvelopeTuple} indicating that an object on the
 * blackboard has been modified.
 */
public class ChangeEnvelopeTuple extends EnvelopeTuple {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final Object object;
  @Override
public Object getObject() { return object; }
  private final List changes;

  // perhaps at some point we should complain if we aren't told what the
  // changes are...
  public ChangeEnvelopeTuple(Object o, List changes) {
    if (o == null) throw new IllegalArgumentException("Object is null");
    object = o;
    this.changes = changes;
  }

  @Override
public final int getAction() { return Envelope.CHANGE; }
  @Override
public final boolean isChange() { return true; }

  // useful for Logic Providers.
  public Collection getChangeReports() { return changes; }

  @Override
boolean applyToSubscription(Subscription s, boolean isVisible) {
    return s.conditionalChange(object, changes, isVisible);
  }

}
