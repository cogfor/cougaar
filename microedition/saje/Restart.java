package org.cougaar.microedition.saje;
import com.ajile.jem.rawJEM;
/**
 * This class requires changes to the JVM. See Readme.saje.txt for instructions
 */
public class Restart implements org.cougaar.microedition.node.Rebooter {

	public void reboot() {
		System.out.println("Rebooting ...");
		try {Thread.sleep(3000);} catch (Exception ex){}
		trap26();
	}
// Native menthod for the trap instruction with index 26. 
public static native void trap26();
// Native method for halt instruction to restart the JVM. 
public static native void halt(int code);
// This method has to be entered in the trap vector table. 
// which is defined in "Startup.txt". 
public static void trap26Handler() {
// The halt instruction must be executed from a trap handler. 
// The argument of 0 causes a JVM restart. 
halt(0);
}
}
