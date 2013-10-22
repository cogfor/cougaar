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
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;
import com.klg.jclass.swing.gauge.beans.*;
import javax.swing.border.*;

public class Graphs extends JApplet {
  Hashtable slg = new Hashtable();
  Hashtable slf = new Hashtable();
  boolean isStandalone = false;
  String PSP;
  GridLayout gridLayout1 = new GridLayout();
  JPanel jPanel4 = new JPanel();
  JPanel jPanel1 = new JPanel();
  JCCircularGaugeBean PDAGuage1 = new JCCircularGaugeBean();
  BorderLayout borderLayout1 = new BorderLayout();
  GridLayout gridLayout3 = new GridLayout();
  Border border1;
  TitledBorder titledBorder1;
  Border border2;
  TitledBorder titledBorder2;
  Border border3;
  TitledBorder titledBorder3;
  JLabel PDAField1 = new JLabel();
  JLabel temperatureField3 = new JLabel();
  JCCircularGaugeBean lightGuage3 = new JCCircularGaugeBean();
  BorderLayout borderLayout4 = new BorderLayout();
  GridLayout gridLayout4 = new GridLayout();
  BorderLayout borderLayout5 = new BorderLayout();
  JCCircularGaugeBean temperatureGuage3 = new JCCircularGaugeBean();
  JLabel lightField3 = new JLabel();
  JPanel jPanel6 = new JPanel();
  JPanel jPanel7 = new JPanel();
  JPanel jPanel8 = new JPanel();
  JLabel temperatureField2 = new JLabel();
  JCCircularGaugeBean lightGuage2 = new JCCircularGaugeBean();
  BorderLayout borderLayout7 = new BorderLayout();
  GridLayout gridLayout5 = new GridLayout();
  BorderLayout borderLayout8 = new BorderLayout();
  JCCircularGaugeBean temperatureGuage2 = new JCCircularGaugeBean();
  JLabel lightField2 = new JLabel();
  JPanel jPanel10 = new JPanel();
  JPanel jPanel11 = new JPanel();
  JPanel jPanel12 = new JPanel();
  JLabel temperatureField1 = new JLabel();
  JCCircularGaugeBean lightGuage1 = new JCCircularGaugeBean();
  BorderLayout borderLayout10 = new BorderLayout();
  GridLayout gridLayout6 = new GridLayout();
  BorderLayout borderLayout11 = new BorderLayout();
  JCCircularGaugeBean temperatureGuage1 = new JCCircularGaugeBean();
  JLabel lightField1 = new JLabel();
  JPanel jPanel14 = new JPanel();
  JPanel jPanel15 = new JPanel();
  JPanel jPanel16 = new JPanel();
  TitledBorder titledBorder4;
  Border border4;
  TitledBorder titledBorder5;
  Border border5;
  TitledBorder titledBorder6;
  Border border6;
  TitledBorder titledBorder7;
  Border border7;
  TitledBorder titledBorder8;
  /**Get a parameter value*/
  public String getParameter(String key, String def) {
    return isStandalone ? System.getProperty(key, def) :
      (getParameter(key) != null ? getParameter(key) : def);
  }

