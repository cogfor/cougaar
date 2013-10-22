/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.plugin.freeze;

import java.io.Serializable;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * A {@link org.cougaar.core.relay.Relay.TargetFactory} for
 * freeze {@link Relay}s.
 * <p>
 * NOTE: This is part of the older mechanism for freezing the society.  The
 * current mechanism uses FreezeServlet located on every agent in the society,
 * and depends on some external process to tell all agents to freeze.  This older
 * mechanism has not been removed so that people can continue to use a single servlet
 * to freeze the entire society, but the FreezeServlet mechanism is preferred now.
 */
public class FreezeRelayFactory
  implements Relay.TargetFactory, NotPersistable, Serializable
{
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private static FreezeRelayFactory instance;

  public static synchronized Relay.TargetFactory getTargetFactory() {
    if (instance == null) {
      instance = new FreezeRelayFactory();
    }
    return instance;
  }

  // TargetFactory implementation
  public Relay.Target create(UID uid,
                             MessageAddress source,
                             Object content,
                             Relay.Token token)
  {
    FreezeRelayTarget result = new FreezeRelayTarget(source);
    result.setUID(uid);
    return result;
  }
}
