/*
 * <copyright>
 *  
 *  Copyright 2001-2007 BBNT Solutions, LLC
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

package org.cougaar.core.util;

/**
 * Minimal base class for UniqueObjects that use the UID for equality.
 */
public class UniqueObjectBase implements UniqueObject {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;

  public UniqueObjectBase(UID uid) {
    if (uid == null) {
      throw new IllegalArgumentException("null uid");
    }
    this.uid = uid;
  }

  public final UID getUID() {
    return uid;
  }
  public final void setUID(UID uid) {
    throw new IllegalStateException("UID cannot be changed");
  }

  @Override
public final boolean equals(Object o) {
    return 
      ((o == this) ||
       ((o instanceof UniqueObject) &&
        uid.equals(((UniqueObject) o).getUID())));
  }
  @Override
public final int hashCode() {
    return uid.hashCode();
  }
  @Override
public String toString() {
    return "("+getClass().getName()+" uid="+uid+")";
  }
}
