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

package org.cougaar.core.plugin.freeze;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Filtering rules for selecting which components to freeze.
 */
public class Rules {
  private List rules = new ArrayList();

  static class Rule {
    boolean deny = false;
    Class cls;

    /**
     * Create a rule from a rule specificaton of the form:
     * [{allow,deny} ]<classname>
     * If allow/deny is missing, allow is assumed.
     */
    public Rule(String ruleSpec) throws ClassNotFoundException {
      int pos = ruleSpec.indexOf(' ');
      String spec = ruleSpec;
      if (pos >= 0) {
        String ad = ruleSpec.substring(0, pos);
        spec = ruleSpec.substring(pos + 1);
        if (ad.equalsIgnoreCase("allow")) {
          deny = false;
        } else if (ad.equalsIgnoreCase("deny")) {
          deny = true;
        } else {
          throw new IllegalArgumentException("Bad ruleSpec");
        }
      }
      cls = Class.forName(spec);
    }

    public Rule(Class c, boolean deny) {
      this.cls = c;
      this.deny = deny;
    }

    public boolean matches(Class c) {
      return cls.isAssignableFrom(c);
    }

    public boolean isAllowRule() {
      return !deny;
    }

    @Override
   public String toString() {
      return (deny ? "deny " : "allow ") + cls.getName();
    }
  }
  
  public void addDenyRule(Class c) {
      rules.add(new Rule(c, true));
  }

  public void addAllowRule(Class c) {
    rules.add(new Rule(c, false));
  }

  public void addRule(String ruleSpec) throws ClassNotFoundException {
    rules.add(new Rule(ruleSpec));
  }

  public boolean allow(Object o) {
    return allow(o.getClass());
  }

  public boolean allow(Class c) {
    for (int i = 0, n = rules.size(); i < n; i++) {
      Rule rule = (Rule) rules.get(i);
      if (rule.matches(c)) return rule.isAllowRule();
    }
    return true;
  }

  Iterator iterator() {
    return rules.iterator();
  }

  @Override
public String toString() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0, n = rules.size(); i < n; i++) {
      if (i > 0) buf.append(",");
      buf.append(rules.get(i));
    }
    return buf.toString();
  }

  public static void main(String[] args) {
    Rules rules = new Rules();
    boolean testMode = false;
    try {
      for (int i = 0; i < args.length; i++) {
        if (testMode) {
          Class cls = Class.forName(args[i]);
          System.out.println("rules.allow(" + cls.getName() + ")=" + rules.allow(cls));
        } else if ("-test".equals(args[i])) {
          testMode = true;
          System.out.println("rules=" + rules);
        } else {
          rules.addRule(args[i]);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
