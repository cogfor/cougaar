package org.cougaar.planning.ldm.measure;

/**
 * Shorthand for a Measure/Duration, e.g. Volume/Duration (FlowRate).
 *
 * @author gvidaver@bbn.com
 *         Date: May 17, 2007
 *         Time: 3:27:19 PM
 *         To change this template use File | Settings | File Templates.
 */
public class GenericRate<N extends Measure> extends GenericDerivative<N, Duration> implements Rate {
  public GenericRate(N numerator, Duration denom) {
    super(numerator, denom);
  }

  public GenericRate() {
    super();
  }

  protected GenericDerivative<N, Duration> newInstance(N numerator, Duration denominator) {
    return new GenericRate<N>(numerator, denominator);
  }

  public static void main(String [] args) {
    test1();
  }

  private static void test1() {
    Volume tenGallons = Volume.newGallons(10);
    Duration oneHour = Duration.newHours(1);
    GenericRate<Volume> gallonsPerHour =
      new GenericRate<Volume>(tenGallons, oneHour);

    System.out.println("generic rate is " + gallonsPerHour);

    Measure volume = gallonsPerHour.multiply(oneHour);
    System.out.println("volume " + volume.getValue(Volume.GALLONS) + " should be " + 10 + " gallons");
    volume = gallonsPerHour.multiply(Duration.newHours(10));
    System.out.println("volume " + volume.getValue(Volume.GALLONS)  + " should be " + 100 + " gallons");

    GenericRate<Volume> twentyGallonsPerHour = (GenericRate<Volume>)gallonsPerHour.add(gallonsPerHour);
    int gallonsPerHourUnit = 0;
    for (int i = 0; i < twentyGallonsPerHour.getMaxUnit(); i++) {
      if(twentyGallonsPerHour.getUnitName(i).equals("gallons/hours")) {
        gallonsPerHourUnit = i;
        System.out.println("index is " + i + " to get " + twentyGallonsPerHour.getUnitName(i));
      }
    }
    System.out.println("Rate : " + gallonsPerHour.getValue(gallonsPerHourUnit) + " + " +
      gallonsPerHour.getValue(gallonsPerHourUnit) + " = " + twentyGallonsPerHour.getValue(gallonsPerHourUnit));

    GenericRate<Volume> zeroGallonsPerHour = (GenericRate<Volume>)gallonsPerHour.subtract(gallonsPerHour);
    System.out.println("Rate : " + gallonsPerHour.getValue(gallonsPerHourUnit) + " - " +
      gallonsPerHour.getValue(gallonsPerHourUnit) + " = " + zeroGallonsPerHour.getValue(gallonsPerHourUnit));

    Volume hundredGallons = (Volume) tenGallons.scale(10);
    System.out.println("Volume : " + tenGallons.getNativeValue() + " or " + tenGallons.getGallons() +
      " * 10 " + " = " + hundredGallons.getNativeValue() + " or " + hundredGallons.getGallons());

    GenericRate<Volume> hundredGallonsPerHour = (GenericRate<Volume>)gallonsPerHour.scale(10);

    System.out.println("" + gallonsPerHour.getValue(gallonsPerHourUnit) + " * 10" + " = " +
      hundredGallonsPerHour.getValue(gallonsPerHourUnit));

    double tenGallonsPerHour = hundredGallonsPerHour.divideRate(twentyGallonsPerHour);

    System.out.println("" + hundredGallonsPerHour.getValue(gallonsPerHourUnit) +
      " / " + twentyGallonsPerHour.getValue(gallonsPerHourUnit) + " = " +
      tenGallonsPerHour);

    String commonUnit = gallonsPerHour.getUnitName(gallonsPerHour.getCommonUnit());

    System.out.println("" + gallonsPerHour + " common unit is " + commonUnit);

    gallonsPerHour = new GenericRate<Volume>(Volume.newGallons(1), oneHour);
    System.out.println("max is " + gallonsPerHour.getMaxUnit());
    for (int i = 0; i < gallonsPerHour.getMaxUnit() ; i++) {
      System.out.println(i + " " + gallonsPerHour.getUnitName(i) + " : " + gallonsPerHour.getValue(i) +
        " floor " + gallonsPerHour.floor(i).getValue(i));
    }
  }
}
