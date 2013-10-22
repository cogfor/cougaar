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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.cougaar.core.component.Service;

/**
 * This service provides access to database configuration data.
 */
public interface DBInitializerService extends Service {

  Map createSubstitutions();

  String getNonNullString(ResultSet rs, int ix, String query)
    throws SQLException;

  String getQuery(String queryName, Map substitutions);

  Connection getConnection() throws SQLException;

  ResultSet executeQuery(Statement stmt, String query) throws SQLException;

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
  Object[] translateAttributeValue(
      String type, String key) throws SQLException;

}
