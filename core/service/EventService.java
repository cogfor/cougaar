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

import org.cougaar.core.component.Service;

/** 
 * This service is used to log assessment events.
 * <p>
 * Events are intended for external profiling and monitoring
 * applications and often follow a strict application-defined
 * syntax.  In contrast, {@link LoggingService} logs are primarily
 * for human-readable debugging.
 * <p>
 * EventService clients should always check "isEventEnabled()"
 * before logging an event, for the same reasons as noted
 * in the LoggingService.
 * <p>
 * Events are currently equivalent to using the logging service
 * with the "EVENT.<i>classname</i>" log category and INFO
 * log level.  For example, if component "org.foo.Bar" emits an
 * event, it will be logged as category "EVENT.org.foo.Bar" and
 * level INFO.
 *
 * @see LoggingService
 */
public interface EventService extends Service {

  boolean isEventEnabled();

  void event(String s);

  void event(String s, Throwable t);

}
