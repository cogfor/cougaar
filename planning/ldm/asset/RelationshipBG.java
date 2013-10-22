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

import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.RelationshipScheduleImpl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class RelationshipBG implements PGDelegate {
  protected transient NewRelationshipPG myPG;

  public PGDelegate copy(PropertyGroup pg) {
    if (!(pg instanceof NewRelationshipPG)) {
      throw new java.lang.IllegalArgumentException("Property group must be a RelationshipPG");
    }

    NewRelationshipPG relationshipPG = (NewRelationshipPG ) pg;

    HasRelationships x = null;
    if (relationshipPG.getRelationshipSchedule() != null) {
      x = relationshipPG.getRelationshipSchedule().getHasRelationships();
    }
    RelationshipBG bg = new RelationshipBG();
    bg.init(relationshipPG, x);
    return bg;
  }

  public void readObject(ObjectInputStream in) {
    try {
      in.defaultReadObject();

      if (in instanceof org.cougaar.core.persist.PersistenceInputStream){
        myPG = (NewRelationshipPG) in.readObject();
      } else {
        // If not persistence, need to initialize the relationship schedule
        myPG = (NewRelationshipPG) in.readObject();
        init(myPG, myPG.getRelationshipSchedule().getHasRelationships());
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new RuntimeException();
    } catch (ClassNotFoundException cnfe) {
      cnfe.printStackTrace();
      throw new RuntimeException();
    }
  }

  public void writeObject(ObjectOutputStream out) {
    try {
      // Make sure that it agrees with schedule
      out.defaultWriteObject();

      if (out instanceof org.cougaar.core.persist.PersistenceOutputStream) {
        out.writeObject(myPG);
      } else {
        // Clear schedule before writing out
        myPG.getRelationshipSchedule().clear();
        out.writeObject(myPG);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new RuntimeException();
    }
  }

  public void init(NewRelationshipPG pg, HasRelationships hasRelationships) {
    myPG = (NewRelationshipPG) pg;

    RelationshipScheduleImpl pgSchedule = (RelationshipScheduleImpl) pg.getRelationshipSchedule();
    if ((pgSchedule == null) ||
      (pgSchedule.isEmpty())){
      myPG.setRelationshipSchedule(new RelationshipScheduleImpl(hasRelationships));
    } else if (!pgSchedule.getHasRelationships().equals(hasRelationships)) {
      throw new java.lang.IllegalArgumentException("");
    }

    pg.setRelationshipBG(this);
  }

  public boolean isSelf() {
    return isLocal();
  }

  public boolean isLocal() {
    return myPG.getLocal();
  }
}



