/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.core.persist;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

import org.cougaar.core.plugin.ServiceUserPlugin;

/**
 * This component exercises the blackboard and can be used to debug
 * trivial persistence problems.
 */
public class Exercise extends ServiceUserPlugin {
  private Item[] objects = new Item[1000];
  private Random random = new Random();
  private int executionMinDelay;
  private int executionMaxDelay;
  private int serializationTime;
  private int maxPublishCount;

  private static class Item implements Serializable {
    int serializationTime;

    public Item(int st) {
      serializationTime = st;
    }

    public long wasteTime() {
      long t = 0;
      for (int i = 0; i < 10 * serializationTime; i++) {
        t += System.currentTimeMillis();
      }
      return t;
    }

    private void writeObject(ObjectOutputStream oos)
      throws IOException
    {
      oos.writeInt(serializationTime);
      long t = wasteTime();
      oos.writeLong(t);
    }

    private void readObject(ObjectInputStream ois)
      throws IOException
    {
      serializationTime = ois.readInt();
      ois.readInt();
    }
  }

  public Exercise() {
    super(new Class[0]);
  }

  private int parseParameter(String prefix, int dflt) {
    for (Iterator i = getParameters().iterator(); i.hasNext(); ) {
      String param = (String) i.next();
      if (param.startsWith(prefix)) {
        try {
          return Integer.parseInt(param.substring(prefix.length()));
        } catch (Exception e) {
          logger.error("parseParameter error: " + param);
          return dflt;
        }
      }
    }
    return dflt;
  }

  @Override
public void setupSubscriptions() {
    int nItems = parseParameter("nItems=", 1000);
    executionMinDelay = parseParameter("executionMinDelay=", 5000);
    executionMaxDelay = parseParameter("executionMaxDelay=", 50000);
    serializationTime = parseParameter("serializationTime=", 1000);
    maxPublishCount = parseParameter("maxPublishCount=", 400);
    objects = new Item[nItems];
    logger.warn("Running " + blackboardClientName);
    resetTimer(executionMinDelay + random.nextInt(executionMaxDelay - executionMinDelay));
  }

//  private void wasteTime(Item item, int factor) {
//    for (int i = 0; i < factor; i++) {
//      item.wasteTime();
//    }
//  }

  @Override
public void execute() {
    if (timerExpired()) {
      long st = System.currentTimeMillis();
      cancelTimer();
      int n = random.nextInt(maxPublishCount);
      for (int i = 0; i < n; i++) {
        int x = random.nextInt(objects.length);
        if (objects[x] == null) {
          objects[x] = new Item(random.nextInt(serializationTime));
          //          wasteTime(objects[x], 1);
          blackboard.publishAdd(objects[x]);
        } else {
//           wasteTime(objects[i], 1);
          blackboard.publishChange(objects[x]);
        }
      }
      long et = System.currentTimeMillis();
      logger.warn("execute for " + (et - st) + " millis");
      resetTimer(executionMinDelay + random.nextInt(executionMaxDelay - executionMinDelay));
    }
  }
}
