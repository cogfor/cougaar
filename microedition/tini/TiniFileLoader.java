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

import org.cougaar.microedition.io.*;
import java.net.*;
import java.io.*;

public class TiniFileLoader implements FileLoader {

	String pnp = "file:";

	public TiniFileLoader() {}

  public void configure(String protocol, String hostName, short hostPort) {

		if (hostPort == 0)
		    pnp = protocol + "://" + hostName + "/";
		else
		    pnp = protocol + "://" + hostName + ":" + hostPort + "/";
	}

	public String showConfig() {

		return(pnp);
	}

	public String getFile(String fileName) throws Exception {

		int i;
		int count = 0;
		byte [] b = new byte[512];
		StringBuffer content = new StringBuffer(512);
		URL path;
		InputStream in = null;

                if (pnp.equals(""))
                  path = new URL(fileName);
                else
		  path = new URL(pnp + fileName);
		in = path.openStream();

		while ((count = in.read(b, 0, 512)) > 0)
			for (i=0; i<count; i++)
				content.append((char)b[i]);

		in.close();
		return content.toString();
	}

	public void sendFile(String fileName, String outstring) throws Exception
	{
		FileOutputStream fout = new FileOutputStream(fileName, true);

		fout.write(outstring.getBytes());
		fout.flush();
		fout.close();
	}

/*
	public static void main(String[] args) {

    String content = "";
		String file = "/node.properties";

    System.out.println("Node starting name = " + args[0] +
                       " name server = " + args[1] +
                       " port = " + args[2]);

		FileLoader fl = new TiniFileLoader();
    fl.configure("http", args[1], Short.parseShort(args[2]));

		try {
		    content = fl.getFile(args[0] + file);
		} catch(Exception e) {
			System.out.println("getFile exception: " + e);
			System.exit(1);
		}
		System.out.println(content);

		System.exit(0);
	}
*/
}
