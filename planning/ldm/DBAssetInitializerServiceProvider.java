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

package org.cougaar.planning.ldm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.DBInitializerService;
import org.cougaar.planning.plugin.asset.AssetDataDBReader;
import org.cougaar.planning.plugin.asset.AssetDataReader;
import org.cougaar.planning.service.AssetInitializerService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Implementation of AssetInitializerServiceProvider that reads
 * initialization information from a database.
 **/
public class DBAssetInitializerServiceProvider implements ServiceProvider {

  private final DBInitializerService dbInit;
  private final Logger logger;

  public DBAssetInitializerServiceProvider(DBInitializerService dbInit) {
    if (dbInit == null)
      throw new IllegalArgumentException("Null DBInitializerService passed to DBAssetInitializerServiceProvider constructor.");

    this.dbInit = dbInit;
    this.logger = Logging.getLogger(getClass());
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != AssetInitializerService.class) {
      throw new IllegalArgumentException(
          getClass()+" does not furnish "+serviceClass);
    }
    return new AssetInitializerServiceImpl();
  }

  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class AssetInitializerServiceImpl implements AssetInitializerService {

    public String getAgentPrototype(String agentName)
      throws InitializerException
    {
      if (logger.isDebugEnabled()) {
        logger.debug("In getAgentPrototype");
      }
      Map substitutions = dbInit.createSubstitutions();
      substitutions.put(":agent_name:", agentName);
      try {
        Connection conn = dbInit.getConnection();
        try {
          Statement stmt = conn.createStatement();
          String query = dbInit.getQuery("queryAgentPrototype",  substitutions);
          ResultSet rs = dbInit.executeQuery(stmt, query);
          if (rs.next()) {
            String result = dbInit.getNonNullString(rs, 1, query);
            if (rs.next())
              throw new InitializerException("Multiple prototypes for " + agentName);
            return result;
          }
          throw new InitializerException("No prototype for " + agentName);
        } finally {
          conn.close();
        }
      } catch (SQLException e) {
        throw new InitializerException(
            "getAgentPrototype("+agentName+")", e);
      }
    }

    public String[] getAgentPropertyGroupNames(String agentName)
      throws InitializerException
    {
      if (logger.isDebugEnabled()) {
        logger.debug("In getAgentPropGroupNames");
      }
      Map substitutions = dbInit.createSubstitutions();
      substitutions.put(":agent_name:", agentName);
      try {
        Connection conn = dbInit.getConnection();
        try {
          Statement stmt = conn.createStatement();
          String query = dbInit.getQuery("queryAgentPGNames",  substitutions);
          ResultSet rs = dbInit.executeQuery(stmt, query);
          List result = new ArrayList();
          while (rs.next()) {
            result.add(dbInit.getNonNullString(rs, 1, query));
          }
          rs.close();
          stmt.close();
          return (String[]) result.toArray(new String[result.size()]);
        } finally {
          conn.close();
        }
      } catch (SQLException e) {
        throw new InitializerException(
            "getAgentPropertyGroupNames("+agentName+")", e);
      }
    }

    /**
     * Return values for all properties of a property group as an
     * array of Object arrays. For each property an array of Objects
     * has the property's name, type, and value or array of values.
     * All non-array objects are Strings. If the value is an array it
     * is an array of Strings
     **/
    public Object[][] getAgentProperties(String agentName, String pgName)
      throws InitializerException
    {
      if (logger.isDebugEnabled()) {
        logger.debug("In getAgentProperties");
      }
      try {
        Connection conn = dbInit.getConnection();
        Map substitutions = dbInit.createSubstitutions();
        substitutions.put(":agent_name:", agentName);
        substitutions.put(":pg_name:", pgName);
        try {
          Statement stmt = conn.createStatement();
          String query = dbInit.getQuery("queryLibProperties",  substitutions);
          ResultSet rs = dbInit.executeQuery(stmt, query);
          List result = new ArrayList();
          while (rs.next()) {
            String attributeName = dbInit.getNonNullString(rs, 1, query);
            String attributeType = dbInit.getNonNullString(rs, 2, query);
            boolean collection = !rs.getString(3).equals("SINGLE");
            Object attributeId = rs.getString(4);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":pg_attribute_id:", attributeId);
            String query2 = dbInit.getQuery("queryAgentProperties",  substitutions);
            ResultSet rs2 = dbInit.executeQuery(stmt2, query2);
            Object value;
            if (collection) {
              List values = new ArrayList();
              while (rs2.next()) {
                String v = dbInit.getNonNullString(rs2, 1, query2);
                values.add(v);
              }
              value = values.toArray(new String[values.size()]);
            } else if (rs2.next()) {
              value = dbInit.getNonNullString(rs2, 1, query2);
              if (rs2.next())
                throw new InitializerException("Multiple values for "
                                                      + attributeId);
            } else {
              continue;         // Skip missing properties
//                throw new InitializerException("No value for " + attributeId);
            }
            Object[] e = {attributeName, attributeType, value};
            result.add(e);
            rs2.close();
            stmt2.close();
          }
          rs.close();
          stmt.close();
          return (Object[][]) result.toArray(new Object[result.size()][]);
        } finally {
          conn.close();
        }
      } catch (SQLException e) {
        throw new InitializerException(
            "getAgentProperties("+agentName+", "+pgName+")", e);
      }
    }

    /**
     * Get the relationships of an agent. Each relationship is
     * represented by a 6-tuple of the roleName, itemId, typeId,
     * otherAgentId, start time, and end time.
     **/
    public String[][] getAgentRelationships(String agentName)
      throws InitializerException
    {
      try {
        Connection conn = dbInit.getConnection();
        Map substitutions = dbInit.createSubstitutions();
        substitutions.put(":agent_name:", agentName);
        try {
          Statement stmt = conn.createStatement();
          String query = dbInit.getQuery("queryAgentRelation",  substitutions);
          ResultSet rs = dbInit.executeQuery(stmt, query);
          List result = new ArrayList();
          while (rs.next()) {
            String[] v = {
              dbInit.getNonNullString(rs, 1, query),
              dbInit.getNonNullString(rs, 2, query),
              dbInit.getNonNullString(rs, 3, query),
              dbInit.getNonNullString(rs, 4, query),
              rs.getString(5),
              rs.getString(6),
            };
            result.add(v);
          }
          rs.close();
          stmt.close();
          String[][] ary = (String[][]) result.toArray(new String[result.size()][]);
          if (false) {
            StringBuffer buf = new StringBuffer();
            buf.append(System.getProperty("line.separator"));
            for (int i = 0; i < ary.length; i++) {
              String[] ary2 = ary[i];
              buf.append("Relationship of ");
              buf.append(agentName);
              buf.append(": ");
              for (int j = 0; j < ary2.length; j++) {
                if (j > 0) buf.append('\t');
                buf.append(ary2[j]);
              }
              buf.append(System.getProperty("line.separator"));
            }
            System.out.println(buf);
          }
          return ary;
        } finally {
          conn.close();
        }
      } catch (SQLException e) {
        throw new InitializerException(
            "getAgentRelationships("+agentName+")",e);
      }
    }

    public AssetDataReader getAssetDataReader() {
      return new AssetDataDBReader(this);
    }

    public Object[] translateAttributeValue(String type, String key)
      throws InitializerException {
        try {
          return dbInit.translateAttributeValue(type, key);
        } catch (SQLException e) {
          throw new InitializerException(
              "translateAttributeValue("+type+", "+key+")", e);
        }
      }
  }
}
