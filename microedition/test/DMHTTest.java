package org.cougaar.microedition.test;

import java.util.*;
import java.io.*;
//import org.cougaar.microedition.dmht.*;

import javax.swing.*;
import java.awt.*;

public class DMHTTest {
  boolean packFrame = false;

  /**Construct the application*/
  public DMHTTest() {
    TestFrame frame = new TestFrame();
    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);

    JTextArea theText = new JTextArea();
    theText.setText("Begin AssignmentAlgorithm" +'\n');
    frame.contentPane.add(theText);
    frame.contentPane.setVisible(true);
    frame.contentPane.setAutoscrolls(true);
    frame.contentPane.updateUI();

    //Example from Blackman and Popoli, Chapter 6, p.347
    //Note: the negative of the scores is taken so that the Kbest solutions maximize the total gain (rather than minimize the total cost)
    //Note; the constraints represent the following: no restriction (-1), unallowed (1), required (0)
    double[][] Scores = { {-10.0, -5.0, -8.0, -9.0}, {-7.0, -100, -20.0, -100}, {-100, -21.0, -100, -100}, {-100, -15.0, -17.0, -100}, {-100, -100, -16.0, -22.0} };
    int[][] Constraints = { {-1, -1, -1, -1}, {-1, 1, -1, 1}, {1, -1, 1, 1}, {1, -1, -1, 1}, {1, 1, -1, -1} };
    int N = Scores[0].length;
    int M = Scores.length;
    int Kbest = 4;

    AssignmentAlgorithms AAlg = new AssignmentAlgorithms();
    AssignmentAlgorithms.AssignmentSolution[] KbestList = AAlg.KbestSolutions(Scores, Constraints, Kbest);
    System.out.println("return: DMHTTest");
    for(int i=0; i<Kbest; i++)
    {
      System.out.println("Assignment solution: " +i);
      theText.append("Assignment solution: " +i +'\n');
       for(int j=0; j<KbestList[i].assignment.length; j++)
	{
	System.out.println("item number: " +j);
	System.out.println("bidder: " +KbestList[i].assignment[j]);
	String assignmentS = Integer.toString(KbestList[i].assignment[j]);
	theText.append("item number: " +j +'\n');
	theText.append("bidder: " +assignmentS +'\n');
	}
      System.out.println("Assignment total value: " +KbestList[i].totalvalue);
      theText.append("Assignment solution: " +KbestList[i].totalvalue +'\n');
    }
    theText.append("End AssignmentAlgorithm");
    frame.contentPane.updateUI();
   }

  /**Main method*/
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    new DMHTTest();

  }
}
