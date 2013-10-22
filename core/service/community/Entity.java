/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Mobile Intelligence Corp
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
package org.cougaar.core.service.community;

import javax.naming.directory.Attributes;

/**
 * Interface defining entities that are associated with a community.  An
 * entity is typically an Agent or a Community.
 */
public interface Entity {

  /**
   * Set entity name.
   * @param name  Entity name
   */
  public void setName(String name);

  /**
   * Get entity name.
   * @return Entity name
   */
  public String getName();

  /**
   * Set entity attributes.
   * @param attrs Entity attributes
   */
  public void setAttributes(Attributes attrs);

  /**
   * Get entity attributes.
   * @return Entity attributes
   */
  public Attributes getAttributes();

  /**
   * Returns an XML representation of Entity.
   */
  public String toXml();

  /**
   * Returns an XML representation of Entity.
   * @param indent Blank string used to pad beginning of entry to control
   *               indentation formatting
   */
  public String toXml(String indent);

  /**
   * Creates a string representation of an Attribute set.
   */
  public String attrsToString();
}