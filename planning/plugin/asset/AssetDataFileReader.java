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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;

import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/**
 * Parses local asset prototype-ini.dat to create local asset and the
 * Report tasks associated with all the local asset's relationships.
 * Local asset must have ClusterPG and RelationshipPG, Presumption is
 * that the 'other' assets in all the relationships have both Cluster
 * and Relationship PGs. Currently assumes that each Agent has
 * exactly 1 local asset.
 *
 * Format:
 * <xxx> - parameter
 * # - comment character - rest of line ignored
 *
 * Skeleton form:
 * [Prototype]
 *  <asset_class_name> # asset class must have both a ClusterPG and a RelationshipPG
 *
 * [Relationship]
 * # <role> specifies Role played by this asset for another asset. 
 * # If start/end be specified as "", they default to 
 * # TimeSpan.MIN_VALUE/TimeSpan.MAX_VALUE
 * <role> <other asset item id> <other asset type id> <other asset agent id> <relationship start time> <relationship end time>
 *
 * [<PG name>]
 * # <slot type> - one of Collection<data type>, List<data type>, String, Integer, Double, Boolean,
 * #  Float, Long, Short, Byte, Character 
 * 
 * <slot name> <slot type> <slot values>
 *
 * Sample:
 * [Prototype]
 * Entity
 *
 * [Relationship]
 * "Subordinate"   "Headquarters"        "Management"   "HQ"           "01/01/2001 12:00 am"  "01/01/2010 11:59 pm"
 * "PaperProvider" "Beth's Day Care"     "Day Care Ctr" "Beth's Home"  "02/13/2001 9:00 am"   "" 
 *
 * [ItemIdentificationPG]
 * ItemIdentification String "Staples, Inc"
 * Nomenclature String "Staples"
 * AlternateItemIdentification String "SPLS"
 *
 * [TypeIdentificationPG]
 * TypeIdentification String "Office Goods Supplier"
 * Nomenclature String "Big Box"
 * AlternateTypeIdentification String "Stationer"
 * 
 * [ClusterPG]
 * MessageAddress MessageAddress "Staples"
 * 
 * [EntityPG]
 * Roles Collection<Role> "Subordinate, PaperProvider, CrayonProvider, PaintProvider"
 * 
 **/
public class AssetDataFileReader implements AssetDataReader {
  private AssetDataCallback cb;
  private String agentId;
  private static Logger logger = Logging.getLogger(AssetDataFileReader.class);
  
  public String getFileName() {
    return agentId + "-prototype-ini.dat";
  }

