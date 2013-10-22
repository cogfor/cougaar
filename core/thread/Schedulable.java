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


/** 
 * A Schedulable is an API provided by the {@link
 * org.cougaar.core.service.ThreadService} that takes the place
 * of standard Java {@link java.lang.Thread}s and 
 * {@link java.util.TimerTask}s.
 * <p> 
 * Aside from a few special internal cases, all Threads and Tasks in
 * Cougaar should come from the ThreadService in the form of
 * Schedulables.  To treat a Schedulable like a Thread, use the start
 * method.  To treat a Schedulable like a TimerTask, use the schedule
 * methods.  The ThreadService is the only source of usable
 * Schedulables.
 */
public interface Schedulable
{
    /**
     * Starting a Schedulable is conceptually the same as starting a
     * Java Thread.
     * <p>
     * Note these differences:<ol><li>
     * The {@link java.lang.Runnable#run()} method invoked due to the
     * {@link #start()} should return promptly, to free the pooled
     * Thread for use by other waiting Schedulables.</li><li>
     * If no thread resources are available, the Schedulable
     * will be queued instead of running right away.  It will only
     * run when enough resources have become available for it to
     * reach the head of the queue.</li><li>
     * If the Schedulable is running at the time of the
     * call, it will restart itself after the current run finishes
     * (unless it's cancelled in the meantime).  The {@link
     * java.lang.Runnable#run()} called by {@link #start()} is
     * single-threaded.</li></ol>
     */ 
    void start();

    /**
     * Like {@link #cancel} but with two differences: the Schedulable
     * can be resumed later, and a special callback is invoked when
     * the current run finishes.
     * 
     */
    void suspend(SuspendCallback callback);

    /**
     * Restart after a {@link #suspend}.
     *
     */
    void resume();
    
    
    /**
     * Cancelling a Schedulable will prevent starting if it's
     * currently queued or from restarting if it was scheduled to do
     * so.  It will not cancel the current run.
     */
    boolean cancel();

    /**
     * Returns the current state of the Schedulable.  The states are
     * described in {@link CougaarThread}.
     */
    int getState();

    /**
     * Returns the requestor for whom the ThreadService made this
     * Schedulable.
     */
    Object getConsumer();


    /**
     * Lane
     */
    int getLane();


    /**
     * Other status methods, for the ThreadStatusService
     */

    long getTimestamp(); // start time

    String getName();

    int getBlockingType();

    String getBlockingExcuse();

    /**
     * The following methods behave more or less as they on
     * TimerTasks, except that the schedule methods can be called more
     * than once.  In that case, later calls effectively reschedule
     * the Schedulable.  Since 'cancel' was already in use, a new
     * method had to be introduced to cancel a scheduled task.  Thus
     * 'cancelTimer'.
     */
    void schedule(long delay);
    void schedule(long delay, long interval);
    void scheduleAtFixedRate(long delay, long interval);
    void cancelTimer();
    
    /**
     * Notification that a suspend request did its work, ie
     * the Schedulable is actually suspended.
     *
     */
    interface SuspendCallback {
	void suspended(Schedulable s);
    }
}
