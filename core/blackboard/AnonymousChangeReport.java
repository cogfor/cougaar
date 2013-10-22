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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;

/**
 * A change report for an anonymous (unspecified) change.
 * <p>
 * This is used whenever a subscriber does:<pre>
 *     - publishChange(o)
 * or
 *     - publishChange(o, null)
 * or
 *     - publishChange(o, an-empty-collection)</pre>
 * <p>
 * Subscribers should watch for the "AnonymousChangeReport.SET".
 */
public final class AnonymousChangeReport implements ChangeReport {

  // singleton instance
  public static final AnonymousChangeReport INSTANCE =
    new AnonymousChangeReport();

  // a list containing the singleton
  public static final List<ChangeReport> LIST = new AnonList();

  // a set containing the singleton
  public static final Set<ChangeReport> SET = new AnonSet();


  private AnonymousChangeReport() { }

  private Object readResolve() { return INSTANCE; }

  @Override
public String toString() {
    return "anonymous";
  }

  static final long serialVersionUID = 1209837181010093282L;

  // singleton LIST with singleton-friendly "readResolve()":
  private static class AnonList extends AbstractList<ChangeReport>
    implements RandomAccess, Serializable {
      private AnonList() { }
      @Override
      public int size() {return 1;}
      @Override
      public boolean contains(Object obj) {return (obj == INSTANCE);}
      @Override
      public ChangeReport get(int index) {
        if (index != 0)
          throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
        return INSTANCE;
      }
      private Object readResolve() { return LIST; }
      static final long serialVersionUID = 3190948102986892191L;
    }

  // singleton SET with singleton-friendly "readResolve()":
  private static class AnonSet extends AbstractSet<ChangeReport>
    implements Serializable
    {
      private AnonSet() {}
      @Override
      public Iterator<ChangeReport> iterator() {
        return new Iterator<ChangeReport>() {
          private boolean hasNext = true;
          public boolean hasNext() {
            return hasNext;
          }
          public ChangeReport next() {
            if (hasNext) {
              hasNext = false;
              return INSTANCE;
            }
            throw new NoSuchElementException();
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
      @Override
      public int size() {return 1;}
      @Override
      public boolean contains(Object obj) {return (obj == INSTANCE);}
      private Object readResolve() { return SET; }
      private static final long serialVersionUID = 409580998934879938L;
    }

}
