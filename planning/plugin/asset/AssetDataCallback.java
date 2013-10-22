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

import java.text.ParseException;

import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.util.ConfigFinder;

/**
 * Callback provided by AssetDataPlugin (or extension) to AssetDataReader implementation.
 * Typically the implementation is an inner class to the Plugin.
 */
public interface AssetDataCallback {
    ConfigFinder getConfigFinder();
    void createMyLocalAsset(String assetClassName);
    boolean hasMyLocalAsset();
    NewPropertyGroup createPropertyGroup(String propertyName) throws Exception;
    Object parseExpr(String dataType, String value);
    long parseDate(String dateString) throws ParseException;
    String getType(String type);
    void callSetter(NewPropertyGroup propertyGroup, String setterName, 
		    String type, Object[] arguments);
    void setLocationSchedule(String latStr, String lonStr);
    long getDefaultStartTime();
    long getDefaultEndTime();
    void addPropertyToAsset(PropertyGroup propertyGroup);
    void addRelationship(String typeId, String itemId,
                         String otherAgentId, String roleName,
                         long start, long end);
}
