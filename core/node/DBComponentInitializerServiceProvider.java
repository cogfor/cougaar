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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * {@link ServiceProvider} for a {@link ComponentInitializerService}
 * backed by the {@link DBInitializerService}.
 */
class DBComponentInitializerServiceProvider implements ServiceProvider {

  private final DBInitializerService dbInit;
  private final Logger logger;

  public DBComponentInitializerServiceProvider(DBInitializerService dbInit) {
    this.dbInit = dbInit;
    this.logger = Logging.getLogger(getClass());
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != ComponentInitializerService.class) {
      throw new IllegalArgumentException(
          getClass()+" does not furnish "+serviceClass);
    }
    return new ComponentInitializerServiceImpl();
  }

  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class ComponentInitializerServiceImpl implements ComponentInitializerService {
    // Remember, this returns only items strictly _below_ the given 
    // insertion point, listed as children of the given component.
    public ComponentDescription[] getComponentDescriptions(
        String parentName, String containerInsertionPoint) throws InitializerException
    {
      if (logger.isDebugEnabled()) {
        logger.debug("In getComponentDescriptions");
      }
      if (parentName == null) throw new IllegalArgumentException("parentName cannot be null");
      // append a dot to containerInsertionPoint if not already there
      if (!containerInsertionPoint.endsWith(".")) containerInsertionPoint += ".";
      Map substitutions = dbInit.createSubstitutions();
      substitutions.put(":parent_name:", parentName);
      substitutions.put(":container_insertion_point:", containerInsertionPoint);
      if (logger.isInfoEnabled()) {
        logger.info(
            "Looking for direct sub-components of " + parentName +
            " just below insertion point " + containerInsertionPoint);
      }

      try {
        Connection conn = dbInit.getConnection();
        try {
          Statement stmt = conn.createStatement();
          String query = dbInit.getQuery("queryComponents",  substitutions);

          /*
             if (logger.isDebugEnabled()) {
             logger.debug("getComponentDescriptions doing query " + query);
             }
           */

          ResultSet rs = dbInit.executeQuery(stmt, query);
          List componentDescriptions = new ArrayList();
          while (rs.next()) {
            String componentName = dbInit.getNonNullString(rs, 1, query);
            String componentClass = dbInit.getNonNullString(rs, 2, query);
            String componentId = dbInit.getNonNullString(rs, 3, query);
            String insertionPoint = dbInit.getNonNullString(rs, 4, query);
            String priority = dbInit.getNonNullString(rs, 5, query);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":component_id:", componentId);
            String query2 = dbInit.getQuery("queryComponentParams",  substitutions);

            ResultSet rs2 = dbInit.executeQuery(stmt2, query2);
            ArrayList vParams = null;
            while (rs2.next()) {
              String param = dbInit.getNonNullString(rs2, 1, query2);
              if (!param.startsWith("PROP$")) { // CSMART private arg
                if (vParams == null) vParams = new ArrayList(1); // lazy create
                vParams.add(param);
              }
            }

            ComponentDescription desc =
              new ComponentDescription(componentName,
                  insertionPoint,
                  componentClass,
                  null,  // codebase
                  vParams,
                  null,  // certificate
                  null,  // lease
                  null, // policy
                  ComponentDescription.parsePriority(priority));
            componentDescriptions.add(desc);
            rs2.close();
            stmt2.close();
          } // end of loop over result set

          int len = componentDescriptions.size();
          if (logger.isDebugEnabled()) {
            logger.debug("... returning " + len + " CDescriptions");
          }
          ComponentDescription[] result = new ComponentDescription[len];
          result = (ComponentDescription[])
            componentDescriptions.toArray(result);

	  // Print out each component description
          if (logger.isDetailEnabled()) {
            for (int i = 0; i < result.length; i++) {
              StringBuffer buf = new StringBuffer();
              buf.append(result[i].getInsertionPoint());
              if(result[i].getPriority() != ComponentDescription.PRIORITY_STANDARD) {
                buf.append("(");
                buf.append(ComponentDescription.priorityToString(result[i].getPriority()));
                buf.append(") ");

              }
              buf.append("=");
              buf.append(result[i].getClassname());
              ArrayList params = (ArrayList) result[i].getParameter();
              int n = params.size();
              if (n > 0) {
                for (int j = 0; j < n; j++) {
                  if (j == 0)
                    buf.append("(");
                  else
                    buf.append(", ");
                  buf.append(params.get(j));
                }
                buf.append(")");
              }
	      logger.detail(buf.toString());
            }
          } // end of if(detail)

          return result;
        } finally {
          conn.close();
        }
      } catch (Exception e) {
        throw new InitializerException(
            "getComponentDescriptions("+parentName+", "+containerInsertionPoint+")",
            e);
      }
    }

    public boolean includesDefaultComponents() {
      return false;
    }
  }
}
