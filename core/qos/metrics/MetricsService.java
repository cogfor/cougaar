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

import java.util.Observer;
import java.util.Properties;

import org.cougaar.core.component.Service;

/**
 * This is the query interface to the metrics services.  The two basic
 * operations, query and unubscribe, have several optional paramters,
 * which is wny each comes in four variants.  The structure of paths
 * is defined in the Metrics Services html documenation and won't be
 * covered here.
 */
public interface MetricsService extends Service
{
    /**
     * Retrieves the current value at the given path.  The path can
     * include special context-dependent variables whose runtime value
     * will be determined by the evaluator.  For now the qos_tags are
     * not used.
     */
    Metric getValue(String path, 
		    VariableEvaluator evaluator,
		    Properties qos_tags);

    /**
     * Retrieves the current value at the given path.  The path can
     * include special context-dependent variables whose runtime value
     * will be determined by the evaluator.
     */
    Metric getValue(String path, VariableEvaluator evaluator);

    /**
     * Retrieves the current value at the given path. No
     * context-dependent variable handling will be done.  For now the
     * qos_tags are not used.
     */
    Metric getValue(String path, Properties qos_tags);

    /**
     * Retrieves the current value at the given path. No
     * context-dependent variable handling will be done.
     */
    Metric getValue(String path);


    /**
     * Subscribes the given observer to the given path.  The usual
     * Observer api will be used for callbacks. The path can include
     * special context-dependent variables whose runtime value will be
     * determined by the evaluator.  Ordinarily a callback will be
     * invoked whenever the value at the path changes.  This can be
     * restricted with the qualifier.
    */
    Object subscribeToValue(String path, 
			    Observer observer,
			    VariableEvaluator evaluator,
			    MetricNotificationQualifier qualifier);

    /**
     * Subscribes the given observer to the given path.  The usual
     * Observer api will be used for callbacks. The path can include
     * special context-dependent variables whose runtime value will be
     * determined by the evaluator.  No restrictions are imposed
     * on the callbacks, which will be invoked whenever the value at
     * the path changes.
    */
    Object subscribeToValue(String path, 
			    Observer observer,
			    VariableEvaluator evaluator);

    /**
     * Subscribes the given observer to the given path.  The usual
     * Observer api will be used for callbacks. No context-dependent
     * variable handling will be done.  Ordinarily a callback will be
     * invoked whenever the value at the path changes.  This can be
     * restricted with the qualifier.
    */
    Object subscribeToValue(String path, 
			    Observer observer,
			    MetricNotificationQualifier qualifier);

    /**
     * Subscribes the given observer to the given path.  The usual
     * Observer api will be used for callbacks. No context-dependent
     * variable handling will be done, and no restrictions are imposed
     * on the callbacks, which will be invoked whenever the value at
     * the path changes.
    */
    Object subscribeToValue(String path, Observer observer);

    /**
     * End a previously established subscription.  The handle is as
     * returned by one of the subscribeToValue calls.
    */
    void unsubscribeToValue(Object subscription_handle);


}

