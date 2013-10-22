/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;


/** 
 * A control input for a component (for example, a plugin). The {@link
 * AdaptivityEngine} sets OperatingMode values according to the plays
 * it selects. OperatingModes are generally created by the component
 * whose operation can be controlled. OperatingModes should be
 * published to the blackboard so they are accessible to the {@link
 * OperatingModeServiceProvider}. The component should also subscribe
 * to the OperatingMode if it needs to know when changes occur.
 * Alternatively, the value can be examined as needed.
 **/
public interface OperatingMode extends java.io.Serializable {
    /**
     * Gets the (distinct) name of this OperatingMode. The names of
     * all the OperatingMode on a particular blackboard must be
     * distinct. This can be achieved by establishing naming
     * conventions such has including the class name of the plugin or
     * other component in the name. Where the same component class may
     * be instantiated multiple times, the multiple instances may
     * already have some sor of name that can be used in addition to
     * the class name. It is the responsibility of the component
     * designer to insure that OperatingMode names are not ambiguous.
     * @return the name of this OperatingMode
     **/
    String getName();

    /**
     * Get the list of allowed value ranges for this OperatingMode.
     * Attempts to set the value outside these ranges will fail.
     * @return a list of allowed value ranges
     **/
    OMCRangeList getAllowedValues();

    /**
     * Get the current value of this OperatingMode. This value must
     * never be outside the allowed value ranges.
     * @return the current value of this OperatingMode
     **/
    Comparable getValue();

    /**
     * Set a new value for this OperatingMode. Used by the
     * AdaptivityEngine to alter the mode of the component having this
     * OperatingMode.
     * @param newValue the new value for this OperatingMode. Must be
     * in the allowed value ranges.
     * @exception IllegalArgumentException if the newValue is not
     * allowed.
     **/
    void setValue(Comparable newValue);
}
