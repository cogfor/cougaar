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
package org.cougaar.microedition.demo;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.awt.event.*;
import java.io.*;

/**
 */

public class RobotImageDisplay extends JFrame {

  static int xScreenLoc, yScreenLoc;
  static double screenWidth, screenHeight;
  Image image;
  ImageObserver observer;
  String robotId;
  String fullUrl;
  public RobotImageDisplay(String robotId)
  {
    this.setTitle("Image from "+robotId);
    this.robotId=robotId;
    setSize(660,480);
    try {

      String ipproperty = robotId+"_IPAddress";
      String robotipaddress = System.getProperty(ipproperty);
      InetAddress addr = InetAddress.getByName(robotipaddress);

      String importproperty = robotId+"_ImageFilePort";
      Integer intport = Integer.getInteger(importproperty);
      int port = intport.intValue();

      System.out.println("RobotImageDisplay: open socket to " +robotipaddress+":"+port);

      Socket imsocket = new Socket(addr, port);

      DataInputStream datain = new DataInputStream(imsocket.getInputStream());
      int nbytes = datain.readInt();
      if(nbytes > 0)
      {
        byte [] imagedata = new byte[nbytes];
	int nread = 0;
	while(nread < nbytes)
	{
	  int nval = datain.read(imagedata, nread, nbytes - nread);
	  if (nval < 0) break;

	  nread += nval;
	  System.out.println("RobotImageDisplay: nread "+nread+" of "+nbytes);
	}

	if(nread != nbytes)
	{
	  System.out.println("RobotImageDisplay: nread != nbytes "+nread+"!="+nbytes);
	}
	else
	{
          image=Toolkit.getDefaultToolkit().createImage(imagedata);
          addNotify();
          repaint();
	}
      }
    }
    catch (Exception ex)
    {
        System.err.println("Error: obtaining image from robot (ID: ["
          +robotId+"]) indicated a failure.");
    }

    try {
      screenWidth=Toolkit.getDefaultToolkit().getScreenSize().getWidth();
      screenHeight=Toolkit.getDefaultToolkit().getScreenSize().getHeight();
      xScreenLoc=Integer.parseInt(System.getProperty(robotId+".image.x"));
      yScreenLoc=Integer.parseInt(System.getProperty(robotId+".image.y"));
    } catch (Exception ex) {
    }

    if (xScreenLoc > screenWidth) xScreenLoc=0;
    if (yScreenLoc > screenHeight) yScreenLoc=0;

    setLocation(xScreenLoc, yScreenLoc);
    xScreenLoc+=50;
    yScreenLoc+=50;
    show();
  }

  JDialog jd;
  boolean displayedError=false;
  private void doCancelAction() {
    jd.dispose();
    jd=null;
    hide();
  }
  public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
    //System.out.println("imageUpdate "+img+" flags "+flags+" x y w h "+x+" "+y+" "+w+" "+h);
    repaint();
    if ((flags&ImageObserver.ERROR)!=0) {
      System.err.println("Error loading image for robot ["+robotId+"] from URL ["+fullUrl+"]");
      if (!displayedError) {
        displayedError=true;
        jd=new JDialog();
        jd.getContentPane().setLayout(new BorderLayout());
        jd.setTitle("Error Loading image");
        JPanel labelPanel=new JPanel();
        JPanel buttonPanel=new JPanel();
        JLabel label=new JLabel("Error loading image for "+robotId+"\n");
        labelPanel.add(label);
        jd.getContentPane().add(labelPanel);
        JButton jb1=new JButton("Cancel");
        buttonPanel.add(jb1);
        jd.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        jb1.addActionListener(new java.awt.event.ActionListener() {
          public void actionPerformed(ActionEvent e) {
            doCancelAction();
          }
        });
        Dimension size=label.getSize();
        Dimension jbsize=jb1.getSize();
        jd.setSize(size.width+jbsize.width, size.height+jbsize.height);
        System.out.println("sizes: "+size+" "+jbsize+" "+jd.getContentPane().getSize()+" "+(size.width+jbsize.width)+", "+(size.height+jbsize.height));
        jd.setSize(400, 100);
        jd.show();

        hide();
      }
    }
    if ((flags&ImageObserver.ALLBITS)==ImageObserver.ALLBITS) {
      return false;
    } else {
      return true;
    }
  }
  public void update(Graphics g) {
    paint( g);
  }
  public void paint(Graphics g) {
    if (image!=null) {
      g.drawImage(image, 10, 10, this);
    }
  }
}