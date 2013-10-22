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
package org.cougaar.microedition.shared.tinyxml;

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

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import org.cougaar.microedition.shared.tinyxml.util.CharacterUtility;

/** <p>Class which parses XML without validation.  The parsing offered
 *  by this class is not quite conformant. Refer to the development
 *  diary for details of non-conformance.  Using this class will incur
 *  only ~ 17k code overhead.</p>
 *
 *  <p>The parser currently supports the following encoding type:</p>
 *  <ul>
 *  <li>ASCII</li>
 *  </ul>
 *  Christian Sauer: i deleted other encodings, because the method 'intern'
 *  of class String to check the encoding of a platform is native and
 *  not implemented in CLDC/KVM 1.0B3.
 *
 *  <p>After parsing has been completed, the same instance of this class
 *  may be reused to parse another document. Don't forget to set a new
 *  XMLInputStream and another DocumentHandler before.</p>
 *
 *  @author Tom Gibara
 *  @version 0.7
 */

public class XMLParser {
	private static final ParseException EOS = new ParseException("unexpected end of document");
	private Hashtable paramEntities;
	private Hashtable genEntities;
	private boolean skipDoctype;
	private XMLReader reader;
	private char cn;
	private DocumentHandler xr;

	//document characteristics
	//not currently used
	private String version = null;
	private boolean standalone = true;
	private boolean dtd = false;
/**
 * XMLParser constructor.
 */
public XMLParser() {}
/**
 * Checks if character is a %
 * if it is then it reads a Parameter Entity Reference
 * terminates when ; is read but automatically performs a reread
 */
private void checkPEReference() throws ParseException {
	if (cn == '%') {
		if (!dtd)
			throw new ParseException("incorrect use of PE within internal DTD");
		read();
		String name = readName();
		if (cn != ';')
			throw new ParseException("parameter entity not terminated with ';'");
		if (!paramEntities.containsKey(name))
			throw new ParseException("parameter entity not recognised ");
		String[] attr = (String[]) paramEntities.get(name);
		if (attr[0] == null) {
			attr[0] = name;
			skipDoctype = !parseExternal(attr);
		} else {
			reader.push(attr[0]);
			read();
			//should check for degeneration here
			checkPEReference();
		}
	}
}
/**
 * returns true if this character  may legally
 * appear as the first letter of a name
 * as defined in the spec.
 * static method on Character was replaced with
 * method on ccare.palm.util.CharacterUtility
 *
 * @param char the character to check
 */
private static boolean isFirstNameChar(char c) {
	return (CharacterUtility.isLetter(c) || c == ':' || c == '_');
}
/**
 * Returns true if this character is a ' or "
 *
 * @param char the character to check
 */
private static boolean isQuote(char c) {
	return (c == '\'' || c == '"');
}
/**
 * returns true if character is white space
 * as defined by XML spec.
 *
 * @param char the character to check
 */
private static boolean isWhite(char c) {
	return (c == 32) || (c == 9) || (c == 13) || (c == 10);
}
/**
 * Called to parse the underlying XML document.
 * "Parsing events" are sent to the document handler
 * you have registrated before.
 */
public void parse() throws ParseException {
	genEntities = new Hashtable();
	paramEntities = new Hashtable();
	readDocument();
	try {
		reader.close();
		reader = null;
	} catch (IOException e) {
		throw new ParseException(e.getMessage());
	}
	paramEntities = null;
	genEntities = null;
	xr = null;
}
/**
 * assumes name of entity has been 'temporarily' stuffed into
 * first index of array returns false if the responder
 * declined to resolve it
 *
 * @param String[] the attributes
 */
private boolean parseExternal(String[] attr) throws ParseException {
	InputStream is = xr.resolveExternalEntity(attr[0], attr[1], attr[2]);
	if (is == null)
		return false;
	XMLReader oldReader = reader;
	reader = new XMLReader(is);
	readXMLTag();
	StringBuffer sb = new StringBuffer();
	try {
		while (true) {
			read();
			sb.append(cn);
		}
	} catch (ParseException e) {
		if (e != EOS)
			throw e;
	}
	attr[0] = sb.toString();
	reader = oldReader;
	return true;
}
/**
 * Reads a single character from the underlying reader
 * throws an EOS exception if it was there are no more.
 */
private void read() throws ParseException {
	try {
		cn = (char) reader.read();
	} catch (IOException e) {
		throw new ParseException(e.getMessage());
	}
	if (cn == (char) - 1) {
		throw EOS;
	}
}
//reads tag attributes
//eats trailing whitespace
//expects first letter to have been read
//whether or not attributes exist
private Hashtable readAttributes() throws ParseException {
	Hashtable ht = null;
	String a;
	String v;
	while (isFirstNameChar(cn)) {
		a = readName();
		if (cn != '=')
			throw new ParseException("expected = after attribute name");
		read();
		readWhite();
		v = readAttrValue();
		if (ht == null)
			ht = new Hashtable();
		ht.put(a, v);
	}
	return ht;
}
//reads a quoted string value
//expands references
//expects first quote to have been read
//eats trailing whitespace
private String readAttrValue() throws ParseException {
	StringBuffer sb = new StringBuffer();
	if (!isQuote(cn))
		throw new ParseException("unquoted attribute value");
	char term = cn;
	read();
	while (cn != term) {
		if (cn == '<')
			throw new ParseException("unescaped < in attribute value");
		else
			if (cn == '&') {
				sb.append(readReference());
			} else {
				sb.append(cn);
			}
		read();
	}
	read();
	readWhite();
	return sb.toString();
}
//reads a single character reference
//expects # to have already been read
//terminates when ; is read
//this could be made more efficient by replacing startsWith by a stribgbuffer function
private char readCharacterRef() throws ParseException {
	StringBuffer sb = new StringBuffer();
	read();
	while (cn != ';') {
		sb.append(cn);
		read();
	}
	String ref = sb.toString();
	int radix = 10;
	if (ref.startsWith("x")) {
		ref = ref.substring(1);
		radix = 16;
	}
	try {
		return (char) Integer.parseInt(ref, radix);
	} catch (NumberFormatException e) {
		throw new ParseException("unrecognized character reference");
	}
}
//expects first character of contents to be read
//does not read beyond the tag-close
private void readCharData() throws ParseException {
	StringBuffer sb = new StringBuffer();
	while (cn != '<') {
		if (cn == '&') {
			sb.append(readReference());
		} else {
			sb.append(cn);
		}
		read();
	}
	xr.charData(sb.toString());
}
//expects first to have been read
private String readChars(int count) throws ParseException {
	StringBuffer sb = new StringBuffer();
	while (sb.length() < count) {
		sb.append(cn);
		read();
	}
	return sb.toString();
}
//reads </ > tags
private void readClosingTag() throws ParseException {
	read();
	String closeName = readName();
	readWhite();
	if (readTagClose())
		throw new ParseException("close tag ended with />");
	xr.elementEnd(closeName);
}
private void readDocument() throws ParseException {
	boolean inProlog = true;
	boolean inEpilog = false;
	boolean isEmpty = true;
	int depth = 0;
	readXMLTag();
	xr.docStart();
	read();
	readWhite();
	while (true) {
		//read in a node
		if (cn == '<') {
			read();
			switch (cn) {
				case '?' :
					readPITag();
					break;
				case '!' :
					readBangTag();
					break;
				case '/' :
					readClosingTag();
					depth--;
					break;
				default :
					String closeName = readTag();
					if (closeName == null)
						depth++;
					else
						xr.elementEnd(closeName);
					if (inEpilog)
						throw new ParseException("element found outside root element");
					inProlog = false;
					break;
			}
			if (!inEpilog && !inProlog && depth == 0)
				inEpilog = true;
			try {
				read();
				readWhite();
			} catch (ParseException e) {
				if (e == EOS)
					break;
				else
					throw e;
			}
		} else {
			readCharData();
			if (inProlog || inEpilog)
				throw new ParseException("character data outside root element");
		}
	}
	if (!inEpilog)
		throw new ParseException("no root element in document");
	xr.docEnd();
}
private void readElementTag() throws ParseException {
	String name = readName();
	String content;
	checkPEReference();
	if (cn == '(')
		content = readParens(true);
	else {
		content = readName();
		if (!content.equals("ANY") && !content.equals("EMPTY"))
			throw new ParseException("expected 'EMPTY' or 'ANY'");
	}
	if (cn != '>')
		throw new ParseException("expected tag close");
	xr.elementDeclaration(name, content);
	//System.out.println("** GOT ELEMENT NAME: "+name+" CONTENT: "+content+" **");
}
//parses an entity reference
//expects first letter to have been read (ie. 'P' or 'S')
//the return type is REALLY ugly but I don't want the expense of a wrapper class
//eats trailing whitespace
private String[] readExternalID(boolean allowPubOnly) throws ParseException {
	String[] ret = new String[2];
	String sorp = readName();
	if (sorp.equals("SYSTEM")) {
		ret[0] = null;
		ret[1] = readPubSysID(false);
	} else
		if (sorp.equals("PUBLIC")) {
			ret[0] = readPubSysID(true);
			if (!allowPubOnly || isQuote(cn))
				ret[1] = readPubSysID(false);
			else
				ret[1] = null;
		} else
			throw new ParseException("expected external ID");
	return ret;
}
//reads a tag name
//expects the first letter to have been read
//eats trailing whitespace
private String readName() throws ParseException {
	if (!isFirstNameChar(cn))
		throw new ParseException("name in tag started without letter, : or _");
	StringBuffer sb = new StringBuffer();
	do {
		sb.append(cn);
		read();
	} while (CharacterUtility.isLetterOrDigit(cn) || cn == '.' || cn == '-' || cn == '_' || cn == ':');
	readWhite();
	return sb.toString();
}
//used to read element content dec. and enumerated type dec.
//boolean parameter indicates whether the expression contains regexp stuff
//eats trailing whitespace
private String readParens(boolean regexp) throws ParseException {
	if (cn != '(')
		throw new ParseException("( expected");
	int bc = 0;
	StringBuffer sb = new StringBuffer();
	do {
		checkPEReference();
		if (cn == '(')
			bc++;
		if (cn == ')')
			bc--;
		sb.append(cn);
		read();
	} while (bc != 0);
	if (regexp) {
		if (cn == '?' || cn == '+' || cn == '*') {
			sb.append(cn);
			read();
		}
	}
	readWhite();
	return sb.toString();
}
//reads PI <? ?> tags
//expects ? to have been read
private void readPITag() throws ParseException {
	read();
	String name = readName();
	if (name.toLowerCase().equals("xml"))
		throw new ParseException("<?xml?> tag must start document");
	StringBuffer sb = new StringBuffer();
	char c1 = cn;
	read();
	while (c1 != '?' || cn != '>') {
		sb.append(c1);
		c1 = cn;
		read();
	}
	xr.pi(name, sb.toString());
}

//reads Comment <!-- --> tags
//expects ! to have been read
private void readBangTag() throws ParseException {
  read();
  char under1 = cn;
  read();
  if ((under1 != '-') || (cn != '-'))
    throw new ParseException("Unknown bang tag:\"!"+under1+cn+"\"");
  // read until "-->"
  char [] close = {'-','-','>'};
  int idx = 0;

  while (idx < close.length) {
    read();
    if (cn == close[idx])
      idx++;
    else
      idx = 0;
  }
}


//reads a system or public id (depending on boolean passed in
//does not currently limit the characters in pubid and it should
//expects first character to have been read (should be " or ')
//eats trailing whitespace
private String readPubSysID(boolean pub) throws ParseException {
	checkPEReference();
	if (!isQuote(cn))
		throw new ParseException("unquoted system or public ID");
	char term = cn;
	StringBuffer sb = new StringBuffer();
	read();
	while (cn != term) {
		//should change false to test for illegal character
		if (pub && false)
			throw new ParseException("illegal character in Public ID");
		sb.append(cn);
		read();
	}
	read();
	readWhite();
	return sb.toString();
}
//converts a reference into it's string value
//expects & to have been read
//terminates when ; is read
private String readReference() throws ParseException {
	read();
	if (cn == '#')
		return String.valueOf(readCharacterRef());
	String ref = readName();
	if (ref.equals("quot"))
		return "\"";
	if (ref.equals("apos"))
		return "'";
	if (ref.equals("lt"))
		return "<";
	if (ref.equals("gt"))
		return ">";
	if (ref.equals("amp"))
		return "&";
	String[] store = (String[]) genEntities.get(ref);
	if (store == null)
		throw new ParseException("unrecognized character reference");
	if (store[3] != null)
		throw new ParseException("cannot parse notation");
	if (store[0] == null)
		parseExternal(store);
	reader.push(store[0]);
	return "";
}
//reads < > tags
//expects first letter of name to have been read
//returns name if it's closed
private String readTag() throws ParseException {
	String name = readName();
	Hashtable attr = (readAttributes());
	xr.elementStart(name, attr);
	return (readTagClose()) ? name : null;
}
//reads > or />
//returns true if the later was read
private boolean readTagClose() throws ParseException {
	if (cn != '/' && cn != '>')
		throw new ParseException("expected tag close");
	boolean f = false;
	while (cn != '>') {
		f = (cn == '/');
		read();
	}
	return f;
}
//used to eat up unwanted whitepace
//terminates at first non-white character
private void readWhite() throws ParseException {
	while (isWhite(cn))
		read();
}
//parses <?xml?> declaration
//allows version, encoding and standalone to be supplied out-of-order
//also allows illegal attribute names in xml tag
private void readXMLTag() throws ParseException {
	StringBuffer sb = new StringBuffer();
	//snoop the first five characters
	try {
		for (int n = 0; n < 5; n++) {
			read();
			sb.append(cn);
		}
	} catch (ParseException e) {
		reader.push(sb.toString());
		if (e == EOS)
			return;
		else
			throw e;
	}
	String firstFive = sb.toString();
	if (firstFive.equals("<?xml")) {
		read();
		readWhite();
		//parse the tag here
		Hashtable ht = readAttributes();
		if (ht == null)
			throw new ParseException("empty <?xml?> tag");
		if (ht.size() > 3)
			throw new ParseException("too many attributes in <?xml?> tag");
		version = (String) ht.get("version");
		//	    String encoding = (String)ht.get("encoding");
		String ss = (String) ht.get("standalone");
		if (version == null)
			throw new ParseException("no xml version");
		/*		try {
		if (encoding != null)
		reader.setEncoding(encoding);
		} catch (UnsupportedEncodingException e) {
		throw new ParseException("unsupported encoding");
		}
		*/
		if (cn != '?')
			throw new ParseException("illegal character in <?xml?> tag");
		read();
		if (cn != '>')
			throw new ParseException("expected tag close");
	} else {
		reader.push(firstFive);
	}
}
/**
 * Sets the DocumentHandler for the Parser.
 *
 * @param aHandler ccare.palm.xml.DocumentHandler the handler for this document
 */
public void setDocumentHandler(DocumentHandler aHandler) {
	xr = aHandler;
}
/**
 * Sets the XMLInputStream, the stream from which
 * the parser parses from.
 *
 * @param theInputStream java.io.InputStream the input stream
 */
public void setInputStream(InputStream theInputStream) {
	reader = new XMLReader(theInputStream);
}
}
