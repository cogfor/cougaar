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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.planning.service.AssetInitializerService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/**
 * Parses a set of parameters to create local asset.
 * Relationships must also be passed as parameters but will be handled by the
 * AssetDataPlugin. See AssetDataPlugin for format of Relationship parameters.
 *
 * Recognized Parameter Formats:
 * Prototype:<self org asset class>  # Must be the first parameter
 *   Prototype parameter used to create the self org asse
 *
 * LocationSchedulePG:FixedLocation:(<lat>, <lon>)
 *   Very specific to LocationSchedulePG, creates a single 
 *   LocationScheduleElementImpl which has a LatLonImpl as its Location. The
 *   TimeSpan to hardcoded to run from TimeSpan.MIN_VALUE to TimeSpan.MAX_VALUE
 *
 * <property group name>:<slot name>:<data type>:<data value>
 *  General format for setting a slot on one of the self orgs property groups. Must
 *  have a parameter for each slot on each property group.
 *
 * Sample:
 * Prototype:Organization
 * ItemIdentificationPG:ItemIdentification:String:Staples, Inc
 * ItemIdentificationPG:Nomenclature:String:Staples
 * ItemIdentificationPG:AlternateItemIdentification:String:SPLS
 * TypeIdentificationPG:TypeIdentification:String:Office Goods Supplier
 * TypeIdentificationPG:Nomenclature String:Big Box
 * TypeIdentificationPG:AlternateTypeIdentification:String:Stationer
 * ClusterPG:MessageAddress:MessageAddress:Staples
 * OrganizationPG:Roles:Collection<Role>:Subordinate, PaperProvider, CrayonProvider, PaintProvider
 * LocationSchedulePG:FixedLocation:(31.8500, -81.6000)
 *
 *    <em>Note that when using XSLNode, instead of < and >, use &lt; and &gt;</em>
 * 
 **/
public class AssetDataParamReader implements AssetDataReader {
  private AssetDataCallback cb;
  private String agentId;
  private static Logger logger = Logging.getLogger(AssetDataParamReader.class);
  private Collection myParams;
  private HashMap myPropertyGroups = new HashMap();
  AssetInitializerService assetInitService = null;

  public AssetDataParamReader(Collection params, AssetInitializerService ais) {
    myParams = params;
    assetInitService = ais;
  }
    
  public static final String ATTRIBUTE_DELIMITER = ":";
  public static final String PROTOTYPE_KEY = "Prototype";
  public static final String RELATIONSHIP_KEY = "Relationship";

  /**
   * 
   */
  public void readAsset(String aId, AssetDataCallback cb) {
    this.cb = cb;
    agentId = aId;

    for (Iterator iterator = myParams.iterator();
	 iterator.hasNext();) {
      String param = (String) iterator.next();
      StringTokenizer tokenizer = 
	new StringTokenizer(param, ATTRIBUTE_DELIMITER);

      if (logger.isDebugEnabled()) {
	logger.debug("Parameter: " + param + " token count: " + tokenizer.countTokens());
      }

      String nextAttribute = tokenizer.nextToken();
      
      if (logger.isDebugEnabled()) {
	logger.debug("Attribute: " + nextAttribute);
      }

      if (nextAttribute == null) {
	logger.error("Unable to parse parameter: " + param);
	continue;
      }

      if (nextAttribute.equals(PROTOTYPE_KEY)) {
	nextAttribute = tokenizer.nextToken();
	if (logger.isDebugEnabled()) {
	  logger.debug("Prototype: " + nextAttribute);
	}
	cb.createMyLocalAsset(nextAttribute);
	continue;
      }

      if (!cb.hasMyLocalAsset()) {
	logger.error("Prototype parameter must be first. Parameter  " + param +
		     " will be ignored.");
	continue;
      }

      if (nextAttribute.equals(AssetDataPlugin.RELATIONSHIP_KEY)) {
	if (logger.isDebugEnabled()) {
	  logger.debug("AssetDataParamReader: Skipping relationship: " + param);
	}
	continue;
      }
      
      if (nextAttribute.equals("LocationSchedulePG")) {
	// parser language is currently incapable of expressing a 
	// complex schedule, so here we hack in some minimal support.
	setLocationSchedulePG(param, tokenizer);
	continue;
      }

      // Must be a PG
      if (logger.isDebugEnabled()) {
	logger.debug("Property group: " + param + " nextAttribute " + 
		     nextAttribute);
      }
      setPropertyForAsset(param, nextAttribute, tokenizer);
    }

    //Add all property groups
    for (Iterator iterator = myPropertyGroups.values().iterator();
	 iterator.hasNext();) {
      cb.addPropertyToAsset((NewPropertyGroup) iterator.next());
    }
  } 

