To use the reset logic in Restart.java you have to add access to an opcode in the aJile JVM.  To do that add some lines to files in the aJile/Runtime_cldc/txt directory.  The three files are in this list and can be just dropped into version 3.15.  If you're using a different version, you probably need to merge.  Look for comments containing "cougaarme" in the files here. 

The changes get picked up by Jembuilder the next time you build.
