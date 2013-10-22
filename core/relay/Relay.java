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

package org.cougaar.core.relay;

import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A Relay is a blackboard object that allows a source agent
 * to stream content changes to multiple target agents, and
 * for each target to stream back a response.
 * <p>
 * The Relay API is a intended to be a generic mechanism for
 * transfering data between agents.  Specific subclasses and
 * clients may not need the full Relay features, such as
 * multiple-targets or target-responses.
 */
public interface Relay extends UniqueObject {
  // The kinds of changes caused by updateResponse and updateContent
  int NO_CHANGE       = 0;
  int CONTENT_CHANGE  = 1;
  int RESPONSE_CHANGE = 2;

  // UID support from UniqueObject -- note that the
  // Source and Target(s) all have the same UID.

  /**
   * The source-side Relay, which contains the content and
   * a set of target-addresses, and also receives response 
   * updates from the Target(s).
   */
  interface Source extends Relay {

    /**
     * Get the addresses of the target agents to which this
     * Relay should be sent.
     */
    Set getTargets();

    /**
     * Get an object representing the value of this Relay
     * suitable for transmission. Caution!!! The content should, in
     * general <em>not</em> contain targets, source, responses,
     * factories or anything else that is not essential for specifying
     * the content. For implementations wishing to use the same class
     * for implementing Relay.Source, Relay.Target, and Relay.Content,
     * these non-essential items should be declared transient and
     * writeObject and readObject methods written to serialize these
     * items only for persistence, that is, only if the object streams
     * are instances of PersistenceOutputStream or
     * PersistenceInputStream.
     * <p>Also, see the cautions for
     * {@link #getTargetFactory getTargetFactory}
     */
    Object getContent();

    /**
     * Get a factory for creating the target.
     * Null indicates that the content can be directly cast into
     * the Target object. Be wary of aliasing of the content between
     * multiple agents in the same node. If the content is immutable,
     * there is no problem, but if the content can be changed by
     * either the source or target, the content should be cloned or
     * otherwise used to create a new target object.
     */
    TargetFactory getTargetFactory();

    /**
     * Update the source with the new response.
     * @return NO_CHANGE if the update caused no change to the source<br>
     *         RESPONSE_CHANGED if the source is also a target and the
     * incoming response requires that a new outgoing response be sent
     * to the source.<br>
     *         CONTENT_CHANGED if the update has changed the content of
     * the source<br>
     *         RESPONSE_CHANGED|CONTENT_CHANGED if both changes occurred
     */
    int updateResponse(MessageAddress target, Object response);
  }

  /**
   * The target-side Relay, which receives content updates and
   * can send response updates back to the Source.
   */
  interface Target extends Relay {

    /**
     * Get the address of the Agent holding the Source copy of this
     * Relay. If null is returned, then responses will not be sent.
     * @return the address from which this Relay came.
     */
    MessageAddress getSource();

    /**
     * Get the current Response for this target. Null indicates that
     * this target has no response.
     */
    Object getResponse();

    /**
     * Update the target with the new content.
     * @return NO_CHANGE if the update caused no change to the target<br>
     *         RESPONSE_CHANGED if the update has changed the response to
     * be sent to the source.<br>
     *         CONTENT_CHANGED if the update has changed the content of
     * the target<br>
     *         RESPONSE_CHANGED|CONTENT_CHANGED if both changes occurred
     */
    int updateContent(Object content, Token token);
  }

  /**
   * A factory for creating a Target from the Source's content.
   */
  interface TargetFactory {

    /**
     * Convert the given content and related information into a 
     * Target, that will be published on the target's blackboard.
     * <p>
     * If the implementation simply casts the content into the
     * Target then the Source can instead simply use:<pre>
     *   public TargetFactory getTargetFactory() { return null; }
     * </pre>
     * <p>
     * Care must be taken when the same class implements both
     * Relay.Source and Relay.Target. When a relay is transmitted between agents
     * in the same node the received content is the same object as the
     * source content. Usually, the factory should create a new
     * instance to avoid aliasing pitfalls. The factory can avoid the
     * expense of creating a new instance by taking advantage of the
     * treatment of transient fields in the content. Such fields are
     * initialized to default values when deserialized in another node
     * and such default values can be used to detect that the content
     * has already been copied and a new copy is not necessary.
     * <p>
     * Other implementations may create an instance of a different 
     * class that is just sufficient to implement the Relay.Target 
     * interface.  In particular, the content can be trimmed to the
     * minimal information needed to create the Target.
     * <p>
     * In some cases the UID and source address may be redundant
     * with the content information; this information allows
     * the content to be trimmed, such as just passing a String.
     */
    Relay.Target create(
        UID uid, MessageAddress source, Object content, Token token);

  }

  /**
   * An object that is passed from the Source to the Target(s), which
   * authorizes content updates.
   * <p>
   * The Target implementation can optionally save the Token
   * that was used in the factory "create" and later assert that
   * all content-updates must pass the same Token instance.
   */
  class Token implements java.io.Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   /** Restricted to infrastructure-only construction. */
    Token() {} 
  }
}
