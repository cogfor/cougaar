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
 * Defines a set of Schedulable state constants.  The only states
 * supported right now are RUNNING (active), PENDING (queued), and
 * DORMANT (neither).  We may eventually have a use for DISQUALIFIED
 * (the ThreadControlService has prevented it from running).
 * SUSPENDED is here for historical reasons and will almost never be
 * supported. 
 * 
 * Note that these states are purely for informational purposes.
 * They're not used internally in any way.
 */
public interface CougaarThread
{

    /**
     * The Schedulable is currently running. 
     */
    public static final int THREAD_RUNNING = 0;

    /**
     * Not supported, but would in theory mean the Schedulable has
    suspended itself.
    */
    public static final int THREAD_SUSPENDED = 1;

    /**
     * The Schedulable is currently queued. 
     */
    public static final int THREAD_PENDING = 2;

    /**
     * The Schedulable is qualified but neither running nor queued. 
     */
    public static final int THREAD_DORMANT = 3;


    /**
     * The Schedulable has been disqualified by the ThreadControlService.
     */
    public static final int THREAD_DISQUALIFIED = 4;


}
