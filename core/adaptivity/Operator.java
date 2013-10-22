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


/**
 * Defines boolean operator constants.
 */
public class Operator implements java.io.Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public static final int MAXP = 10;
    protected String op;
    protected int lp;
    protected int rp;
    protected int nOperands;

    protected Operator(String op, int nOps, int lp, int rp) {
        this.op = op;
        this.lp = lp;
        this.rp = rp;
        nOperands = nOps;
    }

    public int getLP() {
        return lp;
    }
    public int getRP() {
        return rp;
    }
    public int getOperandCount() {
        return nOperands;
    }
    @Override
   public int hashCode() {
        return op.hashCode();
    }
    @Override
   public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Operator) {
            Operator that = (Operator) o;
            return (this.op.equals(that.op) &&
                    this.nOperands == that.nOperands);
        }
        return false;
    }
    @Override
   public String toString() {
        return op;
    }
}

















