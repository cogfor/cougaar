/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.qos.metrics;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.Date;
/**
 * Utility class with static fields and methods that are useful for
 * html display of metrics data.
 *
 * Colors picks a color for displaying a Metric. The both metric value
 * and metric credibility a taken into account when choosing the
 * color.  Nine color bins are defined, with each dimension having
 * three places. The metric value can has thresholds for Ignore,
 * Normal, Highlight and the credibility has thresholds for Default,
 * Config, Measured
 *
 * The current color scheme uses credibility to gray out the metric value
 * and uses the background color to indicate if the metric value has
 * exceeded the threshold
 */
public class ServletUtilities implements Constants 
{
    private static final DecimalFormat f2_1 = new DecimalFormat("0.0");
    private static final DateFormat dateFormatter = 
	DateFormat.getDateTimeInstance();

    private static final String PALE_YELLOW = "\"#ffffee\"";
    private static final String PALE_GREEN = "\"#eeffee\"";
    private static final String PALE_PINK = "\"#ffeeee\"";

    private static final String LIGHT_GRAY = "\"#cccccc\"";
    private static final String MEDIUM_GRAY = "\"#888888\"";
    private static final String BLACK = "\"#000000\"";

    private static String brightness(Metric metric) 
    {
	double credibility = metric.getCredibility();
	if (credibility <=  DEFAULT_CREDIBILITY) {
	    return LIGHT_GRAY;
	} else if (credibility <=  USER_DEFAULT_CREDIBILITY) {
	    return MEDIUM_GRAY;
	} else {
	    return BLACK;
	}
    }

    private static String bgcolor(Metric metric,
				  double uninteresting_value,
				  double threshold,
				  boolean increasing)
    {
	double value = metric.doubleValue();
	if (value == uninteresting_value) {
	    return  PALE_YELLOW;
	} else if (increasing && value >= threshold ||
		   !increasing && value <= threshold) {
	    return PALE_PINK;
	} else {
	    return PALE_GREEN;
	}
    }



    public static String credibilityDoc(double credibility) {
	if (credibility >=  ORACLE_CREDIBILITY ) 
	    return "Oracle";
	else if (credibility >= CONFIRMED_MEAS_CREDIBILITY) 
	    return "Confirmed Measurement";
	else if (credibility >= SECOND_MEAS_CREDIBILITY ) 
	    return "Seconds Measurement";
	else if (credibility >=  MINUTE_MEAS_CREDIBILITY ) 
	    return "Minutes Measurement";
	else if (credibility >= HOURLY_MEAS_CREDIBILITY ) 
	    return "Hourly Measurement";
	else if (credibility >= USER_BASE_CREDIBILITY) 
	    return  "User Baseline";
	else if (credibility >= SYS_BASE_CREDIBILITY) 
	    return "System Baseline";
	else if (credibility >= USER_DEFAULT_CREDIBILITY ) 
	    return "User Configuration File";
	else if (credibility >=SYS_DEFAULT_CREDIBILITY ) 
	    return "System Configuration File";
	else if (credibility >= DEFAULT_CREDIBILITY) 
	    return "Compile Time Default";
	else if (credibility >= NO_CREDIBILITY) 
	    return "NO SOURCE";
	else
	    return "Bogus value";
    }				  


    private static final FieldPosition FP = 
	new FieldPosition(DecimalFormat.INTEGER_FIELD);

    public static String mouseDoc(Metric data,
				  double threshold,
				  boolean increasing,
				  DecimalFormat formatter) 
    {
	StringBuffer buf = new StringBuffer();
	Date timestamp = new Date(data.getTimestamp());
	String units =  data.getUnits();
	double credibility = data.getCredibility();
	String provenance = data.getProvenance();
	long halflife = data.getHalflife();
	formatter.format(data.getRawValue(), buf, FP);
	if (units != null) {
	    buf.append(' ');
	    buf.append(units);
	}
	buf.append(" with credibility ");
	buf.append(credibility);

        buf.append(" threshold ");
	if (increasing) buf.append(">"); else buf.append("<");
	buf.append(threshold);

	if (provenance != null) {
	    buf.append(" from ");
	    buf.append(provenance);
	}

	buf.append(" at ");
	dateFormatter.format(timestamp, buf, FP);
	if (halflife > 0) {
	    buf.append(" [halflife=");
	    buf.append(halflife);
	    buf.append(']');
	}
	return buf.toString();
    }