  /**
   * 
   */
  public void readAsset(String aId, AssetDataCallback cb) {
    this.cb = cb;
    agentId = aId;
    String dataItem = "";
    int newVal;

    String filename = getFileName();
    BufferedReader input = null;
    Reader fileStream = null;

    try {
      fileStream = 
        new InputStreamReader(cb.getConfigFinder().open(filename));
      input = new BufferedReader(fileStream);
      StreamTokenizer tokens = new StreamTokenizer(input);
      tokens.commentChar('#');
      tokens.wordChars('[', ']');
      tokens.wordChars('_', '_');
      tokens.wordChars('<', '>');      
      tokens.wordChars('/', '/');      
      tokens.ordinaryChars('0', '9');      
      tokens.wordChars('0', '9');      

      newVal = tokens.nextToken();
      // Parse the prototype-ini file
      while (newVal != StreamTokenizer.TT_EOF) {
        if (tokens.ttype != StreamTokenizer.TT_WORD)
          formatError("ttype: " + tokens.ttype + " sval: " + tokens.sval);
        dataItem = tokens.sval;
        if (dataItem.equals("[Prototype]")) {
          newVal = tokens.nextToken();
          String assetClassName = tokens.sval;
          cb.createMyLocalAsset(assetClassName);
          newVal = tokens.nextToken();
          continue;
        }
        if (!cb.hasMyLocalAsset())
          formatError("Missing [Prototype] section");
        if (dataItem.equals("[Relationship]")) {
          newVal = fillRelationships(newVal, tokens);
          continue;
        }
        if (dataItem.equals("[LocationSchedulePG]")) {
          // parser language is currently incapable of expressing a 
          // complex schedule, so here we hack in some minimal support.
          newVal = setLocationSchedulePG(dataItem, newVal, tokens);
          continue;
        }
        if (dataItem.startsWith("[")) {
          // We've got a property or capability
          newVal = setPropertyForAsset(dataItem, newVal, tokens);
          continue;
        }
        // if The token you read is not one of the valid
        // choices from above
        formatError("Incorrect token: " + dataItem);
      }

      // Closing BufferedReader
      if (input != null)
	input.close();

      // Only generates a NoSuchMethodException for AssetSkeleton
      // because of a coding error. If we are successul in creating it
      // here it then the AssetSkeleton will end up with two copies
      // the add/search criteria in AssetSkeleton is for a Vector and
      // does not gurantee only one instance of each class. Thus the
      // Org allocator plugin fails to recognize the correct set of
      // capabilities.
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  } 

  private void formatError(String msg) {
    throw new RuntimeException("Error parsing " + getFileName() + ": "
                               + msg);
  }

  /**
   * Creates the property, fills in the slots based on what's in the
   * prototype-ini file and then sets it for (or adds it to) the asset
   **/
  protected int setPropertyForAsset(String prop, int newVal,
                                    StreamTokenizer tokens)
    throws IOException
  {
    String propertyName = prop.substring(1, prop.length()-1).trim();
    NewPropertyGroup propertyGroup = null;
    try {
      propertyGroup = cb.createPropertyGroup(propertyName);
    } catch (Exception e) {
      formatError("Unrecognized keyword for a prototype-ini file: ["
                  + propertyName + "]");
      return StreamTokenizer.TT_EOF;
    }
    try {
      newVal = tokens.nextToken();
      String member = tokens.sval;
      // Parse through the property section of the file
      while (newVal != StreamTokenizer.TT_EOF) {
        if ((tokens.ttype == StreamTokenizer.TT_WORD)
            && !(tokens.sval.substring(0,1).equals("["))) {
          newVal = tokens.nextToken();
          String dataType = tokens.sval;
          newVal = tokens.nextToken();
          // Call appropriate setters for the slots of the property
          Object[] args = new Object[] {cb.parseExpr(dataType, tokens.sval)};
          cb.callSetter(propertyGroup, "set" + member, 
			cb.getType(dataType), args);
          newVal = tokens.nextToken();
          member = tokens.sval;
        } else {
          // Reached a left bracket "[", want to exit block
          break;
        }
      } //while

      // Add the property to the asset
      cb.addPropertyToAsset(propertyGroup);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      formatError("Exception during parse");
    }
    return newVal;
  }


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
   *   "FixedLocation \"(" + LATITUDE + ", " + LONGITUDE + ")\""
   *
   * For example, all of time at latitude 12.3 longitude -45.6:
   *   FixedLocation "(12.3, -45.6)"
   * </pre>
   */
  protected int setLocationSchedulePG(String prop, int newVal,
                                      StreamTokenizer tokens)
    throws IOException
  {
    // read two strings
    String firstStr;
    String secondStr;
    newVal = tokens.nextToken();
    if ((newVal == StreamTokenizer.TT_EOF) ||
        (tokens.sval.substring(0,1).equals("["))) {
      // Reached a left bracket "[", want to exit block
      return newVal;
    }
    firstStr = tokens.sval;

    newVal = tokens.nextToken();
    if ((newVal == StreamTokenizer.TT_EOF) ||
        (tokens.sval.substring(0,1).equals("["))) {
      // Reached a left bracket "[", want to exit block
      return newVal;
    }
    secondStr = tokens.sval;

    newVal = tokens.nextToken();

    // skip "FixedLocation " string
    if (!(firstStr.equals("FixedLocation"))) {
      logger.error(
          "Expecting: FixedLocation \"(LAT, LON)\"\n"+
          "Not: "+firstStr+" .. ");
      return newVal;
    }

    // parse single Location
    if ((!(secondStr.startsWith("("))) ||
        (!(secondStr.endsWith(")"))))  {
      logger.error(
          "Expecting: FixedLocation \"(LAT, LON)\"\n"+
          "Not: FixedLocation "+secondStr+" ..");
      logger.error("SWith(: "+secondStr.startsWith("("));
      logger.error("EWith): "+secondStr.endsWith(")"));
      return newVal;
    }
    String locStr = 
      secondStr.substring(
          1, secondStr.length()-1);
    int sepIdx = locStr.indexOf(",");
    if (sepIdx < 0) formatError("Bad Location syntax: " + locStr);
    String latStr = locStr.substring(0, sepIdx).trim();
    String lonStr = locStr.substring(sepIdx+1).trim();
    cb.setLocationSchedule(latStr, lonStr);
    // done
    return newVal;
  }

  /**
   * Fills in myRelationships with arrays of relationship, agentName and capableroles triples.
   */
  protected int fillRelationships(int newVal, StreamTokenizer tokens) throws IOException {
    newVal = tokens.nextToken();
    while ((newVal != StreamTokenizer.TT_EOF) &&
           (!tokens.sval.substring(0,1).equals("["))) {

      String roleName = "";
      String itemId = "";
      String typeId = "";
      String otherAgentId = "";
      long start = cb.getDefaultStartTime();
      long end = cb.getDefaultEndTime();
          
      for (int i = 0; i < 6; i++) {
        if ((tokens.sval.length()) > 0  &&
            (tokens.sval.substring(0,1).equals("["))) {
          throw new RuntimeException("Unexpected character: " + 
                                     tokens.sval);
        }
            
        switch (i) {
        case 0:
          roleName = tokens.sval.trim();
          break;

        case 1:
          itemId = tokens.sval.trim();
          break;

        case 2:
          typeId = tokens.sval.trim();
          break;
          
        case 3:
          otherAgentId = tokens.sval.trim();
          break;

        case 4:
          if (!tokens.sval.equals("")) {
            try {
              start = cb.parseDate(tokens.sval);
            } catch (java.text.ParseException pe) {
              logger.error("Unable to parse: " + tokens.sval + 
			   ". Start time defaulting to " + 
			   cb.getDefaultStartTime());
            }
          }
          break;

        case 5:
          if (!tokens.sval.equals("")) {
            try {
              end = cb.parseDate(tokens.sval);
            } catch (java.text.ParseException pe) {
              logger.error("Unable to parse: " + tokens.sval + 
			   ". End time defaulting to " + 
			   cb.getDefaultEndTime());
            }
          }
          break;
        }
        newVal = tokens.nextToken();
      }
      cb.addRelationship(typeId, itemId, otherAgentId, roleName, start, end);
    } //while
    return newVal;
  }
}
