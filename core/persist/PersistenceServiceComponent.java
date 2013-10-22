/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.agent.ClusterContextTable;
import org.cougaar.core.blackboard.Envelope;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DataProtectionKey;
import org.cougaar.core.service.DataProtectionKeyEnvelope;
import org.cougaar.core.service.DataProtectionService;
import org.cougaar.core.service.DataProtectionServiceClient;
import org.cougaar.core.service.PersistenceControlService;
import org.cougaar.core.service.PersistenceMetricsService;
import org.cougaar.util.CSVUtility;
import org.cougaar.util.GC;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.LinkedByteOutputStream;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This component advertises the {@link PersistenceService} and
 * manages all persistence activities except for the actual storage
 * of persistence deltas, which is done by {@link PersistencePlugin}s.
 * <p>
 * As the distributor is about to about to distribute the objects in a
 * set of envelopes, those envelopes are passed to an instance of this
 * class.  The contents of those envelopes are serialized into a
 * storage medium. These objects may refer to other plan objects that
 * have not changed.  These objects are not in any of the envelopes,
 * but they must have been stored in earlier deltas.  Instead of
 * rewriting those objects to the new delta, references to the earlier
 * objects are stored instead.
 * <p>
 * Restoring the state from this series is a bit problematic in that a
 * given object may have been written to several deltas; only the
 * latest copy is valid and all references from other objects must be
 * made to this latest copy and all others should be ignored.  This is
 * handled by overwriting the value of the earlier objects with newer
 * values from later versions of the objects.  
 *
 * @property org.cougaar.core.persistence.class
 * Specify the persistence classes to be used (if persistence is enabled).
 * The value consists of one or more elements separated by commas,
 * where element specifies a persistence class name and zero
 * or more semi-colon-separated parameters for that class.
 * For example, the default persistence class is:<br>
 * &nbsp;&nbsp;-Dorg.cougaar.core.persist.class=org.cougaar.core.persist.FilePersistence\;P<br>
 * Here is another example:<br>
 * &nbsp;&nbsp;-Dorg.cougaar.core.persist.class=org.cougaar.core.persist.DummyPersistence\;dummy<br>
 * Multiple classes can be specified with the "," separator, e.g.:<br>
 * &nbsp;&nbsp;-Dorg.cougaar.core.persist.class=Alpha\;a1\;a2\;a3,Beta\;b1\;b2<br>
 * where class Alpha is passed [a1, a2, a3] and Beta is passed [b1, b2].<br>
 * The interpretation of the parameters depends on the persistence
 * class, so see the documentation of the individual plugin classes
 * for details.
 *
 * @property org.cougaar.core.persistence.archivingDisabled
 * Set true
 * to discard archival deltas. Overridden by
 * org.cougaar.core.persistence.archiveCount.
 *
 * @property org.cougaar.core.persistence.archiveCount
 * An integer
 * specifying how may persistence archive snapshots to keep. In the
 * absence of a value for this property, the archivingDisabled
 * property is used to set this value to 0 for disabled and
 * Integer.MAX_VALUE for enabled.
 *
 * @property org.cougaar.core.persistence.clear
 * Set true to discard all deltas on startup
 *
 * @property org.cougaar.core.persistence.consolidationPeriod
 * The number of incremental deltas between full deltas (default = 10)
 *
 * @property org.cougaar.core.persistence.lazyInterval
 * specifies the
 * interval in milliseconds between the generation of persistence
 * deltas. Default is 300000 (5 minutes). This will be overridden if
 * the persistence control and adaptivity engines are running.
 *
 * @property org.cougaar.core.persistence.DataProtectionServiceStubEnabled
 * set to true to enable 
 * a debugging implementation of DataProtectionService if no real one is found.
 */
