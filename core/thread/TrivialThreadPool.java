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

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A simple pool of Java threads, used by the trivial {@link ThreadService}. The
 * pool can grown without bounds, since there are no limits on the number of
 * concurrent threads.
 */
class TrivialThreadPool {
    // As noted in Starter, we need at least one thread to be non-daemon
    // to keep our JVM alive. We mark the first ThreadRunner as non-daemon.
    private static final boolean DAEMON =
            SystemProperties.getBoolean("org.cougaar.core.thread.daemon");

    private static TrivialThreadPool singleton;

    private final Logger logger = Logging.getLogger(getClass().getName());
    private final List<ThreadRunner> list_pool = new ArrayList<ThreadRunner>();
    private ThreadRunner[] pool = new ThreadRunner[100];
    int anon_count = 0;

    private TrivialThreadPool() {
        if (!DAEMON) {
            // Start our keep-alive
            pool[0] = new ThreadRunner(false);
        }
    }

    synchronized String generateName() {
        return "TrivialThread-" + anon_count++;
    }

    Thread getThread(TrivialSchedulable schedulable, Runnable runnable, String name) {
        ThreadRunner result = null;

        synchronized (this) {
            if (pool == null) {
                throw new RuntimeException("The ThreadPool has been stopped");
            }
            for (int i = 0; i < pool.length; i++) {
                ThreadRunner candidate = pool[i];
                if (candidate == null) {
                    result = new ThreadRunner();
                    pool[i] = result;
                    result.in_use = true;
                    break;
                } else if (!candidate.in_use) {
                    result = candidate;
                    result.in_use = true;
                    break;
                }
            }

            if (result == null && list_pool != null) {
                // Use the slow ArrayList.
                for (int i = 0; i < list_pool.size(); i++) {
                    ThreadRunner candidate = list_pool.get(i);
                    if (!candidate.in_use) {
                        result = candidate;
                        result.in_use = true;
                        break;
                    }
                }
            }

            if (result == null) {
                // None in the list either. Make one and add it,
                result = new ThreadRunner();
                result.in_use = true;
                list_pool.add(result);
            }
        }

        result.configure(schedulable, runnable, name);

        return result;
    }

    // Unsynchronized read access to list_pool. This is by design
    // (this operation cannot block the entire service) and should be
    // ok. If it isn't, the list could be copied.
    int iterateOverRunningThreads(ThreadStatusService.Body body) {
        int count = 0;
        int n = pool == null ? 0 : pool.length;
        for (int i = 0; i < n; i++) {
            ThreadRunner thread = pool[i];
            count += runBody(thread, body);
        }
        if (list_pool != null) {
            for (int i = 0, size = list_pool.size(); i < size; i++) {
                ThreadRunner thread = null;
                try {
                    thread = list_pool.get(i);
                } catch (Exception ex) {
                    // list_pool size has changed - doesn't matter
                    if (logger.isDebugEnabled()) {
                        logger.debug("list_pool size changed");
                    }
                }
                count += runBody(thread, body);
            }
        }
        return count;
    }

    private int runBody(ThreadRunner thread, ThreadStatusService.Body body) {
        if (thread != null && thread.in_use) {
            try {
                TrivialSchedulable sched = thread.schedulable;
                if (sched != null) {
                    body.run("root", sched);
                    return 1; // one Schedulable processed
                }
            } catch (Throwable t) {
                logger.error("ThreadStatusService error in body", t);
            }
        }

        // No Schedulable processed
        return 0;
    }

    void stopAllThreads() {
        synchronized (this) {
            int n = pool == null ? 0 : pool.length;
            for (int i = 0; i < n; i++) {
                ThreadRunner thread = pool[i];
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

    static void makePool() {
        if (singleton == null) {
            singleton = new TrivialThreadPool();
        }
    }

    static TrivialThreadPool pool() {
        return singleton;
    }

    private class ThreadRunner extends Thread {
        TrivialSchedulable schedulable;
        Runnable body;
        boolean in_use;
        boolean should_stop;
        final Object lock = new Object();
    
        ThreadRunner() {
            this(true);
        }
    
        ThreadRunner(boolean daemon) {
            super("ThreadRunner");
            setDaemon(daemon);
            super.start();
        }
    
        @Override
      public void start() {
            throw new RuntimeException("You can't call start() on a PooledThread");
        }
    
        void configure(TrivialSchedulable schedulable, Runnable body, String name) {
            synchronized (lock) {
                this.schedulable = schedulable;
                this.body = body;
                // thread.setName(name);
                lock.notify();
            }
        }
    
        @Override
      public void run() {
            while (true) {
                synchronized (lock) {
                    if (body != null) {
                        body.run();
                    }
                    if (schedulable != null) {
                        schedulable.thread_stop();
                    }
                    in_use = false;
                    if (should_stop) {
                        break;
                    }
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                    }
                    if (should_stop) {
                        break;
                    }
                }
    
            }
        }
    
        void stop_running() {
            synchronized (lock) {
                should_stop = true;
                lock.notify();
            }
            try {
                join();
            } catch (InterruptedException ie) {
            }
            synchronized (lock) {
                should_stop = false;
            }
        }
    }
}