  /**
   * Creates the property, fills in the slots based on what's in the
   * prototype-ini file and then sets it for (or adds it to) the asset
   **/
  protected void setPropertyForAsset(String param, String pgName, 
				     StringTokenizer tokenizer)
  {
    NewPropertyGroup propertyGroup = 
      (NewPropertyGroup) myPropertyGroups.get(pgName);

    if (propertyGroup == null) {
      try {
	propertyGroup = cb.createPropertyGroup(pgName);
	myPropertyGroups.put(pgName, propertyGroup);
      } catch (Exception e) {
	logger.error("Unrecognized property group name " + pgName + 
		     ". Parameter - " + param + " will be ignored.");
	return;
      }
    }

    try {
      String slotName = tokenizer.nextToken();

      String dataType = tokenizer.nextToken();

      String val = ((String) tokenizer.nextToken()).trim();

      // For the MilitaryOrgPG:HomeLocation we want the ability to look up
      // the geoloc and get the various detailed attributes.
      // So lets hope this assetInitService if available can do that
      if (dataType.startsWith("query") && assetInitService != null) {
	try {
	  Object[] r = assetInitService.translateAttributeValue(dataType, val);
	  if (r[0] instanceof String)
	      dataType = (String) r[0];
	  val = (String)r[1];
	} catch (Exception sqe) {
	  logger.error(agentId + ": Unable to query init service for item of type " + dataType + ", with input value " + val, sqe);
	}
      }

      Object[] args = new Object[] {
	cb.parseExpr(dataType, val)
      };

      // Call appropriate setters for the slots of the property
      cb.callSetter(propertyGroup, "set" + slotName, 
		    cb.getType(dataType), args);


    } catch (NoSuchElementException nsee) {
      logger.error("Unable to parse property group setting. Parameter " +
		   " will be ignored.");
    }
    return;
  }

  public static final String FIXEDLOCATION = "FixedLocation";

  /**
   * Hack to attach a LocationSchedulePG to an Asset.
   * <pre>
   * For now we only support a single LocationScheduleElementImpl
   * which has a LatLonPointImpl as it's Location.  The TimeSpan
   * is hard-coded to TimeSpan.MIN_VALUE .. TimeSpan.MAX_VALUE.
   * In the future this can be enhanced to support full location
   * schedules, but that would likely require a new file format.
   * 
   * The format is:
   *   "FixedLocation:\"(" + LATITUDE + ", " + LONGITUDE + ")\""
   *
   * For example, all of time at latitude 12.3 longitude -45.6:
   *   FixedLocation:"(12.3, -45.6)"
   * </pre>
   */
  protected void  setLocationSchedulePG(String param, StringTokenizer tokenizer) {
    try {
      String nextToken = tokenizer.nextToken();

      // skip "FixedLocation " string
      if (!(nextToken.equals(FIXEDLOCATION))) {
	logger.error("Expecting: " + FIXEDLOCATION + 
		     ATTRIBUTE_DELIMITER + "\"(LAT, LON)\"\n" +
		     "Not: " + nextToken + " .. ");
	return;
      }
	
      nextToken = tokenizer.nextToken();

      // parse single Location
      if ((!(nextToken.startsWith("("))) ||
	  (!(nextToken.endsWith(")"))))  {
	  logger.error("Expecting: " + FIXEDLOCATION + 
		       ATTRIBUTE_DELIMITER + "\"(LAT, LON)\"\n" +
		       "Not: " + FIXEDLOCATION + nextToken + " ..");
	  return;
      }

      String locationStr = 
	nextToken.substring(1, nextToken.length()-1);

      int commaIndex = locationStr.indexOf(",");
      if (commaIndex < 0) {
	// formatError throws a RuntimeException
	logger.error("Expecting: " + FIXEDLOCATION + 
		     ATTRIBUTE_DELIMITER + "\"(LAT, LON)\"\n" +
		     "Not: " + FIXEDLOCATION + nextToken + " ..");
      }
	
      String latStr = locationStr.substring(0, commaIndex).trim();
      String lonStr = locationStr.substring(commaIndex + 1).trim();
      cb.setLocationSchedule(latStr, lonStr);

    } catch (NoSuchElementException nsee) {
      logger.error("Unable to parse LocationSchedulePG parameter." +
		   "Expecting: " + FIXEDLOCATION + 
		   ATTRIBUTE_DELIMITER + "\"(LAT, LON)\"\n" +
		   "Not: " + param);
    }
    
    // done
    return;
  }
}

