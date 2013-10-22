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

package org.cougaar.core.thread;

import java.util.TimerTask;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This implementation of {@link Schedulable} is used by the trivial
 * {@link ThreadService}, which has no queueing and always runs threads
 * immediately.
 */
class TrivialSchedulable implements Schedulable {
    private final Object consumer;
    private final Runnable runnable;
    private Thread thread;
    private String name;
    private int start_count;
    private boolean cancelled;
    private TimerTask task;
    private long start_time;
    private int state;
    private boolean suspending; // suspend requested but not yet achieved
    private boolean suspended;  // truly suspended
    private SuspendCallback suspendCallback;

    TrivialSchedulable(Runnable runnable, String name, Object consumer) {
        this.runnable = runnable;
        if (name == null) {
            this.name = TrivialThreadPool.pool().generateName();
        } else {
            this.name = name;
        }
        this.consumer = consumer;
        this.start_count = 0;
    }

    // should only be used by the SerialThreadRunner/Queue
    void setState(int state) {
        this.state = state;
    }

    Runnable getRunnable() {
        return runnable;
    }

    public int getLane() {
        return org.cougaar.core.service.ThreadService.BEST_EFFORT_LANE;
    }

    public String getName() {
        return name;
    }

    public int getBlockingType() {
        return SchedulableStatus.NOT_BLOCKING;
    }

    public String getBlockingExcuse() {
        return "";
    }

    public long getTimestamp() {
        return start_time;
    }

    @Override
   public String toString() {
        return "<Schedulable " + (name == null ? "anonymous" : name) + ">";
    }

    public Object getConsumer() {
        return consumer;
    }

    // caller synchronizes
    private void thread_start() {
        start_count = 1; // forget any extra intervening start() calls
        start_time = System.currentTimeMillis();
        state = CougaarThread.THREAD_RUNNING;
        thread = runThread();
    }

    Thread runThread() {
        return TrivialThreadPool.pool().getThread(this, runnable, name);
    }

    void thread_stop() {
        SuspendCallback cb = null;
        boolean restart = false;
        synchronized (this) {
            if (suspending) {
                cb = suspendCallback;
                suspendCallback = null;
                suspended = true;
                suspending = false;
            }       
            state = CougaarThread.THREAD_DORMANT;
            // If start_count > 1, start() was called while the
            // Schedulable was running. Now that it's finished, start it
            // again.
            restart = !suspended && --start_count > 0;
            thread = null;
        }
        if (cb != null) {
            cb.suspended(this);
        }
        if (restart) {
            thread_start();
        }
    }

    public void suspend(SuspendCallback cb) {
        boolean runCallback = false;
        synchronized (this) {
            if (thread == null) {
                // Already suspended, run the callback immediately
                runCallback = suspended = true;

            } else if (suspendCallback != null) {
                throw new RuntimeException("More than one SuspendCallback!");
            } else {
                suspendCallback = cb;
                suspending = true;
            }
        }
        if (runCallback) {
            cb.suspended(this);
        }
    }

    public void resume() {
        boolean restart = false;
        synchronized (this) {
            if (!suspended) {
                Logger logger = Logging.getLogger(getClass());
                logger.warn("Resuming " +this+ ", which was not suspended");
                return;
            }
            suspended = false;
            restart = start_count > 0;
        }   
        if (restart) {
            thread_start();
        }
    }

    public void start() {
        synchronized (this) {
            // If the Schedulable has been cancelled, or has already
            // been asked to start, there's nothing further to do.
            if (cancelled) {
                return;
            }
            if (++start_count > 1 || suspended || suspending) {
                return;
            }
            thread_start();
        }
    }

    private TimerTask task() {
        cancelTimer();
        task = new TimerTask() {
            @Override
            public void run() {
                start();
            }
        };
        return task;
    }

    public synchronized void schedule(long delay) {
        TreeNode.timer().schedule(task(), delay);
    }

    public synchronized void schedule(long delay, long interval) {
        TreeNode.timer().schedule(task(), delay, interval);
    }

    public synchronized void scheduleAtFixedRate(long delay, long interval) {
        TreeNode.timer().scheduleAtFixedRate(task(), delay, interval);
    }

    public synchronized void cancelTimer() {
        if (task != null) {
            task.cancel();
        }
        task = null;
    }

    public synchronized int getState() {
        return state;
    }

    public boolean cancel() {
        synchronized (this) {
            cancelTimer();
            cancelled = true;
            start_count = 0;
            if (thread != null) {
                // Currently running.
                return false;
            }
            return true;
        }
    }
}
