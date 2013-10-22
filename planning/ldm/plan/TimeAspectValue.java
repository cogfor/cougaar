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
import org.cougaar.util.LRUCache;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.BitSet;

/** An AspectValue implementation which stores a time.
 */
 
public abstract class TimeAspectValue extends AspectValue {
  // 0 = LRU
  // 1 = hashmap
  private static final int CACHE_STYLE_DEFAULT = 0;
  private static final int CACHE_STYLE = 
    Integer.getInteger("org.cougaar.planning.ldm.plan.timeAV.cacheStyle",
                       CACHE_STYLE_DEFAULT).intValue();

  private static final int CACHE_SIZE_DEFAULT = 256;
  private static final int CACHE_SIZE =
    Integer.getInteger("org.cougaar.planning.ldm.plan.timeAV.cacheSize",
                       CACHE_SIZE_DEFAULT).intValue();

  // blank final!
  protected final long value;

  protected TimeAspectValue(long l) {
    this.value = l;
  }

  public final double doubleValue() { return (double) value; }
  public final long longValue() { return value; }
  public final float floatValue() { return (float) value; }
  public final int intValue() { return (int) value; }

  public Date dateValue() { return new Date(longValue()); }
  public long timeValue() { return longValue(); }
  
  // ignore type because they shouldn't coexist anyway
  public boolean equals(Object v) {
    return (v instanceof TimeAspectValue) &&
      ((TimeAspectValue) v).value == value;
  }
  public final int hashCode() {
    return (int)value;
  }

  public static AspectValue create(int type, Object o) {
    long l;
    if (o instanceof Date) {
      l = ((Date)o).getTime();
    } else if (o instanceof Number) {
      l = ((Number)o).longValue();
    } else {
      throw new IllegalArgumentException("Cannot create a TimeAspectValue from "+o);
    }

    return create(type, l);
  }
   
  // not thrilled with this...
  private static SimpleDateFormat dateTimeFormat =
    new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS z");
  private static Date formatDate = new Date();

  public String toString() {
    synchronized (formatDate) {
      formatDate.setTime(longValue());
      return dateTimeFormat.format(formatDate) + "[" + getType() + "]";
    }
  }

  //
  // unoptimized TAV
  //
  public static class TypedTimeAspectValue extends TimeAspectValue {
    private int type;
    protected TypedTimeAspectValue(int type, long value) {
      super(value);
      this.type = type;
    }
    public final int getType() { return type; }
  }

  //
  // optimized time AVs
  //

  /** Base class for optimized time-based Aspect Values
   **/

  public static abstract class OptimizedTimeAspectValue extends TimeAspectValue {
    protected OptimizedTimeAspectValue(long value) {
      super(value);
    }
    
    // always go via the factories on reads
    private Object readResolve() {
      return create(getType(), value);
    }

  }

  public static final class StartTAV extends OptimizedTimeAspectValue {
    public final int getType() { return AspectType.START_TIME; }
    public  StartTAV(long l) { super(l); }
  }
  public static final class EndTAV extends OptimizedTimeAspectValue {
    public final int getType() { return AspectType.END_TIME; }
    public EndTAV(long l) { super(l); }
  }
  public static final class DurationTAV extends OptimizedTimeAspectValue {
    public final int getType() { return AspectType.DURATION; }
    public DurationTAV(long l) { super(l); }
  }
  public static final class IntervalTAV extends OptimizedTimeAspectValue {
    public final int getType() { return AspectType.INTERVAL; }
    public IntervalTAV(long l) { super(l); }
  }
  public static final class PodTAV extends OptimizedTimeAspectValue {
    public final int getType() { return AspectType.POD_DATE; }
    public PodTAV(long l) { super(l); }
  }


  private static abstract class OTF {
    /** actually create a new instance **/
    public abstract AspectValue create(long l);
    
    private final Map cache;
    public OTF() {
      if (CACHE_STYLE == 1) {
        cache = new HashMap(CACHE_SIZE);
      } else {
        cache = new LRUCache(CACHE_SIZE);
      }
    }

    /** Find or create an aspect value **/
    public AspectValue get(long l) {
      // this is nominally faster than using AVs as keys
      Long key = new Long(l);
      synchronized (cache) {
        AspectValue oav = (AspectValue)cache.get(key);
        if (oav != null) {
          return oav;
        } else {
          AspectValue av = create(l);
          cache.put(key,av);
          return av;
        }
      }
    }
  }

  private static final int OTFL = AspectType.N_CORE_ASPECTS;
  private static final OTF[] factory = new OTF[OTFL];

  static {
    factory[AspectType.START_TIME] = new OTF() { 
        public AspectValue create(long l) { return new StartTAV(l); }};
    factory[AspectType.END_TIME] = new OTF() { 
        public AspectValue create(long l) { return new EndTAV(l); }};
    factory[AspectType.DURATION] = new OTF() { 
        public AspectValue create(long l) { return new DurationTAV(l); }};
    factory[AspectType.INTERVAL] = new OTF() { 
        public AspectValue create(long l) { return new IntervalTAV(l); }};
    factory[AspectType.POD_DATE] = new OTF() { 
        public AspectValue create(long l) { return new PodTAV(l); }}; 
  }
          
  public static AspectValue create(int type, long value) {
    if (type >= 0 && type < OTFL) {
      OTF f = factory[type];
      if (f != null) {
        return f.get(value);
      }
    }
    return new TypedTimeAspectValue(type, value);
  }

  // current tested times on my machine are:
  // secs/ (128 invokes)
  // 0.011657		create(t,v) without using factory
  // 0.063640		create(t,v) with factory (including LRU lookup) (5.46x)
  // 0.035438		create(t,v) with factory (using hashmap) (3.04x)
}
