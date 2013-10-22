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

package org.cougaar.core.wp;


/**
 * A utility class to format longs as:<pre>
 *    TIMESTAMP{[+-]RELATIVE_TO_NOW}
 * </pre>.
 * <p>
 * For example, if t=123 and now=300, then <tt>toString(t,now)</tt>
 * will return:<pre>
 *    123{-177}
 * </pre>
 * but if t=0 the method will return:<pre>
 *    0
 * </pre>
 */
public final class Timestamp {
  private Timestamp() { }

  public static String toString(long t) {
    if (t == 0) {
      return "0";
    } else {
      return toString(t, System.currentTimeMillis());
    }
  }

  public static String toString(long t, long now) {
    if (t == 0) {
      return "0";
    } else {
      long diff = t - now;
      return 
        t+"{"+
        (diff >= 0 ?
         "+"+diff+"}" :
         diff+"}");
    }
  }
}
