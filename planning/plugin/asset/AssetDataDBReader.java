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

package org.cougaar.planning.plugin.asset;

import java.util.Arrays;

import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.planning.service.AssetInitializerService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Populates an "Asset" from the config database.
 **/
public class AssetDataDBReader implements AssetDataReader {
  private static Logger logger = Logging.getLogger(AssetDataDBReader.class);

  private AssetDataCallback cb;
  private String agentId;
  AssetInitializerService assetInitService;

  public AssetDataDBReader(AssetInitializerService ais) {
    assetInitService = ais;
  }

  /**
   * 
   */
  public void readAsset(String aId, AssetDataCallback cb) {
    this.cb = cb;

    if (assetInitService == null) {
      logger.fatal("AssetInitializerService is null." +
		   " Unable to create local asset for " + aId);
      return;
    }
    try {
      agentId = aId;
      cb.createMyLocalAsset(assetInitService.getAgentPrototype(aId));
      String[] pgNames = assetInitService.getAgentPropertyGroupNames(aId);
      for (int i = 0; i < pgNames.length; i++) {
        String pgName = pgNames[i];
        NewPropertyGroup pg = cb.createPropertyGroup(pgName);
        cb.addPropertyToAsset(pg);
        Object[][] props = assetInitService.getAgentProperties(aId, pgName);
        for (int j = 0; j < props.length; j++) {
          Object[] prop = props[j];
          String attributeName = (String) prop[0];
          String attributeType = (String) prop[1];
          Object attributeValue = prop[2];
          if (attributeType.startsWith("query")) {
            String v = ((String) attributeValue).trim();
            Object[] r = assetInitService.translateAttributeValue(attributeType, v);
            attributeType = (String) r[0];
            attributeValue = r[1];
          }
          if (attributeType.equals("FixedLocation")) {
            String v = ((String) attributeValue).trim();
            if (v.startsWith("(")) v = v.substring(1);
            if (v.endsWith(")")) v = v.substring(0, v.length() - 1);
            int ix = v.indexOf(',');
            String latStr = v.substring(0, ix);
            String lonStr = v.substring(ix + 1);
            cb.setLocationSchedule(latStr.trim(), lonStr.trim());
          } else {
            if (attributeValue.getClass().isArray()) {
              String[] rv = (String[]) attributeValue;
              Object[] pv = new Object[rv.length];
              for (int k = 0; k < rv.length; k++) {
                pv[k] = cb.parseExpr(attributeType, rv[k]);
              }
              Object[] args = {Arrays.asList(pv)};
              cb.callSetter(pg, "set" + attributeName, "Collection", args);
            } else {
              Object[] args = {cb.parseExpr(attributeType, (String) attributeValue)};
              cb.callSetter(pg, "set" + attributeName, cb.getType(attributeType), args);
            }
          }
        }
      }
      String[][] relationships = assetInitService.getAgentRelationships(aId);
      for (int i = 0; i < relationships.length; i++) {
        String[] r = relationships[i];
        long start = cb.getDefaultStartTime();
        long end = cb.getDefaultEndTime();
        try {
          start = cb.parseDate(r[4]);
        } catch (java.text.ParseException pe) {
          logger.error("Unable to parse: " + r[4] +
		       ". Start time defaulting to " +
                       start);
        }
        try {
          if (r[5] != null) {
            end = cb.parseDate(r[5]);
          }
        } catch (java.text.ParseException pe) {
          logger.error("Unable to parse: " + r[5] +
		       ". End time defaulted to " + end);
        }
        cb.addRelationship(r[2],     // Type id
                           r[1],     // Item id
                           r[3],     // Other agent
                           r[0],     // Role
                           start,    // Start time
                           end);     // End time
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }
}
