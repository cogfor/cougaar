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
 * Objects which wish to subscribe to the ThreadListenerService should
 * implement this interface.  The methods in the API are all callback
 * from the corresponding ThreadService.
 */
public interface ThreadListener
{
    /**
     * Indicates that the given Schedulable has been queued because it
     * was unable to to run.  
     */
    void threadQueued(Schedulable schedulable, Object consumer);


    /**
     * Indicates that the given Schedulable, which was previously
     * queued, has now been dequeued and is about to run.
    */
    void threadDequeued(Schedulable schedulable, Object consumer);

    /**
     * Indicates that the given Schedulable is about to start
     * running.  This will be called from within the Schedulable's own
     * thread. 
    */
    void threadStarted(Schedulable schedulable, Object consumer);

    /**
     * Indicates that the given Schedulable has just stopped
     * running.  This will be called from within the Schedulable's own
     * thread.
    */
    void threadStopped(Schedulable schedulable, Object consumer);

    /**
     * Indicates that the given scheduler has been allocated a run-
     * right, which it will use to run one of its Schedulables.   That
     * Schedulable might be a queued one or might be a new one asking
     * to start.
     */
    void rightGiven(String scheduler);

    /**
     * Indicates that some running Schedulable managed by the given
     * Scheduler has given up its run-right.
    */
    void rightReturned(String consumer);
}
