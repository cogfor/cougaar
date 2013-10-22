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

package org.cougaar.core.node;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.Service;

/**
 * This service provides access to the initial component model
 * {@link ComponentDescription} list, specifying which components
 * to load into the agents.
 */
public interface ComponentInitializerService extends Service {

  /**
   * Get the descriptions of components with the named parent having
   * an insertion point <em>below</em> the given container insertion point.
   * <p> 
   * For example, to get items with insertion point Node.AgentManager.Agent,
   * pass in Node.AgentManager. Then use 
   * <code>ComponentDescriptions.extractInsertionPointComponent("Node.AgentManager.Agent")</code> 
   * to get just those with the required insertion point. Typical
   * usage however is for the Node.AgentManager component to pass in its
   * own insertion point as a way to find its child components. 
   * <p>
   * Note that the returned value is in whatever order the underlying 
   * implementation uses.  It is <em>not</em> sorted by priority.
   */
  ComponentDescription[] getComponentDescriptions(
      String parentName, String insertionPoint)
    throws InitializerException;

  /**
   * @return true if the component initializer implementation includes
   * the default components (i.e. template based, such as the XML
   * initializer)
   */
  boolean includesDefaultComponents();

  /**
   * Generic exception for component initializer failures.
   */
  public class InitializerException extends Exception {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public InitializerException(String msg, Throwable t) {
      super(msg, t);
    }
  }
}
