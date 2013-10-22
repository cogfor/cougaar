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
 * Metrics are the information abstraction for the values manipulated
 * by the MetricsService.  This includes the value itself, which can
 * be of various types, and meta-data about the value: how credible it
 * is, where it come from, when it was collected, etc.
 */
public interface Metric
{
    /**
     * Returns the value as a String.  The caller is assumed to know
     * that the value is, in fact, a String.
    */
    String stringValue();

    /**
     * Returns the value as a byte.  The caller is assumed to know
     * that the value is, in fact, a number that can coerced to a
     * byte.
    */
    byte byteValue();

    /**
     * Returns the value as a short.  The caller is assumed to know
     * that the value is, in fact, a number that can be coerced to a
     * short.
    */
    short shortValue();

    /**
     * Returns the value as an int.  The caller is assumed to know
     * that the value is, in fact, a number that can be coerced to an
     * int.
    */
    int intValue();

    /**
     * Returns the value as a long.  The caller is assumed to know
     * that the value is, in fact, a number that can be coerced to a
     * long.
    */
    long longValue();

    /**
     * Returns the value as a float.  The caller is assumed to know
     * that the value is, in fact, a number that can be coerced to a
     * float.
    */
    float floatValue();

    /**
     * Returns the value as a double.  The caller is assumed to know
     * that the value is, in fact, a number that can be coerced to a
     * double.
    */
    double doubleValue();

    /**
     * Returns the value as a char.  The caller is assumed to know
     * that the value is, in fact, a char.
    */
    char charValue();

    /**
     * Returns the value as a boolean.  The caller is assumed to know
     * that the value is, in fact, a boolean.
    */
    boolean booleanValue();

    /**
     * Returns the raw value.  Use this only if you don't know what
     * type the value should be.
     */
    Object getRawValue();
    
    /**
     * Returns the credibility of the value, as double in the range
     * 0.0 (no knowledge) to 1.0 (perfect knowledge).
    */
    double getCredibility();

    /**
     * Returns the units of the value.  This is not currently used in
     * Cougaar
    */
    String getUnits();

    /**
     * Returns the source of the value, as a string with no semantics.
    */
    String getProvenance();

    /**
     * Returns the time the value was collected or generated.
    */
    long getTimestamp();

    /**
     * Returns a numeric measure of how long the value is good for.
    */
    long getHalflife();
}
