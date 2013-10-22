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

package org.cougaar.microedition.io;

/**
 * This interface provides a description of a generic file loader.
 * Versions for the Tini board and KVM will be written.
 */
public interface FileLoader {

	/**
	 * The string where the Protocol, hostName and hostPort are stored.
	 * Basicly, the URL minus the file and its local path.
	 * @since   Oct 2, 2000
	 */
	String pnp = "";

	/**
	 * Concatenates the Protocol, hostName and hostPort and stores in pnp.
	 *
	 * @param   protocol	"http", etc.
	 * @param   hostName	"www.bbn.com", etc
	 * @param   hostPort	use default if = zero, default for http is 80
	 * @since   Oct 2, 2000
	 */
         void configure(String protocol, String hostName, short hostPort);

	/**
	 * Returns the contents of the pnp String. (pnp=Protocol + hostName + hostPort).
	 *
	 * @return  the contents of the String pnp.
	 * @since   Oct 2, 2000
	 */
	String showConfig();

	/**
	 * opens the URL constructed from the pnp and file name and returns contents.
	 *
	 * @param   fileName	file name to be concatenated onto the pnp.
	 * @return  String that contains the contents of the file
	 * @exception	possible IO or URL exceptions
	 * @since   Oct 2, 2000
	 */
	String getFile(String fileName) throws Exception;

	/**
	 * opens the URL constructed from the pnp and file name and writes contents.
	 *
	 * @param   fileName	file name to be concatenated onto the pnp.
	 * @param   String that contains the contents to write to file
	 * @exception	possible IO or URL exceptions
	 * @since   Oct 2, 2000
	 */
	void sendFile(String fileName, String contents) throws Exception;
}
