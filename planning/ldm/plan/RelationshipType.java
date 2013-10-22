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

import java.util.HashMap;

/**
 * Relationship - maps relationship between any two objects
 * Role describes the Role the direct object is performing for the 
 * indirect object.
 * BOZO think up better terms than direct/indirect object
 **/

public class RelationshipType {
  private static HashMap myTypes = new HashMap(3);

  public static RelationshipType create(String firstSuffix,
                                        String secondSuffix) {

    RelationshipType existing = get(firstSuffix);

    if (existing == null) {
      return new RelationshipType(firstSuffix, secondSuffix);
    } else if ((existing.getFirstSuffix().equals(firstSuffix)) &&
               (existing.getSecondSuffix().equals(secondSuffix))) {
      return existing;
    } else {
      throw new java.lang.IllegalArgumentException("First suffix " +
                                                   firstSuffix + " or " + 
                                                   " second suffix " + 
                                                   secondSuffix + 
                                                   " already used in - " +
                                                   existing);
    }
    

  }

  public static RelationshipType get(String suffix) {
    return (RelationshipType) myTypes.get(suffix);
  }


  private String myFirstSuffix;
  private String mySecondSuffix;

  public String getFirstSuffix() {
    return myFirstSuffix;
  }

  public String getSecondSuffix() {
    return mySecondSuffix;
  }

  public String toString() {
    return myFirstSuffix + ", " + mySecondSuffix;
  }

  private RelationshipType(String firstSuffix, String secondSuffix) {
    myFirstSuffix = firstSuffix;
    mySecondSuffix = secondSuffix;
    
    myTypes.put(firstSuffix, this);
    myTypes.put(secondSuffix, this);
  }

  public static void main(String []args) {
    create("Superior", "Subordinate");
    create("Provider", "Customer");
    //create("aaa", "Customer");
    //create("Superior", "bbb");

    System.out.println("Match on chocolate - " + get("chocolate"));
    System.out.println("Match on Superior - " + get("Superior"));
    System.out.println("Match on Customer - " + get("Customer"));
    
  }
}
  

