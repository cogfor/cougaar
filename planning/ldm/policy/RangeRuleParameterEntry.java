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


package org.cougaar.planning.ldm.policy;

/** Simple entry for RangeRuleParameters : 
    holds int min/max range and a value 
**/
public class RangeRuleParameterEntry implements java.io.Serializable {
  
  private Object my_value;
  private int my_min;
  private int my_max;

  public RangeRuleParameterEntry(Object value, int min, int max)  {
    my_value = value; 
    my_min = min; 
    my_max = max;
  }

  public RangeRuleParameterEntry() {
  }

  
  public int getMin() {
    return my_min;
  }
  public int getRangeMin() { 
    return getMin();
  }
  public void setMin(int min) {
    my_min = min;
  }
  
  public Object getValue() { return my_value; }
  public void setValue(Object value) {
    my_value = value;
  }

  public void setMax(int max) {
    my_max = max;
  }
  public int getMax() {
    return my_max;
  }
  public int getRangeMax() { 
    return getMax();
  }
  
  public String toString() { 
    return "[" + my_value + "/" + my_min + "-" + 
      my_max + "]"; 
  }
  
}






