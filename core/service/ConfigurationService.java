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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cougaar.core.component.Service;
import org.w3c.dom.Document;

/**
 * This service provides access to the file system by searching
 * the configuration path.
 * <p>
 * The standard implementation is backed by the {@link
 * org.cougaar.util.ConfigFinder}.  See the {@link
 * org.cougaar.util.ConfigFinder} javadoc for the default
 * configuration path and configurable system properties.
 */
public interface ConfigurationService extends Service {
  /** the (immutable) set of configuration URLs used by this Service */
  List getConfigurationPath();
  
  /**
   * Locate an actual file in the config path. This will skip over
   * elements of org.cougaar.config.path that are not file: urls.
   */
  File locateFile(String filename);

  /**
   * Resolve a logical reference to a URL.
   * @return null if unresolvable.
   */
  URL resolveName(String logicalName) throws MalformedURLException;

  /**
   * Opens an InputStream to access the named file. The file is sought
   * in all the places specified in configPath.
   * @throws IOException if the resource cannot be found.
   */
  InputStream open(String aURL) throws IOException;

  /**
   * Attempt to find the URL which would be opened by the open method.
   * Note that this must actually attempt to open the various URLs
   * under consideration, so this is <em>not</em> an inexpensive operation.
   */
  URL find(String aURL) throws IOException;

  /** Read and parse an XML file somewhere in the configpath */
  Document parseXMLConfigFile(String xmlfile) throws IOException;

  /** Delegate class for easing implementation of Binders, etc */
  class Delegate implements ConfigurationService {
    private final ConfigurationService _delegate;
    protected final ConfigurationService getDelegate() { return _delegate; }
    public Delegate(ConfigurationService cs) { _delegate = cs; }
    public List getConfigurationPath() { return _delegate.getConfigurationPath(); }
    public File locateFile(String filename) { return _delegate.locateFile(filename); }
    public URL resolveName(String name) throws MalformedURLException { return _delegate.resolveName(name); }
    public InputStream open(String u) throws IOException { return _delegate.open(u); }
    public URL find(String u) throws IOException { return _delegate.find(u); }
    public Document parseXMLConfigFile(String f) throws IOException { return _delegate.parseXMLConfigFile(f); }
  }
}

