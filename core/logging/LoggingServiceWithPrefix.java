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

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerAdapter;

/**
 * Utility class to wrap all {@link LoggingService} messages with a
 * prefix string, unless already wrapped by a
 * <tt>LoggingServiceWithPrefix</tt> with that prefix.
 * 
 * @property org.cougaar.core.logging.trackDuplicateLoggingPrefix
 *   Enable debug tracking of duplicate "AgentX: AgentX:"
 *   LoggingService log messages within the LoggingServiceWithPrefix,
 *   possibly due to enabling
 *   "-Dorg.cougaar.core.logging.addAgentPrefix".  Performs an
 *   expensive check at DETAIL log level, regardless of logger
 *   configuration, and logs any duplicate prefixes at SHOUT level.
 *   Defaults to false.
 */
public class LoggingServiceWithPrefix extends LoggerAdapter implements LoggingService {

  private static final boolean TRACK_DUPLICATE_LOGGING_PREFIX =
    SystemProperties.getBoolean(
        "org.cougaar.core.logging.trackDuplicateLoggingPrefix");

  private final String prefix;
  private final Logger logger;

  public static LoggingService add(LoggingService ls, String prefix) {
    if (ls instanceof LoggingServiceWithPrefix) {
      LoggingServiceWithPrefix lswp = (LoggingServiceWithPrefix) ls;
      String s = lswp.prefix;
      if (prefix == null ? s == null : prefix.equals(s)) {
        // already prefixed
        return lswp;
      }
    }
    return new LoggingServiceWithPrefix(ls, prefix);
  }

  public static LoggingService add(Logger logger, String prefix) {
    if (logger instanceof LoggingService) {
      return add(((LoggingService) logger), prefix);
    }
    return new LoggingServiceWithPrefix(logger, prefix);
  }

  public LoggingServiceWithPrefix(Logger logger, String prefix) {
    this.logger = logger;
    this.prefix = prefix;
  }

  @Override
public boolean isEnabledFor(int level) {
    if (TRACK_DUPLICATE_LOGGING_PREFIX) {
      // we want to see all messages, even they won't be logged.
      // note that this causes extra string consing!
      return true;
    }
    return logger.isEnabledFor(level);
  }
  @Override
public void log(int level, String message, Throwable t) {
    if (TRACK_DUPLICATE_LOGGING_PREFIX &&
        message.regionMatches(0, prefix, 0, prefix.length()-1)) {
      StackTraceElement[] stack = (new Throwable()).getStackTrace();
      logger.log(
          SHOUT,
          "DuplicateLoggingPrefix - "+stack[2]+": "+message,
          null);
    }
    logger.log(level, prefix + message, t);
  }
  public void printDot(String dot) {logger.printDot(dot);}
}
