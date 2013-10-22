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

package org.cougaar.core.service;

import java.util.Comparator;

import org.cougaar.core.component.Service;
import org.cougaar.core.thread.RightsSelector;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.UnaryPredicate;

/**
 * This service controls the {@link ThreadService}.
 */
public interface ThreadControlService extends Service
{
    // General
    void setMaxRunningThreadCount(int count, int lane);
    void setQueueComparator(Comparator<Schedulable> comparator, int lane);
    void setRightsSelector(RightsSelector selector, int lane);
    boolean setQualifier(UnaryPredicate predicate, int lane);
    boolean setChildQualifier(UnaryPredicate predicate, int lane);

    // Status
    int runningThreadCount(int lane);
    int pendingThreadCount(int lane);
    int activeThreadCount(int lane);
    int maxRunningThreadCount(int lane);


    // Default lane
    int getDefaultLane();
    void setDefaultLane(int lane);

    // General
    void setMaxRunningThreadCount(int count);
    void setQueueComparator(Comparator<Schedulable> comparator);
    void setRightsSelector(RightsSelector selector);
    boolean setQualifier(UnaryPredicate predicate);
    boolean setChildQualifier(UnaryPredicate predicate);

    // Status
    int runningThreadCount();
    int pendingThreadCount();
    int activeThreadCount();
    int maxRunningThreadCount();


}
