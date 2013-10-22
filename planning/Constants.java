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

package org.cougaar.planning;


/**
 * Define standard Planning domain constants for use by Plugins and LPs.
 * Most domains will define a set of constants. Here they are Verbs and Prepositions.
 */
public interface Constants {

  
  interface Verb {
    // Cougaar Planning defined verb types
    // Keep in alphabetical order

    /** The Verb for an Entity to Report "up" to another **/
    String REPORT = "Report";

    org.cougaar.planning.ldm.plan.Verb Report= org.cougaar.planning.ldm.plan.Verb.get(REPORT);
  }

  /** Prepositions for use in PrepositionalPhrases off of Tasks **/
  interface Preposition {
    // Cougaar Planning defined prepositions
    String WITH        = "With"; 	// typically used for the OPlan object
    String TO          = "To"; 	// typically used for a destination geoloc
    String FROM        = "From"; 	// typically used for an origin geoloc
    String FOR         = "For"; 	// typically used for the originating organization
    String OFTYPE      = "OfType"; 	// typically used with abstract assets
    String USING       = "Using"; 	// typically used for ???
    String AS          = "As"; 	// used with Roles for RFS/RFD task
  }
}







