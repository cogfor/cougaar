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

package org.cougaar.core.wp.resolver;

import java.io.Serializable;

/**
 * A simple wrapper around an object that adds a String name.
 * <p>
 * This is used to tag a request (e.g. a Record-based modify)
 * with the name of the agent requesting the action.  Most
 * clients will simply remove the tag to get to the wrapped
 * object.
 */
public final class NameTag implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final String name;
  private final Object obj;

  public NameTag(String name, Object obj) {
    this.name = name;
    this.obj = obj;
    // validate
    String s =
      ((name == null) ? "null name" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * @return the name 
   */
  public String getName() {
    return name;
  }

  /**
   * @return the wrapped object
   */
  public Object getObject() {
    return obj;
  }

  @Override
public String toString() {
    return "(tag name="+name+" value="+obj+")";
  }
}
