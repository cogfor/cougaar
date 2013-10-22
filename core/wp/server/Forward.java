/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.wp.server;

import java.io.Serializable;

import org.cougaar.core.wp.resolver.Lease;
import org.cougaar.core.wp.resolver.Record;

/**
 * Data ({@link Record}s) replicated between servers through
 * "forward"ing.
 */
public final class Forward implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final Lease lease;
  private final Record record;

  public Forward(
      Lease lease,
      Record record) {
    this.lease = lease;
    this.record = record;
    // validate
    String s =
      (lease == null ? "null lease" :
       (record != null &&
        !record.getUID().equals(lease.getUID())) ?
       "record "+record+" doesn't match lease "+lease :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The non-null lease.
   */
  public Lease getLease() {
    return lease;
  }

  /**
   * The record, which may be null.
   * <p>
   * If the record is null then the lease was renewed based upon
   * the UID.  If the recipient doesn't know the matching record
   * then it should send back a LeaseNotKnown response.
   */
  public Record getRecord() {
    return record;
  }

  @Override
public String toString() {
    return "(forward lease="+lease+" record="+record+")";
  }
}
