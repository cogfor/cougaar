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

import java.util.Collections;
import java.util.Map;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Utility methods for batching requests.
 * <p>
 * All these methods have a lot of knowledge that's specific to the
 * cache and lease managers, so perhaps it should be refactored to
 * the client APIs.
 */
public final class Util {

  private Util() {}

  /**
   * Avoid nagle on first-time name listings, since these are
   * typically user-interface requests.
   */
  public static boolean mustSendNow(
      boolean lookup,
      String name,
      Object query) {
    // we could do an expensive stack check for a UI thread
    return
      lookup &&
      name != null &&
      name.length() > 0 &&
      name.charAt(0) == '.' &&
      query == null;
  }

  /**
   * Figure out if we should send this request, either because it's
   * new or it supercedes the pending request. 
   */
  public static boolean shouldSend(
      boolean lookup,
      String name,
      Object query,
      Object sentObj) {
    Object sentO = sentObj;
    if (sentO instanceof NameTag) {
      sentO = ((NameTag) sentO).getObject();
    }
    Object q = query;
    if (q instanceof NameTag) {
      q = ((NameTag) q).getObject();
    }

    boolean create = false;
    if (sentO == null ? q == null : sentO.equals(q)) {
      // already sent?
    } else {
      if (lookup) {
        if (q == null) {
          // promote from uid-based validate to full-lookup
          create = true;
        } else if (q instanceof UID) {
          if (sentO == null) {
            // already sent a full-lookup
          } else {
            // possible bug in cache manager, which is supposed to
            // prevent unequal uid-based validate races.  The uids
            // may have different owners, so we can't figure out
            // the correct order by comparing uid counters.
            Logger logger = Logging.getLogger(Util.class);
            if (logger.isWarnEnabled()) {
              logger.warn(
                  "UID mismatch in WP uid-based lookup validation, "+
                  "name="+name+", "+
                  "sentObj="+sentObj+", query="+query,
                  new Throwable("stacktrace"));
            }
          }
        } else {
          // invalid
        }
      } else {
        UID sentUID = 
          (sentO instanceof UID ? ((UID) sentO) :
           sentO instanceof Record ? ((Record) sentO).getUID() :
           null);
        UID qUID = 
          (q instanceof UID ? ((UID) q) :
           q instanceof Record ? ((Record) q).getUID() :
           null);
        if (sentUID != null &&
            qUID != null &&
            sentUID.getOwner().equals(qUID.getOwner())) {
          if (sentUID.getId() < qUID.getId()) {
            // send the query, since it has a more recent uid.
            // Usually q is a full-renew that should replace an
            // pending sentObj (either uid-renew or full-renew)
            // that's now stale.
            create = true;
          } else if (
              sentUID.getId() == qUID.getId() &&
              sentO instanceof UID &&
              q instanceof Record) {
            // promote from uid-renew to full-renew.  This is
            // necessary to handle a "lease-not-known" response
            // while a uid-renew is pending.
            create = true;
          } else {
            // ignore this query.  Usually the uids match, q is
            // a uid-renew, and we're still waiting for the
            // sentObj full-renew.  This also handles rare race
            // conditions, where the order of multi-threaded queries
            // passing through the lease manager is jumbled.
          }
        } else {
          // invalid
        }
      }
    }

    return create;
  }

  public static Map updateNodeModify(
      boolean lookup,
      Map m,
      MessageAddress agentId,
      Map sentM) {
    if (lookup || m == null) {
      return sentM;
    }
    String name = agentId.getAddress();
    Object q = m.get(name);
    if (q instanceof NameTag) {
      q = ((NameTag) q).getObject();
    }
    if (!(q instanceof Record)) {
      return sentM;
    }
    Record sentRecord;
    if (sentM == null) {
      sentRecord = null;
    } else {
      Map m2 = sentM;
      Object o2 = m2.values().iterator().next();
      if (o2 instanceof NameTag) {
        o2 = ((NameTag) o2).getObject();
      }
      sentRecord = (Record) o2;
    }
    Record r = (Record) q;
    if (sentRecord != null) {
      UID sentUID = sentRecord.getUID();
      UID qUID = r.getUID();
      if (!sentUID.getOwner().equals(qUID.getOwner())) {
        return sentM;
      }
      if (sentUID.getId() >= qUID.getId()) {
        return sentM;
      }
    }
    return Collections.singletonMap(name, q);
  }

  /**
   * Special test for local-node uid-based modify requests.
   */
  public static Object shortcutNodeModify(
      boolean lookup,
      MessageAddress agentId,
      String name,
      Object query) {
    // see if this is a uid-based modify for our own node
    if (lookup || !name.equals(agentId.getAddress())) {
      return null;
    }
    Object q = query;
    if (q instanceof NameTag) {
      q = ((NameTag) q).getObject();
    }
    if (!(q instanceof UID)) {
      return null;
    }
    // this is a uid-based renewal of our node, but maybe the server
    // crashed and forgot the record data necessary to send back a
    // "lease-not-known" response!
    //
    // the ugly fix is to pretend that the server sent back a
    // lease-not-known response.  This will force the lease
    // manager to send a renewal that contains the full record.
    //
    // if we're correct then the server's queued lease-not-known
    // messages may stream back, which is wasteful but okay.
    UID uid = (UID) q;
    Object answer = new LeaseNotKnown(uid);
    return answer;
  }

  /**
   * Figure out if we should accept this request response, including
   * whether or not we sent it and any necessary ordering/version
   * tests.
   */
  public static boolean shouldReceive(
      boolean lookup, 
      String name,
      Object answer,
      Object sentObj) {
    // see if this matches what we sent
    boolean accepted = false;
    if (lookup) {
      if (answer instanceof Record) {
        // we accept this, even if we sent a different UID,
        // since this is the latest value
        accepted = true;
      } else if (answer instanceof RecordIsValid) {
        if (sentObj == null) {
          // either we didn't send a uid-based lookup
          // or we just sent a full-record look request,
          // so ignore this.
        } else {
          UID uid = ((RecordIsValid) answer).getUID();
          if (uid.equals(sentObj)) {
            // okay, we sent this
            accepted = true;
          } else {
            // we sent a uid-based validation and
            // an ack for a different uid came back.  Our
            // uid-based message is still in flight, so either
            // we didn't send the lookup or this is a stale
            // message.
          }
        }
      } else {
        // invalid response
      }
    } else {
      UID uid;
      if (answer instanceof Lease) {
        uid = ((Lease) answer).getUID();
      } else if (answer instanceof LeaseNotKnown) {
        uid = ((LeaseNotKnown) answer).getUID();
      } else if (answer instanceof LeaseDenied) {
        uid = ((LeaseDenied) answer).getUID();
      } else {
        // invalid response
        uid = null;
      }
      UID sentUID;
      Object sentO = sentObj;
      if (sentO instanceof NameTag) {
        sentO = ((NameTag) sentO).getObject();
      }
      if (sentO instanceof UID) {
        sentUID = (UID) sentO;
      } else if (sentO instanceof Record) {
        sentUID = ((Record) sentO).getUID();
      } else {
        // we sent an invalid query?
        sentUID = null;
      }
      if (uid != null && uid.equals(sentUID)) {
        // okay, we sent this
        accepted = true;
      } else {
        // either we never sent this, or it's stale,
        // or the server is confused.  If we're mistaken
        // then our retry timer will resent the modify.
      }
    }

    return accepted;
  }
}
