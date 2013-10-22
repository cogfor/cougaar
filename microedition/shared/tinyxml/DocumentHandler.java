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
import java.util.Hashtable;

/** <p>Interface which must be implemented by any XML application which
 *  wishes to use the XMLParser class.  It consists of a set of call
 *  back methods which are called by the XML parser when specific
 *  XML entities are encountered.</p>
 *
 *  <p>Very few of the methods return a value.  Of those that do, only
 *  the DocumentStream method actually needs to return a non-null
 *  value. This makes the interface very easy to implement.</p>
 *  
 *  <p>For developers unfamiliar with the concepts or terminology of XML,
 *  an introductory document on XML may help to explain some of the
 *  terms used in this documentation.</p>
 *
 *  <p>This paragraph describes the order in which the methods may be
 *  called. The getDocumentStream is always the first method called,
 *  followed by the recordDocStart method, after which comments and
 *  PI's may be received at any time until recordDocEnd method is
 *  called. All 'declaration' method calls are guaranteed to occur
 *  before any call to recordDocTypeDeclaration which is called
 *  <em>after</em> both the internal and external DTD's (if they
 *  existed) have been sucessfully parsed. no DTD related methods are
 *  called after the first call to recordElementStart. Each such
 *  method call is matched with a call to recordElementEnd. Character
 *  data will only be received within some element (ie. after the
 *  first element start and before the last element end). Contiguous
 *  character data may be reported in repeated calls to
 *  recordCharData.</p>
 *
 *  <p>All callbacks are made on the single thread used to start the
 *  parsing.</p>
 *
 *  <p>All character data supplied to the parser by this interface
 *  must be supplied in one of the following encodings:</p>
 *
 *  <ul>
 *  <li>ASCII</li>
 *  </ul>
 *
 * Christian Sauer:
 * - The encodings were removed for use on palm
 * - name of the Interface XMLResponder changed to DocumentHandler.
 * 
 *  @author Tom Gibara
 *  @version 0.7
 *  @see XMLParser 
 */


public interface DocumentHandler {

/** 
 * This method is called to return character data to the
 * application.  As per the XML specification, newlines on all
 * platforms are converted to single 0x0A's.  Contiguous
 * character data may be returned with successive calls to this
 * method.
 *
 * @param charData character data from the document.
 */
public void charData(String charData);
/** 
 * This method is called to return comments to the application.
 * Most applications will have no use for this information.
 *
 * @param comment the contents of the comment tag
 */
public void comment(String comment);
/** 
 * This method is called to indicate that the document 
 * stream has been successfully closed.
 */
public void docEnd();
/** 
 * This method is called to indicate that the document 
 * stream has been opened successfully.
 */
public void docStart();
/** 
 * This method is called when an element declaration is met in a DTD.
 * Most applications will have no use for this information.
 *  
 * @param name the tag name given to this element
 * @param content a regexp-like expression which specifies which elements and character data may appear within this element
 */
public void elementDeclaration(String name, String content) throws ParseException;
/** 
 * This method is called to indicate the closure of an element(tag).
 *  
 * @param name the name of the element (tag) being closed
 */
public void elementEnd(String name) throws ParseException;
/** 
 * This method is called to indicate the start of an element (tag).
 *
 * @param name the name of the element (tag name)
 * @param attr a hashtable containing the explicitly supplied attributes (as strings)
 */
public void elementStart(String name, Hashtable attr) throws ParseException;
/** This method is called to passback program instructions to the
 *  application.
 *
 *  @param name the name of the program instruction
 *  @param value the data associated with this program instruction
 */
public void pi(String name, String value) throws ParseException;
/** 
 * This method is called when an external entity must be resolved
 * for insertion into the document its DTD.  This method may
 * return null as an indication that the application declines to
 * retrieve the entity.
 *
 * @param name the name of the entity to be retrieved.
 * @param pubID the public ID of the entity (may be null)
 * @param sysID the system ID of the entity
 * @return an InputStream which supplies the entity's replacement text
 */
public InputStream resolveExternalEntity(String name, String pubID, String sysID) throws ParseException;
}
