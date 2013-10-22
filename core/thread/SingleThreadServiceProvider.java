/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.thread;

import org.cougaar.core.service.ThreadService;

/**
 * This Component provides a very simple, serializing {@link
 * ThreadService} for its container.  The implementation is
 * SingleThreadServiceProxy.
 */
public class SingleThreadServiceProvider
    extends TrivialThreadServiceProvider
{
    private SingleThreadServiceProxy proxy;

    SingleThreadServiceProvider()
    {
	super();
    }

    @Override
   ThreadStatusService makeThreadStatusService()
    {
	return new ThreadStatusService() {
		public int iterateOverStatus(ThreadStatusService.Body body) {
		    return proxy.iterateOverThreads(body);
		}
	    };
    }

    @Override
   ThreadService makeThreadServiceProxy()
    {
	proxy = new SingleThreadServiceProxy();
	return proxy;
    }

}
