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

package org.cougaar.planning.ldm.asset;

import org.cougaar.core.domain.FactoryException;

/**
 *   This is the class that is responsible for generating a LDM object 
 * based upon a Data Dictionary string 
 */
public class EssentialAssetFactory {

  /**
   *   Constructor. Create a new instance of a the Factory.
   **/
  public EssentialAssetFactory( ) {}

  /** 
   *       Copy method for any ASSET object.
   *       @return ASSET object The object created by the factory returned as a unique instance
   *       @exception ASSETFactoryException If the method fails to create a new ASSET object .
   **/
  public Asset copy( Asset original, String aUniqueID ) throws FactoryException {
    // We need to clone it before treating it like a unique instance
    Asset anInstance = null;
    try {
      anInstance = (Asset)(original.clone());

      // Associate an ItemIdentifierProperty with the new Asset.
      // The clone already has it's own IIP, no need to cons a new one.
      //NewItemIdentificationPG iip = PropertyGroupFactory.newItemIdentificationPG();
      NewItemIdentificationPG iip = (NewItemIdentificationPG) anInstance.getItemIdentificationPG();
      iip.setItemIdentification( aUniqueID );
      iip.setNomenclature( aUniqueID );
      //iip.setAlternateItemIdentification( aUniqueID );
      //anInstance.setItemIdentificationPG( iip );
    } catch( CloneNotSupportedException ce ) {
      ce.printStackTrace();
      throw new FactoryException( "Clone not supported for " + aUniqueID + " " + original.getClass() );
    }

    return anInstance;
  }

  /**
   *       Method to create a new object of type Asset.
   *       This should only be called from the LDMFactory.
   *       @return Asset object The object created by the factory returned as an abstract object type
   *       @exception FactoryException If the method fails to create a new Asset .
   **/
  public Asset create( Class myClass, String aTypeID ) throws FactoryException {
    // Assign existing prototypes from the cache
    //  otherwise create a new prototype and add it to the cache
    // The dictionary name is required to find the correct instance of a
    // prototype object.
    Asset theAsset = null;
    try {
      theAsset = (Asset)myClass.newInstance();
      if( theAsset == null )
        throw new FactoryException( "Could not create the Data Model Object for class " + myClass );
            
      // Attach the Type Identification property here because
      //   1. We have the value, and
      //   2. We depend on the Type Identification value to form Domain Names for other Properties
      //        TypeIdentificationPGImpl tip = (TypeIdentificationPropertyImpl)theAsset.createTypeIdentificationProperty();

      String convertedID = aTypeID;
      if( aTypeID == null )
        convertedID = "RuntimePrototype";

      NewTypeIdentificationPG tip = (NewTypeIdentificationPG) theAsset.getTypeIdentificationPG();
      tip.setTypeIdentification( convertedID );
      tip.setNomenclature(convertedID);
      //theAsset.setTypeIdentificationPG( tip );  // All TypeIds are mutable!

      /*
      theAsset.setTypeIdentificationPG((TypeIdentificationPG) 
                                             tip.lock(getKey()));
      */
      theAsset.setTypeIdentificationPG(tip);
    } catch( Exception be ) {
      be.printStackTrace();
      throw new FactoryException("Exception caught while building " +
                                  aTypeID +
                                  " based on asset class: " + 
                                  myClass + 
                                  ".\n" + 
                                  be.toString() );
    }

    return theAsset;
  }

  /** return the assetFactory's property key **/
  private Object getKey() { return this; }

  public int hashCode() { return this.getClass().hashCode(); }
  public boolean equals(Object o) {
    return o != null && 
      ( this==o || this.getClass()==o.getClass());
  }
}
