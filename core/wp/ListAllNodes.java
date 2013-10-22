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

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;

/**
 * A <i>non-scalable</i> utility class to recursively find all nodes
 * in the white pages.
 * <p>
 * Not scalable, so the methods of this class are deprecated.
 */
public final class ListAllNodes {

  private ListAllNodes() { }

  /**
   * Get a Set of all node names.
   * <p>
   * @deprecated not scalable!
   */
  public static Set listAllNodes(
      WhitePagesService wps) throws Exception {
      return listAllNodes(wps, 0);
  }


  public static Set listAllNodes(
				 WhitePagesService wps,
				 long timeout)
      throws Exception 
    {
	Set toSet = new HashSet();
	recurse(toSet, wps, ".", timeout);
	return toSet;
    }

  // recursive!
  private static void recurse(
      Set toSet, 
      WhitePagesService wps,
      String suffix,
      long timeout) throws Exception {
    Set names = wps.list(suffix, timeout);
    for (Iterator iter = names.iterator(); iter.hasNext(); ) {
      String s = (String) iter.next();
      if (s == null) {
      } else if (s.length() > 0 && s.charAt(0) == '.') {
        recurse(toSet, wps, s, timeout);
      } else {
        String node = getNode(wps, s, timeout);
        if (node != null) {
          toSet.add(node);
        }
      }
    }
  }

  private static String getNode(
      WhitePagesService wps,
      String s,
      long timeout) throws Exception {

      // do a WP lookup for the agent's node
      AddressEntry ae = wps.get(s, "topology", timeout);
      if (ae != null) {
        URI uri = ae.getURI();
        return uri.getPath().substring(1);
      }

    return null;
  }
}
