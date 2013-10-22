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

package org.cougaar.core.node.service.jvmdump;


/**
 * The JNI implementation to signal a stack dump.
 * <p>
 * The library name is "jvmdump".  On Unix compiled file that
 * must be in your $LD_LIBRARY_PATH (or -Djava.library.path) is
 * "$CIP/bin/libjvmdump.so".  On Windows the compiled file that
 * must be in your %PATH% (or -Djava.library.path) is
 * "%CIP%\bin\jvmdump.dll".
 * <p>
 * This class is package private, but includes a "main(..)"
 * method for testing.
 */
class JniStackDump {

  private static final String LIBRARY_NAME = "jvmdump";

  private static boolean needLibrary = true;
  private static boolean haveLibrary = false;

  /**
   * Load library if necessary, check for loading errors and 
   * disable the dump if the library can't be loaded.
   * @return true if the libary is available.
   */
  private static boolean checkLibrary() {
    if (needLibrary) {
      try {
        System.loadLibrary(LIBRARY_NAME);
        haveLibrary = true;
      } catch (UnsatisfiedLinkError e) {
        // missing library
      }
      needLibrary = false;
    }
    return haveLibrary;
  }

  // native method declaration
  private static native boolean jvmdump();

  /**
   * Request that the JVM dump its stack to std-out.
   *
   * @return true if the stack was dumped
   */
  public static synchronized boolean dumpStack() {
    return checkLibrary() && jvmdump();
  }

  public static void main(String args[]) {
    System.out.println("Dump JVM stack: ");
    boolean ret = dumpStack();
    System.out.println("response: "+ret);
    if (!ret) {
      try {
        System.loadLibrary(LIBRARY_NAME);
      } catch (UnsatisfiedLinkError e) {
        // print library error
        e.printStackTrace();
      }
    }
  }
}
