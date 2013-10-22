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

package org.cougaar.core.blackboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.cougaar.util.StackElements;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A generic object tracker which is currently used to identify 
 * {@link CollectionSubscription}s that could be converted into
 * {@link DeltaSubscription}s.
 * <p>
 * This could be repackaged into "org.cougaar.util".
 */
class ObjectTracker {

  // TODO make this a weak map?  but we want == lookups..
  private final Map objects = new IdentityHashMap();
  private final Map stacks = new WeakHashMap();

  public void add(Object o) {
    StackElements se = captureStack();
    synchronized (objects) {
      objects.put(o, se);
    }
  }

  private StackElements captureStack() {
    StackElements se = new StackElements(new Throwable());
    synchronized (stacks) {
      StackElements cached_se = (StackElements) stacks.get(se);
      if (cached_se == null) {
        stacks.put(se, se);
      } else {
        se = cached_se;
      }
    }
    return se;
  }

  public void remove(Object o) {
    synchronized (objects) {
      objects.remove(o);
    }
  }

  /**
   * @return map from StackElements to Integer count
   */
  public Map getObjects() {
    // map stacks to count
    Map ret;
    synchronized (objects) {
      ret = new HashMap();
      for (Iterator iter = objects.values().iterator(); iter.hasNext(); ) {
        StackElements se = (StackElements) iter.next();
        Counter c = (Counter) ret.get(se);
        if (c == null) {
          c = new Counter();
          ret.put(se, c);
        }
        c.i++;
      }
    }
    // replace Counters with Integers
    for (Iterator iter = ret.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry me = (Map.Entry) iter.next();
      me.setValue(new Integer(((Counter) me.getValue()).i));
    }
    return ret;
  }
  private static class Counter { public int i; } 

  /**
   * Flatten a "getUsed()" map to just the active stack_element and counter.
   * <p>
   * @param m "getUsed()" map
   * @param ignored_classes optional low-level stack classnames to ignore
   * @return a list of Entry objects for each lowest-level stack_element
   */
  public static List flattenMap(Map m, String[] ignored_classes) {
    if (m == null || m.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    // get complete array with duplicate stack_elements
    List l = new ArrayList();
    for (Iterator iter = m.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry me = (Map.Entry) iter.next();
      StackElements se = (StackElements) me.getKey();
      int count = ((Integer) me.getValue()).intValue();
      StackTraceElement[] stea = se.getThrowable().getStackTrace();
      StackTraceElement ste = null;
      if (ignored_classes == null) {
        ste = stea[0];
      } else {
        for (int i = 0; i < stea.length; i++) {
          String s = stea[i].getClassName();
          boolean skip = false;
          for (int j = 0; j < ignored_classes.length; j++) {
            if (s.startsWith(ignored_classes[j])) {
              skip = true;
              break;
            }
          }
          if (skip) continue;
          ste = stea[i];
          break;
        }
      }
      l.add(new Entry(ste, count));
    }
    // sort by stack_element
    Collections.sort(l);
    // merge duplicate stack_element counters
    List l2 = new ArrayList();
    Entry prev = (Entry) l.get(0);
    l2.add(prev);
    for (int i = 1; i < l.size(); i++) {
      Entry e = (Entry) l.get(i);
      if (e.getStackTraceElement().equals(prev.getStackTraceElement())) {
        prev.setCount(prev.getCount() + e.getCount());
        continue;
      }
      prev = e;
      l2.add(e);
    }
    // sort by decreasing count then stack_element
    Collections.sort(l2, new Comparator() {
      public int compare(Object o1, Object o2) {
        Entry e1 = (Entry) o1;
        Entry e2 = (Entry) o2;
        int cmp = (e2.getCount() - e1.getCount());
        if (cmp != 0) return cmp;
        return e1.compareTo(e2);
      }
    });
    return l2;
  }

  public static void appendTo(Map m, boolean asMap, StringBuffer buf) {
    if (m == null || m.isEmpty()) {
      return;
    }
    boolean first = true;
    for (Iterator iter = m.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry me = (Map.Entry) iter.next();
      StackElements se = (StackElements) me.getKey();
      int count = ((Integer) me.getValue()).intValue();
      if (!first) {
        first = true;
        buf.append("\n");
      }
      buf.append("\"stack\" count=").append(count);
      StackTraceElement[] stea = se.getThrowable().getStackTrace();
      for (int i = 0; i < stea.length; i++) {
        buf.append("\n\tat ").append(stea[i]);
      }
      buf.append("\n");
    }
  }

  public static void appendTo(List l, boolean asMap, StringBuffer buf) {
    int n = (l == null ? 0 : l.size());
    for (int i = 0; i < n; i++) {
      Entry e = (Entry) l.get(i);
      if (i > 0) buf.append("\n");
      buf.append(e.getCount()).append(",\t").append(e.getStackTraceElement());
    }
  }

  /**
   * Periodically prints the object table to stdout.
   */
  public void startThread(final long period, final String[] ignored_classes) {
    final Logger log = Logging.getLogger(getClass());
    Runnable r = new Runnable() {
      public void run() {
        while (true) {

          StringBuffer buf = new StringBuffer();
          buf.append("############################################################\n");
          Map m = getObjects();
          //appendTo(m, false, buf);
          //buf.append("\n------------------------------------------------------------\n");
          buf.append("COUNT,\tMETHOD\n");
          List l = flattenMap(m, ignored_classes);
          appendTo(l, false, buf);
          buf.append("\n############################################################");
          log.shout(buf.toString());

          try {
            Thread.sleep(period);
          } catch (Exception e) {
            break;
          }
        }
      }
    };
    Thread t = new Thread(r, "Object Tracker");
    t.setDaemon(true);
    t.start();
  }

  public static class Entry implements Comparable {

    private final StackTraceElement ste;
    private int count;

    public Entry(StackTraceElement ste, int count) {
      this.ste = ste;
      this.count = count;
    }

    public StackTraceElement getStackTraceElement() { return ste; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    @Override
   public String toString() {
      return count+",\t"+ste;
    }

    public int compareTo(Object o2) {
      // first compare by stack
      StackTraceElement s1 = ste;
      StackTraceElement s2 = ((Entry) o2).getStackTraceElement();
      int cmp = s1.getClassName().compareTo(s2.getClassName());
      if (cmp != 0) return cmp;
      if (s1.getLineNumber() >= 0 && s2.getLineNumber() >= 0) {
        cmp = s1.getLineNumber() - s2.getLineNumber();
        if (cmp != 0) return cmp;
      }
      cmp = comp(s1.getMethodName(), s2.getMethodName());
      if (cmp != 0) return cmp;
      cmp = comp(s1.getFileName(), s2.getFileName());
      if (cmp != 0) return cmp;

      // next compare by decreasing count
      return count - ((Entry) o2).count;
    }

    private int comp(Comparable a, Comparable b) {
      return 
        (a == b ? 0 :
         a == null ? -1 : 
         b == null ? 1 :
         a.compareTo(b));
    }
  }
}
