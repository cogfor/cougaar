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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A bit set for keeping track of sequence numbers, used by
 * the {@link MessageManager}.
 * <p> 
 * This is similar to a BitSet, but has a shifting base index. The
 * position of the first zero bit in the set can be queried and the
 * representation shifted so that the one bits prior to that first
 * zero don't require storage.
 */
class AckSet implements Serializable {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;

/** The sequence number of the first zero bit. */
  private int minSequence = 0;

  /** The sequence number corresponding to bit 0 of the bits array */
  private int baseSequence = 0;

  /** The index of the last word with bits */
  private transient int maxIndex = 0;

  /** The values of the bits */
  private transient long[] bits = new long[4];

  /** Computed index in bits of a given sequence number */
  private transient int index;

  /** Computed bit of a given sequence number */
  private transient long bit;

  /**
   * Construct with sequence numbers starting at zero.
   */
  public AckSet() {
  }

  /**
   * Construct with sequence numbers starting at a specified position.
   * @param initialSequenceNumber the position of the first zero in
   * the bit set. 
   */
  public AckSet(int initialSequenceNumber) {
    computeIndexInfo(initialSequenceNumber);
    baseSequence = index * 64;
    minSequence = initialSequenceNumber;
    bits[0] = bit - 1L;
  }

  /**
   * Compute the index into the bits array and the bit corresponding
   * to a given sequence number.
   * @param sequenceNumber
   */
  private void computeIndexInfo(int sequenceNumber) {
    sequenceNumber -= baseSequence;
    index = (sequenceNumber / 64);
    int bitIndex = (sequenceNumber % 64);
    bit = 1L << bitIndex;
  }

  /**
   * Find the first zero in the bitset, advance minSequence to that
   * position, and return that sequence number.
   * @return the position of the first bit that has not be set.
   */
  public synchronized int advance() {
    try {
      computeIndexInfo(minSequence);
      if (index >= bits.length) {         // Beyond the end
        return minSequence;
      }
      if ((bits[index] & bit) == 0L) return minSequence;
      while (index <= maxIndex) { // Check all the words in the array
        long word = bits[index];
        if (word == 0xffffffffffffffffL) {
          bit = 1L;               // All ones, advance to first bit
          ++index;                // of the next word
          minSequence = baseSequence + index * 64;
        } else {                  // Some zeros here
          while (bit != 0x8000000000000000L && (word & bit) != 0L) {
            ++minSequence;
            bit <<= 1;
          }
          return minSequence;     // That's the answer
        }
      }
      // All ones everywhere we looked
      return minSequence;
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(13);
      return 0;
    }
  }

  /**
   * Get the current minSequence value. This is always the same as the
   * last value returned from advance.
   * @return the position of first zero in the set as of the last call
   * to advance.
   */
  public int getMinSequence() {
    return minSequence;
  }

  /**
   * Set the bit corresponding to a particular sequence number. If the
   * word containing the bit falls beyond the end of the array, first
   * discard the words before the word containing minSequence. Then
   * expand the array if necessary.
   */
  public synchronized void set(int sequenceNumber) {
    computeIndexInfo(sequenceNumber);
    if (index < 0) return;              // These bits are already set.
    if (index >= bits.length) {         // Beyond the end
      computeIndexInfo(minSequence);    // Find out where the min is
      if (index > 0) {                  // There is room to shift down
        int nw = maxIndex - index + 1;  // The number of words to retain
        System.arraycopy(bits, index, bits, 0, nw);
        while (nw < bits.length) {
          bits[nw++] = 0L;              // Zero vacated words
        }
        baseSequence += index * 64;     // Advance by bits shifted
        maxIndex -= index;              // Decrease by words shifted
      }
      computeIndexInfo(sequenceNumber); // Re-evaluate
    }
    if (index >= bits.length) {         // Still beyond the end?
      long[] oldBits = bits;            // Need to expand
      bits = new long[index + 4];       // Make it a little bigger
                                        // than necessary
      System.arraycopy(oldBits, 0, bits, 0, maxIndex + 1);
    }
    bits[index] |= bit;
    if (index > maxIndex) {
      maxIndex = index;
    }
  }

  /**
   * Test if a particular bit is set.
   * @param sequenceNumber the bit to test.
   * @return true if the specified bit has been set.
   */
  public boolean isSet(int sequenceNumber) {
    if (sequenceNumber < minSequence) {
      return true;                      // No need to test these
    }
    computeIndexInfo(sequenceNumber);
    if (index > maxIndex) {
      return false;                     // These are always zero
    }
    return ((bits[index] & bit) != 0L);
  }

  /**
   * Write this object. We only write the active portion of the bits
   * array
   */
  private synchronized void writeObject(ObjectOutputStream os) throws IOException {
    os.defaultWriteObject();
    os.writeInt(maxIndex);
    for (int i = 0; i <= maxIndex; i++) {
      os.writeLong(bits[i]);
    }
  }

  /**
   * Read this object. The bits array is restored.
   */
  private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
    is.defaultReadObject();
    maxIndex = is.readInt();
    bits = new long[maxIndex + 1];
    for (int i = 0; i <= maxIndex; i++) {
      bits[i] = is.readLong();
    }
  }

  @Override
public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(0);
    boolean inRange = false;
    int prevSeq = minSequence-1;
    for (int seq = minSequence; ; seq++) {
      if (isSet(seq)) {
        if (seq != prevSeq + 1) {
          if (inRange) {
            buf.append(prevSeq);
            inRange = false;
          }
          buf.append(",");
          buf.append(seq);
          prevSeq = seq;
        } else {
          if (!inRange) {
            buf.append("..");
            inRange = true;
          }
          prevSeq = seq;
        }
      } else if (index > maxIndex) {
        break;
      }
    }
    if (inRange) {
      buf.append(prevSeq);
    }
    return buf.substring(0);
  }
}