    public static String XMLString(Metric data) {
	Metric metric = data;
	Object value = null;
	if (metric != null) value=metric.getRawValue();
	StringBuffer buf = new StringBuffer();

	buf.append("<metric>");
	if (value == null) {
	    buf.append("<type>Undefined</type>" );
	} else {
	    buf.append("<type>");
	    if (value instanceof Float || value instanceof Double)  
		buf.append("Double");
	    else if (value instanceof Number)
		buf.append("Long");
	    else if (value instanceof Boolean)  
		buf.append("Boolean");
	    else if (value instanceof String)  
		buf.append("String");
	    else if (value instanceof Character)
		buf.append("Character");
	    buf.append("</type>");
	    buf.append("<value>");
	    buf.append(value.toString());
	    buf.append("</value>");
	    buf.append("<credibility>");
	    buf.append(metric.getCredibility());
	    buf.append("</credibility>");
	    if (metric.getUnits() != null) {
		buf.append("<units>");
		buf.append(metric.getUnits());
		buf.append("</units>");
	    }
	    if (metric.getProvenance() != null) {
		buf.append("<provenance>");
		buf.append(metric.getProvenance());
		buf.append("</provenance>");
	    }
	    buf.append("<timestamp>");
	    buf.append(metric.getTimestamp());
	    buf.append("</timestamp>");
	    buf.append("<halflife>");
	    buf.append(metric.getHalflife());
	    buf.append("</halflife>");
	}
	buf.append("</metric>");
	return buf.toString();
    }

    public static void valueTable(Metric metric, 
				  double uninteresting_value, // the SPECIAL! one
				  double threshold,
				  boolean increasing, // polarity of comparison
				  DecimalFormat formatter,
				  PrintWriter out) 
    {
	if (metric == null) {
	    out.print("<td>&lt;no value&gt;</td>");
	    return;
	}
	String brightness = brightness(metric);
	String bgcolor = bgcolor(metric, uninteresting_value, threshold, increasing);
	String mouse_doc = mouseDoc(metric,threshold,increasing, formatter);
	String value_text = formatter.format(metric.doubleValue());

	out.print("<td");
	out.print(" onmouseover=\"window.status='");
	out.print(mouse_doc);
	out.print("'; return true;\"");

	out.print(" onmouseout=\"window.status=''; return true;\"");

	out.print(" bgcolor=");
	out.print(bgcolor);

	out.print(">");

	out.print("<font color=");
	out.print(brightness);
	out.print(">");

	out.print(value_text);

	// end <font>
	out.print("</font>");


	out.print("</td>");
    }

    private static final Metric m1_1 = 
	new MetricImpl(new Double(0.00), DEFAULT_CREDIBILITY,
		       "units","test");
    private static final Metric m1_2 = 
	new MetricImpl(new Double(0.00),SYS_DEFAULT_CREDIBILITY,
		       "units","test");
    private static final Metric m1_3 = 
	new MetricImpl(new Double(0.00),SECOND_MEAS_CREDIBILITY,
		       "units","test");
    private static final Metric m2_1 = 
	new MetricImpl(new Double(0.50), DEFAULT_CREDIBILITY,
		       "units","test");
    private static final Metric m2_2 = 
	new MetricImpl(new Double(0.50),SYS_DEFAULT_CREDIBILITY,
		       "units","test");
    private static final Metric m2_3 = 
	new MetricImpl(new Double(0.50),SECOND_MEAS_CREDIBILITY,
		       "units","test");
    private static final Metric m3_1 = 
	new MetricImpl(new Double(1.00), DEFAULT_CREDIBILITY,
		       "units","test");
    private static final Metric m3_2 = 
	new MetricImpl(new Double(1.00),SYS_DEFAULT_CREDIBILITY,
		       "units","test");
    private static final Metric m3_3 = 
	new MetricImpl(new Double(1.00),SECOND_MEAS_CREDIBILITY,
		       "units","test");


    public static void colorTest(PrintWriter out) {
	out.print("<table border=1>\n <tr>");

	out.print("<th>VALUE \\ CRED</th>");
	out.print("<th>Default</th>");
	out.print("<th>Config</th>");
	out.print("<th>Measured</th>");
	out.print("</tr>");
	
	// row "ignore"
	out.print("<tr><td><b>Ignore</b></td>");
	valueTable(m1_1,0.0,1.0,true,f2_1, out);
	valueTable(m1_2,0.0,1.0,true,f2_1, out);
	valueTable(m1_3,0.0,1.0,true,f2_1, out);
	out.print("</tr>");

	// row "Normal"
	out.print("<tr><td><b>Normal</b></td>");
	valueTable(m2_1,0.0,1.0,true,f2_1, out);
	valueTable(m2_2,0.0,1.0,true,f2_1, out);
	valueTable(m2_3,0.0,1.0,true,f2_1, out);
	out.print("</tr>");

	// row "highlight"
	out.print("<tr><td><b>Highlight</b></td>");
	valueTable(m3_1,0.0,1.0,true,f2_1, out);
	valueTable(m3_2,0.0,1.0,true,f2_1, out);
	valueTable(m3_3,0.0,1.0,true,f2_1, out);
	out.print("</tr>");
	
	out.print("</table>");
    }

}
