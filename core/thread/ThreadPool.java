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

import java.util.ArrayList;
import java.util.List;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A pool of native Java threads used by the standard implementation of the
 * {@link ThreadService}. By default this pool has a fixed size.
 */
class ThreadPool {

    static final class PooledThread extends Thread {
        private static final long MAX_CONTINUATION_TIME = 100;

        private SchedulableObject schedulable;

        private boolean in_use = false;

        private boolean should_stop = false;

        /** reference to our thread pool so we can return when we die * */
        private final ThreadPool pool;

        /**
         * Has this thread already be actually started yet? access needs to be
         * guarded by runLock.
         */
        private boolean isStarted = false;

        /** are we actively running the runnable? * */
        private boolean isRunning = false;

        /**
         * guards isRunning, synced while actually executing and waits when
         * suspended.
         */
        private final Object runLock;

        SchedulableObject getSchedulable() {
            return schedulable;
        }

        /** The only constructor. * */
        private PooledThread(ThreadPool p, String name) {
            super(p.getThreadGroup(), null, name);
            runLock = new NamedLock(name);
            setDaemon(true);
            pool = p;
        }

        // Hook for subclasses
        private void claim() {
            schedulable.claim();
        }

        // Keep running Schedulables as long as we have one to run,
        // or we've been running for too long.
        private void continuationLoop() {
            SchedulableObject last_schedulable = null;
            long continuation_start = System.currentTimeMillis();
            while (schedulable != null) {
                last_schedulable = schedulable;
                claim();
                try {
                    schedulable.run();
                } catch (Throwable any_ex) {
                    pool.logger.error("Uncaught exception in pooled thread (" + schedulable
                            + ")", any_ex);
                }

                isRunning = false;
                long elapsed = System.currentTimeMillis() - continuation_start;
                if (elapsed < MAX_CONTINUATION_TIME) {
                    schedulable = reclaim(true);
                } else {
                    // Too much time, 
                    reclaim(false);
                    schedulable = null;
                    break; // don't look for anything else to run
                }
            }
            in_use = false; // thread is now reusable
            if (last_schedulable != null) {
                last_schedulable.addToReclaimer();
            }
        }
               
        @Override
      public final void run() {
            synchronized (runLock) {
                while (true) {
                    
                    // Run schedulables as long as we have one to run
                    continuationLoop();
                    
                    if (should_stop) {
                    	// Thread service is shutting down
                        return;
                    }
                    
                    // Wait for more work, ignoring spurious wakeups and interrupts
					do {
						try {
							runLock.wait();
							if (should_stop) {
								// Thread service is shutting down
								return;
							}
							if (schedulable == null) {
								pool.logger.warn("Spurious wake up (no work), runLock = "
												+ runLock);
							}
						} catch (InterruptedException ie) {
							pool.logger.warn("Unexpected interrupt, thread="
									+ this);
						}
					} while (schedulable == null);                
                }
            }
        }

        @Override
      public void start() {
            throw new RuntimeException("You can't call start() on a PooledThread");
        }

        void start_running(SchedulableObject newSchedulable) throws IllegalThreadStateException {
            synchronized (runLock) {
            	schedulable=newSchedulable;
                if (isRunning) {
                    throw new IllegalThreadStateException("PooledThread already started: "
                            + schedulable);
                }
                isRunning = true;

                if (!isStarted) {
                    isStarted = true;
                    super.start();
                } else {
                    if (schedulable == null) {
                        pool.logger.warn(this + " was started with no work");
                    }
                    runLock.notify(); // resume
                }
            }
        }

        void stop_running() {
            synchronized (runLock) {
                should_stop = true;
                runLock.notify();
            }
            try {
                join();
            } catch (InterruptedException ie) {
            }
            synchronized (runLock) {
                should_stop = false;
                isStarted = false;
            }
        }

        private SchedulableObject reclaim(boolean reuse) {
            SchedulableObject new_schedulable = schedulable.reclaim(reuse);
            if (pool.logger.isInfoEnabled()) {
                if (new_schedulable != null) {
                    setName(new_schedulable.getName());
                } else {
                    setName("Reclaimed");
                }
            }
            return new_schedulable;
        }
    }

    /**
     * The ThreadGroup of the pool - all threads in the pool must be members of
     * the same threadgroup.
     */
    private final ThreadGroup group;
    /**
     * The maximum number of unused threads to keep around in the pool. anything
     * beyond this may be destroyed or GCed.
     */

