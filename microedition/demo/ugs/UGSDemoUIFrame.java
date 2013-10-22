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
package org.cougaar.microedition.demo.ugs;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 */

public class UGSDemoUIFrame extends JFrame {
  JPanel contentPane;
  JLabel statusBar = new JLabel();
  BorderLayout borderLayout1 = new BorderLayout();
  double lat1, lon1, lat2, lon2;

  JPanel jPanel1;
  JPanel jPanel2 = new JPanel();
  JButton jButton1 = new JButton();
  JButton jButton2 = new JButton();
  JButton toggleBearingButton = new JButton();
  JButton toggleTracksButton = new JButton();
  UGSDemoUI robotDemo;

  /**Construct the frame*/
  public UGSDemoUIFrame(UGSDemoUI rd) {
    robotDemo=rd;
    initialize();
    jPanel1 = new UGSDemoPanel(lat1, lon1, lat2, lon2, this);
    //enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  /**Component initialization*/
  private void jbInit() throws Exception  {
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(borderLayout1);
    this.setSize(new Dimension(800, 600));
    this.setTitle("MicroCougaar Robot Demo UI Main Frame");
    statusBar.setText(" ");
    final String STOP_DISPLAY_LABEL="Stop Displaying Updates";
    final String START_DISPLAY_LABEL="Start Displaying Updates";
    jButton1.setText(START_DISPLAY_LABEL);
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (jButton1.getText().equalsIgnoreCase(STOP_DISPLAY_LABEL)) {
          robotDemo.stopUpdates();
          jButton1.setText(START_DISPLAY_LABEL);
        } else {
          robotDemo.startUpdates();
          jButton1.setText(STOP_DISPLAY_LABEL);
        }
      }
    });
    final String STOP_SENSOR_LABEL="Stop Sensors";
    final String START_SENSOR_LABEL="Start Sensors";
    jButton2.setText(START_SENSOR_LABEL);
    jButton2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (jButton2.getText().equalsIgnoreCase(STOP_SENSOR_LABEL)) {
          robotDemo.stopSensors();
          jButton2.setText(START_SENSOR_LABEL);
        } else {
          robotDemo.startSensors();
          jButton2.setText(STOP_SENSOR_LABEL);
        }
      }
    });
    toggleBearingButton.setText("Toggle Bearing Display ");
    toggleBearingButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        robotDemo.toggleBearingsDisplay();
      }
    });
    toggleTracksButton.setText("Toggle Tracks Display ");
    toggleTracksButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        robotDemo.toggleTracksDisplay();
      }
    });
    contentPane.setPreferredSize(new Dimension(600, 400));
    //jPanel1.setPreferredSize(new Dimension(600, 400));
    contentPane.add(statusBar, BorderLayout.NORTH);
    contentPane.add(jPanel1, BorderLayout.CENTER);
    contentPane.add(jPanel2, BorderLayout.SOUTH);
    jPanel2.add(jButton2, null);
    jPanel2.add(jButton1, null);
    jPanel2.add(toggleBearingButton, null);
    jPanel2.add(toggleTracksButton, null);
  }
  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }


  private void  initialize() {
    try {
      lat1=Double.parseDouble(System.getProperty("area.lat1"));
      lon1=Double.parseDouble(System.getProperty("area.lon1"));
      lat2=Double.parseDouble(System.getProperty("area.lat2"));
      lon2=Double.parseDouble(System.getProperty("area.lon2"));
    } catch (Exception ex) {
      lat1=40.5; lon1=-100.2; lat2=40.4; lon2=-100.1;
    }
  }


  public void repaint() {
    jPanel1.repaint();
    jPanel2.repaint();
  }

  public void update(Graphics g) { paint(g); }
  public void paint(Graphics g) {
//      System.out.println("UGSDemoUIFrame.paint -- Update on " + Thread.currentThread());
      super.paint(g);
  }

}
