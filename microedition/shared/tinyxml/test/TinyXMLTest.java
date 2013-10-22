package org.cougaar.microedition.shared.tinyxml.test;

/*
 *  TinyXMLTest: a tiny application which outputs the structure of XML files
 *  Copyright (C) 1999  Tom Gibara <tom@srac.org>
 *
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

import java.io.InputStream;

import java.util.Hashtable;
import java.util.Enumeration;

import org.cougaar.microedition.shared.tinyxml.HandlerBase;
import org.cougaar.microedition.shared.tinyxml.ParseException;
import org.cougaar.microedition.shared.tinyxml.XMLInputStream;
import org.cougaar.microedition.shared.tinyxml.XMLParser;

/**
 * This class uses the xml data from TestData to test
 * the XMLParser on the palm pilot.
 * This class extends the HandlerBase and therefor does
 * not implement all methods provided by DocumentHandler.
 *
 * @author: Christian Sauer
 */
public class TinyXMLTest extends HandlerBase {
/**
 * Default constructor for TinyXMLTest.
 */
private TinyXMLTest() {
	super();
}
/**
 * This method is called to indicate that the document
 * stream has been successfully closed.
 */
public void docEnd() {
	System.out.println("**** END OF DOCUMENT ****");
}
/**
 * This method is called to indicate that the document
 * stream has been opened successfully.
 */
public void docStart() {
	System.out.println("**** START OF DOCUMENT ****\n");
}

public void charData(String charData) {
  System.out.println("CHARDATA: "+charData);
}

/**
 * This method is called to indicate the start of an element (tag).
 *
 * @param name the name of the element (tag name)
 * @param attr a hashtable containing the explicitly supplied attributes (as strings)
 */
public void elementStart(String name, Hashtable attr) throws ParseException {
	System.out.println("Element: " + name);
	if (attr != null) {
		Enumeration e = attr.keys();
		while (e.hasMoreElements()) {
			Object k = e.nextElement();
			System.out.println(k + " = " + attr.get(k));
		}
		System.out.println("");
	}
}
/*public static void main(String[] args) {
	// start a new test
	new TinyXMLTest().start();
}*/
/**
 * The main functionality of the test is implemented here.
 */
private void start() {
	try {
System.out.println("START");
		// get a new XMLInputStream with the xml string from TestData
		XMLInputStream aStream = new XMLInputStream(new TestData().toString());
		// get parser instance
		XMLParser aParser = new XMLParser();
		// set this class as the handler for the parser
		aParser.setDocumentHandler(this);
		// set the input stream
		aParser.setInputStream(aStream);
		// and parse the xml
		aParser.parse();
	} catch (ParseException e) {
		// e.printStacktrace() is still a dummy in CLDC1.0
		System.out.println(e.toString());
	}
System.out.println("DONE");
}
}
