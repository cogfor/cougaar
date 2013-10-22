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
// Source file: LDM/AggregateAsset.java
// Subsystem: LDM
// Module: AggregateAsset


package org.cougaar.planning.ldm.asset ;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class AggregateAssetAdapter extends Asset  {
  private transient Asset myAsset;
  private long thequantity;
    
  AggregateAssetAdapter() { }

  AggregateAssetAdapter(AggregateAssetAdapter prototype) {
    super(prototype);
    myAsset = prototype.getAsset();
  }

  public Asset getAsset() {
    return myAsset;
  }

  public void setAsset(Asset arg_Asset) {
    myAsset= arg_Asset;
  }

  public long getQuantity() {
    return thequantity;
  }
  
  void setQuantity(long quantity){
    thequantity = quantity;
  }

  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
    myAsset = (Asset) stream.readObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(myAsset);
  }

  private static PropertyDescriptor properties[];
  static {
    try {
      properties = new PropertyDescriptor[2];
      properties[0] = new PropertyDescriptor("Asset", AggregateAssetAdapter.class, "getAsset", null);
      properties[1] = new PropertyDescriptor("Quantity", AggregateAssetAdapter.class, "getQuantity", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pds = super.getPropertyDescriptors();
    PropertyDescriptor[] ps = new PropertyDescriptor[pds.length+properties.length];
    System.arraycopy(pds, 0, ps, 0, pds.length);
    System.arraycopy(properties, 0, ps, pds.length, properties.length);
    return ps;
  }

  public int hashCode() {
    int hc = 0;
    if (myAsset != null) hc=myAsset.hashCode();
    hc += thequantity;
    return hc;
  }

  /** Equals for aggregate assets is defined as having the
   * same quantity of the same (equals) asset.  TID and IID are
   * ignored.
   **/
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(getClass() == o.getClass())) return false;
    AggregateAssetAdapter oaa = (AggregateAssetAdapter) o;
    if (myAsset != null && !(myAsset.equals(oaa.getAsset()))) return false;
    if (thequantity != oaa.getQuantity()) return false;
    ItemIdentificationPG pg1 = getItemIdentificationPG();
    String id1 = (pg1 ==null)?null:pg1.getItemIdentification();
    ItemIdentificationPG pg2 = oaa.getItemIdentificationPG();
    String id2 = (pg2 ==null)?null:pg2.getItemIdentification();

                                // return true IFF
    return (id1 != null &&      // both have non-null item ids
            id1.equals(id2)     //  which are .equals
            );
  }
}
