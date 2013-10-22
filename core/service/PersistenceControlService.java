/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.service;

import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.component.Service;

/**
 * This service can be used to view and modify persistence
 * settings. 
 */
public interface PersistenceControlService extends Service {
    /**
     * Gets the names of all controls (operating modes). These are
     * controls over persistence as a whole, not media-specific.
     * @return an array of all control names
     */
    String[] getControlNames();

    /**
     * Gets the (allowed) values of a given control. Values used to
     * set a control must be in the ranges specified by the return.
     * @return a list of ranges of values that are allowed for the
     * named control.
     * @param controlName the name of a persistence-wide control
     */
    OMCRangeList getControlValues(String controlName);

    /**
     * Sets the value for the named control. The value must be in the
     * list or ranges returned by {@link #getControlValues}.
     * @param controlName the name of the control to set.
     * @param newValue the new value for the control. Must be in the
     * allowed ranges.
     */
    void setControlValue(String controlName, Comparable newValue);

    /**
     * Gets the names of the installed media plugins.
     * @return an array of the names of the installed media plugins.
     */
    String[] getMediaNames();

    /**
     * Gets the names of the controls for the named media (plugin).
     * @return an array of all control names for the given media.
     * @param mediaName the name of the media.
     */
    String[] getMediaControlNames(String mediaName);

    /**
     * Gets the allowed values of then named control for the named
     * media plugin. Values used to set a control must be in the
     * ranges specified by the return.
     * @return a list of ranges of values that are allowed for the
     * named control.
     * @param mediaName the name of the media having the control
     * @param controlName the name of a media-specific control
     * @return a list of the allowed value ranges.
     */
    OMCRangeList getMediaControlValues(String mediaName, String controlName);

    /**
     * Sets the value for the named control for the named media
     * plugin. The value must be in the list or ranges returned by
     * {@link #getMediaControlValues}.
     * @param mediaName the name of the media having the control
     * @param controlName the name of the control to set.
     * @param newValue the new value for the control. Must be in the
     * allowed ranges.
     */
    void setMediaControlValue(String mediaName, String controlName, Comparable newValue);
}
