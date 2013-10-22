package org.cougaar.planning.ldm.plan;

import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;

public class NamedPositionImpl extends LatLonPointImpl
  implements NamedPosition {

  private String uid;
  private String name;

  public NamedPositionImpl() {
    super();
  }

  public NamedPositionImpl(Latitude la, Longitude lo, String aname) {
    super(la,lo);
    setName(aname);
  }

  /** @return String - the string name representing this position */
  public String getName() {
    return name;
  }

  /** @param aName - set the string name representing this position */
  public void setName(String aName) {
    if (aName != null) aName = aName.intern();
    name = aName;
  }



  public Object clone() {
    return new NamedPositionImpl(lat, lon, name);
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }
}
