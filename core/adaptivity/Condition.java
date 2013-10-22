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
 * A Condition holds a value that the {@link AdaptivityEngine} or
 * {@link PolicyManager PolicyManager} uses for selecting
 * {@link Play Play}s or
 * {@link OperatingModePolicy OperatingModePolicy}s.
 * Conditions include the measurements made by sensors, external
 * conditions distributed throughout portions of the society and
 * inputs from higher level adaptivity engines. This interface does
 * not define a setValue method because establishing the current value
 * is the responsibility of the owner of the Condition.
 **/
public interface Condition extends java.io.Serializable {

    /**
     * Gets the (distinct) name of this Condition. The names of all
     * the Condition on a particular blackboard must be distinct. This
     * can be achieved by establishing naming conventions such has
     * including the class name of the plugin or other component that
     * created the Condition in the name. Where the same component
     * class may be instantiated multiple times, the multiple
     * instances may already have some sor of name that can be used in
     * addition to the class name. It is the responsibility of the
     * component designer to insure that Condition names are not
     * ambiguous.
     * @return the name of this Condition
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
}
