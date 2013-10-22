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

package org.cougaar.core.qos.metrics;



/**
 * Instances of this class can be used to restrict callbacks from
 * MetricsService subscriptions unless the new value differs from the
 * previous value by a given delta. These qualifiers are stateful and
 * therefore cannot be shared across multiple subscriptions.
 * 
 * @see CredibilityQualifier
 */
public class DeltaValueQualifier 
    implements MetricNotificationQualifier, Constants
{
    private double min_delta;
    private Metric last_qualified;

    public DeltaValueQualifier(double min_delta) {
	this.min_delta = min_delta;
    }

    public boolean shouldNotify(Metric metric) {
	if (metric.getCredibility() <= SYS_DEFAULT_CREDIBILITY)
	    return false;

	if (last_qualified == null) {
	    last_qualified = metric;
	    return true;
	}

	double old_value = last_qualified.doubleValue();
	double new_value = metric.doubleValue();
	if (Math.abs(new_value-old_value) > min_delta) {
	    last_qualified = metric;
	    return true;
	} else {
	    return false;
	}
	
    }

}

