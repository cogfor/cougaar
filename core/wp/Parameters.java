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

package org.cougaar.core.wp;

import org.cougaar.util.Arguments;

/**
 * TODO modify wp code to use {@link Arguments} directly, since this
 * is now a dumb wrapper.
 */
public class Parameters {

  private final Arguments args;

  public Parameters(Object o) {
    this(o, null);
  }
  public Parameters(Object o, String prefix) {
    this.args = new Arguments(o, prefix);
  }

  // forward everything:
  public String getString(String key, String deflt) {
    return args.getString(key, deflt);
  }
  public String getString(String key) {
    return args.getString(key, null);
  }
  public boolean getBoolean(String key, boolean deflt) {
    return args.getBoolean(key, deflt);
  }
  public int getInt(String key, int deflt) {
    return args.getInt(key, deflt);
  }
  public long getLong(String key, long deflt) {
    return args.getLong(key, deflt);
  }
  public double getDouble(String key, double deflt) {
    return args.getDouble(key, deflt);
  }

  @Override
public String toString() {
    return args.toString();
  }
}