public class PersistenceServiceComponent
  extends GenericStateModelAdapter
  implements Component, PersistencePluginSupport, PersistenceNames
{
  private static final long MIN_PERSISTENCE_INTERVAL = 5000L;
  private static final long MAX_PERSISTENCE_INTERVAL = 1200000L; // 20 minutes max
  private static final String DUMMY_MEDIA_NAME = "dummy";
  private static final String FILE_MEDIA_NAME = "P";
  private static final char PARAM_SEP = ';';

  private static final long PERSISTENCE_INTERVAL_DFLT = 300000L;
  private static final boolean WRITE_DISABLED_DFLT =
    SystemProperties.getBoolean(PERSISTENCE_DISABLE_WRITE_PROP);

  private static final String PERSISTENCE_CONSOLIDATION_PERIOD_PROP =
    PERSISTENCE_PROP_PREFIX + PERSISTENCE_CONSOLIDATION_PERIOD_NAME;
  private static final int PERSISTENCE_CONSOLIDATION_PERIOD_DFLT = 10;

  private static final String[] PERSISTENCE_CLASSES_DFLT = getPersistenceClassesDflt();

  private static String[] getPersistenceClassesDflt() {
    String prop = SystemProperties.getProperty(PERSISTENCE_CLASS_PROP);
    if (prop != null) {
      try {
        return CSVUtility.parse(prop);
      } catch (Exception e) {
        Logging.getLogger(PersistenceServiceComponent.class).error("Failed to parse " + prop, e);
      }
    }
    boolean disabled =
      SystemProperties.getProperty(PERSISTENCE_ENABLE_PROP, "false").equals("false");
    if (disabled) return new String[0];
    return new String[] {
      FilePersistence.class.getName() + PARAM_SEP + FILE_MEDIA_NAME
    };
  }

  private static String getDummyPersistenceClass() {
    return DummyPersistence.class.getName()
      + PARAM_SEP + DUMMY_MEDIA_NAME
      + PARAM_SEP + PERSISTENCE_INTERVAL_PREFIX + MAX_PERSISTENCE_INTERVAL;
  }

  private long PERSISTENCE_INTERVAL =
    SystemProperties.getLong(PERSISTENCE_INTERVAL_PROP,
                 PERSISTENCE_INTERVAL_DFLT);
  private int PERSISTENCE_CONSOLIDATION_PERIOD =
    SystemProperties.getInt(PERSISTENCE_CONSOLIDATION_PERIOD_PROP,
                            PERSISTENCE_CONSOLIDATION_PERIOD_DFLT);

  private static class RehydrationSet {
    PersistencePlugin ppi;
    SequenceNumbers sequenceNumbers;
    RehydrationSet(PersistencePlugin ppi, SequenceNumbers sn) {
      this.ppi = ppi;
      this.sequenceNumbers = sn;
    }
  }

  private static class ClientStuff extends RehydrationData implements Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public void addAssociation(PersistenceAssociation pAssoc) {
      envelope.addObject(pAssoc.getObject());
    }
    public void setObjects(List l) {
      objects = l;
    }
  }

  private class PersistencePluginInfo {
    PersistencePlugin ppi;
    long nextPersistenceTime;
    SequenceNumbers cleanupSequenceNumbers = null;

    PersistencePluginInfo(PersistencePlugin ppi) {
      this.ppi = ppi;
      if (ppi.getPersistenceInterval() <= 0L) {
        setInterval(PERSISTENCE_INTERVAL);
      }
      if (ppi.getConsolidationPeriod() <= 0) {
        setConsolidationPeriod(PERSISTENCE_CONSOLIDATION_PERIOD);
      }
    }

    long getBehind(long now) {
      return now - nextPersistenceTime;
    }

    /**
     * This is complicated because we want this plugin to be ahead or
     * behind to the same degree it was ahead or behind before the
     * change. To do this, we get how much we are currently behind and
     * multiply that by the ratio of the new and old intervals. The
     * new base time and delta count are set so that we appear to be
     * behind by the adjusted amount.
     */
    void setInterval(long newInterval) {
      long now = System.currentTimeMillis();
      long persistenceInterval = ppi.getPersistenceInterval();
      if (persistenceInterval == 0L) {
        nextPersistenceTime = newInterval + now;
      } else {
        long behind = getBehind(now);
        long newBehind = behind * newInterval / persistenceInterval;
        nextPersistenceTime = now - newBehind;
      }
      ppi.setPersistenceInterval(newInterval);
      if (logger.isDebugEnabled()) {
        logger.debug(ppi.getName() + " persistenceInterval = " + newInterval);
        logger.debug(ppi.getName() + " nextPersistenceTime = " + nextPersistenceTime);
      }
    }

    void setConsolidationPeriod(int newPeriod) {
      ppi.setConsolidationPeriod(newPeriod);
      if (logger.isDebugEnabled()) {
        logger.debug(ppi.getName() + " consolidationPeriod = " + newPeriod);
      }
    }
  }

  private DataProtectionServiceClient dataProtectionServiceClient =
    new DataProtectionServiceClient() {
      public Iterator iterator() {
        return getDataProtectionKeyIterator();
      }
      public MessageAddress getAgentIdentifier() {
        return PersistenceServiceComponent.this.getMessageAddress();
      }
    };

  private static class PersistenceKeyEnvelope implements DataProtectionKeyEnvelope {
    PersistencePlugin ppi;
    int deltaNumber;
    DataProtectionKey theKey = null;

    public PersistenceKeyEnvelope(PersistencePlugin ppi, int deltaNumber) {
      this.deltaNumber = deltaNumber;
      this.ppi = ppi;
    }
    public void setDataProtectionKey(DataProtectionKey aKey) throws IOException {
      theKey = aKey;
      ppi.storeDataProtectionKey(deltaNumber, aKey);
    }
    public DataProtectionKey getDataProtectionKey() throws IOException {
      if (theKey == null) {
        theKey = ppi.retrieveDataProtectionKey(deltaNumber);
      }
      return theKey;
    }
  }

  private Iterator getDataProtectionKeyIterator() {
    List envelopes = new ArrayList();
    RehydrationSet[] rehydrationSets = getRehydrationSets("");
    for (int i = 0; i < rehydrationSets.length; i++) {
      SequenceNumbers sequenceNumbers = rehydrationSets[i].sequenceNumbers;
      final PersistencePlugin ppi = rehydrationSets[i].ppi;
      for (int seq = sequenceNumbers.first; seq < sequenceNumbers.current; seq++) {
        envelopes.add(new PersistenceKeyEnvelope(ppi, seq));
      }
    }
    return envelopes.iterator();
  }

  private PersistencePluginInfo getPluginInfo(String pluginName) {
    PersistencePluginInfo ppio = (PersistencePluginInfo) plugins.get(pluginName);
    if (ppio != null) return ppio;
    throw new IllegalArgumentException("No such persistence medium: "
                                       + pluginName);
  }

  private String getAgentName() {
    MessageAddress addr = getMessageAddress();
    return (addr == null ? "null" : addr.toString());
  }

  public class PersistenceControlServiceImpl
    implements PersistenceControlService
  {
    public String[] getControlNames() {
      return new String[0];
    }

    public OMCRangeList getControlValues(String controlName) {
      throw new IllegalArgumentException("No such control: " + controlName);
    }

    public void setControlValue(String controlName, Comparable newValue) {
      throw new IllegalArgumentException("No such control: " + controlName);
    }

    public String[] getMediaNames() {
      return (String[]) plugins.keySet().toArray(new String[plugins.size()]);
    }

    public String[] getMediaControlNames(String mediaName) {
      getPluginInfo(mediaName); // Test existence
      return new String[] {
        PERSISTENCE_INTERVAL_NAME,
        PERSISTENCE_CONSOLIDATION_PERIOD_NAME,
      };
    }

    public OMCRangeList getMediaControlValues(String mediaName, String controlName) {
      getPluginInfo(mediaName);// Test existence
      if (controlName.equals(PERSISTENCE_INTERVAL_NAME)) {
        return new OMCRangeList(new Long(MIN_PERSISTENCE_INTERVAL),
                                new Long(MAX_PERSISTENCE_INTERVAL));
      }
      if (controlName.equals(PERSISTENCE_CONSOLIDATION_PERIOD_NAME)) {
        return new OMCRangeList(new Integer(1),
                                new Integer(20));
      }
      throw new IllegalArgumentException(mediaName + " has no control named: " + controlName);
    }

    public void setMediaControlValue(String mediaName,
                                     String controlName,
                                     Comparable newValue)
    {
      PersistencePluginInfo ppio = getPluginInfo(mediaName);
      if (controlName.equals(PERSISTENCE_INTERVAL_NAME)) {
        ppio.setInterval(((Number) newValue).longValue());
        recomputeNextPersistenceTime = true;
        return;
      }
      if (controlName.equals(PERSISTENCE_CONSOLIDATION_PERIOD_NAME)) {
        ppio.setConsolidationPeriod(((Number) newValue).intValue());
        return;
      }
      throw new IllegalArgumentException(mediaName + " has no control named: " + controlName);
    }
  }

  // Component implementation

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  /**
   * Ideally, our agent would give us our parameters, but limitations
   * in ACME or CSMART may preclude this possibility. So, this method
   * is an alternate using system properties. The property named
   * org.cougaar.core.persistence.<agent> may have a value that is a
   * comma separated list of values of parameters. Each item of the
   * list is of the form <name>=<value> where <name> is the simple
   * name for the parameter (without the org.cougaar.persistence.
   * prefix). Many of the values in this list will contain commas and
   * this must be quoted with backslash or quotes.
   */
  private static List getParametersFromProperties(MessageAddress agentId) {
    List ret = new ArrayList();
    String pname = PERSISTENCE_PARAMETERS_PROP + "." + agentId;
    String pvalue = SystemProperties.getProperty(pname);
    if (pvalue != null) {
      String[] ps = CSVUtility.parse(pvalue);
      for (int i = 0; i < ps.length; i++) {
        ret.add(PERSISTENCE_PROP_PREFIX + ps[i]);
      }
    }
    return ret;
  }

  /**
   * Optional persistence parameters for this agent
   */
  public void setParameter(Object o) {
    if (o instanceof List) {
      params.addAll((List) o);
    } else {
      throw new IllegalArgumentException("Illegal parameter " + o);
    }
  }

  /**
   * Load
   */
  @Override