  /**Construct the applet*/
  public Graphs() {
  }
  /**Initialize the applet*/
  public void init() {
    try {
      PSP = this.getParameter("PSP", "");
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    try {
      jbInit();
      initializeHashtable();
      startListener();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  /**Component initialization*/
  private void jbInit() throws Exception {
    border1 = BorderFactory.createEmptyBorder();
    titledBorder1 = new TitledBorder(border1,"Temperature");
    border2 = BorderFactory.createEmptyBorder();
    titledBorder2 = new TitledBorder(border2,"Light");
    border3 = BorderFactory.createEmptyBorder();
    titledBorder3 = new TitledBorder(border3,"PDA");
    titledBorder4 = new TitledBorder("");
    border4 = BorderFactory.createLineBorder(Color.black,2);
    titledBorder5 = new TitledBorder(border4,"TINI One");
    border5 = BorderFactory.createLineBorder(Color.black,2);
    titledBorder6 = new TitledBorder(border5,"PDA Emulator");
    border6 = BorderFactory.createLineBorder(Color.black,2);
    titledBorder7 = new TitledBorder(border6,"TINI Two");
    border7 = BorderFactory.createLineBorder(Color.black,2);
    titledBorder8 = new TitledBorder(border7,"TINI Three");
    this.getContentPane().setBackground(Color.white);
    this.setSize(new Dimension(586, 710));
    this.getContentPane().setLayout(gridLayout1);
    gridLayout1.setRows(4);
    gridLayout1.setColumns(3);
    jPanel1.setLayout(borderLayout1);
    jPanel1.setBorder(titledBorder3);
    jPanel1.setToolTipText("");
    PDAGuage1.setAutoTickGeneration(false);
    PDAGuage1.setDrawTickLabels(false);
    PDAGuage1.setPaintCompleteBackground(true);
    PDAGuage1.setScaleColor(SystemColor.info);
    PDAGuage1.setScaleMax(20.0);
    PDAGuage1.setScaleMin(-20.0);
    PDAGuage1.setTickIncrement(4.0);
    PDAGuage1.setTickStartValue(-20.0);
    PDAGuage1.setTickStopValue(20.0);
    PDAGuage1.setTickStyle(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TICK_REVERSE_TRIANGLE);
    PDAGuage1.setType(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TYPE_TOP_HALF_CIRCLE);
    PDAGuage1.setDirection(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.DIRECTION_CLOCKWISE);
    jPanel4.setLayout(gridLayout3);
    titledBorder1.setTitleJustification(2);
    titledBorder2.setTitleJustification(2);
    titledBorder3.setTitleJustification(2);
    jPanel4.setBorder(titledBorder6);
    PDAField1.setHorizontalAlignment(SwingConstants.CENTER);
    PDAField1.setText(" ");
    temperatureField3.setHorizontalAlignment(SwingConstants.CENTER);
    temperatureField3.setText(" ");
    lightGuage3.setDirection(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.DIRECTION_CLOCKWISE);
    lightGuage3.setType(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TYPE_TOP_HALF_CIRCLE);
    lightGuage3.setTickStyle(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TICK_REVERSE_TRIANGLE);
    lightGuage3.setScaleColor(SystemColor.info);
    lightGuage3.setScaleMax(5.0);
    lightGuage3.setTickIncrement(0.5);
    lightGuage3.setTickStopValue(5.0);
    lightGuage3.setPaintCompleteBackground(true);
    lightGuage3.setAutoTickGeneration(false);
    lightGuage3.setDrawTickLabels(false);
    temperatureGuage3.setDirection(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.DIRECTION_CLOCKWISE);
    temperatureGuage3.setType(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TYPE_TOP_HALF_CIRCLE);
    temperatureGuage3.setTickStyle(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TICK_REVERSE_TRIANGLE);
    temperatureGuage3.setScaleColor(SystemColor.info);
    temperatureGuage3.setScaleMax(85.0);
    temperatureGuage3.setScaleMin(15.0);
    temperatureGuage3.setTickIncrement(7.0);
    temperatureGuage3.setTickStartValue(15.0);
    temperatureGuage3.setTickStopValue(85.0);
    temperatureGuage3.setPaintCompleteBackground(true);
    temperatureGuage3.setAutoTickGeneration(false);
    temperatureGuage3.setDrawTickLabels(false);
    lightField3.setHorizontalAlignment(SwingConstants.CENTER);
    lightField3.setText(" ");
    jPanel6.setLayout(gridLayout4);
    jPanel6.setBorder(titledBorder8);
    jPanel7.setLayout(borderLayout4);
    jPanel7.setBorder(titledBorder1);
    jPanel8.setLayout(borderLayout5);
    jPanel8.setBorder(titledBorder2);
    jPanel8.setPreferredSize(new Dimension(75, 75));
    temperatureField2.setHorizontalAlignment(SwingConstants.CENTER);
    temperatureField2.setText(" ");
    lightGuage2.setDirection(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.DIRECTION_CLOCKWISE);
    lightGuage2.setType(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TYPE_TOP_HALF_CIRCLE);
    lightGuage2.setTickStyle(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TICK_REVERSE_TRIANGLE);
    lightGuage2.setScaleColor(SystemColor.info);
    lightGuage2.setScaleMax(5.0);
    lightGuage2.setTickIncrement(0.5);
    lightGuage2.setTickStopValue(5.0);
    lightGuage2.setPaintCompleteBackground(true);
    lightGuage2.setAutoTickGeneration(false);
    lightGuage2.setDrawTickLabels(false);
    temperatureGuage2.setDirection(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.DIRECTION_CLOCKWISE);
    temperatureGuage2.setType(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TYPE_TOP_HALF_CIRCLE);
    temperatureGuage2.setTickStyle(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TICK_REVERSE_TRIANGLE);
    temperatureGuage2.setScaleColor(SystemColor.info);
    temperatureGuage2.setScaleMax(85.0);
    temperatureGuage2.setScaleMin(15.0);
    temperatureGuage2.setTickIncrement(7.0);
    temperatureGuage2.setTickStartValue(15.0);
    temperatureGuage2.setTickStopValue(85.0);
    temperatureGuage2.setPaintCompleteBackground(true);
    temperatureGuage2.setAutoTickGeneration(false);
    temperatureGuage2.setDrawTickLabels(false);
    lightField2.setHorizontalAlignment(SwingConstants.CENTER);
    lightField2.setText(" ");
    jPanel10.setLayout(gridLayout5);
    jPanel10.setBorder(titledBorder7);
    jPanel11.setLayout(borderLayout7);
    jPanel11.setBorder(titledBorder1);
    jPanel12.setLayout(borderLayout8);
    jPanel12.setBorder(titledBorder2);
    jPanel12.setPreferredSize(new Dimension(75, 75));
    temperatureField1.setHorizontalAlignment(SwingConstants.CENTER);
    temperatureField1.setText(" ");
    lightGuage1.setDirection(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.DIRECTION_CLOCKWISE);
    lightGuage1.setType(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TYPE_TOP_HALF_CIRCLE);
    lightGuage1.setTickStyle(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TICK_REVERSE_TRIANGLE);
    lightGuage1.setScaleColor(SystemColor.info);
    lightGuage1.setScaleMax(5.0);
    lightGuage1.setTickIncrement(0.5);
    lightGuage1.setTickStopValue(5.0);
    lightGuage1.setPaintCompleteBackground(true);
    lightGuage1.setAutoTickGeneration(false);
    lightGuage1.setDrawTickLabels(false);
    temperatureGuage1.setDirection(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.DIRECTION_CLOCKWISE);
    temperatureGuage1.setType(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TYPE_TOP_HALF_CIRCLE);
    temperatureGuage1.setTickStyle(com.klg.jclass.swing.gauge.beans.JCCircularGaugeBean.TICK_REVERSE_TRIANGLE);
    temperatureGuage1.setScaleColor(SystemColor.info);
    temperatureGuage1.setScaleMax(85.0);
    temperatureGuage1.setScaleMin(15.0);
    temperatureGuage1.setTickIncrement(7.0);
    temperatureGuage1.setTickStartValue(15.0);
    temperatureGuage1.setTickStopValue(85.0);
    temperatureGuage1.setPaintCompleteBackground(true);
    temperatureGuage1.setAutoTickGeneration(false);
    temperatureGuage1.setDrawTickLabels(false);
    lightField1.setHorizontalAlignment(SwingConstants.CENTER);
    lightField1.setText(" ");
    jPanel14.setLayout(gridLayout6);
    jPanel14.setBorder(titledBorder5);
    jPanel15.setLayout(borderLayout10);
    jPanel15.setBorder(titledBorder1);
    jPanel16.setLayout(borderLayout11);
    jPanel16.setBorder(titledBorder2);
    jPanel16.setPreferredSize(new Dimension(75, 75));
    this.getContentPane().add(jPanel4, null);
    jPanel4.add(jPanel1, null);
    jPanel1.add(PDAGuage1, BorderLayout.CENTER);
    jPanel1.add(PDAField1, BorderLayout.SOUTH);
    this.getContentPane().add(jPanel14, null);
    jPanel14.add(jPanel15, null);
    jPanel15.add(temperatureField1, BorderLayout.SOUTH);
    jPanel15.add(temperatureGuage1, BorderLayout.CENTER);
    jPanel14.add(jPanel16, null);
    jPanel16.add(lightField1, BorderLayout.SOUTH);
    jPanel16.add(lightGuage1, BorderLayout.CENTER);
    this.getContentPane().add(jPanel10, null);
    jPanel10.add(jPanel11, null);
    jPanel11.add(temperatureGuage2, BorderLayout.CENTER);
    jPanel11.add(temperatureField2, BorderLayout.SOUTH);
    jPanel10.add(jPanel12, null);
    jPanel12.add(lightField2, BorderLayout.SOUTH);
    jPanel12.add(lightGuage2, BorderLayout.CENTER);
    this.getContentPane().add(jPanel6, null);
    jPanel6.add(jPanel7, null);
    jPanel7.add(temperatureGuage3, BorderLayout.CENTER);
    jPanel7.add(temperatureField3, BorderLayout.SOUTH);
    jPanel6.add(jPanel8, null);
    jPanel8.add(lightField3, BorderLayout.SOUTH);
    jPanel8.add(lightGuage3, BorderLayout.CENTER);
  }

  public void initializeHashtable() {
    slg.put("PDA1", PDAGuage1);
    slf.put("PDA1", PDAField1);
    slg.put("Temperature1", temperatureGuage1);
    slf.put("Temperature1", temperatureField1);
    slg.put("Temperature2", temperatureGuage2);
    slf.put("Temperature2", temperatureField2);
    slg.put("Temperature3", temperatureGuage3);
    slf.put("Temperature3", temperatureField3);
    slg.put("Light1", lightGuage1);
    slf.put("Light1", lightField1);
    slg.put("Light2", lightGuage2);
    slf.put("Light2", lightField2);
    slg.put("Light3", lightGuage3);
    slf.put("Light3", lightField3);

    titledBorder5.setTitleFont(new java.awt.Font("Dialog", 1, 16));
    titledBorder6.setTitleFont(new java.awt.Font("Dialog", 1, 16));
    titledBorder7.setTitleFont(new java.awt.Font("Dialog", 1, 16));
    titledBorder8.setTitleFont(new java.awt.Font("Dialog", 1, 16));

  }
  /**Start the applet*/
  public void start() {
  }
  /**Stop the applet*/
  public void stop() {
  }
  /**Destroy the applet*/
  public void destroy() {
  }
  /**Get Applet information*/
  public String getAppletInfo() {
    return "Applet Information";
  }
  /**Get parameter info*/
  public String[][] getParameterInfo() {
    String[][] pinfo =
      {
      {"PSP", "String", ""},
      };
    return pinfo;
  }
  /**Main method*/
  public static void main(String[] args) {
    Graphs applet = new Graphs();
    applet.isStandalone = true;
    JFrame frame = new JFrame();
    //EXIT_ON_CLOSE == 3
    frame.setDefaultCloseOperation(3);
    frame.setTitle("Applet Frame");
    frame.getContentPane().add(applet, BorderLayout.CENTER);
    applet.init();
    applet.start();
    frame.setSize(400,320);
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation((d.width - frame.getSize().width) / 2, (d.height - frame.getSize().height) / 2);
    frame.setVisible(true);
  }

  //static initializer for setting look & feel
  static {
    try {
      //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    }
    catch(Exception e) {
    }
  }

  private void startListener() {
    Thread t = new Thread(new Listener(), "PSP_LISTENER");
    t.start();
  }

  private class Listener implements Runnable {
    NumberFormat fmt = new DecimalFormat("##0.00");
    public void run() {
      // Open the PSP input stream
      try {
        URL pspURL = new URL(PSP);

        URLConnection conn = pspURL.openConnection();
        InputStream in = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        while (true) {
            String line = reader.readLine();
            parseLine(line);
        }
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    private void parseLine(String line) {
      StringTokenizer st = new StringTokenizer(line, ":");
      String src = st.nextToken();
      String label = st.nextToken();
      String value = st.nextToken();
      if ((src == null) || (label == null) || (value == null)) return; // format error

      String dest = null;
      for (int i=0; i<src.length(); i++) {
        if (Character.isDigit(src.charAt(i)))
          dest = src.substring(i);
      }
      if (dest == null) return ; // format error
      String key = label + dest;
      if (!slg.containsKey(key)) return;
      if (!slf.containsKey(key)) return;

      double val = Double.parseDouble(value);
      value = fmt.format(val);
      ((JLabel)slf.get(key)).setText(value);
      ((JCCircularGaugeBean)slg.get(key)).setNeedleValue(val);
    }
  }

}
