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
package org.cougaar.microedition.shared;

public interface Constants {

  public interface Aspects {

    static int LATITUDE = 100;
    static int LONGITUDE = 101;
    static int HEADING = 102;
    static int BEARING = 103;
    static int DETECTION = 104;
    static int SCANDIR = 105;
    static int IMAGE = 106;
    static int FLASHLIGHT = 107;
    static int DETECTION_TIME = 108;
    static int TIME = 109;
  }

  public interface Robot  {

    static int min = 2;

    static String [] roles = { "SurveillanceProvider", "ImageProvider"};
    static int SURVEILLANCEPROVIDER = 0;
    static int IMAGEPROVIDER = 1;

    static String [] meRoles = { "Everything", "PositionProvider", "LocomotionController", "TargetingController",
      "FlashlightController", "TurretController", "SONARSensor", "CameraController", "WeaponProvider"};
    static int EVERYTHING = 0;
    static int POSITIONPROVIDER = 1;
    static int LOCOMOTIONCONTROLLER = 2;
    static int TARGETINGCONTROLLER = 3;
    static int FLASHLIGHTCONTROLLER = 4;
    static int TURRETCONTROLLER = 5;
    static int SONARSENSOR = 6;
    static int CAMERACONTROLLER = 7;
    static int WEAPONPROVIDER = 8;

    static String [] verbs = { "ReportPosition", "Advance", "TraverseWaypoints",
      "ReportTarget", "ControlFlashlight", "RotateTurret", "ReportDetection",
      "GetImage", "StartSystem", "SetOrientation", "DetectTarget", "SetWaypoint",
      "LaunchWeapon", "SetLaunchTime", "EngageWeapon"};
    static int REPORTPOSITION = 0;
    static int ADVANCE = 1;
    static int TRAVERSEWAYPOINTS = 2;
    static int REPORTTARGET = 3;
    static int CONTROLFLASHLIGHT = 4;
    static int ROTATETURRET = 5;
    static int REPORTDETECTION = 6;
    static int GETIMAGE = 7;
    static int STARTSYSTEM = 8;
    static int SETORIENTATION = 9;
    static int DETECTTARGET = 10;
    static int SETWAYPOINT = 11;
    static int LAUNCHWEAPON = 12;
    static int SETLAUNCHTIME = 13;
    static int ENGAGEWEAPON = 14;

    static String [] prepositions = { "Degrees", "Speed", "Velocity",
      "TurretHemisphere", "Lat", "Lon", "Bearing", "Rotation", "Translation",
      "StartAngle", "StopAngle", "Time", "Range"};
    final static int ORIENTATIONPREP= 0; // "Degrees"
    final static int SPEEDPREP= 1; // "Speed";
    final static int VELOCITYPREP=2; // "Velocity";
    final static int TURRETDIRECTIONPREP=3; // "TurretHemisphere"
    final static int LATPREP=4; // "Lat"
    final static int LONPREP=5; // "Lon"
    final static int BEARINGPREP=6; // "Bearing"
    final static int ROTATEPREP=7;
    final static int TRANSLATEPREP=8;
    final static int STARTANGLEPREP=9;
    final static int STOPANGLEPREP=10;
    final static int TIMEPREP=11;
    final static int RANGEPREP=12;

    public static final String SEARCHLEFT = "left";
    public static final String SEARCHFRONT = "front";
    public static final String SEARCHRIGHT = "right";

    public static final int TURRET_LEFT = 0;
    public static final int TURRET_MIDDLE = 1;
    public static final int TURRET_RIGHT = 2;

  }

  public interface Geophysical {

    static final double EARTH_RADIUS_METERS = 6378137.0; //meters;
    static final double MAGNETIC_DECLINATION = 0.0; //degrees E=+ W=-
    static final double DEGTOBILLIONTHS = 1000000000.0; //10^9
    static final double BILLIONTHSTODEG = 0.000000001; //10^-9

  }
}