/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.core.persist;

import java.io.Serializable;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/**
 * This component tests {@link 
 * org.cougaar.core.service.BlackboardService#persistNow()}
 */
public class TestFullSnapshot extends ComponentPlugin {
  private LoggingService logger;

  private static class Item implements Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
  }

  @Override
public void setupSubscriptions() {
    logger = getServiceBroker().getService(this, LoggingService.class, null);
  }

  @Override
public void execute() {
    if (logger.isShoutEnabled())
      logger.shout("Running TestFullSnapShot");

    Item[] items = new Item[1000];
    // Publish a bunch
    for (int i = 0; i < items.length; i++) {
      items[i] = new Item();
      blackboard.publishAdd(items[i]);
    }

    // Force a persist
    try {
      blackboard.persistNow();
    } catch (PersistenceNotEnabledException pnee) {
      logger.error("Persistence not enabled", pnee);
    }

    printBBSize();

    // publishRemove a bunch
    for (int i = 0; i < items.length; i++) {
      blackboard.publishRemove(items[i]);
      items[i] = null;
    }
    blackboard.closeTransaction();

    try {
      Thread.sleep(5000);
    } catch (InterruptedException ie) {
    }

    blackboard.openTransaction();

    // Now force another persist
    try {
      blackboard.persistNow();
    } catch (PersistenceNotEnabledException pnee) {
      logger.error("Persistence not enabled", pnee);
    }
    printBBSize();
  }

  private void printBBSize() {
    Runtime rt = Runtime.getRuntime();
    long heap = rt.totalMemory() - rt.freeMemory();
    if (logger.isShoutEnabled())
      logger.shout(blackboard.query(new UnaryPredicate() {
        /**
          * 
          */
         private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
          return true;
        }
      }).size() + " objects on blackboard, heap = " + heap);
  }
}
