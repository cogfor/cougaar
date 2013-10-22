/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Mobile Intelligence Corp
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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

/**
 * Defines an initial configuration for a community.
 */
public class CommunityConfig {

  private String name;
  private Attributes attributes = new BasicAttributes();
  private Map entities = new HashMap();

  public CommunityConfig(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void addAttribute(String id, String value) {
    Attribute attr = attributes.get(id);
    if (attr == null) {
      attr = new BasicAttribute(id, value);
      attributes.put(attr);
    } else {
      if (!attr.contains(value)) attr.add(value);
    }
  }

  public void setAttributes(Attributes attrs) {
    this.attributes = attrs;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public void addEntity(EntityConfig entity) {
    entities.put(entity.getName(), entity);
  }

  public EntityConfig getEntity(String name) {
    return (EntityConfig)entities.get(name);
  }

  public Collection getEntities() {
    return entities.values();
  }

  public boolean hasEntity(String entityName) {
    return entities.containsKey(entityName);
  }

  /**
   * Creates a printable representation of Community data.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    try {
      sb.append("<Community Name=\"" + getName() + "\" >\n");
      Attributes attributes = getAttributes();
      if (attributes != null && attributes.size() > 0) {
        NamingEnumeration en = attributes.getAll();
        while (en.hasMore()) {
          Attribute attr =
            (Attribute)en.next();
          String id = attr.getID();
          NamingEnumeration valuesEnum = attr.getAll();
          while (valuesEnum.hasMore()) {
            String value = (String)valuesEnum.next();
            sb.append("  <Attribute ID=\"" + id +
              " Value=\"" + value + "\" />\n");
          }
        }
      }
      Collection entities = getEntities();
      for (Iterator it2 = entities.iterator(); it2.hasNext();) {
        EntityConfig entity = (EntityConfig)it2.next();
        sb.append("  <Entity Name=\"" + entity.getName() + "\"");
        attributes = entity.getAttributes();
        if (attributes.size() > 0) {
          sb.append(" />\n");
        } else {
          sb.append(" >\n");
        }
        NamingEnumeration en = attributes.getAll();
        while (en.hasMore()) {
          Attribute attr =
            (Attribute)en.next();
          String id = attr.getID();
          NamingEnumeration valuesEnum = attr.getAll();
          while (valuesEnum.hasMore()) {
            String value = (String)valuesEnum.next();
            sb.append("    <Attribute ID=\"" + id +
              " Value=\"" + value + "\" />\n");
          }
        }
        sb.append("  </Entity>\n");
      }
      sb.append("</Community>");
    } catch (NamingException ne) {}
    return sb.toString();
  }

}
