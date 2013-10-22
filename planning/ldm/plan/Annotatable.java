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

/** Annotatable marks a Plan Object which supports attachment
 * of a plugin annotation.
 **/

public interface Annotatable 
{
  /**
   * Get the plugin annotation (if any) attached to the Annotatable
   * object.  
   *
   * Only the creating plugin of an Annotatable object
   * should use this accessor.
   *
   * @see org.cougaar.planning.ldm.plan.Annotation
   * @return the Annotation or null.
   **/
  Annotation getAnnotation();

  /**
   * Set the plugin annotation attached to the Annotatable 
   * Object.
   *
   * Only the creating plugin of an Annotatable object
   * should use this accessor.
   *
   * Plugins are encouraged but not required to only
   * set the annotation one.
   *
   * @see org.cougaar.planning.ldm.plan.Annotation
   **/
  void setAnnotation(Annotation pluginAnnotation);
}

