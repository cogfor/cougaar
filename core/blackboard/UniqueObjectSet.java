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

package org.cougaar.core.blackboard;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.KeyedSet;

/**
 * A custom {@link java.util.Set} that maintains a {@link
 * java.util.Map}-like associations from {@link 
 * UID}s to {@link UniqueObject}s.
 */
public class UniqueObjectSet 
  extends KeyedSet
{
  @Override
protected Object getKey(Object o) {
    if (o instanceof UniqueObject) {
      return ((UniqueObject) o).getUID();
    } else {
      return null;
    }
  }

  public UniqueObject findUniqueObject(Object o) {
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject)o).getUID();
      return (UniqueObject) inner.get(uid);
    } else {
      return null;
    }
  }

  public UniqueObject findUniqueObject(UniqueObject o) {
    UID uid = o.getUID();
    return (UniqueObject) inner.get(uid);
  }

  public UniqueObject findUniqueObject(UID uid) {
    return (UniqueObject) inner.get(uid);
  }

}
