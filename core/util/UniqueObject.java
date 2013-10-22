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

package org.cougaar.core.util;

import java.io.Serializable;

/**
 * An object with a {@link UID} created by the {@link
 * org.cougaar.core.service.UIDService}.
 * <p>
 * The UID is often set by a {@link org.cougaar.core.domain.Domain}
 * {@link org.cougaar.core.domain.Factory}, and/or as a final field
 * set in the object's constructor.
 * <p>
 * Objects that implement UniqueObject often use the UID for the
 * "equals(Object)" and "hashCode()" methods, e.g.<pre>
 *   public class X implements UniqueObject .. {.. 
 *     public boolean equals(Object o) {
 *       return
 *         (o == this ||
 *         ((o instanceof X) &amp;&amp;
 *          getUID().equals(((X) o).getUID()));
 *     }
 *     public int hashCode() {
 *       return getUID().hashCode();
 *     }
 *   } 
 * </pre>
 */
public interface UniqueObject extends Serializable {
  /**
   * @return the UID of this UniqueObject.
   */
  UID getUID();

  /**
   * Set the UID of this UniqueObject, which may throw a
   * RuntimeException if the UID is already set.
   */
  void setUID(UID uid);
}
