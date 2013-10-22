echo on
rem /*
rem  * <copyright>
rem  * Copyright 1997-2000 Defense Advanced Research Projects Agency (DARPA)
rem  * and ALPINE (A BBN Technologies (BBN) and Raytheon Systems Company
rem  * (RSC) Consortium). This software to be used in accordance with the
rem  * COUGAAR license agreement.  The license agreement and other
rem  * information on the Cognitive Agent Architecture (COUGAAR) Project can
rem  * be found at http://www.cougaar.org or email: info@cougaar.org.
rem  * </copyright>
rem  */
rem Script to generate asset classes

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\clib\build.jar

rem Regenerate and recompile all property/asset files
java -classpath %LIBPATHS% org.cougaar.tools.build.AssetWriter  -Porg.cougaar.microedition.se.domain assets.def
java -classpath %LIBPATHS% org.cougaar.tools.build.PGWriter properties.def
