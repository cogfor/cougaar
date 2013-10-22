/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.microedition.demo;

import java.io.*;
import java.util.*;
import java.net.*;

public class SWHub {

  private Hashtable socketConnections = new Hashtable();
  private boolean debugging = true;

  public static void main(String [] argv) {
    new SWHub(argv);
  }

  public SWHub(String [] argv) {
    short commSocketPort = 7777;
    if (debugging) { System.out.println("SWHub::setupSubscriptions");}

    // Parse xml parameters for "port"
    if (argv.length > 0) {
      String portNum = argv[0];
      if (portNum != null) {
        commSocketPort = Short.parseShort(portNum);
      }
    }


    try {
      ServerSocket ss = new ServerSocket(commSocketPort);
      if (debugging) System.out.println("Listening on port: " + commSocketPort);

      // Spawn thread to handle connections
      ConnectionHandler CH = new ConnectionHandler(ss);
      Thread CHThread = new Thread(CH);
      CHThread.start();
    } catch (IOException e) {
	System.err.println("Error opening serversocket"+e);
	return;
    }
  }

  // set up new connections on the server socket
  public class ConnectionHandler implements Runnable  {

    private ServerSocket SCN;

    // constructor
    public ConnectionHandler(ServerSocket SCN) {
      this.SCN = SCN;
    }

    public void run() {

      if (debugging) {System.out.println("SWHub::ConnectionHandler thread = " + Thread.currentThread());}

      for (;;) {
        try {
	  if (debugging) {System.out.println("SWHub::Awaiting next connection...");}
          Socket sc = SCN.accept();
	  if (debugging) {System.out.println("SWHub::Next connection accepted...");}
	  DataInputStream din = new DataInputStream(sc.getInputStream());
	  if (debugging) {System.out.println("SWHub::New DataInputStream created...");}
	  DataOutputStream dout = new DataOutputStream(sc.getOutputStream());
	  if (debugging) {System.out.println("SWHub::New DataOutputStream created...");}
          socketConnections.put(din, dout);
	  if (debugging) {System.out.println("SWHub::New Connection received:\ninstream =  " + din.toString() + "\noutstream = " + dout.toString());}
	  // kick off thread to service incoming data on the new socket
          new InStreamHandler(din).start();
        } catch (Throwable ex) {
          System.err.println("SWHub::Error servicing serversocket " + ex);
        }

      }
    }  // end method run()
  }  // end class ConnectionHandler

  // service connections on the server socket
  public class InStreamHandler extends Thread {

    private DataInputStream datain;

    // constructor
    public InStreamHandler(DataInputStream datain) {
      this.datain = datain;
    }

    public void run() {
      int podnumber = 0;
      long longdate = 0;
      byte datatype = 0;
      short datavalue = 0;
      int sourcetype = 0;

      if (debugging) {System.out.println("SWHub::InStreamHandler thread = " + Thread.currentThread().getName());}

      while (true) {
	try {
          // Blocking read
	  if (debugging)
	     System.out.println("SWHub blocking on datain read...");

          podnumber = datain.readInt();
	  longdate = datain.readLong();
          datatype = (byte) datain.read();
          datavalue = datain.readShort();
          sourcetype = datain.readInt();
	  if (debugging) {
            System.out.println("SWHub::SWRecord received from pod " + podnumber + ":");
	    System.out.println("pn " + podnumber + ", ld " + longdate + ", dt " + datatype + ", dv " + datavalue + ", st " + sourcetype + ".");
          }
        }
	catch (EOFException e) {
	  System.err.println("SWHub::EOF Exception. \n" + e);
	  break;
        }
	catch (Exception e) {
	  System.err.println("SWHub::Unable to read from input stream. \n" + e);
	  continue;
        }

        // write out to other streams in hashtable

	synchronized (socketConnections) {
	  Enumeration e = socketConnections.keys();
	  while (e.hasMoreElements()) {
	    Object k = e.nextElement();
	    if (debugging) {System.out.println("SWHub::Preparing to retransmit.");}
	    if (k != datain) {
	      if (debugging) {System.out.println("SWHub::Retransmitting to connection " + k);}
	      DataOutputStream output = (DataOutputStream) socketConnections.get(k);
	      try {
	        output.writeInt(podnumber);
	        output.writeLong(longdate);
	        output.write(datatype);
	        output.writeShort(datavalue);
	        output.writeInt(sourcetype);
		output.flush();
              } catch (Exception ex) {
	        System.err.println("SWHub::Unable to write to output stream. \n" + ex);
              }

	    }  // end if
	    else
	    {
	      if (debugging) {System.out.println("SWHub::Will not retransmit to self.");}
	    }
	  }  // end while
	}  // end synchronized
      }  // end while
    }  // end method run()
  }  // end class InStreamHandler
}




