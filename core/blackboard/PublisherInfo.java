/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.blackboard;

import java.io.Serializable;
import java.util.Set;

import org.cougaar.util.StackElements;

/**
 * A data structure returned by {@link PublisherSubscription}.
 */
public final class PublisherInfo implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final String publisher;
  private final StackElements add_stack;
  private final Set change_stacks;

  public PublisherInfo(
      String publisher,
      StackElements add_stack,
      Set change_stacks) {
    this.publisher = publisher;
    this.add_stack = add_stack;
    this.change_stacks = change_stacks;
  }

  /**
   * @return the plugin that published the object.
   */
  public String getPublisher() { return publisher; }

  /**
   * @return the stack where the publishAdd occurred.
   */
  public StackElements getAddStack() { return add_stack; }

  /**
   * @return an ordered set of unique stacks where the object
   * was publishChanged, or null if it was never changed.
   */
  public Set getChangeStacks() { return change_stacks; }

  @Override
public String toString() {
    return 
      "(publisherInfo "+
      "\n  publisher="+publisher+
      "\n  add_stack="+(add_stack != null)+
      "\n  change_stacks="+(change_stacks == null ? 0 : change_stacks.size())+
      ")";
  }
}
