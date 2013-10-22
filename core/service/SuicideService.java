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

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.Service;

/** 
 * This service is used to tell the agent (or node) to exit, for
 * example due to an {@link java.lang.OutOfMemoryError}.
 * <p> 
 * The intent is to allow major components to report that they
 * have recognised that they have been corrupted by various means and
 * so should be restarted.
 *
 * @property org.cougaar.core.service.SuicideService.enable If true, will enable 
 * suicide of Nodes and Agents.  Otherwise, the suicide API exists but only logs
 * attempts rather than actually kills anything. The default is false.
 * @property org.cougaar.core.service.SuicideService.proactive If true, the SuicideService
 * will attempt to kill things proactively when it notices low-memory situations.  Defaults
 * to true, but only takes effect if the SuicideService is enabled.
 * @property org.cougaar.core.service.SuicideService.proactivePeriod Defines the period
 * of proactive suicide checks, in seconds.  By default, this is 1.
 * @property org.cougaar.core.service.SuicideService.lowMem Specify a quantity of
 * memory to use as the definition of "dangerously low" for proactive kill situations.
 * This is interpreted as a factor of Runtime.maxMemory().  The default value is "0.02" meaning
 * 2 percent.  If this value is greater than or equal to 1.0, then it is interpreted
 * as a number of kilobytes.  Examples: if maxMemory is 100Mb, then "0.005" and "512" are both
 * interpreted as one half a megabyte.
 */
public interface SuicideService
  extends Service
{
  /**
   * If the VM exits as a result of a suicide call, it will do so
   * using this value.
   */
  int EXIT_CODE = 5;

  String SUICIDE_PROP = (SuicideService.class).getName()+".enable";
  boolean isSuicideEnabled_default = false;
  boolean isSuicideEnabled = SystemProperties.getBoolean(SUICIDE_PROP, isSuicideEnabled_default); 

  String PROACTIVE_PROP = (SuicideService.class).getName()+".proactive";
  boolean isProactiveEnabled_default = true;
  boolean isProactiveEnabled = SystemProperties.getBoolean(PROACTIVE_PROP, isProactiveEnabled_default);

  String PROPERIOD_PROP = (SuicideService.class).getName()+".proactivePeriod";
  double proactivePeriod_default = 1.0;
  double proactivePeriod = SystemProperties.getDouble(PROPERIOD_PROP, proactivePeriod_default);

  String LOWMEM_PROP = (SuicideService.class).getName()+".lowMem";
  double lowMem_default = 0.02;
  double lowMem = SystemProperties.getDouble(LOWMEM_PROP, lowMem_default);
  

  /**
   * Report a fatal error and die.  This call might not return.
   *
   * @param component Which component should be killed.  May be specified as
   * null, implying that the whole node is suspect, or a specific component 
   * descriptor (e.g. an agent name).  It is probably illegal to specify a component
   * other than yourself or null.
   * @param error The error indicating the problem.  An attempt will be made
   * to log the error during the component's death throws.  May not be specified
   * as null.
   */
  void die(Object component, Throwable error);
}
