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

import java.util.Collection;
import java.util.HashSet;

/** ContextOfOplanIds is an implementation of Context. It is simply a Set of Oplan IDs. 
 * It can be used when the current oplan under which to operate needs to be referenced.
 * @see "org.cougaar.glm.ldm.oplan.Oplan"
 * @see Context
 */
public class ContextOfOplanIds 
  extends HashSet<String> implements Context, Collection<String>
{

  public ContextOfOplanIds (Collection<String> ids) {
    addAll(ids);
  }

  /** 
   * Constructor that creates a collection with one and only one oplanID
   * @param oneOplanId to add
   */
  public ContextOfOplanIds(String oneOplanId){
    add(oneOplanId);
  }

  /**
   * A constructor that copies the elements of the passed in array into the collection
   */
  public ContextOfOplanIds(String[] arrayOfOplanIDS) {
    for (String anArrayOfOplanIDS : arrayOfOplanIDS) {
      add(anArrayOfOplanIDS);
    }
  }

  /**
   * There should always be exactly one oplan
   * @return only oplan id
   */
  public String getOPlanId() {
    if (isEmpty()) {
      System.err.println("huh? no oplans?");
      return "";
    }
    return iterator().next();
  }
}
