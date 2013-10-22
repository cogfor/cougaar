/*
 * <copyright>
 *  
 *  Copyright 1997-2006 BBNT Solutions, LLC
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.net.URL;

import org.cougaar.core.component.Service;

/**
 * This service provides access to Applet methods, and is only available if
 * the node was launched from within a browser {@link java.applet.Applet}.
 * <p>
 * There are dozens of Applet methods, including all methods defined in
 * AWT "Component".  For now we support the basic methods and can add
 * more in the future as required.
 *
 * @see java.applet.Applet
 */
public interface AppletService extends Service {

  boolean isActive();
  URL getDocumentBase();
  URL getCodeBase();
  String getParameter(String name);
  void showStatus(String msg);
  void showDocument(URL url, String target);

  Dimension getSize();
  void setLayout(LayoutManager mgr);
  Component add(Component comp);
  Component add(String name, Component comp);

  void override_action(ActionHandler handler);
  void override_paint(PaintHandler handler);

  interface ActionHandler { 
    boolean action(
        ActionHandler super_action,
        Event evt, Object what);
  }
  interface PaintHandler {
    void paint(
        PaintHandler super_paint,
        Graphics g);
  }
}
