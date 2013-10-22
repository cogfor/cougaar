/*
 * <copyright>
 * 
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.shared.tinyxml.test;

/*
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
/**
 * This class represents XML data as a String object.
 * The toString() method returns the String. 
 * NOTE: Used for testing on palm pilot.
 *
 * @author: Christian Sauer
 */

class TestData {
	private static String[] myData = { 
	"<?xml version=\"1.0\"?>",
	"<ALL>",
	"<CUSTOMER id=\"000000.956\"", 
		" name=\"Meier\"",
		" kind=\"Customer Type\"",
		" currency=\"USD\"", 
		" organisation=\"true\" />",
		
	"<CUSTOMER id=\"000000.654\"", 
		" name=\"Schulz\"", 
		" kind=\"Customer Type\"", 
		" currency=\"CHF\"", 
		" organisation=\"true\" />",
		
	"<CUSTOMER id=\"000000.873\"",
		" name=\"Kunz\"", 
		" kind=\"Customer Type\"", 
		" currency=\"FFR\"", 
		" organisation=\"true\" />",
		
	"<CUSTOMER id=\"000000.916\"",
		" name=\"Schreier\"", 
		" kind=\"Customer Type\"",
		" currency=\"EUR\"", 
		" organisation=\"true\" />",

		"<CUSTOMER id=\"000000.956\"", 
		" name=\"Meier\"",
		" kind=\"Customer Type\"",
		" currency=\"USD\"", 
		" organisation=\"true\" />",
		
	"<CUSTOMER id=\"000000.654\"", 
		" name=\"Schulz\"", 
		" kind=\"Customer Type\"", 
		" currency=\"CHF\"", 
		" organisation=\"true\" />",
		
	"<CUSTOMER id=\"000000.873\"",
		" name=\"Kunz\"", 
		" kind=\"Customer Type\"", 
		" currency=\"FFR\"", 
		" organisation=\"true\">",
  "This is the char data",
	"<FOOBEN id=\"000000.873\"",
		" name=\"Kunz\"",
		" kind=\"Customer Type\"",
		" currency=\"FFR\"",
		" organisation=\"true\">",
  "This is the fooben data",
  "</FOOBEN>",
  "</CUSTOMER>",
		
	"<CUSTOMER id=\"000000.916\"",
		" name=\"Schreier\"", 
		" kind=\"Customer Type\"",
		" currency=\"EUR\"", 
		" organisation=\"true\" />",
	"</ALL>"};
/**
 * Return the xml as string.
 * 
 * @return java.lang.String
 */
public String toString() {
	StringBuffer aBuffer = new StringBuffer();
	for (int i=0; i<myData.length; i++)
		aBuffer.append(myData[i]);
	
	return aBuffer.toString();
}
}