public void load() {
    super.load();

    // Get our local agent's address
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      agentId = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // Create our logger
    logger = Logging.getLogger(this.getClass());
    logger = new LoggingServiceWithPrefix(logger, getAgentName() + ": ");

    // Add persistence parameters defined by system properties
    params.addAll(getParametersFromProperties(agentId));

    // Set agent-wide persistence defaults
    List overridePluginClasses = new ArrayList();
    for (int i = 0, n = params.size(); i < n; i++) {
      String fullParam = (String) params.get(i);
      if (!fullParam.startsWith(PERSISTENCE_PROP_PREFIX)) continue; // Not mine
      String param = fullParam.substring(PERSISTENCE_PROP_PREFIX.length());
      try {
        if (param.startsWith(PERSISTENCE_INTERVAL_PREFIX)) {
          PERSISTENCE_INTERVAL =
            Long.parseLong(param.substring(PERSISTENCE_INTERVAL_PREFIX.length()));
          continue;
        }
        if (param.startsWith(PERSISTENCE_CONSOLIDATION_PERIOD_PREFIX)) {
          PERSISTENCE_CONSOLIDATION_PERIOD =
            Integer.parseInt(param.substring(PERSISTENCE_CONSOLIDATION_PERIOD_PREFIX.length()));
          continue;
        }
        if (param.startsWith(PERSISTENCE_DISABLE_WRITE_PREFIX)) {
          writeDisabled = "true".equals(param.substring(PERSISTENCE_DISABLE_WRITE_PREFIX.length()));
          continue;
        }
        if (param.startsWith(PERSISTENCE_ARCHIVE_NUMBER_PREFIX)) {
          archiveNumber = param.substring(PERSISTENCE_ARCHIVE_NUMBER_PREFIX.length());
          writeDisabled = true; // Setting the archive number only makes sense if not writing
          continue;
        }
        if (param.startsWith(PERSISTENCE_CLASS_PREFIX)) {
          String plugin = param.substring(PERSISTENCE_CLASS_PREFIX.length());
          overridePluginClasses.add(plugin);
          continue;
        }
      } catch (Exception e) {
        logger.error("Error parsing parameter: " + fullParam);
      }
    }
    String[] pluginClasses = PERSISTENCE_CLASSES_DFLT;
    if (overridePluginClasses.size() > 0) {
      // Replace default with specific classes
      pluginClasses = new String[overridePluginClasses.size()];
      pluginClasses = (String[]) overridePluginClasses.toArray(pluginClasses);
    }

    identityTable = new IdentityTable(logger);
    registerServices(sb);
    try {
      for (int i = 0; i < pluginClasses.length; i++) {
        addPlugin(pluginClasses[i]);
      }
      // There must be at least one writable plugin
      boolean haveWritablePlugin = false;
      for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
        PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
        if (ppio.ppi.isWritable()) {
          haveWritablePlugin = true;
          break;
        }
      }
      if (!haveWritablePlugin) {
        // Add a dummy persistence plugin
        addPlugin(getDummyPersistenceClass());
        isDummy = true;
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception in load()", e);
    }
  }

  private void addPlugin(String pluginSpec) throws PersistenceException {
    String[] paramTokens = CSVUtility.parse(pluginSpec, PARAM_SEP);
    if (paramTokens.length < 1) {
      throw new PersistenceException("No plugin class specified: " + pluginSpec);
    }
    if (paramTokens.length < 2) {
      throw new PersistenceException("No plugin name: " + pluginSpec);
    }
    try {
      Class pluginClass = Class.forName(paramTokens[0]);
      String pluginName = paramTokens[1];
      String[] pluginParams = new String[paramTokens.length - 2];
      System.arraycopy(paramTokens, 2, pluginParams, 0, pluginParams.length);
      PersistencePlugin ppi = (PersistencePlugin) pluginClass.newInstance();
      if (writeDisabled) ppi.setWritable(false); // Force write off
      addPlugin(ppi, pluginName, pluginParams);
    } catch (ClassNotFoundException cnfe) {
      throw new PersistenceException("Bad plugin class", cnfe);
    } catch (InstantiationException ie) {
      throw new PersistenceException("Plugin instantiation failed", ie);
    } catch (IllegalAccessException iae) {
      throw new PersistenceException("Plugin constructor inaccessible", iae);
    }
  }

  @Override
