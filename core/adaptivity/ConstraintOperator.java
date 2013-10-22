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
 * Defines constants for operators used for describing and interpreting
 * relations between policy components.
 */
public class ConstraintOperator extends Operator {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private ConstraintOperator(String op) {
        super(op, 2, 4, 4);
    }

    /* equality and inequalities */
    public static final ConstraintOperator GREATERTHAN = new ConstraintOperator(">");
    public static final ConstraintOperator GREATERTHANOREQUAL = new ConstraintOperator(">=");
    public static final ConstraintOperator LESSTHAN = new ConstraintOperator("<");
    public static final ConstraintOperator LESSTHANOREQUAL = new ConstraintOperator("<=");
    public static final ConstraintOperator EQUAL = new ConstraintOperator("==");
    public static final ConstraintOperator ASSIGN = new ConstraintOperator("=");
    public static final ConstraintOperator NOTEQUAL = new ConstraintOperator("!=");
    
    /** set operators */
    
    public static final ConstraintOperator IN = new ConstraintOperator("IN");
    public static final ConstraintOperator NOTIN = new ConstraintOperator("NOT IN");
    /* What else ?*/
}
