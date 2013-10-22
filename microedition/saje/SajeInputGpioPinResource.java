/*
 * SajeInputGpioPinResource.java
 *
 * Created on August 27, 2002, 8:52 AM
 */

package org.cougaar.microedition.saje;

import org.cougaar.microedition.ldm.Distributor;
import org.cougaar.microedition.shared.Constants;

import com.ajile.drivers.gpio.GpioPin;
import com.ajile.drivers.gpio.GpioField;
import com.ajile.events.AssertEventListener;

/**
 *
 * @author  wwright
 * @version 
 */
public class SajeInputGpioPinResource extends org.cougaar.microedition.asset.ControllerResource {

    private GpioPin inPin;

    private boolean state; 
    private boolean isundercontrol;
    /** Creates new SajeInputGpioPinResource */
    public SajeInputGpioPinResource() {
        //System.out.println("Creating GPIO Resource");
	org.cougaar.microedition.node.Node.setRebooter(new Restart());
    }
  
    public void setParameters(java.util.Hashtable t)
    {

    super.setParameters(t);
    
    if (debugging) System.out.println("setParameters GPIO Resource");
    String bank = (String)t.get("pinBank");
    String number = (String)t.get("pinNumber");
    
    int pinCode = getPinCode(bank, number);
    
    setName("SajeInputGpioResource-GPIO"+bank+"_BIT"+number);
    inPin = new GpioPin(pinCode);
    inPin.setPinReportPolicy(GpioPin.REPORT_ANY_EDGE);
    inPin.setOutputPin(false);
    state = inPin.getPinState();
    if (debugging) System.out.println("setParameters name set to "+getName()+ " initial state is "+state);
    
    setScalingFactor(1);
    
    // implement an anonymous class, then register a listener
    inPin.addStateListener(
      new AssertEventListener() {
        public void assertEvent(boolean active) {
	  if (debugging) System.out.println("AssertEvent: active = "+active);
          updateState(active);
          }
      });
  }

  // what a waste.....
  private int [][] pinCodes = {
      {GpioPin.GPIOA_BIT0, GpioPin.GPIOA_BIT1, GpioPin.GPIOA_BIT2, GpioPin.GPIOA_BIT3, 
       GpioPin.GPIOA_BIT4, GpioPin.GPIOA_BIT5, GpioPin.GPIOA_BIT6, GpioPin.GPIOA_BIT7}, 
      {GpioPin.GPIOB_BIT0, GpioPin.GPIOB_BIT1, GpioPin.GPIOB_BIT2, GpioPin.GPIOB_BIT3, 
       GpioPin.GPIOB_BIT4, GpioPin.GPIOB_BIT5, GpioPin.GPIOB_BIT6, GpioPin.GPIOB_BIT7},
      {GpioPin.GPIOC_BIT0, GpioPin.GPIOC_BIT1, GpioPin.GPIOC_BIT2, GpioPin.GPIOC_BIT3, 
       GpioPin.GPIOC_BIT4, GpioPin.GPIOC_BIT5, GpioPin.GPIOC_BIT6, GpioPin.GPIOC_BIT7},
      {GpioPin.GPIOD_BIT0, GpioPin.GPIOD_BIT1, GpioPin.GPIOD_BIT2, GpioPin.GPIOD_BIT3, 
       GpioPin.GPIOD_BIT4, GpioPin.GPIOD_BIT5, GpioPin.GPIOD_BIT6, GpioPin.GPIOD_BIT7},
      {GpioPin.GPIOE_BIT0, GpioPin.GPIOE_BIT1, GpioPin.GPIOE_BIT2, GpioPin.GPIOE_BIT3, 
       GpioPin.GPIOE_BIT4, GpioPin.GPIOE_BIT5, GpioPin.GPIOE_BIT6, GpioPin.GPIOE_BIT7}
  }; 

  private int getPinCode(String bank, String number) throws IllegalArgumentException {
      int ret = 0;
      if (debugging) System.out.println("getPinCode: "+bank+number);
      if (debugging) System.out.println("getPinCode: ["+(bank.charAt(0)-'A')+"]"+"["+(number.charAt(0)-'0')+"]");
      if ((bank == null) || (bank.length() != 1) || (bank.charAt(0) < 'A') || (bank.charAt(0) > 'E'))
          throw new IllegalArgumentException ("Invalid GPIO pinBank [A-E valid]: "+bank);
      if ((number == null) || (number.length() != 1) || (number.charAt(0) < '0') || (number.charAt(0) > '7'))
          throw new IllegalArgumentException ("Invalid GPIO pinNumber [0-7 valid]: "+number);
      
      ret = pinCodes[bank.charAt(0)-'A'][number.charAt(0)-'0'];
      return ret;
  }
  
  private void updateState(boolean newState) {
      if (newState != state) {
          state = newState;
          if (debugging) System.out.println("GPIO Resource publishChange: "+getName()+":"+state);
          getDistributor().openTransaction(Thread.currentThread());
          getDistributor().publishChange(this);
          getDistributor().closeTransaction(Thread.currentThread());
          if (debugging) System.out.println("GPIO Resource publishChange DONE! ");
      }
  }

    public boolean conditionChanged() {return false;} // changes show up as publishChange()
    
  public void getValueAspects(int [] aspects)
  {
    aspects[0] = Constants.Aspects.DETECTION;
  }

  public int getNumberAspects()
  {
    return 1;
  }
    
    public void getValues(long[] values) {
        values[0] = (state ? 1 : 0);
    }
    
    public void modifyControl(String controlparameter, String controlparametervalue) {
    }
    
    public void setChan(int c) { }
    
    public void setUnits(String u) { }
    
  public void startControl()
  {
    isundercontrol = true;
  }

  public void stopControl()
  {
    isundercontrol = false;
  }

  public boolean isUnderControl()
  {
    return isundercontrol;
  }
    
}
