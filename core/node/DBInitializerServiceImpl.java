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

package org.cougaar.core.node;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.util.DBConnectionPool;
import org.cougaar.util.DBProperties;
import org.cougaar.util.Parameters;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.NullLogger;

/**
 * Implementation of {@link DBInitializerService}.
 */
public class DBInitializerServiceImpl implements DBInitializerService {

  public static final String QUERY_FILE = "DBInitializer.q";

  private final Logger logger;
  private final String trialId;
  private final String assemblyMatch;
  private final DBProperties dbp;
  private final String database;
  private final String username;
  private final String password;

  /**
   * Constructor creates a DBInitializer from the DBInitializer.q
   * query control file and sets up variables for referencing the database.
   * <p>
   * @param trialId the Trial identifier.
   */
  public DBInitializerServiceImpl(String trialId)
    throws SQLException, IOException
  {
    Logger l = Logging.getLogger(getClass());
    logger = ((l == null) ? NullLogger.getLogger() : l);

    dbp = DBProperties.readQueryFile(QUERY_FILE);
    database = dbp.getProperty("database");
    username = dbp.getProperty("username");
    password = dbp.getProperty("password");
    this.trialId = trialId;
    if (logger.isInfoEnabled()) {
      logger.info(
          "Will initialize for trial " + trialId + " from DB " + database);
    }

    try {
      String dbtype = dbp.getDBType();
      ensureDriverClass(dbtype);
      Connection conn = getConnection();
      try {
        Statement stmt = conn.createStatement();
        String query = getQuery(
            "queryExperiment", 
            Collections.singletonMap(":trial_id:", trialId));
        ResultSet rs = executeQuery(stmt, query);
        boolean first = true;
        StringBuffer asbBuffer = new StringBuffer();
        asbBuffer.append("in (");
        while (rs.next()) {
          if (first) {
            first = false;
          } else {
            asbBuffer.append(", ");
          }
          asbBuffer.append("'");
          asbBuffer.append(getNonNullString(rs, 1, query));
          asbBuffer.append("'");
        }
        asbBuffer.append(")");
        assemblyMatch = asbBuffer.toString();
        rs.close();
        stmt.close();
      } finally {
        conn.close();
      }
    } catch (ClassNotFoundException e) {
      throw new SQLException("Driver not found for " + database);
    }
    if (logger.isInfoEnabled()) {
      logger.info(
          "Initializing from assemblies: AsbMatch: " + assemblyMatch);
    }
  }

  public Map createSubstitutions() {
    Map m = new HashMap(7);
    m.put(":trial_id:", trialId);
    m.put(":assemblyMatch:", assemblyMatch);
    return m;
  }

  public String getNonNullString(ResultSet rs, int ix, String query)
    throws SQLException
  {
    String result = rs.getString(ix);
    if (result == null)
      throw new RuntimeException("Null in DB ix=" + ix + " query=" + query);
    return result;
  }

  public String getQuery(String queryName, Map substitutions) {
    return dbp.getQuery(queryName,  substitutions);
  }

  public Connection getConnection() throws SQLException {
    return DBConnectionPool.getConnection(database, username, password);
  }

  public ResultSet executeQuery(Statement stmt, String query) throws SQLException {
    try {
      boolean shouldLog = logger.isDebugEnabled();
      long startTime = (shouldLog ? 0L : System.currentTimeMillis());
      ResultSet rs = stmt.executeQuery(query);
      if (shouldLog) {
        long endTime = System.currentTimeMillis();
        logger.debug((endTime - startTime) + " " + query);
      }
      return rs;
    } catch (SQLException sqle) {
      if (logger.isErrorEnabled()) {
        logger.error("Query failed: "+query, sqle);
      }
      throw sqle;
    }
  }

  /**
   * Translate the value of a "query" attribute type. The "key"
   * should be one or more query substitutions. Each substitution is
   * an equals separated key and value. Multiple substitutions are
   * separated by semi-colon. Backslash can quote a character. The
   * query may be in a different database. If so, then the dbp
   * should contain properties named by concatenating the query
   * name with .database, .username, .password describing the
   * database to connect to.
   * @param type is the "data type" of the attribute value and
   * names a query that should be done to obtain the actual
   * value. 
   * @return a two-element array of attribute type and value.
   */
  public Object[] translateAttributeValue(String type, String key) throws SQLException {
    Map substitutions = createSubstitutions();
    substitutions.put(":key:", key);
    String db = dbp.getProperty(type + ".database", database);
    String un = dbp.getProperty(type + ".username", username);
    String pw = dbp.getProperty(type + ".password", password);
    try {
      ensureDriverClass(dbp.getDBType(db));
    } catch (ClassNotFoundException cnfe) {
      throw new SQLException("Driver not found for " + db);
    }
    Connection conn = DBConnectionPool.getConnection(db, un, pw);
    try {
      Statement stmt = conn.createStatement();
      String query = dbp.getQueryForDatabase(type, substitutions, type + ".database");
      ResultSet rs = executeQuery(stmt, query);
      Object[] result = new Object[2];
      if (rs.next()) {
        result[0] = rs.getString(1);
        result[1] = rs.getString(2);
      } else {
        // It would be nice to not die if the GEOLOC or whatever
        // is not found. I'm just not certain
        // how the caller (ie AssetDataDBReader) will react
        // if the result is an empty String.
        // result[0] = type;
        // result[1] = "";
        throw new SQLException(
            "No row returned for attribute value query "+
            type+"("+key+")");
      }
      rs.close();
      stmt.close();
      return result;
    } finally {
      conn.close();
    }
  }

  private void ensureDriverClass(String dbtype) throws SQLException, ClassNotFoundException {
    String driverParam = "driver." + dbtype;
    String driverClass = Parameters.findParameter(driverParam);
    if (driverClass == null) {
      // this is likely a "cougaar.rc" problem.
      // Parameters should be modified to help generate this exception:
      throw new SQLException("Unable to find driver class for \""+
                             driverParam+"\" -- check your \"cougaar.rc\"");
    }
    Class.forName(driverClass);
  }

}
