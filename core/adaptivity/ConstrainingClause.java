/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/** 
 * Holds a parsed expression for testing the values of Conditions. The
 * expression is inserted in reverse Polish or postfix notation, but
 * read out in the opposite (prefix) order.
 **/

public class ConstrainingClause implements java.io.Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
public static final ConstrainingClause TRUE_CLAUSE;
  public static final ConstrainingClause FALSE_CLAUSE;
  static {
    TRUE_CLAUSE = new ConstrainingClause();
    TRUE_CLAUSE.push(BooleanOperator.TRUE);
    FALSE_CLAUSE = new ConstrainingClause();
    FALSE_CLAUSE.push(BooleanOperator.FALSE);
  }

  private List list = new ArrayList();
  /** 
   * Append an operator or operand onto the list. It is assumed that
   * the caller is constructing a well-formed expression.
   * @param o a String, Operator, ConstraintOpValue, or
   * ConstrainingClause. If another ConstrainingClause is pushed, its
   * entire contents is appended, otherwise the item itself is
   * appended.
   **/
  public void push(Object o) {
    if (o instanceof ConstrainingClause) {
      list.addAll(((ConstrainingClause) o).list);
    } else {
      list.add(o);
    }
  }
  
  /**
   * Gets an iterator over the contents in prefix order.
   * @return an iterator that can walk the clause for evaluation. The
   * iterator runs through the contents in the reverse order from
   * which the information was appended. This has the effect of
   * turning the reverse Polish (postfix) entry order into forward
   * Polish (prefix) order.
   **/
  public Iterator iterator() {
    return new Iterator() {
      private ListIterator iter = list.listIterator(list.size());
      public boolean hasNext() {
        return iter.hasPrevious();
      }
      public Object next() {
        return iter.previous();
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
public int hashCode() {
    return list.hashCode();
  }

  @Override
public boolean equals(Object o) {
    if (o instanceof ConstrainingClause) return list.equals(((ConstrainingClause) o).list);
    return false;
  }

  /**
   * Print this clause in infix notation with liberal parentheses
   **/
  private String toString(Iterator x) {
    if (!x.hasNext()) return "";
    String r = null;
    String l = null;
    StringBuffer buf = new StringBuffer();
    Object o = x.next();
    if (o instanceof ConstraintOpValue) {
      buf.append('(').append(toString(x)).append(' ').append(o).append(')');
    } else if (o instanceof Operator) {
      Operator op = (Operator) o;
      switch (op.getOperandCount()) {
      case 2:
        r = toString(x);
        l = toString(x);
        break;
      case 1:
        r = toString(x);
        break;
      default:
      }
      buf.append('(');
      if (l != null) buf.append(l).append(' ');
      buf.append(op);
      if (r != null) buf.append(' ').append(r);
      buf.append(')');
    } else {
      buf.append(o);
    }
    return buf.toString();
  }

  /**
   * Gets the expression in infix format.
   * @return a string representation of the expression using infix
   * notation. Parentheses are inserted liberally to make operator
   * precedence clear.
   **/
  @Override
public String toString() {
    return toString(iterator());
  }
}
