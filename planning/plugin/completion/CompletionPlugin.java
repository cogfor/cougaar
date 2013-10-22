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

package org.cougaar.planning.plugin.completion;

import java.text.SimpleDateFormat;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.util.EmptyIterator;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin gathers and integrates completion information from
 * agents in a society to determin the "completion" of the current
 * tasks. In most agents, it gathers the information and forwards the
 * completion status of the agent to another agent. This process
 * continues through a hierarchy of such plugins until the plugin at
 * the root of the tree is reached. When the root determines that
 * completion has been acheived (or is never going to be achieved), it
 * advances the clock with the expectation that the advancement will
 * engender additional activity and waits for the completion of that
 * work.
 **/

public abstract class CompletionPlugin extends ServiceUserPlugin {
  protected CompletionPlugin(Class[] requiredServices) {
    super(requiredServices);
  }
  protected static UnaryPredicate targetRelayPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof CompletionRelay) {
          CompletionRelay relay = (CompletionRelay) o;
          return relay.getSource() != null;
        }
        return false;
      }
    };

  /**
   * A Collection implementation the retains nothing. Used for a
   * Subscription to note publish events of interest, but for which
   * the actual objects are unneeded. Most of the work is done by the
   * AbstractCollection base class. We implement the minimum required
   * for a mutable Collection.
   **/
  protected class AmnesiaCollection extends AbstractCollection {
    public Iterator iterator() {
      return EmptyIterator.iterator();
    }
    public int size() {
      return 0;
    }
    public boolean add(Object o) {
      return false;
    }
  }

  protected void checkPersistenceNeeded(Collection relays) {
    for (Iterator i = relays.iterator(); i.hasNext(); ) {
      CompletionRelay relay = (CompletionRelay) i.next();
      if (relay.persistenceNeeded()) {
        setPersistenceNeeded();
        relay.resetPersistenceNeeded();
        blackboard.publishChange(relay);
      }
    }
  }

  protected abstract void setPersistenceNeeded();

  private static final SimpleDateFormat dateFormat =
    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
  private static Date fdate = new Date();
  public static String formatDate(long time) {
    synchronized (fdate) {
      fdate.setTime(time);
      return dateFormat.format(fdate);
    }
  }
}
