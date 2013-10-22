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

package org.cougaar.core.logging;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.SuicideService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.log.log4j.ShoutPriority;

/**
 * This component calls {@link SuicideService.die} if any logging
 * client logs an ERROR or FATAL string.
 * <p>
 * This optional component is useful in deployed or automated
 * environments, where any error should kill the node.  For example,
 * an exception thrown during a component's load may result in a
 * partially configured (broken) node, in which case a nightly
 * testing script would likely want a quick "System.exit" instead
 * of waiting forever.
 * <p>
 * To load, add the following XML to your node definition:<pre> 
 *   &lt;component 
 *     class='org.cougaar.core.logging.DieOnErrorComponent' 
 *     priority='HIGH' 
 *     insertionpoint='Node.AgentManager.Agent.Component'/&gt;
 * </pre> 
 * To enable the "System.exit(..)", which is disabled by
 * default, add the following system property:<pre>
 *   &lt;vm_parameter&gt;
 *     -Dorg.cougaar.core.service.SuicideService.enable=true
 *   &lt;/vm_parameter&gt;
 * </pre> 
 * <p> 
 * Note that the optional ACME {@link 
 * org.cougaar.util.log.log4j.SocketAppender} will redirect {@link
 * java.lang.System#err} output to an ERROR log, so users may
 * want to change this to WARN logging by setting:<pre>
 *   &lt;vm_parameter&gt;
 *   -Dorg.cougaar.util.log.log4j.stderrLogLevel=WARN
 *   &lt;/vm_parameter&gt;
 * </pre> 
 * <p>
 * Some of the alternate design options that were considered:
 * <ul>
 * <li>A binder around LoggingService clients, but this would miss
 *     the static logger clients.</li>
 * <li>A Log4J appender defined in the "loggingConfig.conf" properties
 *     file.  However, it would be awkward to set the SuicideService
 *     in the appender, which would likely require assistance from a
 *     Cougaar component.  The advantage of the appender-based design
 *     would be the ability to filter out ERRORs for specific log
 *     categories (e.g. ignore errors from "org.foo").</li>
 * </ul>
 */
public final class DieOnErrorComponent
extends GenericStateModelAdapter
implements Component
{
  // we use a ThreadLocal to avoid logging recursion by the
  // SuicideService (e.g. "log.error" calls "die", which calls
  // "log.error", etc).
  private static final Object RECURSION_MARKER = new Object();
  private static final ThreadLocal RECURSION_DETECTOR = new ThreadLocal();

  private static final Level SHOUT = ShoutPriority.toLevel("SHOUT", null);

  private ServiceBroker sb;

  private Appender appender;

  private SuicideService suicideService;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    // get the suicide service
    suicideService = sb.getService(this, SuicideService.class, null);
    if (suicideService == null) {
      throw new RuntimeException("Unable to obtain SuicideService");
    }

    // attach our log4j appender to observe error logging
    Logger cat = Logger.getRootLogger();
    AppenderSkeleton a = new AppenderSkeleton() {
      @Override
      protected void append(LoggingEvent event) {
        handleError(event);
      }
      @Override
      public boolean requiresLayout() {
        return false;
      }
      @Override
      public void close() {
      }
    };
    a.setThreshold(Level.ERROR);
    appender = a;
    cat.addAppender(appender);
  }

  @Override
public void unload() {
    if (appender != null) {
      Logger cat = Logger.getRootLogger();
      cat.removeAppender(appender);
      appender = null;
    }
    if (suicideService != null) {
      sb.releaseService(this, SuicideService.class, suicideService);
      suicideService = null;
    }
    super.unload();
  }

  private void handleError(LoggingEvent event) {
    if (SHOUT.equals(event.getLevel())) {
      // ignore SHOUT; it's not an error
      return;
    }

    try {
      // we use a ThreadLocal to ignore our own SuicideService
      // calls, which use the logger
      if (RECURSION_DETECTOR.get() != null) {
        return;
      }
      RECURSION_DETECTOR.set(RECURSION_MARKER);

      // another concern is that the SuicideService implementation
      // may attempt to acquire locks (e.g. obtain services, etc),
      // which risks deadlock.  This would require us to do our
      // work in a separate Thread.  The standard SuicideService 
      // is well behaved, so we ignore this potential issue.

      // decide what to kill (null=node, string=agentName)
      Object target = null;

      // include optional reason
      ThrowableInformation ti = event.getThrowableInformation();
      Throwable cause = new RuntimeException(
          "Observed Log4j "+event.getLevel()+": "+event.getMessage(),
          (ti == null ? null : ti.getThrowable()));

      // call "die", which will likely call "System.exit"
      suicideService.die(target, cause);
    } finally {
      RECURSION_DETECTOR.set(null);
    }
  }
}
