package org.cougaar.planning.ldm.plan;

public interface NamedPosition extends LatLonPoint, Cloneable {
  String getUid();
  /** @return String - the string name representing this position */
  String getName();
}
