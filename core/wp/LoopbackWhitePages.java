/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.wp;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is a node-local (loopback) implementation of 
 * the {@link WhitePagesService}.
 */
public class LoopbackWhitePages
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;
  private ServiceBroker rootsb;
  private LoggingService log;

  private ServiceProvider sp;

  private final Map table = new HashMap();

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setNodeControlService(NodeControlService ncs) {
    rootsb = (ncs == null ? null : ncs.getRootServiceBroker());
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  @Override
public void load() {
    super.load();

    if (log.isDebugEnabled()) {
      log.debug("Loading resolver");
    }

    sp = new MySP();
    rootsb.addService(WhitePagesService.class, sp);

    if (log.isInfoEnabled()) {
      log.info("Loaded white pages resolver");
    }
  }

  @Override
public void unload() {
    super.unload();

    if (sp != null) {
      rootsb.revokeService(WhitePagesService.class, sp);
      sp = null;
    }

    if (log != null) {
      sb.releaseService(
          this, LoggingService.class, log);
      log = null;
    }
  }

  private Response submit(Request req) {
    if (log.isDetailEnabled()) {
      log.detail("handling wp request: "+req);
    }
    Object ret;

    if (req instanceof Request.Bind) {
      Request.Bind r = (Request.Bind) req;
      AddressEntry ae = r.getAddressEntry();
      boolean overWrite = r.isOverWrite();
      String name = ae.getName();
      String type = ae.getType();
      synchronized (table) {
        Map m = (Map) table.get(name);
        if (overWrite || m == null || !m.containsKey(type)) {
          // replace table entry with modified copy
          m = (m == null ? new HashMap() : new HashMap(m));
          m.put(type, ae);
          m = Collections.unmodifiableMap(m);
          table.put(name, m);
          // return "forever"
          ret = new Long(Long.MAX_VALUE);
        } else {
          // already bound, return conflict
          ret = m.get(type);
        }
      }
    } else if (req instanceof Request.Unbind) {
      Request.Unbind r = (Request.Unbind) req;
      AddressEntry ae = r.getAddressEntry();
      String name = ae.getName();
      String type = ae.getType();
      synchronized (table) {
        Map m = (Map) table.get(name);
        AddressEntry m_ae = (m == null ? null : (AddressEntry) m.get(type));
        if (ae.equals(m_ae)) {
          // replace table entry with modified copy
          if (m.size() == 1) {
            table.remove(name);
          } else {
            m = new HashMap(m);
            m.remove(type);
            m = Collections.unmodifiableMap(m);
            table.put(name, m);
          }
          ret = Boolean.TRUE;
        } else {
          // unknown name or non-matching entry data
          ret = Boolean.FALSE;
        }
      }
    } else if (req instanceof Request.List) {
      String suffix = ((Request.List) req).getSuffix();
      int len = suffix.length();
      if (len <= 0 || suffix.charAt(0) != '.') {
        // invalid suffix, expecting ".*."
        ret = null;
      } else {
        Set set = null;
        synchronized (table) {
          // return matching names, if any
          boolean isRoot = (len == 1);
          for (Iterator iter = table.keySet().iterator(); iter.hasNext(); ) {
            String s = (String) iter.next();
            if (isRoot) {
              int sep = s.lastIndexOf('.');
              if (sep >= 0) {
                s = s.substring(sep);
              }
            } else {
              if ((s.length() <= len) || !s.endsWith(suffix)) {
                continue;
              }
              int sep = -1;
              for (int j = s.length() - len - 1; j >= 0; j--) {
                if (s.charAt(j) == '.') {
                  sep = j;
                  break;
                }
              }
              if (sep >= 0) {
                s = s.substring(sep);
              }
            }
            if (set == null) {
              set = new HashSet();
            }
            set.add(s);
          }
        }
        ret = (set == null ? null : Collections.unmodifiableSet(set));
      }
    } else {
      String name =
        (req instanceof Request.Get ? ((Request.Get) req).getName() :
         req instanceof Request.GetAll ? ((Request.GetAll) req).getName() :
         null);
      synchronized (table) {
        ret = table.get(name);
      }
    }

    Response res = req.createResponse();
    res.setResult(ret);
    return res;
  }

  private class MySP implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (WhitePagesService.class.isAssignableFrom(serviceClass)) {
        return new WhitePagesService() {
          @Override
         public Response submit(Request req) {
            return LoopbackWhitePages.this.submit(req);
          }
        };
      } else {
        return null;
      }
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
    }
  }
}
