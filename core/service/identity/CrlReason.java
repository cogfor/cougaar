/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Networks Associates Technology, Inc
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
 *
 * Created on September 12, 2001, 10:55 AM
 */

package org.cougaar.core.service.identity;


/**
 * A reason for revoking a certificate.
 * <p>
 * These reasons match RFC 2459 section 5.3.1.
 */
public class CrlReason
{
  // revocation reasons:
  public static final int UNSPECIFIED            = 0;
  public static final int KEY_COMPROMISE         = 1;
  public static final int CA_COMPROMISE          = 2;
  public static final int AFFILIATION_CHANGED    = 3;
  public static final int SUPERSEDED             = 4;
  public static final int CESSATION_OF_OPERATION = 5;
  public static final int CERTIFICATE_HOLD       = 6;
  public static final int REMOVE_FROM_CRL        = 8;
  public static final int PRIVILEGE_WITHDRAWN    = 9;
  public static final int AA_COMPROMISE          = 10;

  private int reason;

  // private constructor, called only within this class
  public CrlReason(int value) {
    reason = value;
  }

  public int getReason() {
    return reason;
  }

  public String getReasonAsString() {
    switch (reason) {
      case UNSPECIFIED: return "UNSPECIFIED";
      case KEY_COMPROMISE: return "KEY_COMPROMISE";
      case CA_COMPROMISE: return "CA_COMPROMISE";
      case AFFILIATION_CHANGED: return "AFFILIATION_CHANGED";
      case SUPERSEDED: return "SUPERSEDED";
      case CESSATION_OF_OPERATION: return "CESSATION_OF_OPERATION";
      case CERTIFICATE_HOLD: return "CERTIFICATE_HOLD";
      case REMOVE_FROM_CRL: return "REMOVE_FROM_CRL";
      case PRIVILEGE_WITHDRAWN: return "PRIVILEGE_WITHDRAWN";
      case AA_COMPROMISE: return "AA_COMPROMISE";
      default: return "UNKNOWN ("+reason+")";
    }
  }

  @Override
public String toString() {
    return "Certificate revoked due to "+getReasonAsString();
  }
}
