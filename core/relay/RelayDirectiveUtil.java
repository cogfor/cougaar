/* 
 * <copyright>
 *  
 *  Copyright 2002-2008 BBNT Solutions, LLC
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
package org.cougaar.core.relay;

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

/**
 * Misc utility methods on RelayDirectives
 *
 */
public class RelayDirectiveUtil {

    /**
       * Return the given Directive as RelayDirective if possible. Otherwise
       * return null. Usually this just involves a downcast. If the given
       * Directive has change reports we need to extract the true Directive it
       * holds and operate on that one instead.
       */
      public static RelayDirective getRelayDirective(Directive directive) {
          Directive candidate;
          if (directive instanceof DirectiveMessage.DirectiveWithChangeReports) {
              candidate = ((DirectiveMessage.DirectiveWithChangeReports) directive).getDirective();
          } else {
              candidate = directive;
          }
          if (candidate instanceof RelayDirective) {
              return (RelayDirective) candidate;
          } else {
              return null;
          }
      }

    /**
       * @return true iff any of the Directives in the given message, directly
       * or indirectly, are RelayDirectives.
       */
      public static boolean hasRelayDirectives(Message message) {
          if (message instanceof DirectiveMessage) {
              DirectiveMessage dmesg = (DirectiveMessage) message;
              for (Directive directive : dmesg.getDirectives()) {
                  if (getRelayDirective(directive) != null) {
                      return true;
                  }
              }
          }
          return false;
      }

    /**
       * Make a receipt directive for the given relay and receipt
       */
      public static Directive makeReceiptDirective(RelayDirective original, Object receipt) {
         UID requestorUID = original.getUID();
         RelayDirective.Response dir = 
             new RelayDirective.Response(requestorUID, receipt);
         dir.setSource(original.getDestination());
         dir.setDestination(original.getSource());
         return dir;
      }

    /**
       * Make a receipt message fro the given directive message, using the
       * given receipt in each RelayDirective.
       */
      public static Message makeReceiptMessage(MessageAddress originator,
                                               DirectiveMessage message, 
                                               Object receipt) {
          Directive[] originalDirectives = message.getDirectives();
          MessageAddress dest = message.getOriginator();
          
          // Construct and collect receipt directives for each RelayDirective
          List<RelayDirective> relevantDirectives = 
              new ArrayList<RelayDirective>(originalDirectives.length);
          for (Directive directive : originalDirectives) {
              RelayDirective relayDirective = RelayDirectiveUtil.getRelayDirective(directive);
              if (relayDirective != null) {
                  relevantDirectives.add(relayDirective);
              }
          }
          Directive[] receipts = new Directive[relevantDirectives.size()];
          for (int i=0; i<receipts.length; i++) {
              RelayDirective requestDirective = relevantDirectives.get(i);
              Directive responseDirective = makeReceiptDirective(requestDirective, receipt);
              receipts[i] = responseDirective;
          }
          
          // Construct and send the receipt message
          long incarnation = message.getIncarnationNumber(); // XXX: Is this right?
          return new DirectiveMessage(originator, dest, incarnation, receipts);
      }

}
