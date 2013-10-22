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

package org.cougaar.core.qos.metrics;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * This helper class is used to keep a history of sensor values and
 * process them into metrics representing rates. The snapshots of
 * the sensor values are kept in a series of cascading stores thats
 * systematically discard the data as it gets older.  Instantiable
 * extensions must implement the {@link #newAddition newAddition}, which is
 * called when a snapshot is added or when its propagate to the next
 * level in the cascade.
 */
public abstract class DecayingHistory
    implements Constants
{
    
    /**
     * Callback with a pair of snapshots that need to be processed.
     * The KeyMap holds the key string the averaging period for this
     * pair.
     */
    public abstract void newAddition(KeyMap keys,
				     SnapShot now, 
				     SnapShot last);

    /**
     * These are snapshots as stored in history.  The abstract class
     * only stores timestamps.  Extend this class to store the
     * accumulators values
     */
    public static class SnapShot {
	public long timestamp;

	public SnapShot() {
	    timestamp = System.currentTimeMillis();
	}
    }

    private int basePeriod;
    private KeyMap baseKeys;
    private int rows;

    // Convert column to the 1xxxSecAvg String
    // Columns start a 1, base is 0
    private String columnToSecavg(int column) {
	long periodInt= basePeriod * 
	    Math.round(Math.pow(rows, column));
	return periodInt +SecAvgKeySuffix;
    }


    /**
     * Keeps a map of interned strings Each 1xxxSecAvg will have its
     * own map. The customer of DecayingHistory will supply the key
     * prefix and full key will be stored in map.
     */
    public class KeyMap {
	private String suffix;
	HashMap map = new HashMap();

	KeyMap(String suffix) {
	    super();
	    this.suffix=suffix;
	}

	public void addKey (String prefix) {
	    String full = prefix + suffix;
	    map.put(prefix.intern(),full.intern());
	}
	
	public String getKey(String prefix) {
	    return  (String) map.get(prefix);
	}
	public String getAvgPeriod() {
	    return  suffix;
	}
    }

    //  The column data structure,
    //  Keep a history and pass on the now-snapshot to the next column
    //  after "length" number of elements have been added
    private class DecayingHistoryList extends LinkedList {
	/**
       * 
       */
      private static final long serialVersionUID = 1L;
   private int column;
	private int length;
	private int index;
	private boolean full;
	private KeyMap keys;

	DecayingHistoryList(int column, int length, int basePeriod) {
	    this.column = column;
	    this.length = length;
	    keys = new KeyMap(columnToSecavg(column+1));
	    index = 0;
	    full = false;
	}

	public void addSnapShot(SnapShot new_elt) {
	    //First Element (No calculation)
	    if ( size() == 0) {
		addFirst(new_elt);
		DecayingHistoryList next = getList(column+1);
		if (next != null) next.addSnapShot(new_elt); 
		return ;
	    }

	    // Notify the listener to Calculate Delta between SnapShot
	    SnapShot last = (SnapShot) getLast();
	    newAddition(keys, new_elt, last);

	    // make room, if full
	    if (full) last = (SnapShot) removeLast();	
            // Rember SnapShot
	    addFirst(new_elt);

            // Time to give element to next history
	    boolean shift = ++index == length;
	    if (shift) {
		DecayingHistoryList next = getList(column+1);
		if (next != null) next.addSnapShot(new_elt);
		full = true;
		index = 0;
	    } 
	}
    }


    // History for each column/exponent 
    private DecayingHistoryList[] history;

    public DecayingHistory(int rows, int columns, int basePeriod)
    {
	this.rows=rows;
	this.basePeriod=basePeriod;
	baseKeys= new KeyMap(columnToSecavg(0));
	history = new DecayingHistoryList[columns];
	for (int i=0; i<history.length; i++) {
	    history[i] = new DecayingHistoryList(i, rows,basePeriod);
	}
    }

    public DecayingHistory(int rows, int columns)
    {
	this(rows, columns, 1);
    }

    private DecayingHistoryList getList(int index) {
	if (index < history.length)
	    return history[index];
	else
	    return null;
    }

    public synchronized void addKey(String prefix) {
	baseKeys.addKey(prefix);
	for (int i=0; i<history.length; i++) {
	    history[i].keys.addKey(prefix);
	}
    }

    public synchronized void add(SnapShot new_elt) {
        // Do Base Period Average
        if ( history[0].size() > 0 ) {
	    SnapShot last = (SnapShot) history[0].getFirst();
	    newAddition(baseKeys, new_elt, last);
	}
        // Added to history
	history[0].addSnapShot(new_elt);
    }
}