public void unload() {
    unregisterServices(sb);
    if (dataProtectionService != null) {
      sb.releaseService(dataProtectionServiceClient,
                        DataProtectionService.class,
                        dataProtectionService);
    }
  }

  private DataProtectionService getDataProtectionService() {
    if (dataProtectionService == null) {

      // For running with security, getting the service, which may be
      // the security service, is a privileged action
      // See RFE 3771
      dataProtectionService = (DataProtectionService)
	AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              return sb.getService(dataProtectionServiceClient, DataProtectionService.class, null);
            }
          });
      
      if (dataProtectionService == null) {
        if (logger.isInfoEnabled()) logger.info("No DataProtectionService Available.");
        if (SystemProperties.getBoolean("org.cougaar.core.persistence.DataProtectionServiceStubEnabled")) {
          dataProtectionService = new DataProtectionServiceStub();
        }
      } else {
        if (logger.isInfoEnabled()) logger.info("DataProtectionService is "
                                                + dataProtectionService.getClass().getName());
      }
    }
    return dataProtectionService;
  }

  /**
   * Keeps all associations of objects that have been persisted.
   */
  private IdentityTable identityTable;
  private SequenceNumbers sequenceNumbers = null;
  private ObjectOutputStream currentOutput;
  private List params = new ArrayList();
  private MessageAddress agentId;
  private List associationsToPersist = new ArrayList();
  private boolean previousPersistFailed = false;
  private Logger logger;
  private DataProtectionService dataProtectionService;
  private boolean writeDisabled = WRITE_DISABLED_DFLT;
  private String archiveNumber = "";
  private Map plugins = new HashMap();
  private boolean isDummy = false;
  private Map rehydrationResult = null;
  private Map clients = new HashMap();
  private ServiceBroker sb;
  private boolean full;         // Private to persist method and methods it calls

  /**
   * The current PersistencePlugin being used to generate persistence
   * deltas. This is changed just prior to generating a full delta if
   * there is a different plugin having priority.
   */
  private PersistencePluginInfo currentPersistPluginInfo;

  private long previousPersistenceTime = System.currentTimeMillis();

  private long nextPersistenceTime;

  private boolean recomputeNextPersistenceTime = true;

  private PersistenceMetricsServiceImpl metricsService =
    new PersistenceMetricsServiceImpl();

  private void addPlugin(PersistencePlugin ppi, String pluginName, String[] pluginParams)
    throws PersistenceException
  {
    boolean deleteOldPersistence = SystemProperties.getBoolean("org.cougaar.core.persistence.clear");
    if (deleteOldPersistence && logger.isInfoEnabled()) {
      logger.info("Clearing old persistence data");
    }
    ppi.init(this, pluginName, pluginParams, deleteOldPersistence);
    plugins.put(ppi.getName(), new PersistencePluginInfo(ppi));
  }

  public MessageAddress getMessageAddress() {
    return agentId;
  }

  public boolean isDummyPersistence() {
    return isDummy;
  }

  /**
   * Gets the system time when persistence should be performed. We do
   * persistence periodically with a period such that all the plugins
   * will, on the average create persistence deltas with their
   * individual periods. The average frequency of persistence is the
   * sum of the individual media frequencies. Frequency is the
   * reciprocal of period. The computation is:<p>
   *
   * &nbsp;&nbsp;T = 1/(1/T1 + 1/T2 + ... + 1/Tn)
   * <p>
   * @return the time of the next persistence delta
   */
  public long getPersistenceTime() {
    if (recomputeNextPersistenceTime) {
      double sum = 0.0;
      for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
        PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
        if (ppio.ppi.isWritable()) {
          sum += 1.0 / ppio.ppi.getPersistenceInterval();
        }
      }
      long interval = (long) (1.0 / sum);
      nextPersistenceTime = previousPersistenceTime + interval;
      recomputeNextPersistenceTime = false;
      if (logger.isDebugEnabled()) logger.debug("persistence interval=" + interval);
    }
    return nextPersistenceTime;
  }

  private ClientStuff getClientStuff(PersistenceIdentity clientId) {
    if (rehydrationResult == null) return null; // No rehydration
    ClientStuff clientStuff = (ClientStuff) rehydrationResult.get(clientId);
    if (clientStuff == null) {
      clientStuff = new ClientStuff();
      rehydrationResult.put(clientId, clientStuff);
    }
    return clientStuff;
  }

  private RehydrationData getRehydrationData(PersistenceIdentity clientId) {
    return getClientStuff(clientId);
  }

  /**
   * Rehydrate a persisted agent. Reads all the deltas in order
   * keeping the latest (last encountered) values from every object.
   * The rehydrated state is saved in rehydrationResult.
   * @param state a PersistenceObject if rehydrating from a saved
   * state object. If null, rehydrate from media plugins
   */
  private void rehydrate(final PersistenceObject pObject) {
    if (isDummy && pObject == null) {
      return; // Nothing to rehydrate
    }
    synchronized (identityTable) {
      final List rehydrationCollection = new ArrayList();
      identityTable.setRehydrationCollection(rehydrationCollection);
      try {
        final RehydrationSet[] rehydrationSets = getRehydrationSets(archiveNumber);
        if (pObject != null || rehydrationSets.length > 0) { // Deltas exist
          try {
            final Map[] resultPtr = new Map[1];
            Runnable thunk = new Runnable() {
                public void run() {
                  try {
                    if (pObject != null) {
                      if (logger.isInfoEnabled()) {
                        logger.info("Rehydrating " + getMessageAddress()
                                    + " from " + pObject);
                      }
                      resultPtr[0] = rehydrateFromBytes(pObject.getBytes());
                    } else {
                      // Loop through the available RehydrationSets and
                      // attempt to rehydrate from each one until no errors
                      // occur. This will normally happen on the very first
                      // set, but might fail if the data has been corrupted
                      // in some way.
                      boolean success = false;
                      for (int i = 0; i < rehydrationSets.length; i++) {
                        SequenceNumbers rehydrateNumbers = rehydrationSets[i].sequenceNumbers;
                        PersistencePlugin ppi = rehydrationSets[i].ppi;
                        if (logger.isInfoEnabled()) {
                          logger.info("Rehydrating "
                                      + getMessageAddress()
                                      + " "
                                      + rehydrateNumbers.toString());
                        }
                        try {
                          while (rehydrateNumbers.first < rehydrateNumbers.current - 1) {
                            rehydrateOneDelta(ppi, rehydrateNumbers.first++, false);
                          }
                          resultPtr[0] =
                            rehydrateOneDelta(ppi, rehydrateNumbers.first++, true);
                          success = true;
                          break;      // Successful rehydration
                        } catch (Exception e) { // Rehydration failed
                          logger.error("Rehydration from " + rehydrationSets[i] + " failed: ", e);
                          resetRehydration(rehydrationCollection);
                          continue;   // Try next RehydrationSet
                        }
                      }
                      if (!success) {
                        logger.error("Rehydration failed. Starting over from scratch");
                      }
                    }
                  } catch (Exception ioe) {
                    throw new RuntimeException("withClusterContext", ioe);
                  }
                }};

            ClusterContextTable.withClusterContext(getMessageAddress(), thunk);
            Map clientData = resultPtr[0];
            if (clientData == null) return; // Didn't rehydrate
            rehydrationResult = new HashMap();
            for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
              PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
              Object obj = pAssoc.getObject();
              PersistenceIdentity clientId = pAssoc.getClientId();
              if (pAssoc.isActive()) {
                if (logger.isDetailEnabled()) logger.detail(clientId + ": addAssociation " + pAssoc);
                getClientStuff(clientId).addAssociation(pAssoc);

		// Bug 3588: Do postRehydration work here, only for objects on 
		// BBoard, instead of for all.
		// This is wrong if the Composition on an InActive MPTask is 
		// wanted  and not on another MPTask but could be later. 
		// Also if an inactive object can become active.
                if (obj instanceof ActivePersistenceObject) {
                  ((ActivePersistenceObject) obj).postRehydration(logger);
                }
              } else {
                if (logger.isDetailEnabled()) logger.detail(clientId + ": inactive " + pAssoc);
              }
            }
            if (logger.isDetailEnabled()) {
              printIdentityTable("");
            }
            for (Iterator i = clientData.entrySet().iterator(); i.hasNext(); ) {
              Map.Entry entry = (Map.Entry) i.next();
              PersistenceIdentity clientId = (PersistenceIdentity) entry.getKey();
              List clientObjects = (List) entry.getValue();
              ClientStuff clientStuff = getClientStuff(clientId);
              clientStuff.setObjects(clientObjects);
              if (logger.isDetailEnabled()) {
                logger.detail("PersistenceEnvelope of " + clientId);
                logEnvelopeContents(clientStuff.getPersistenceEnvelope());
                logger.detail("Other objects of " + clientId + ": " + clientStuff.getObjects());
              }
            }
            clearMarks(identityTable.iterator());
          } catch (Exception e) {
            logger.error("Error during rehydration", e);
          }

        }
      }
      finally {
        rehydrationCollection.clear(); // Allow garbage collection
        identityTable.setRehydrationCollection(null); // Perform garbage collection
      }
    }
  }

  /**
   * Loop through all plugins and get their available sequence number
   * sets. Create RehydrationSets from the sequence numbers sets and
   * sort them by timestamp.
   * @return the sorted RehydrationSets
   */
  private RehydrationSet[] getRehydrationSets(String suffix) {
    List result = new ArrayList();
    for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
      PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
      SequenceNumbers[] pluginNumbers = ppio.ppi.readSequenceNumbers(suffix);
      for (int j = 0; j < pluginNumbers.length; j++) {
        result.add(new RehydrationSet(ppio.ppi, pluginNumbers[j]));
      }
    }
    Collections.sort(result, new Comparator() {
        public int compare(Object o1, Object o2) {
          RehydrationSet rs1 = (RehydrationSet) o1;
          RehydrationSet rs2 = (RehydrationSet) o2;
          int diff = rs1.sequenceNumbers.compareTo(rs1.sequenceNumbers);
          if (diff != 0) return -diff;
          return rs1.ppi.getName().compareTo(rs2.ppi.getName());
        }
      });
    return (RehydrationSet[]) result.toArray(new RehydrationSet[result.size()]);
  }

  private void logEnvelopeContents(Envelope env) {
    for (Iterator cc = env.getAllTuples(); cc.hasNext(); ) {
      EnvelopeTuple t = (EnvelopeTuple) cc.next();
      String action = "";
      switch(t.getAction()) {
      case Envelope.ADD: action = "ADD"; break;
      case Envelope.REMOVE: action = "REMOVE"; break;
      case Envelope.CHANGE: action = "CHANGE"; break;
      case Envelope.BULK: action = "BULK"; break;
      }
      logger.detail(action + " " + t.getObject());
    }
  }

  public static String hc(Object o) {
    return (Integer.toHexString(System.identityHashCode(o)) +
            " " +
            (o == null ? "<null>" : o.toString()));
  }

  /**
   * Erase all the effects of a failed rehydration attempt.
   */
  private void resetRehydration(Collection rehydrationCollection) {
    identityTable = new IdentityTable(logger);
    rehydrationCollection.clear();
    identityTable.setRehydrationCollection(rehydrationCollection);
  }

  private Map rehydrateOneDelta(PersistencePlugin ppi, int deltaNumber, boolean lastDelta)
    throws IOException, ClassNotFoundException
  {
    InputStream is = ppi.openInputStream(deltaNumber);
    DataProtectionService dataProtectionService = getDataProtectionService();
    if (dataProtectionService != null) {
      PersistenceKeyEnvelope keyEnvelope = new PersistenceKeyEnvelope(ppi, deltaNumber);
      is = dataProtectionService.getInputStream(keyEnvelope, is);
    }
    ObjectInputStream ois = new ObjectInputStream(is);
    try {
      return rehydrateFromStream(ois, deltaNumber, lastDelta);
    } finally {
      ois.close();
      ppi.finishInputStream(deltaNumber);
    }
  }

  private Map rehydrateFromBytes(byte[] bytes)
    throws IOException, ClassNotFoundException
  {
    ByteArrayInputStream bs = new ByteArrayInputStream(bytes);
    return rehydrateFromStream(new ObjectInputStream(bs), 0, true);
  }

  private Map rehydrateFromStream(ObjectInputStream currentInput,
                                  int deltaNumber, boolean lastDelta)
    throws IOException, ClassNotFoundException
  {
    try {
      identityTable.setNextId(currentInput.readInt());
      int length = currentInput.readInt();
      if (logger.isDebugEnabled()) {
        logger.debug("Reading " + length + " objects");
      }
      PersistenceReference[][] referenceArrays = new PersistenceReference[length][];
      for (int i = 0; i < length; i++) {
	referenceArrays[i] = (PersistenceReference []) currentInput.readObject();
      }
      //        byte[] bytes = (byte[]) currentInput.readObject();
      //        PersistenceInputStream stream = new PersistenceInputStream(bytes);
      PersistenceInputStream stream = new PersistenceInputStream(currentInput, logger);
      if (logger.isDetailEnabled()) {
        writeHistoryHeader();
      }
      stream.setIdentityTable(identityTable);
      try {
	for (int i = 0; i < referenceArrays.length; i++) {
          // Side effect: updates identityTable
	  stream.readAssociation(referenceArrays[i]);
	}
	if (lastDelta) {
          return (Map) stream.readObject();
        } else {
          return null;
        }
      } finally {
	stream.close();
      }
    } catch (IOException e) {
      logger.error("IOException reading " + (lastDelta ? "last " : " ") + "delta " + deltaNumber);
      throw e;
    }
  }

  /**
   * Get highest sequence numbers recorded on any medium
   */
  private int getHighestSequenceNumber() {
    int best = 0;
    for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
      PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
      SequenceNumbers[] availableNumbers = ppio.ppi.readSequenceNumbers("");
      for (int j = 0; j < availableNumbers.length; j++) {
        SequenceNumbers t = availableNumbers[j];
        best = Math.max(best, t.current);
      }
    }
    return best;
  }

  private void initSequenceNumbers() {
    int highest = getHighestSequenceNumber();
    sequenceNumbers = new SequenceNumbers(highest, highest, System.currentTimeMillis());
  }

  /**
   * Persist a List of Envelopes. First the objects in the envelope
   * are entered into the persistence identityTable. Then the objects
   * are serialized to an ObjectOutputStream preceded with their
   * reference id.  Other references to objects in the identityTable
   * are replaced with reference objects.
   */