    /** the actual pool * */
    private PooledThread pool[];
    private List<PooledThread> list_pool;
    private final Logger logger;
    private int index = 0;

    ThreadPool(int maximumSize, int initialSize, String name) {
        // Maybe give each pool its own group?
        group = new ThreadGroup(name);
        // Thread.currentThread().getThreadGroup();

        logger = Logging.getLogger(getClass().getName());
        if (maximumSize < 0) {
            // Unlimited. Make an array of a somewhat arbitrary size
            // (100 or initialSize, whichever is larger), and also
            // make an ArrayList which will be used if the array runs
            // out.
            pool = new PooledThread[Math.max(initialSize, 100)];
            list_pool = new ArrayList<PooledThread>(100);
        } else {
            if (initialSize > maximumSize) {
                initialSize = maximumSize;
            }
            pool = new PooledThread[maximumSize];
        }
        for (int i = 0; i < initialSize; i++) {
            pool[i] = constructReusableThread();
        }
    }

    private synchronized String nextName() {
        return "CougaarPooledThread-" + index++;
    }

    ThreadGroup getThreadGroup() {
        return group;
    }

    PooledThread getThread(String name) {
        PooledThread thread = null;
        PooledThread candidate = null;

        synchronized (this) {
            if (pool == null) {
                throw new RuntimeException("The ThreadPool has been stopped");
            }

            for (int i = 0; i < pool.length; i++) {
                candidate = pool[i];
                if (candidate == null) {
                    thread = constructReusableThread();
                    pool[i] = thread;
                    thread.in_use = true;
                     break;
                } else if (!candidate.in_use) {
                    thread = candidate;
                    thread.in_use = true;
                    break;
                }
            }

            if (thread == null && list_pool != null) {
                // Use the slow ArrayList. This is only enabled if
                // there's no thread limit.
                for (int i = 0, n = list_pool.size(); i < n; i++) {
                    candidate = list_pool.get(i);
                    if (!candidate.in_use) {
                        thread = candidate;
                        thread.in_use = true;
                        break;
                    }
                }
                if (thread == null) {
                    // None in the list either. Make one and add it,
                    thread = constructReusableThread();
                    thread.in_use = true;
                    list_pool.add(thread);
                }
            }
        }

        if (thread == null) {
            // None available. This is unrecoverable.
            throw new RuntimeException("Exceeded ThreadPool max");
        }

        if (logger.isInfoEnabled()) {
            thread.setName(name);
        }

        return thread;
    }

    /** actually construct a new PooledThread * */
    PooledThread constructReusableThread() {
        // If info logging is enabled the thread's name will get set
        // when it's run, so it can start out empty.
        String name = "";
        if (!logger.isInfoEnabled()) {
            name = nextName();
        }
        return new PooledThread(this, name);
    }

    String generateName() {
        // Generate a name for a Schedulable. If info logging is
        // enabled the name won't be used anywhere, so just return
        // null. Otherwise make a unique one.
        if (logger.isInfoEnabled()) {
            return nextName();
        } else {
            return null;
        }
    }

    int iterateOverRunningThreads(ThreadStatusService.Body body) {
        PooledThread[] p = pool;
        if (p == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < p.length; i++) {
            PooledThread thread = p[i];
            if (thread == null || !thread.isRunning) {
                continue;
            }
            try {
                SchedulableObject sched = thread.schedulable;
                // Even though thread.isRunning was true just
                // above, thread.schedulable could have become
                // null by now (since iterateOverRunningThreads
                // doesn't lock anything).
                if (sched != null) {
                    Scheduler scheduler = sched.getScheduler();
                    String scheduler_name = null;
                    if (scheduler != null) {
                        scheduler_name = scheduler.getName();
                    }
                    body.run(scheduler_name, sched);
                    count++;
                }
            } catch (Throwable t) {
                logger.error("ThreadStatusService error in body", t);
            }
        }
        return count;
    }

    void stopAllThreads() {
        synchronized (this) {
            int n = pool == null ? 0 : pool.length;
            for (int i = 0; i < n; i++) {
                PooledThread thread = pool[i];
                if (thread == null) {
                    continue;
                }
                thread.in_use = true;
                thread.stop_running();
                pool[i] = null;
            }
            pool = null;
        }
    }

    
    private static class NamedLock {
    	private final String name;
    	
    	NamedLock(String name) {
    		this.name = "Run lock " +name;
    	}
    	
    	@Override
      public String toString() {
    		return name;
    	}
    }
}
