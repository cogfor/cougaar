/*
 * <copyright>
 *  
 *  Copyright 2001-2004 Mobile Intelligence Corp
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

package org.cougaar.community;

import org.cougaar.core.service.community.CommunityResponse;

/**
 * Response to request performed by Community Manger.
 **/
public class CommunityResponseImpl implements CommunityResponse, java.io.Serializable {

  private int statusCode = UNDEFINED;
  private Object content = null;

  public CommunityResponseImpl(int code, Object content) {
    this.statusCode = code;
    this.content = content;
  }

  public void setStatus(int status) {
    statusCode = status;
  }
  public int getStatus() {
    return statusCode;
  }

  public void setContent(Object cont) {
    content = cont;
  }
  public Object getContent() {
    return content;
  }

  public String getStatusAsString() {
    switch (statusCode) {
      case UNDEFINED: return "UNDEFINED";
      case FAIL: return "FAIL";
      case SUCCESS: return "SUCCESS";
      case TIMEOUT: return "TIMEOUT";
    }
    return "INVALID_VALUE";
  }

  public String toString() {
    return "(" + getStatusAsString() + ":" +
        (content == null ? "null" : content.getClass().getName()) + ")";
  }

}
