/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

import java.util.Map;
import java.util.WeakHashMap;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerController;
import org.cougaar.util.log.LoggerControllerProxy;
import org.cougaar.util.log.LoggerProxy;
import org.cougaar.util.log.Logging;

/**
 * This service provider provides the {@link LoggingService}, which
 * is backed by the {@link LoggerFactory}.
 */
public class LoggingServiceProvider implements ServiceProvider {

  // maps of prefix -> key -> logger
  // Map<String, Map<String, LoggerProxy>>
  private static final Map prefixToLoggerCache = new WeakHashMap(11);

  // our prefix
  private final String prefix;

  // our logger cache
  // value of prefixToLoggerCache.get(prefix).
  // Map<String, LoggerProxy>
  private final Map loggerCache;

  public LoggingServiceProvider() {
    this(null);
  }

  public LoggingServiceProvider(String prefix) {
    this.prefix = prefix;
    loggerCache = getLoggerCache(prefix);
  }

  private static final Map getLoggerCache(String prefix) {
    synchronized (prefixToLoggerCache) {
      Map ret = (Map) prefixToLoggerCache.get(prefix);
      if (ret == null) {
        ret = new WeakHashMap(11);
        // new String(key) keeps the entry GCable
        String weakKey = 
          (prefix == null ?
           (null) :
           new String(prefix)); // intentional "new String"
        prefixToLoggerCache.put(weakKey, ret);
      }
      return ret;
    }
  }

  public Object getService(
      ServiceBroker sb,
      Object requestor,
      Class serviceClass) {
    if (LoggingService.class.isAssignableFrom(serviceClass)) {
      return getLoggingService(requestor);
    } else if (LoggingControlService.class.isAssignableFrom(serviceClass)) {
      LoggerController lc = Logging.getLoggerController(requestor);
      return new LoggingControlServiceImpl(lc);
    } else {
      return null;
    }
  }

  public void releaseService(
      ServiceBroker sb,
      Object requestor,
      Class serviceClass,
      Object service) {
  }

  LoggingService getLoggingService(Object requestor) {
    String key = Logging.getKey(requestor);
    LoggingService ls;
    synchronized (loggerCache) {
      ls = (LoggingService) loggerCache.get(key);
      if (ls == null) {
        Logger logger = Logging.getLogger(key);
        if (prefix == null) {
          ls = new LoggingServiceImpl(logger);
        } else {
          ls = new LoggingServiceWithPrefix(logger, prefix);
        }
        // new String(key) keeps the entry GCable
        String weakKey = new String(key); 
        loggerCache.put(weakKey, ls); // intentional "new String"
      }
    }
    return ls;
  }

  private static class LoggingServiceImpl
    extends LoggerProxy
    implements LoggingService {
      public LoggingServiceImpl(Logger l) {
        super(l);
      }
  }

  private static class LoggingControlServiceImpl
    extends LoggerControllerProxy
    implements LoggingControlService {
      public LoggingControlServiceImpl(LoggerController lc) {
        super(lc);
      }
  }
}
