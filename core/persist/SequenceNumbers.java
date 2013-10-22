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

package org.cougaar.core.persist;

/**
 * A persistence sequence range and timestamp.
 */
public class SequenceNumbers implements Comparable {
    int first = 0;
    int current = 0;
    long timestamp;             // The time the highest delta in this set was written.
    public SequenceNumbers() {
        timestamp = System.currentTimeMillis();
    }
    public SequenceNumbers(int first, int current, long timestamp) {
        this.first = first;
        this.current = current;
        this.timestamp = timestamp;
    }
    public SequenceNumbers(SequenceNumbers numbers) {
        this(numbers.first, numbers.current, numbers.timestamp);
    }

  /**
   * @return The first sequence number in the range
   */
  public int getFirst() {
    return first;
  }

  /**
   * @return The current sequence number in the range
   */
  public int getCurrent() {
    return current;
  }

  /**
   * @return Return the time that the highest delta in this set was written.
   */
  public long getTimestamp() {
    return timestamp;
  }

    public int compareTo(Object o) {
        SequenceNumbers that = (SequenceNumbers) o;
        if (this.timestamp < that.timestamp) return -1;
        if (this.timestamp > that.timestamp) return  1;
        return this.current = that.current;
    }
    @Override
   public String toString() {
        return first + ".." + current;
    }
}
