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
// import org.cougaar.planning.ldm.plan.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A Plan is an abstract data structure which consists of
 * a set of PlanElements (which are associations between Tasks and
 *  Allocations).
 */

public final class PlanImpl 
  implements Plan, Cloneable, Serializable
{

  private String planname;

  //no-arg constructor
  public PlanImpl() {
    super();
  }

  //constructor that takes string name of plan
  public PlanImpl (String s) {
    if (s != null) s = s.intern();
    planname = s;
  }

  /**@return String Name of Plan */
  public String getPlanName() {
    return planname;
  }

  public boolean equals(Object p) {
    return (this == p ||
            (planname != null && p instanceof PlanImpl
             && planname.equals(((PlanImpl)p).getPlanName())));
  }


  public String toString() {
    if (planname != null)
      return planname;
    else
      return "(unknown plan)";
  }


  //private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
  //  stream.defaultReadObject();
  //  if (planname != null) planname = planname.intern();
  //}


  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    if (planname != null) planname = planname.intern();
  }

  public static final Plan REALITY = new PlanImpl("Reality");
} 

