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

/**
 * The serializing trivial implementation of Thread Service.  It
 * consists of a single {@link SerialThreadRunner} and {@link
 * SerialThreadQueue}.
 */
final class SingleThreadServiceProxy
    extends TrivialThreadServiceProxy
{
  private static final int NUMBER_OF_RUNNERS = 1;

  private SerialThreadQueue queue;

  private SerialThreadRunner[] runners;

  /**
   * Initialize the data structures used to store thread runners.
   */
  private void initialize() {
    queue = new SerialThreadQueue();
    runners = new SerialThreadRunner[NUMBER_OF_RUNNERS];
    for (int i = 0; i < runners.length; i++)
      runners[i] = new SerialThreadRunner(queue);
  }

  /**
   * Clean up the data structures so they can be reclaimed by the GC.
   */
  @Override
protected void unload() {
    queue = null;
    for (int i = 0; i < runners.length; i++) {
      runners[i].stop();
      // Aggressively nullify for GC.
      runners[i] = null;
    }
    runners = null;
  }
  
  SingleThreadServiceProxy()
  {
    initialize();
  }

    @Override
   public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name) 
    {
    return new SerialSchedulable(runnable, name, consumer, queue);
  }

    int iterateOverThreads(ThreadStatusService.Body body)
    {
    int count = 0;
    for (int i = 0; i < runners.length; i++)
      count += runners[i].iterateOverThreads(body);
    count += queue.iterateOverThreads(body);
    return count;
  }

}
