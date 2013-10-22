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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.StackElements;
import org.cougaar.util.UnaryPredicate;

/**
 * A subscription that tracks the plugin publisher and add/change stacks for
 * UniqueObjects.
 * <p>
 * @see Subscriber must enable "-Dorg.cougaar.core.blackboard.trackPublishers=true"
 */
public class PublisherSubscription extends Subscription {

  // our per-uid table
  //Map<UID, Info>
  private final Map map = new HashMap(13);

  // if we're not shared, this is the transaction-pending removal list
  private final List removedList;

  // our temp variable that's used when updating this subscription.
  // it's only used in the distributor lock, so it's safe.
  private String name;

  public PublisherSubscription(UnaryPredicate p) {
    this(p, true);
  }

  /**
   * @param isShared use "false" for plugins and "true" for servlets.
   * This flag controls whether removes are immediately processed or
   * delayed until the end of the client's transaction.
   */
  public PublisherSubscription(UnaryPredicate p, boolean isShared) {
    super(p);
    removedList = (isShared ? null : new ArrayList(11));
  }

  //
  // all the methods from Subscription are provided.
  //

  /**
   * @return publish details for the UniqueObject.
   */
  public PublisherInfo getInfo(UID uid) {
    synchronized (map) {
      Info info = (Info) map.get(uid);
      if (info == null) {
        return null;
      } else {
        return new PublisherInfo(
            getPublisher(info),
            getAddStack(info),
            getChangeStacks(info));
      }
    }
  }

  private void privateAdd(Object o, StackElements se, boolean isVisible) {
    // always fill in the map, even if (!isVisible)
    if (name != null && o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      if (uid != null) {
        synchronized (map) {
          map.put(uid, newInfo(name, se));
        }
      }
    }
  }
  private void privateChange(Object o, List changes, StackElements se, boolean isVisible) {
    if (se != null && o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      if (uid != null) {
        synchronized (map) {
          Info info = (Info) map.get(uid);
          if (info != null) {
            Info newInfo = changeInfo(info, se);
            if (newInfo != info) {
              map.put(uid, newInfo);
            }
          }
        }
      }
    }
  }
  private void privateRemove(Object o, StackElements se, boolean isVisible) {
    if (removedList == null) {
      removeEntry(o);     // remove immediately
    } else {
      removedList.add(o); // wait until transaction close
    }
  }

  @Override
protected void privateAdd(Object o, boolean isVisible) {
    privateAdd(o, null, isVisible);
  }

  @Override
protected void privateChange(Object o, List changes, boolean isVisible) {
    privateChange(o, changes, null, isVisible);
  }

  @Override
protected void privateRemove(Object o, boolean isVisible) {
    privateRemove(o, null, isVisible);
  }

  private void removeEntry(Object o) {
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      synchronized (map) {
        map.remove(uid);
      }
    }
  }

  @Override
protected void resetChanges() {
    super.resetChanges();
    if (removedList != null) {
      // process removals
      int n = removedList.size();
      if (n > 0) {
        for (int i = 0; i < n; i++) {
          removeEntry(removedList.get(i));
        }
        removedList.clear();
      }
    }
  }

  @Override
public boolean apply(Envelope envelope) {
    if (envelope instanceof TimestampedEnvelope) {
      TimestampedEnvelope te = (TimestampedEnvelope) envelope;
      if (te.getTransactionCloseTime() >= 0) {
        this.name = te.getName();
        //return super.apply(envelope);
        {
          // based on subscription/tuple code:
          boolean vp = te.get_isVisible();
          boolean somethingFired = false;
          List deltas = te.getRawDeltas();
          int l = deltas.size();
          for (int i = 0; i<l; i++) {
            EnvelopeTuple tuple = (EnvelopeTuple) deltas.get(i);
            boolean b;
            if (tuple instanceof AddEnvelopeTuple) {
              Object object = tuple.getObject();
              b = predicate.execute(object);
              if (b) {
                privateAdd(object, tuple.getStack(), vp);
              }
            } else if (tuple instanceof ChangeEnvelopeTuple) {
              Object object = tuple.getObject();
              b = predicate.execute(object);
              if (b) {
                List changes = (List) ((ChangeEnvelopeTuple) tuple).getChangeReports();
                privateChange(object, changes, tuple.getStack(), vp);
              }
            } else if (tuple instanceof RemoveEnvelopeTuple) {
              Object object = tuple.getObject();
              b = predicate.execute(object);
              if (b) {
                privateRemove(object, tuple.getStack(), vp);
              }
            } else {
              b = tuple.applyToSubscription(this, vp);
            }
            somethingFired |= b;
          }
          return vp && somethingFired;
        }
      }
    } else if (envelope instanceof InitializeSubscriptionEnvelope) {
      super.apply(envelope);
    } else {
      // the subscriber -D was not set, so do nothing
    }
    return false;
  }

  // accessor methods
  private static String getPublisher(Info info) {
    return (info == null ? null : info.getPublisher());
  }
  private static StackElements getAddStack(Info info) {
    return (info == null ? null : info.getAddStack());
  }
  private static Set getChangeStacks(Info info) {
    Set ret = null;
    if (info != null) {
      ret = info.getChangeStacks();
      if (ret != null && ret.size() > 1) {
        // must make a copy, since our sub can modify the set
        ret = Collections.unmodifiableSet(new LinkedHashSet(ret));
      }
    }
    return ret;
  }

  // factory methods
  private static Info newInfo(String publisher, StackElements stack) {
    if (stack == null) {
      return new Info(publisher);
    } else {
      return new InfoAdd(publisher, stack);
    }
  }
  private static Info changeInfo(Info info, StackElements stack) {
    InfoAddChange iac;
    if (info instanceof InfoAddChange) {
      iac = (InfoAddChange) info;
    } else {
      iac = new InfoAddChange(info.getPublisher(), info.getAddStack());
    }
    iac.addChangeStack(stack);
    return iac;
  }

  // our info impls, with subclasses to avoid unnecessary fields
  private static class Info {
    private final String publisher;
    public Info(String publisher) {
      this.publisher = publisher;
    }
    public final String getPublisher() { return publisher; }
    public StackElements getAddStack() { return null; }
    // optionally add timestamps here..
    public Set getChangeStacks() { return null; }
  }
  private static class InfoAdd extends Info {
    private final StackElements add_stack;
    public InfoAdd(String publisher, StackElements add_stack) {
      super(publisher);
      this.add_stack = add_stack;
    }
    @Override
   public StackElements getAddStack() { return add_stack; }

  }
  private static class InfoAddChange extends InfoAdd {
    private Set change_stacks;
    public InfoAddChange(String publisher, StackElements stack) {
      super(publisher, stack);
    }
    @Override
   public Set getChangeStacks() {
      return change_stacks;
    }
    public void addChangeStack(StackElements se) { 
      if (change_stacks == null) {
        change_stacks = Collections.singleton(se);
      } else {
        if (change_stacks.size() == 1) {
          Object o = change_stacks.iterator().next();
          if (se.equals(o)) {
            return;
          }
          change_stacks = new LinkedHashSet();
          change_stacks.add(o);
        }
        change_stacks.add(se);
      }
    }
  }
}
