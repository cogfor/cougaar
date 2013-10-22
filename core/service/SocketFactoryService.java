/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
import java.util.Map;

import org.cougaar.core.component.Service;

/**
 * This service is used to get a socket factory, such as an {@link
 * java.rmi.server.RMISocketFactory}.
 */
public interface SocketFactoryService extends Service {
  /**
   * Get an appropriate SocketFactory instance.
   * the return value is typed Object because RMISocketFactory and SSLSocketFactory
   * do not otherwise share a superclass.
   * Implementations will generally support SSLSocketFactory, SSLServerSocketFactory, and RMISocketFactory.
   * RMISocketFactory may be parameterized (via the second argument) with "ssl"=Boolean (default FALSE) and
   * "aspects"=Boolean (default FALSE).
   * <p>
   * Example:<br>
   * <code>
   * Map params = new HashMap(); params.put("ssl", Boolean.TRUE);<br>
   * RMISocketFactory rsf = (RMISocketFactory) socketFactoryService.getSocketFactory(RMISocketFactory.class, params);<br>
   * </code>
   * @param clazz Specifies the class required.  If the class cannot be supported by
   * the service, it will return null.
   * @param m Allows arbitrary preferences and parameters to be specified.
   * @return an object which is instanceof the requested class or null.
   */
  Object getSocketFactory(Class<?> clazz, Map<String,Boolean> m);
}
