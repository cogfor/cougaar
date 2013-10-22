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

package org.cougaar.core.blackboard;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collection;

import org.cougaar.core.agent.ClusterContextTable;
import org.cougaar.core.agent.ClusterMessage;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceInputStream;
import org.cougaar.core.persist.PersistenceOutputStream;
import org.cougaar.util.StringUtility;
import org.cougaar.util.log.Logging;

/**
 * A {@link org.cougaar.core.mts.Message} containing {@link Directive}s.
 */
public class DirectiveMessage extends ClusterMessage
  implements Externalizable
{
  private transient Directive[] directives;

  /**
   * This signals that all messages prior to this have been acked.
   * Used in keep alive messages to detect out-of-sync condition.
   */
  private boolean allMessagesAcknowledged = false;
   
  public DirectiveMessage() {
    super();
  }
    
  /**
   * constructor that takes multiple directives
   * @param someDirectives to send
   */
  public DirectiveMessage(Directive[] someDirectives) {
    directives = someDirectives;
  }
    
  /**
   * constructor that takes source, destination and some directives
   */
  public DirectiveMessage(MessageAddress source, MessageAddress destination,
                          long incarnationNumber,
                          Directive[] someDirectives) 
  {
    super(source, destination, incarnationNumber);
    directives = someDirectives;
  }
    
  /**
   * @return the directives in the message
   */
  public Directive[] getDirectives() {
    return directives;
  }
    
  /**
   * Sets the directives in this message.
   */
  public void setDirectives(Directive[] someDirectives) {
    directives = someDirectives;
  }

  public void setAllMessagesAcknowledged(boolean val) {
    allMessagesAcknowledged = val;
  }

  public boolean areAllMessagesAcknowledged() {
    return allMessagesAcknowledged;
  }

  @Override
public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<DirectiveMessage "+getSource()+" - "+getDestination());
    if (directives == null) {
      buf.append("(Null directives)");
    } else {
      StringUtility.appendArray(buf, directives);
    }
    buf.append(">");
    return buf.substring(0);
  }

  private void withContext(MessageAddress ma, Runnable thunk) 
    throws IOException 
  {
    try {
      ClusterContextTable.withMessageContext(ma, getSource(), getDestination(), thunk);
    } catch (RuntimeException re) {
      Throwable t = re.getCause();
      if (t == null) {
        throw re;
      } else if (t instanceof IOException) {
        throw (IOException) t;
      } else {
        Logging.getLogger(DirectiveMessage.class).error(
            "Serialization of "+this+" caught exception", t);
        throw new IOException("Serialization exception: "+t);
      }
    }
  }
  

  /**
   */
  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    Runnable thunk =
      new Runnable() {
        public void run() {
          try {
            stream.writeInt(directives.length);
            for (int i = 0; i < directives.length; i++) {
              stream.writeObject(directives[i]);
            }
          } catch (Exception e) {
            throw new RuntimeException("Thunk", e);
          }
        }
      };
    if (stream instanceof PersistenceOutputStream) {
      thunk.run();
    } else {
      withContext( getSource(), thunk);
    }
  }

  /**
   * when we deserialize, note the message context with the 
   * ClusterContextTable so that lower-level objects can
   * reattach to the agent.
   * @see ClusterContextTable
   */
  private void readObject(final ObjectInputStream stream) 
    throws IOException, ClassNotFoundException
  {
    stream.defaultReadObject();

    Runnable thunk =
      new Runnable() {
        public void run() {
          try {
            directives = new Directive[stream.readInt()];
            for (int i = 0; i < directives.length; i++) {
              directives[i] = (Directive) stream.readObject();
            }
          } catch (Exception e) {
            throw new RuntimeException("Thunk", e);
          }
        }
      };
    if (stream instanceof PersistenceInputStream) {
      thunk.run();
    } else {
      withContext(getDestination(), thunk);
    }
  }

  // Externalizable support
  /*
  */
  @Override
public void writeExternal(final ObjectOutput out) throws IOException {
    super.writeExternal(out);   // Message

    out.writeBoolean(allMessagesAcknowledged);

    Runnable thunk =
      new Runnable() {
        public void run() {
          try {
            out.writeInt(directives.length);
            for (int i = 0; i < directives.length; i++) {
              out.writeObject(directives[i]);
            }
          } catch (Exception e) {
            throw new RuntimeException("Thunk", e);
          }
        }
      };
    if (out instanceof PersistenceOutputStream) {
      thunk.run();
    } else {
      withContext( getSource(), thunk);
    }
  }

  /**
   * when we deserialize, note the message context with the 
   * ClusterContextTable so that lower-level objects can
   * reattach to the agent.
   * @see ClusterContextTable
   */
  @Override
public void readExternal(final ObjectInput in) 
    throws IOException, ClassNotFoundException
  {
    super.readExternal(in);     // Message

    allMessagesAcknowledged = in.readBoolean();

    Runnable thunk =
      new Runnable() {
        public void run() {
          try {
            directives = new Directive[in.readInt()];
            for (int i = 0; i < directives.length; i++) {
              directives[i] = (Directive) in.readObject();
            }
          } catch (Exception e) {
            throw new RuntimeException("Thunk", e);
          }
        }
      };
    if (in instanceof PersistenceInputStream) {
      thunk.run();
    } else {
      withContext(getDestination(), thunk);
    }
  }

  /**
   * A {@link Directive} with associated {@link ChangeReport}s. 
   */
  public static final class DirectiveWithChangeReports implements Directive {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final Directive real;
    private final Collection changes;
    public DirectiveWithChangeReports(Directive d, Collection cc) {
      real = d;
      changes=cc;
    }
    public Directive getDirective() { return real; }
    public Collection getChangeReports() { return changes; }

    public MessageAddress getSource() { return real.getSource(); }
    public MessageAddress getDestination() { return real.getDestination(); }
    @Override
   public String toString() {return real.toString(); }
  }

}
