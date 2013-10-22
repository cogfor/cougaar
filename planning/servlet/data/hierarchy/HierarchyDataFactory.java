/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.planning.servlet.data.hierarchy;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;
import org.xml.sax.Attributes;

/**
 * Factory that produces sub-objects based on tags and attributes
 * for HierarchyData
 *
 * @since 1/24/01
 **/
public class HierarchyDataFactory implements DeXMLableFactory{

  //Variables:
  ////////////

  //Members:
  //////////
  
  /**
   * This is a look ahead to see if we should start a sub object.
   * The caller will first call this function on startElement.  If
   * this function returns null, the startElement will be reported
   * to the current object with a call to openTag(...).  Otherwise
   * this function should return a new DeXMLable subobject that
   * further output will be deligated to, until the subobject returns
   * true from a call to endElement().
   *
   * @param curObj the current object
   * @param name startElement tag
   * @param attr startElement attributes
   * @return a new DeXMLable subobject if a subobject should be created,
   * otherwise null.
   **/
  public DeXMLable beginSubObject(DeXMLable curObj, String name, 
				  Attributes attr)
    throws UnexpectedXMLException{
    if(curObj==null){
      if(name.equals(HierarchyData.NAME_TAG)){
	return new HierarchyData();
      }else if(name.equals(Failure.NAME_TAG)){
	return new Failure();
      }
    }else if((curObj instanceof HierarchyData) && 
	     name.equals(Organization.NAME_TAG)){
      return new Organization();
    }
    return null;
  }
}

