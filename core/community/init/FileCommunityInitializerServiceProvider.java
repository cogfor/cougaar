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

package org.cougaar.community.init;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.naming.directory.BasicAttributes;

import org.cougaar.community.CommunityUtils;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.Strings;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Generates a community config from xml file.
 */
class FileCommunityInitializerServiceProvider implements ServiceProvider {

  private static final String DEFAULT_FILE = "communities.xml";

  // Node-level cache to avoid reading communities.xml file for each agent
  private static final Map fileCache = new HashMap();

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != CommunityInitializerService.class) {
      throw new IllegalArgumentException(
          getClass() + " does not furnish " + serviceClass);
    }
    return new CommunityInitializerServiceImpl();
  }

  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class CommunityInitializerServiceImpl implements CommunityInitializerService {


    public Collection getCommunityDescriptions (
        String entityName)
    {
      String file = System.getProperty("org.cougaar.community.configfile", DEFAULT_FILE);
      Collection ret;
      try {
        ret = getCommunityConfigsFromFile(file, entityName);
      } catch (RuntimeException ex) {
        System.out.println("Exception in getCommunityDescriptions from File: "+file);
        ret = new Vector();
      }
      return ret;
    }
  }

  /**
   * Get Collection of all CommunityConfig objects from XML file.  Uses
   * standard Cougaar config finder to locate XML file.
   * @param xmlFileName XML file containing community definitions
   * @return Collection of CommunityConfig objects
   */
  private static Collection getCommunityConfigsFromFile(String xmlFileName) {
    synchronized (fileCache) {
      Collection communityConfigs = (Collection)
        fileCache.get(xmlFileName);
      if (communityConfigs != null) {
        return communityConfigs;
      }
      try {
        XMLReader xr = XMLReaderFactory.createXMLReader();
        SaxHandler myHandler = new SaxHandler();
        xr.setContentHandler(myHandler);
        InputSource is =
            new InputSource(ConfigFinder.getInstance().open(xmlFileName));
        xr.parse(is);
        communityConfigs = myHandler.getCommunityConfigs();
        fileCache.put(xmlFileName, communityConfigs);
        return communityConfigs;
      }
      catch (Exception ex) {
        System.out.println("Exception parsing Community XML definition, " + ex);
        //System.out.println(getCommunityDescriptorText(fname));
      }
      return new Vector();
    }
  }

  /**
   * Get Collection of CommunityConfig objects for named entity.  Uses
   * standard Cougaar config finder to locate XML file.  If entityName is null
   * all communities are returned.
   * @param xmlFileName XML file containing community definitions
   * @param entityName Name of member
   * @return Collection of CommunityConfig objects
   */
  private static Collection getCommunityConfigsFromFile(String xmlFileName, String entityName) {
    Collection allCommunities = getCommunityConfigsFromFile(xmlFileName);
    Collection communitiesWithEntity = new Vector();
    for (Iterator it = allCommunities.iterator(); it.hasNext();) {
      CommunityConfig cc = (CommunityConfig)it.next();
      if (entityName == null || cc.hasEntity(entityName)) {
        communitiesWithEntity.add(cc);
      }
    }
    return communitiesWithEntity;
  }

  /*
   * For testing.  Loads CommunityConfigs from XML File or database
   * and prints to screen.
   * @param args
   */
  public static void main(String args[]) throws Exception {
    String entityName = "OSC";
    System.out.print(
        "<!-- load entity=\""+entityName+" -->");
    //
    FileCommunityInitializerServiceProvider me =
      new FileCommunityInitializerServiceProvider();
    CommunityInitializerService cis = (CommunityInitializerService)
      me.getService(null, null, CommunityInitializerService.class);
    Collection configs = cis.getCommunityDescriptions(entityName);
    //
    System.out.println("<Communities>");
    for (Iterator it = configs.iterator(); it.hasNext();) {
      System.out.println(((CommunityConfig)it.next()).toString());
    }
    System.out.println("</Communities>");
  }

  /**
   * SAX Handler for parsing Community XML files
   */
  private static class SaxHandler extends DefaultHandler {

    public SaxHandler () {}

    private Map communityMap = null;

    private CommunityConfig community = null;
    private EntityConfig entity = null;

    public void startDocument() {
      communityMap = new HashMap();
    }

    public Collection getCommunityConfigs() {
      return communityMap.values();
    }

    public void startElement(String uri, String localname, String rawname,
        Attributes p3) {
      try {
        if (localname.equals("Community")){
          String name = null;
          javax.naming.directory.Attributes attrs = new BasicAttributes();
          for (int i = 0; i < p3.getLength(); i++) {
            if (p3.getLocalName(i).equals("Name")) {
              name = p3.getValue(i).trim();
            } else {
              attrs.put(Strings.intern(p3.getLocalName(i)),
                        Strings.intern(p3.getValue(i).trim()));
            }
          }
          community = new CommunityConfig(Strings.intern(name));
          community.setAttributes(attrs);
        } else if (localname.equals("Entity")) {
          String name = null;
          javax.naming.directory.Attributes attrs = new BasicAttributes();
          for (int i = 0; i < p3.getLength(); i++) {
            if (p3.getLocalName(i).equals("Name")) {
              name = p3.getValue(i).trim();
            } else {
              attrs.put(Strings.intern(p3.getLocalName(i)),
                        Strings.intern(p3.getValue(i).trim()));
            }
          }
          entity = new EntityConfig(Strings.intern(name));
          entity.setAttributes(attrs);
        } else if (localname.equals("Attribute")) {
          String id = null;
          String value = null;
          for (int i = 0; i < p3.getLength(); i++) {
            if (p3.getLocalName(i).equals("ID")) {
              id = p3.getValue(i).trim();
            } else if (p3.getLocalName(i).equals("Value")) {
              value = p3.getValue(i).trim();
            }
          }
          if (id != null && value != null) {
            id = Strings.intern(id);
            value = Strings.intern(value);
            if (entity != null)
              entity.addAttribute(id, value);
            else
              community.addAttribute(id, value);
          }
        }
      } catch (Exception ex ){
        ex.printStackTrace();
      }
    }

    public void endElement(String uri, String localname, String qname) {
      try {
        if (localname.equals("Community")) {
          communityMap.put(community.getName(), community);
          community = null;
        } else if (localname.equals("Entity")) {
          // Ensure entity has essential attributes defined, use defaults if absent
          CommunityUtils.setAttribute(entity.getAttributes(), "Role", "Member");
          if (!CommunityUtils.hasAttribute(entity.getAttributes(), "EntityType", "Agent") &&
              !CommunityUtils.hasAttribute(entity.getAttributes(), "EntityType", "Community")) {
            CommunityUtils.setAttribute(entity.getAttributes(), "EntityType", "Agent");
          }
          community.addEntity(entity);
          entity = null;
        }
      } catch (Exception ex ){
        ex.printStackTrace();
      }
    }

  }
}
