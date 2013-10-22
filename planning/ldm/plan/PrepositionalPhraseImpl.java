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

package org.cougaar.planning.ldm.plan;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Setters for PrepositionalPhrase. 
 */

public class PrepositionalPhraseImpl 
  implements PrepositionalPhrase, NewPrepositionalPhrase, java.io.Serializable
{

  private String preposition;
  private transient Object indirectobject; // changed to transient : Persistence

  // no-arg constructor
  public PrepositionalPhraseImpl() { }

  PrepositionalPhraseImpl(String p, Object io) {
    preposition = p;
    indirectobject = io;
  }

  /** @return Answer with a nicely formatted string representation. */
  public String toString()
  {
    if ( indirectobject == null && preposition == null)
      return "[ a null valued prepositional phrase]";
    else if ( indirectobject == null )
      return  preposition + " <null>";
    else
      return preposition + " " + indirectobject.toString() ;
  }

  /** PrepositionalPhrase interface implementations */
	
  /**@return String - String representation of the Preposition */
  public String getPreposition() {
    return preposition;
  }
	
  /** @return Object - the IndirectObject */
  public Object getIndirectObject() {
    return indirectobject;
  }
  
  /** NewPrepositionalPhrase interface implementations */
  	
  /**@param apreposition - Set the String representation of the Preposition */
  public void setPreposition(String apreposition) {
    if (apreposition != null) apreposition = apreposition.intern();
    preposition = apreposition;
  }
	
  /** @param anindirectobject - Set the IndirectObject of the PrespositionalPhrase */
  public void setIndirectObject(Object anindirectobject ) {
    indirectobject = anindirectobject;
  }
	
  /** PP are equals() IFF prepositions are the same and 
   * indirectobjects are .equals()
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (preposition == null) return false;
    if (o instanceof PrepositionalPhraseImpl) {
      PrepositionalPhraseImpl ppi = (PrepositionalPhraseImpl) o;
      String ppip = ppi.preposition;
      return (preposition == ppip) &&
        ((indirectobject==null)?
         (ppi.indirectobject==null):(indirectobject.equals(ppi.indirectobject)));
    } else {
      return false;
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
 
    stream.defaultWriteObject();

    try {
      stream.writeObject(indirectobject);
    } catch (NotSerializableException nse) {
      System.err.println(nse + " for indirectobject of " + preposition + ": " + indirectobject);
      throw nse;
    }
 }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();
    if (preposition != null) preposition = preposition.intern();
    indirectobject = stream.readObject();
  }
}
