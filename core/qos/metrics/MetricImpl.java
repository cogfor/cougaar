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


/**
 * This is the standard implementation of Metric.
 */
public class MetricImpl implements Metric, java.io.Serializable
{
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public static MetricImpl UndefinedMetric = 
	new MetricImpl(0.0, 0.0, null, "undefined");

    private Object rawValue;
    private double credibility;
    private String units;
    private String provenance;
    private long timestamp;
    private long halflife;

    public MetricImpl(double raw, 
		      double credibility, 
		      String units, 
		      String provenance) 
    {
	this(new Double(raw), credibility, units, provenance);
    }

    public MetricImpl(Object raw, 
		      double credibility, 
		      String units, 
		      String provenance) 
    {
	this(raw, credibility, units, provenance, System.currentTimeMillis(),
	     0);
    }


    public MetricImpl(Object raw, 
		      double credibility, 
		      String units, 
		      String provenance,
		      long timestamp,
		      long halflife) 
    {
	this.rawValue = raw;
	this.credibility = credibility;
	this.units = units;
	this.provenance = provenance;
	this.timestamp = timestamp;
	this.halflife = halflife;
    }

    @Override
   public String toString() {
	return "<" +rawValue+ ":" +credibility+ ">";
    }
    
    public String stringValue() {
	return rawValue.toString();
    }

    public byte byteValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).byteValue());
	} else {
	    return 0;
	}
    }

    public short shortValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).shortValue());
	} else {
	    return 0;
	}
    }

    public int intValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).intValue());
	} else {
	    return 0;
	}
    }

    public long longValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).longValue());
	} else {
	    return 0;
	}
    }

    public float floatValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).floatValue());
	} else {
	    return 0;
	}
    }

    public double doubleValue() {
	if (rawValue instanceof Number) {
	    return (((Number) rawValue).doubleValue());
	} else {
	    return 0;
	}
    }

    public char charValue() {
	if (rawValue instanceof String)
	    return ((String) rawValue).charAt(0);
	else if (rawValue instanceof Character) 
	    return (((Character) rawValue).charValue());
	else
	    return '?';
    }

    public boolean booleanValue() {
	if (rawValue instanceof Boolean) 
	    return (((Boolean) rawValue).booleanValue());
	else
	    return false;
    }

    public Object getRawValue() {
	return rawValue;
    }

    public double getCredibility() {
	return credibility;
    }

    public String getUnits() {
	return units;
    }

    public String getProvenance() {
	return provenance;
    }

    public long getTimestamp() {
	return timestamp;
    }

    public long getHalflife() {
	return halflife;
    }

}
