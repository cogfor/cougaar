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
 
 /**
 * This type serves as a adapter class for the 
 * DocumentHandler interface.
 * 
 * @author: Christian Sauer
 */
public class HandlerBase implements DocumentHandler {
/**
 * HandlerBase constructor comment.
 */
public HandlerBase() {
	super();
}
/**
 * charData method comment.
 */
public void charData(String charData) {}
/**
 * comment method comment.
 */
public void comment(String comment) {}
/**
 * docEnd method comment.
 */
public void docEnd() {}
/**
 * docStart method comment.
 */
public void docStart() {}
/**
 * elementDeclaration method comment.
 */
public void elementDeclaration(String name, String content) throws ParseException {}
/**
 * elementEnd method comment.
 */
public void elementEnd(String name) throws ParseException {}
/**
 * elementStart method comment.
 */
public void elementStart(String name, java.util.Hashtable attr) throws ParseException {}
/**
 * pi method comment.
 */
public void pi(String name, String value) throws ParseException {}
/**
 * resolveExternalEntity method comment.
 */
public java.io.InputStream resolveExternalEntity(String name, String pubID, String sysID) throws ParseException {
	return null;
}
}
