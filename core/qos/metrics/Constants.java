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


/**
 * Assorted constants for key and path construction, and credibility.
 */
public interface Constants
{
    /**
     * The character used to separate fields of a data key. */
    static final String KEY_SEPR = "_";

    /**
     * The character used to seperate fields of a data-lookup paths.
     */
    static final String PATH_SEPR = ":";

    /** 
     *Averaging Intervals
     */
    static final String _1_SEC_AVG = "1";
    static final String _10_SEC_AVG = "10";
    static final String _100_SEC_AVG = "100";
    static final String _1000_SEC_AVG = "1000";

    /**
     * The category of thread-service measurements, and some specific
     * ones
     */
    static final String THREAD_SENSOR = "COUGAAR_THREAD";

    static final String SecAvgKeySuffix = "SecAvg";
    static final String CPU_LOAD_AVG = "CPULoadAvg";
    static final String CPU_LOAD_MJIPS = "CPULoadMJips";
    static final String MSG_IN = "MsgIn";
    static final String MSG_OUT = "MsgOut";
    static final String BYTES_IN = "BytesIn";
    static final String BYTES_OUT = "BytesOut";
    static final String MSG_FROM = "MsgFrom";
    static final String MSG_TO = "MsgTo";
    static final String BYTES_FROM = "BytesFrom";
    static final String BYTES_TO = "BytesTo";
    // Traffic Matrix
    static final String MSG_RATE = "MsgRate";
    static final String BYTE_RATE = "ByteRate";


    static final String PERSIST_SIZE_LAST = "PersistSizeLast";

    // Credibility Spectrum: tries to unify many different notions of
    // credibility into a common metric. The Credibility "Calculus" is
    // still undefined, but here is the general notion for credibility
    // values. 
    //
    // The dimensions of credibility include 
    //   Aggregation: Time period over which the observation were made
    //   Staleness: How out-of-date is the data
    //   Source: How was the data collected
    //   Trust or Collector Authority: Can the collector be trusted
    //   Sensitivity: Does a key component of the data have low credibility
    /**
     * No source for data. There was an error or nobody was looking for
     * this data*/
    static final double NO_CREDIBILITY = 0.0;
    /**
     * Compile Time Default was the source for data */
    static final double DEFAULT_CREDIBILITY = 0.1;
    /**
     * System Level configuration file was the source for data */
    static final double SYS_DEFAULT_CREDIBILITY = 0.2;
    /**
     * User Level configuration file was the source for data */
    static final double USER_DEFAULT_CREDIBILITY = 0.3;
    /**
     * System Level Base-Line measurements file was the source
     * for data. This data is aggregated over Days */
    static final double SYS_BASE_CREDIBILITY = 0.4;
    /**
     * User Level Base-Line measurements file was the source for
     * data. This data is aggregated over Days */
    static final double USER_BASE_CREDIBILITY = 0.5;
    /**
     * A Single Active measurment was source for data. 
     * This data is aggregated over Hours and is not stale*/
    static final double HOURLY_MEAS_CREDIBILITY = 0.6;
    /**
     * A Single Active measurment was source for data. 
     * This data is aggregated over Minutes and is not stale*/
    static final double MINUTE_MEAS_CREDIBILITY = 0.7;
    /**
     * A Single Active measurment was source for data. 
     * This data is aggregated over Seconds and is not stale*/
    static final double SECOND_MEAS_CREDIBILITY = 0.8;
    /**
     * A Multiple Active measurments were a Consistant source for data. 
     * This data is aggregated over Seconds and is not stale*/
    static final double CONFIRMED_MEAS_CREDIBILITY = 0.9;
    /**
     * A higher-level system has declared this datarmation to be true. 
     * Mainly used by debuggers and gods */
    static final double ORACLE_CREDIBILITY = 1.0;
}

