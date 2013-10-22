/*
 * <copyright>
 *
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.tini;

import com.dalsemi.system.*;

/**
 * Kills a TINI-OS task.
 * Run: java KillJava.tini <ps substring to kill>
 * eg: "java KillJava.tini Node.tini"
 */
public class KillJava {

  public KillJava(String token) {
  System.err.println("START");
    String [] ps = TINIOS.getTaskTable();
    for (int i=0; i<ps.length; i++) {
      System.out.println("ps: "+ps[i]);
      if (ps[i].indexOf(token) != -1) {
        String pid = ps[i].substring(0, ps[i].indexOf(":"));
        int ipid = Integer.parseInt(pid);
        System.out.println("KILL: "+ipid);
        TINIOS.killTask(ipid);
      }
    }
  }
/*
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("USAGE: java KillJava.tini <what-to-kill>");
    } else {
      KillJava killJava1 = new KillJava("Node.tini");
    }
  }
*/
}