//  private boolean nonEmpty(List subscriberStates) {
//    for (Iterator iter = subscriberStates.iterator(); iter.hasNext(); ) {
//      PersistenceSubscriberState subscriberState = (PersistenceSubscriberState) iter.next();
//      if (subscriberState.pendingEnvelopes.size() > 0) return true;
//      if (subscriberState.transactionEnvelopes.size() > 0) return true;
//    }
//    return false;
//  }

  private boolean isPersistable(Object o) {
    if (o instanceof NotPersistable) return false;
    if (o instanceof Persistable) {
      Persistable pbl = (Persistable) o;
      return pbl.isPersistable();
    }
    return true;
  }

  private void addEnvelope(Envelope e, PersistenceIdentity clientId)
    throws PersistenceException
  {
    if (logger.isDetailEnabled()) logger.detail(clientId + ": addEnvelope " + e);
    for (Iterator envelope = e.getAllTuples(); envelope.hasNext(); ) {
      addEnvelopeTuple((EnvelopeTuple) envelope.next(), clientId);
    }
  }

  private void addEnvelopeTuple(EnvelopeTuple tuple, PersistenceIdentity clientId)
    throws PersistenceException
  {
    if (logger.isDetailEnabled()) logger.detail(clientId + ": addEnvelopeTuple " + tuple);
    switch (tuple.getAction()) {
    case Envelope.BULK:
      Collection collection = (Collection) tuple.getObject();
      for (Iterator iter2 = collection.iterator(); iter2.hasNext(); ) {
        addObjectToPersist(iter2.next(), true, clientId);
      }
      break;
    case Envelope.ADD:
    case Envelope.CHANGE:
      addObjectToPersist(tuple.getObject(), true, clientId);
      break;
    case Envelope.REMOVE:
      addObjectToPersist(tuple.getObject(), false, clientId);
      break;
    }
  }

  private void addObjectToPersist(Object object, boolean newActive, PersistenceIdentity clientId)
    throws PersistenceException
  {
    if (!isPersistable(object)) return;
    PersistenceAssociation pAssoc = identityTable.findOrCreate(object);
    PersistenceIdentity oldClientId = pAssoc.getClientId();
    if (oldClientId == null) {
      pAssoc.setClientId(clientId);
    } else if (!oldClientId.equals(clientId)) {
      throw new PersistenceException(clientId + " not owner");
    }
    if (logger.isDetailEnabled())
      logger.detail(clientId + ": addObjectToPersist " + object + ", " + newActive);
    if (newActive) {
      pAssoc.setActive();
    } else {
      pAssoc.setInactive();
    }
    if (!full) addAssociationToPersist(pAssoc);
  }

  private void addAssociationToPersist(PersistenceAssociation pAssoc) {
    if (pAssoc.isMarked()) return; // Already scheduled to be written
    pAssoc.setMarked(true);
    addMarkedAssociation(pAssoc);
  }

  private void addMarkedAssociation(PersistenceAssociation pAssoc) {
    objectsThatMightGoAway.add(pAssoc.getObject());
    associationsToPersist.add(pAssoc);
  }

  private void clearMarks(Iterator iter) {
    while (iter.hasNext()) {
      PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
      pAssoc.setMarked(false);
    }
  }

  private void addExistingMarkedAssociations(Iterator iter) {
    while (iter.hasNext()) {
      PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
      if (pAssoc.isMarked()) {
        if (logger.isInfoEnabled()) {
          logger.info("Previously marked: " + pAssoc);
        }
        addMarkedAssociation(pAssoc);
      }
    }
  }

  /**
   * Select the next plugin to use for persistence. This is only
   * possible when the delta that is about to be generated will have
   * the full state. For each plugin, we keep track of the
   * nextPersistenceTime based on its persistenceInterval. We select
   * the plugin with the earliest nextPersistenceTime. If the
   * nextPersistenceTime of the selected plugin differs significantly
   * from now, the nextPersistenceTimes of all plugins are adjusted to
   * eliminate that difference.
   *
   * Persistence snapshots are taken with a frequency that is the
   * average of frequencies of all the plugins. This means that any
   * particular plugin will persistence with a frequency greater that
   * its specified frequency for a while, but then will be inactive
   * for a period while other plugins are used for an interval such
   * that its average frequency is close to its spcified frequency.
   *
   * As an example, consider two plugins A and B with periods of 10
   * and 40 respectively. The consolidation period for both is 10. The
   * average frequency is 1/10 + 1/40 or 5/40. This means the
   * inter-snapshot interval will be 8 and the faster plugin (A) will
   * go first since it "nextPersistenceTime will be 10 compared to 40.
   * So we have:
   * next(A) = 10
   * next(B) = 40
   * A: 8, 16, 24, ..., 80
   * next(A) = 10 + 10 * 10 = 110
   * next(B) = 40 (B goes next)
   * B: 88, 96, ..., 160
   * next(A) = 110 (A goes next)
   * next(B) 40 + 400 = 440
   * A: 168, 176, ..., 240
   * next(A) = 110 + 10 * 10 = 210 (A continues)
   * next(B) = 440
   * A: 248, 256, ..., 320
   * next(A) = 210 + 10 * 10 = 310 (A continues)
   * next(B) = 440
   * A: 328, 336, ..., 400
   * next(A) = 310 + 10 * 10 = 410 (A continues)
   * next(B) = 440
   * A: 408, 416, ..., 480
   * next(A) = 410 + 10 * 10 = 510
   * next(B) = 440 (B goes next)
   *
   * This patterm will continue with A running 4 times as much as B.
   * We observe that this leads to fairly long gaps between uses of
   * the lower-rate plugin since it uses up its share of the snapshots
   * fairly quickly and then waits a long time for its turn to come up
   * again. This suggests that it might be wise to reduce the
   * consolidation period of infrequent plugins.
   */

  private void selectNextPlugin() {
    PersistencePluginInfo best = null;
    for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
      PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
      if (best == null || ppio.nextPersistenceTime < best.nextPersistenceTime) {
        best = ppio;
      }
    }
    long adjustment = System.currentTimeMillis() - best.nextPersistenceTime;
    if (Math.abs(adjustment) > 10000L) {
      for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
        PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
        ppio.nextPersistenceTime += adjustment;
      }
    }
    currentPersistPluginInfo = best;
  }

  /**
   * Because PersistenceAssociations have WeakReference objects, the
   * actual object in the association may be garbage collected if
   * there are no other references to it. This list is used to hold a
   * reference to such objects to avoid an NPE when it is not known
   * that a reference exists. Only used during the first delta after
   * rehydration.
   */
  private ArrayList objectsThatMightGoAway = new ArrayList();

  private final static Object vmPersistLock = new Object();

  /**
   * Process the data from all clients. Envelopes and such are put
   * into the identityTable as PersistenceAssociations. The list of
   * everything left over is stored in a Map indexed by client id.
   */
  private Map getClientData() {
    Map data = new HashMap(clients.size());
    for (Iterator i = clients.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry) i.next();
      PersistenceClient client = (PersistenceClient) entry.getValue();
      PersistenceIdentity clientId = (PersistenceIdentity) entry.getKey();
      try {
        List clientData = client.getPersistenceData();
        List clientObjects = new ArrayList();
        if (logger.isDetailEnabled())
          logger.detail(clientId + " clientData: " + clientData);
        data.put(clientId, clientObjects);
        for (int j = 0, m = clientData.size(); j < m; j++) {
          Object o = clientData.get(j);
          if (o instanceof Envelope) {
            addEnvelope((Envelope) o, clientId);
          } else if (o instanceof EnvelopeTuple) {
            addEnvelopeTuple((EnvelopeTuple) o, clientId);
          } else {
            clientObjects.add(o);
          }
        }
        clientData.clear();     // Allow gc
      } catch (Exception e) {
        logger.error("Exception in getPersistenceData(" + client + ")", e);
      }
    }
    return data;
  }

  /**
   * End a persistence epoch by generating a persistence delta.
   */
  PersistenceObject persist(boolean returnBytes, boolean full) {
    if (isDummy && !returnBytes) {
      return null;
    }
    int deltaNumber = -1;
    long startCPU = 0L;
    //startCPU = CpuClock.cpuTimeMillis();
    long startTime = System.currentTimeMillis();
    if (logger.isInfoEnabled()) {
      logger.info("Persist started");
    }
    int bytesSerialized = 0;
    full |= returnBytes;        // Must be a full snapshot to return bytes
    Throwable failed = null;
    recomputeNextPersistenceTime = true;
    PersistenceObject result = null; // Return value if wanted
    synchronized (identityTable) {
      try {
	associationsToPersist.clear();
	objectsThatMightGoAway.clear();
	addExistingMarkedAssociations(identityTable.iterator());
	if (sequenceNumbers == null) {
	  initSequenceNumbers();
	}
	if (previousPersistFailed) {
	  full = true;          // Don't trust the deltas, (try to) do a full
	  previousPersistFailed = false;
	}
	// The following fixes an edge condition. The very first delta
	// (seqno = 0) is always a full delta whether we want it to be
	// or not because there are no previous ones. Setting full =
	// true removes any ambiguity about its fullness.
	if (sequenceNumbers.current == 0) full = true;
	// Every so often generate a full delta to consolidate and
	// prevent the number of deltas from increasing without bound.
	if (!full && currentPersistPluginInfo != null) {
	  // Check if its time to consolidate this plugin's snapshots
	  int consolidationPeriod = currentPersistPluginInfo.ppi.getConsolidationPeriod();
	  // nSnapshots is the number already generated
	  int nSnapshots = sequenceNumbers.current - sequenceNumbers.first;
	  if (nSnapshots + 1 >= consolidationPeriod) {
	    // The next snapshot needs to be full
	    full = true;        // Time for a full snapshot
	  }
	}
	if (full || currentPersistPluginInfo == null) {
	  if (currentPersistPluginInfo != null) {
	    // Cleanup the existing since the full replaces them.
	    // N.B., if there are multiple plugins, the cleanup will
	    // not actually occur until the next time this plugin is
	    // selected as the best and creates a full snapshot. This
	    // is because we will select a new plugin below.
	    currentPersistPluginInfo.cleanupSequenceNumbers =
	      new SequenceNumbers(sequenceNumbers.first + 1,
				  sequenceNumbers.current,
				  sequenceNumbers.timestamp);
	  }
	  sequenceNumbers.first = sequenceNumbers.current;
	  selectNextPlugin();
	}
	if (!currentPersistPluginInfo.ppi.checkOwnership()) {
	  return null;          // We are dead. Don't persist
	}
	if (sequenceNumbers.current == sequenceNumbers.first) full = true;
	this.full = full;    // Global full flag for duration of persist
	// Now gather everything to persist from our clients. Side
	// effect updates identityTable and if !full, associationsToPersist.
	Map clientData = getClientData();
	if (full) {
	  // If full dump, garbage collect unreferenced objects
          GC.gc();
	  for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
	    PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
	    if (!pAssoc.isMarked()) {
	      Object object = pAssoc.getObject();
	      // it is just barely possible that another gc might have
	      // collected some additional objects so do a final check
	      if (object != null) {
		// Prevent additional gc from scavenging the objects
		// we are committed to persisting
		addAssociationToPersist(pAssoc);
	      }
	    }
	  }
	}
	deltaNumber = beginTransaction();
	try {
	  if (currentOutput == null && !returnBytes) {
	    // Only doing dummy persistence
	  } else {
	    PersistenceOutputStream stream = new PersistenceOutputStream(logger);
	    PersistenceReference[][] referenceArrays;
	    if (logger.isDetailEnabled()) {
	      writeHistoryHeader();
	    }
	    stream.setIdentityTable(identityTable);
	    // One agent at a time to avoid inter-agent deadlock due to shared objects
	    try {
	      if (logger.isInfoEnabled()) {
		logger.info("Obtaining JVM persist lock");
	      }
	      synchronized (vmPersistLock) {
		if (logger.isInfoEnabled()) {
		  logger.info("Obtained JVM persist lock, serializing");
		}
		int nObjects = associationsToPersist.size();
		referenceArrays = new PersistenceReference[nObjects][];
		for (int i = 0; i < nObjects; i++) {
		  PersistenceAssociation pAssoc =
		    (PersistenceAssociation) associationsToPersist.get(i);
		  if (logger.isDetailEnabled()) {
		    logger.detail("Persisting " + pAssoc);
		  }
		  referenceArrays[i] = stream.writeAssociation(pAssoc);
		}
		stream.writeObject(clientData);
		bytesSerialized = stream.size();
		if (logger.isInfoEnabled()) {
		  logger.info(
			      "Serialized "+bytesSerialized+
			      " bytes to buffer, releasing lock");
		}
	      } // Ok to let other agents persist while we write out our data
	    } finally {
	      stream.close();
	    } // End of stream protection try-catch
	    if (returnBytes) {
	      int estimatedSize = (int)(1.2 * bytesSerialized);
	      LinkedByteOutputStream returnByteStream = new LinkedByteOutputStream(estimatedSize);
	      ObjectOutputStream returnOutput = new ObjectOutputStream(returnByteStream);
	      writeFinalOutput(returnOutput, referenceArrays, stream);
	      returnOutput.close();
	      result = new PersistenceObject("Persistence state "
					     + sequenceNumbers.current,
					     returnByteStream.toByteArray());
	      if (logger.isInfoEnabled()) {
		logger.info(
			    "Copied persistence snapshot to memory buffer"+
			    " for return to state-capture caller");
	      }
	    }
	    if (currentOutput != null) {
	      writeFinalOutput(currentOutput, referenceArrays, stream);
	      currentOutput.close();
	      if (logger.isInfoEnabled()) {
		logger.info(
			    "Wrote persistence snapshot to output stream");
	      }
	    }
	  } // End of non-dummy persistence
	  clearMarks(associationsToPersist.iterator());
          commitTransaction();
	  logger.printDot("P");
	  // Cleanup old deltas and archived snapshots. N.B. The
	  // cleanup is happening to the plugin that was just used.
	  // When there are several plugins, this is usually different
	  // from the plugin whose cleanupSequenceNumbers were set
	  // above. This cleanup has been pending while the various
	  // other plugins have been in use. This is _ok_! The
	  // snapshot we just took is invariably a full snapshot.
	  if (currentPersistPluginInfo.cleanupSequenceNumbers != null) {
            if (logger.isInfoEnabled()) {
	      logger.info(
                          "Consolidated deltas " +
                          currentPersistPluginInfo.cleanupSequenceNumbers);
            }
	    currentPersistPluginInfo.ppi.cleanupOldDeltas(currentPersistPluginInfo.cleanupSequenceNumbers);
	    currentPersistPluginInfo.ppi.cleanupArchive();
	    currentPersistPluginInfo.cleanupSequenceNumbers = null;
	  }
	} catch (Exception e) { // Transaction protection
	  rollbackTransaction();
	  if (logger.isErrorEnabled()) {
	    logger.error("Persist failed", e);
	  }
	  logger.printDot("X");
	  throw e;
        }
	objectsThatMightGoAway.clear();
      }
      catch (Exception e) {
        failed = e;
        logger.error("Error writing persistence snapshot", e);
      } finally {
        if (isDummy) {
          identityTable.clear(); // Perform garbage collection
        }
      }
      // set persist time to persist completion + epsilon
      previousPersistenceTime = System.currentTimeMillis();
      // Note currentPersistPluginInfo.nextPersistenceTime is _not_
      // relative to the current time; it is relative to the other
      // persistence plugins and is occasionally adjusted when it
      // drifts too far from real time.
      currentPersistPluginInfo.nextPersistenceTime +=
        currentPersistPluginInfo.ppi.getPersistenceInterval();
    }
    //long finishCPU = CpuClock.cpuTimeMillis();
    long finishCPU = 0l;
    long finishTime = System.currentTimeMillis();
    PersistenceMetricImpl metric =
      new PersistenceMetricImpl(formatDeltaNumber(deltaNumber),
                                startTime, finishTime, finishCPU - startCPU,
                                bytesSerialized, full, failed,
                                currentPersistPluginInfo.ppi);
    metricsService.addMetric(metric);
    if (logger.isInfoEnabled()) {
      logger.info(metric.toString());
    }
    return result;
  }

  private void writeFinalOutput(ObjectOutputStream s,
                                PersistenceReference[][] referenceArrays,
                                PersistenceOutputStream stream)
    throws IOException
  {
    s.writeInt(identityTable.getNextId());
    s.writeInt(referenceArrays.length);
    for (int i = 0; i < referenceArrays.length; i++) {
      s.writeObject(referenceArrays[i]);
    }
    stream.writeBytes(s);
    s.flush();
  }

  /** The format of timestamps in the log */
  private static DateFormat logTimeFormat =
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

  private static DecimalFormat deltaFormat = new DecimalFormat("_00000");

  public static String formatDeltaNumber(int deltaNumber) {
    return deltaFormat.format(deltaNumber);
  }

  private void writeHistoryHeader() {
    if (logger.isDetailEnabled()) {
      logger.detail(logTimeFormat.format(new Date(System.currentTimeMillis())));
    }
  }

  void printIdentityTable(String id) {
    logger.detail("IdentityTable begins");
    for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
      PersistenceAssociation pAssoc =
        (PersistenceAssociation) iter.next();
      logger.detail(id + pAssoc);
    }
    logger.detail("IdentityTable ends");
  }

  public Logger getLogger() {
    return logger;
  }

  static String getObjectName(Object o) {
    return o.getClass().getName() + "@" + System.identityHashCode(o);
  }

  private int beginTransaction() throws IOException {
    int deltaNumber = sequenceNumbers.current;
    OutputStream os = currentPersistPluginInfo.ppi.openOutputStream(deltaNumber, full);
    if (os != null) {
      DataProtectionService dataProtectionService = getDataProtectionService();
      if (dataProtectionService != null) {
        PersistenceKeyEnvelope keyEnvelope =
          new PersistenceKeyEnvelope(currentPersistPluginInfo.ppi, deltaNumber);
        os = dataProtectionService.getOutputStream(keyEnvelope, os);
      }
      currentOutput = new ObjectOutputStream(os);
    } else {
      currentOutput = null;
    }
    return deltaNumber;
  }

  private void rollbackTransaction() {
    currentPersistPluginInfo.ppi.abortOutputStream(sequenceNumbers);
    currentOutput = null;
  }

  private void commitTransaction() throws PersistenceException {
    currentPersistPluginInfo.ppi.lockOwnership();
    sequenceNumbers.current += 1;
    currentPersistPluginInfo.ppi.finishOutputStream(sequenceNumbers, full);
    currentOutput = null;
    currentPersistPluginInfo.ppi.unlockOwnership();
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    return currentPersistPluginInfo.ppi.getDatabaseConnection(locker);
  }

  public void releaseDatabaseConnection(Object locker) {
    currentPersistPluginInfo.ppi.releaseDatabaseConnection(locker);
  }

  private class PersistenceServiceImpl implements PersistenceService {
    PersistenceIdentity clientId;

    PersistenceServiceImpl(PersistenceIdentity clientId) {
      this.clientId = clientId;
    }

    public RehydrationData getRehydrationData() {
      return PersistenceServiceComponent.this.getRehydrationData(clientId);
    }
  }
  private class PersistenceServiceForAgentImpl
    extends PersistenceServiceImpl
    implements PersistenceServiceForAgent
  {
    PersistenceServiceForAgentImpl(PersistenceIdentity clientId) {
      super(clientId);
    }

    public void rehydrate(PersistenceObject pObject) {
      PersistenceServiceComponent.this.rehydrate(pObject);
    }

    public void suspend() {
    }
  }

  private class PersistenceServiceForBlackboardImpl
    extends PersistenceServiceImpl
    implements PersistenceServiceForBlackboard
  {
    PersistenceServiceForBlackboardImpl(PersistenceIdentity clientId) {
      super(clientId);
    }

    public PersistenceObject persist(
      boolean returnBytes,
      boolean full) {
      return PersistenceServiceComponent.this.persist(
        returnBytes,
        full);
    }
    public java.sql.Connection getDatabaseConnection(Object locker) {
      return PersistenceServiceComponent.this.getDatabaseConnection(locker);
    }
    public void releaseDatabaseConnection(Object locker) {
      PersistenceServiceComponent.this.releaseDatabaseConnection(locker);
    }
    public boolean isDummyPersistence() {
      return PersistenceServiceComponent.this.isDummyPersistence();
    }
    public long getPersistenceTime() {
      return PersistenceServiceComponent.this.getPersistenceTime();
    }
  }

  ServiceProvider serviceProvider =
    new ServiceProvider() {
      public Object getService(ServiceBroker sb, Object requestor, Class cls) {
        if (cls == PersistenceControlService.class) {
          return new PersistenceControlServiceImpl();
        }
        if (cls == PersistenceMetricsService.class) {
          return metricsService;
        }
        if (cls == PersistenceService.class ||
            cls == PersistenceServiceForBlackboard.class ||
            cls == PersistenceServiceForAgent.class) {
          if (requestor instanceof PersistenceClient) {
            PersistenceClient client = (PersistenceClient) requestor;
            PersistenceIdentity clientId = client.getPersistenceIdentity();
            clients.put(clientId, client);
            if (cls == PersistenceService.class) {
              return new PersistenceServiceImpl(clientId);
            } else if (cls == PersistenceServiceForBlackboard.class) {
              return new PersistenceServiceForBlackboardImpl(clientId);
            } else {
              return new PersistenceServiceForAgentImpl(clientId);
            }
          } else {
            throw new IllegalArgumentException
              ("PersistenceService requestor must be a PersistenceClient");
          }
        }
        throw new IllegalArgumentException("Unknown service class");
      }

      public void releaseService(ServiceBroker sb, Object requestor, Class cls, Object svc) {
        if (cls == PersistenceControlService.class) {
          return;
        }
        if (cls == PersistenceMetricsService.class) {
          return;
        }
        if (cls == PersistenceServiceForBlackboard.class ||
            cls == PersistenceService.class ||
            cls == PersistenceServiceForAgent.class) {
          if (svc instanceof PersistenceServiceImpl) {
            PersistenceServiceImpl impl = (PersistenceServiceImpl) svc;
            clients.remove(impl.clientId);
          }
          return;
        }
        throw new IllegalArgumentException("Unknown service class");
      }
    };

  // More Persistence implementation
  private void registerServices(ServiceBroker sb) {
    sb.addService(PersistenceMetricsService.class, serviceProvider);
    sb.addService(PersistenceControlService.class, serviceProvider);
    sb.addService(PersistenceService.class, serviceProvider);
    sb.addService(PersistenceServiceForBlackboard.class, serviceProvider);
    sb.addService(PersistenceServiceForAgent.class, serviceProvider);
  }

  private void unregisterServices(ServiceBroker sb) {
    sb.revokeService(PersistenceControlService.class, serviceProvider);
    sb.revokeService(PersistenceMetricsService.class, serviceProvider);
    sb.revokeService(PersistenceService.class, serviceProvider);
    sb.revokeService(PersistenceServiceForBlackboard.class, serviceProvider);
    sb.revokeService(PersistenceServiceForAgent.class, serviceProvider);
  }

  @Override
public String toString() {
    return "Persist(" + getAgentName() + ")";
  }
}
