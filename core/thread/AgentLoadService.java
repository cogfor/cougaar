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

import java.util.Collection;

import org.cougaar.core.component.Service;
import org.cougaar.core.qos.metrics.DecayingHistory;

/**
 * This Service provides a simple time-based integral of agent load.
 * This service is provided by {@link AgentLoadSensorPlugin}, an inner
 * class of which implements the service.
 */
public interface AgentLoadService extends Service
{
    
    /**
     * This struct-like class holds the four values of a CPU load
     * snapshot.
     */
    public class AgentLoad extends DecayingHistory.SnapShot {
	/**
	 * The name of the Agent.
	 */
	public String name;

	/**
	 * Gauge of instantaneous Agent Load Average, i.e. number of 
	 * outstanding threads currently being used by the Agent
	 */
	public int outstanding;

	/**
	 * Integral of the Agent Load Average over time.
	 * To calculate Agent Load Average, snapshot twice and 
	 * divide delta value by delta time
	 */
	public double loadAvgIntegrator;

	/**
	 * Integral of the Agent MJIPS  over time.
	 * To calculate Agent average MJIPS snapshot twice and 
	 * divide delta value by delta time.
	 */
	public double loadMjipsIntegrator;
    }

    public Collection snapshotRecords();
}