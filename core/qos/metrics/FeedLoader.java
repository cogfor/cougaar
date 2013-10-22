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

package org.cougaar.core.qos.metrics;

import java.lang.reflect.Constructor;
import java.util.List;

import org.cougaar.core.service.LoggingService;
import org.cougaar.util.annotations.Cougaar.ObtainService;

/**
 * This Component uses its parameters to create and register a DataFeed. The
 * parameters are <code>name</code> (the name of the feed), <code>class</code>
 * (the fully qualified classname of the feed type) and <code>args</code> (the
 * initialization parameters for the feed.
 * 
 * @see DataFeedRegistrationService
 */
public class FeedLoader extends QosComponent {
    private static final Class<?>[] ParamTypes = {String[].class};
    private String[] args;
    private String name;
    private String classname;
    
    @ObtainService
    public DataFeedRegistrationService svc;
    
    @ObtainService
    public LoggingService log;

    private String getURL() {
        int i;
        for (i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-url")) {
                return args[i + 1];
            }
        }
        return null;
    }

    // XXX: Can we replace this with @Arg fields?
    public void setParameter(Object param) {
        if (param instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> parameters = (List<String>) param;
            for (String property : parameters) {
                int sepr = property.indexOf('=');
                if (sepr < 0) {
                    continue;
                }
                String key = property.substring(0, sepr);
                String value = property.substring(++sepr);
                if (key.equalsIgnoreCase("name")) {
                    name = value;
                } else if (key.equalsIgnoreCase("class")) {
                    classname = value;
                } else if (key.equalsIgnoreCase("args")) {
                    args = value.split(" ");
                }
            }
        }
    }

    @Override
   public void load() {
        super.load();
        if (svc != null && name != null && classname != null && args != null) {
            Object feed = null;
            try {
                Class<?> cl = Class.forName(classname);
                Constructor<?> constructor = cl.getConstructor(ParamTypes);
                Object[] argList = {args};
                feed = constructor.newInstance(argList);
                svc.registerFeed(feed, name);
                if (name.equalsIgnoreCase("sites")) {
                    // special Feed that has the URL for the sites file
                    svc.populateSites(getURL());
                }
            } catch (Exception ex) {
                if (log.isErrorEnabled()) {
                    log.error("Error creating DataFeed: " + ex.getMessage());
                }
            }

        }
    }
}
