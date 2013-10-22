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

package org.cougaar.planning.plugin.asset;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.Constants;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;
import org.cougaar.planning.ldm.asset.LocationSchedulePGImpl;
import org.cougaar.planning.ldm.asset.NewClusterPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewLocationSchedulePG;
import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.planning.ldm.asset.NewRelationshipPG;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.RelationshipBG;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.LatLonPointImpl;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.planning.ldm.plan.LocationScheduleElementImpl;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.service.AssetInitializerService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.Reflect;
import org.cougaar.util.StateModelException;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * Generic plugin to create a local asset and the Report tasks
 * associated with all the local asset's relationships. 
 *
 * Local asset must have ClusterPG and 
 * RelationshipPG, Presumption is that the 'other' assets in all the 
 * relationships have both Cluster and Relationship PGs.
 * Currently assumes that each Agent has exactly 1 local asset.
 *
 * Extensions of this class load this information from specific media such as files.
 * Now supports adding additional relationships via plugin parameters. Each
 * parameter represents 1 relationship between the local asset and some 'other' 
 * asset. Parameter must completely identify both the other asset and the 
 * relationship. Building the other asset requires knowing the asset's message 
 * address (ClusterPG), item identification (ItemIdentificationPG), and type 
 * identification (TypeIdentificationPG). Building the relationship requires 
 * knowing the role and optionally the start and end times.
 * 
 * Parameter format -
 * Relationship:MessageAddress=<ClusterPG.getMessageAddress of other asset>,\
 * ItemIdentification=<ItemIdentificationPG.getItemIdentification of other asset>,\
 * TypeIdentification=<TypeIdentificationPG.getTypeIdentification of other asset>,\
 * Role=<Role performed by the local asset>\
 * StartTime=<Date relationship starts>\
 * EndTime=<Date relations ends>
 * 
 * Sample parameter -
 * Relationship:MessageAddress=DISCOM-1-AD,ItemIdentification=DISCOM-1-AD,TypeIdentification=UTC/NBTTT,Role=Superior,StartTime=01/01/2001 12:00 am,EndTime= 01/01/2010 11:59 pm
 *
 * StartTime and EndTime specifications are optional. Will default to 
 * getDefaultStartTime()/getDefaultEndTime() if not included
 **/
public class AssetDataPlugin extends SimplePlugin {
  // See bug 3869 -- should be ComponentPlugin, but not trivial

  public static final String SELF = ("Self");

  protected static TrivialTimeSpan ETERNITY = 
    new TrivialTimeSpan(TimeSpan.MIN_VALUE,
                        TimeSpan.MAX_VALUE);

  protected DateFormat myDateFormat = DateFormat.getInstance(); 

  protected String myAssetClassName = null;
  protected List myRelationships = new ArrayList();
  protected Map myOtherAssets = new HashMap();
  protected Asset myLocalAsset = null;
  protected PlanningFactory myPlanningFactory;
  protected AssetInitializerService myAssetInitializerService;
  protected LoggingService myLogger;

  public void load(Object object) throws StateModelException {
    super.load(object);
    myPlanningFactory = (PlanningFactory) getFactory("planning");
    if (myPlanningFactory == null) {
      throw new RuntimeException("Missing \"planning\" factory");
    }

    myLogger = (LoggingService)
      getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    if (myLogger == null) {
      myLogger = LoggingService.NULL;
    }
    myLogger = 
      LoggingServiceWithPrefix.add(
          myLogger,
          getAgentIdentifier() + ": ");

    initAssets();
  }

  public void execute() {
  }


  public long getDefaultStartTime() {
    return TimeSpan.MIN_VALUE;
  }

  public long getDefaultEndTime() {
    return TimeSpan.MAX_VALUE;
  }


  public void setAssetInitializerService(AssetInitializerService ais) {
    myAssetInitializerService= ais;
  }
                       
  protected void setupSubscriptions() {
    getSubscriber().setShouldBePersisted(false);

    /*
    if (!didRehydrate()) {
      processAssets();	// Objects should already exist after rehydration
      } */
  }


  protected void initAssets() {
    try {
      openTransaction();

      // Look to see whether we've already created the local asset
      if (didRehydrate()) {
	Collection queryCollection = getBlackboardService().query(new UnaryPredicate() {
	  public boolean execute(Object o) {
	    return (o instanceof LocalAssetInfo);
	  }
	});
	
	if (!queryCollection.isEmpty()) {
	  final Object key = 
	    ((LocalAssetInfo) queryCollection.iterator().next()).getKey();
	  String className = 
	    ((LocalAssetInfo) queryCollection.iterator().next()).getClassName();
	  if (queryCollection.size() > 1) {
	    myLogger.warn("Found " + queryCollection.size() + 
			  " keys for the local Asset." +
			  " Using - " + key);
	  }
	  
	  Collection assetCollection = getBlackboardService().query(new UnaryPredicate() {
	    public boolean execute(Object o) {
	      return ((o instanceof Asset) &&
		    (((Asset) o).getKey().equals(key)));
	    }
	  });
	  
	  if (!assetCollection.isEmpty()) {
	    myLocalAsset = (Asset) assetCollection.iterator().next();
	    myAssetClassName = className;
	  } else {
	    myLogger.warn("Did not find local asset for key " + key);
	  }
	}
      }
      
      if (myLocalAsset == null) {
        processAssets();
      } 
    } catch (Exception e) {
      synchronized (System.err) {
	myLogger.error(this+" caught "+e);
	e.printStackTrace();
      }
    } finally {
      closeTransactionDontReset();
    }
  }

  /**
   * Parses the prototype-ini file and in the process sets up
   * the relationships with pairs of "relationship"/"asset
   */

  protected void processAssets() {
    try {
      String aId = getAgentIdentifier().toString();
      
      if (myAssetInitializerService == null) {
	myLogger.fatal("AssetInitializerService is null." +
		       " Unable to create local asset.");
	return;
      }
      AssetDataReader assetDataReader = getAssetDataReader();
      assetDataReader.readAsset(aId, new AssetDataCallbackImpl());

      // Process relationships specified as plugin arguments 
      addParamRelationships();      

      if (myLogger.isDebugEnabled()) {
	myLogger.debug("property groups for local asset: ");
        Vector all = myLocalAsset.fetchAllProperties();
        try {
          for (Iterator i = all.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (!(o instanceof PropertyGroup)) continue;
            PropertyGroup pg = (PropertyGroup) o;
            Class pgc = pg.getClass();
            myLogger.debug(pgc.getName());
            Method[] methods = pgc.getMethods();
            for (int j = 0; j < methods.length; j++) {
              Method method = methods[j];
              if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                Object value = methods[j].invoke(pg, new Object[0]);
                myLogger.debug("  " + method.getName() + " = " + value);
              }
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      getBlackboardService().publishAdd(myLocalAsset);
      LocalAssetInfo localAssetInfo = new LocalAssetInfo(myLocalAsset, 
							 myAssetClassName);
      getBlackboardService().publishAdd(localAssetInfo);

      // Put the assets for this agent into array
      for (Iterator iterator = myRelationships.iterator(); 
             iterator.hasNext();) {
        Relationship relationship = (Relationship) iterator.next();
        report(relationship);
      } 
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void createMyLocalAsset(String assetClassName) {
    myAssetClassName = assetClassName;
    myLocalAsset = myPlanningFactory.createAsset(myAssetClassName);
    // set up this asset's available schedule
    NewSchedule availsched = 
      myPlanningFactory.newSimpleSchedule(getDefaultStartTime(), 
					  getDefaultEndTime());
    // set the available schedule
    ((NewRoleSchedule)myLocalAsset.getRoleSchedule()).setAvailableSchedule(availsched);
            
    // initialize the relationship info
    NewRelationshipPG pg = 
      (NewRelationshipPG) myLocalAsset.getRelationshipPG();

    RelationshipBG bg = new RelationshipBG();
    // init sets the relationship schedule on the PG and sets the pointer from pg to bg
    bg.init(pg, (HasRelationships) myLocalAsset);

    // this asset is local to the agent
    pg.setLocal(true);
  }

  protected void report(Relationship relationship) {
    Asset sendTo = 
      (((Asset) relationship.getA()).getKey().equals(myLocalAsset.getKey())) ?
      myPlanningFactory.cloneInstance((Asset) relationship.getB()) :
      myPlanningFactory.cloneInstance((Asset) relationship.getA());
    
    Asset localClone = myPlanningFactory.cloneInstance(myLocalAsset);

    List roles = new ArrayList(1);
    Role role = 
      (((Asset) relationship.getA()).getKey().equals(myLocalAsset.getKey())) ?
      relationship.getRoleA() : relationship.getRoleB();
    roles.add(role);
    
    NewTask reportTask =
      createReportTask(localClone, sendTo, roles, 
                       relationship.getStartTime(),
                       relationship.getEndTime());
    getBlackboardService().publishAdd(reportTask);
  
  }

  protected NewPropertyGroup createPropertyGroup(String propertyName) 
       throws Exception {
    return (NewPropertyGroup) myPlanningFactory.createPropertyGroup(propertyName);
  }

  protected void addPropertyToAsset(PropertyGroup pg) {
    myLocalAsset.addOtherPropertyGroup(pg);
  }

  protected void setLocationSchedule(String latStr, String lonStr) {
    Latitude lat = Latitude.newLatitude(latStr);

    Longitude lon = Longitude.newLongitude(lonStr);

    LatLonPointImpl loc = new LatLonPointImpl(lat, lon);

    // create LocationScheduleElementImpl
    LocationScheduleElement locSchedElem =
      new LocationScheduleElementImpl(
          TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE, loc);

    // add to schedule
    LocationSchedulePG locSchedPG = myLocalAsset.getLocationSchedulePG();
    if (locSchedPG == null) {
      locSchedPG = new LocationSchedulePGImpl();
      myLocalAsset.setLocationSchedulePG(locSchedPG);
    }
    Schedule locSched = locSchedPG.getSchedule();
    if (locSched == null) {
      locSched = new ScheduleImpl();
      ((NewLocationSchedulePG)locSchedPG).setSchedule(locSched);
    }
    locSched.add(locSchedElem);
  }

  protected void addRelationship(String typeId, String itemId,
                                 String otherAgentId, String roleName,
                                 long start, long end) {
    Asset otherAsset =
      getAsset(myAssetClassName, itemId, typeId, otherAgentId);
    Relationship relationship = 
      myPlanningFactory.newRelationship(Role.getRole(roleName),
                                   (HasRelationships) myLocalAsset,
                                   (HasRelationships) otherAsset,
                                   start,
                                   end);
    myRelationships.add(relationship);
 }

  //create the Report task to be sent to myself which will result in an asset 
  //transfer of the copyOfMyself being sent to the agent I am supporting.
  protected NewTask createReportTask(Asset reportingAsset,
                                     Asset sendto,
                                     Collection roles,
                                     long startTime,
                                     long endTime) {
    NewTask reportTask = myPlanningFactory.newTask();
    reportTask.setDirectObject(reportingAsset);

    Vector prepPhrases = new Vector(2);
    NewPrepositionalPhrase newpp = myPlanningFactory.newPrepositionalPhrase();
    newpp.setPreposition(Constants.Preposition.FOR);
    newpp.setIndirectObject(sendto);
    prepPhrases.add(newpp);

    newpp = myPlanningFactory.newPrepositionalPhrase();
    newpp.setPreposition(Constants.Preposition.AS);
    newpp.setIndirectObject(roles);
    prepPhrases.add(newpp);
    reportTask.setPrepositionalPhrases(prepPhrases.elements());

    reportTask.setPlan(myPlanningFactory.getRealityPlan());
    reportTask.setSource(getAgentIdentifier());

    AspectValue startTAV = 
      TimeAspectValue.create(AspectType.START_TIME, startTime);
    ScoringFunction startScoreFunc = 
      ScoringFunction.createStrictlyAtValue(startTAV);
    Preference startPreference = 
      myPlanningFactory.newPreference(AspectType.START_TIME, startScoreFunc);

    AspectValue endTAV = 
      TimeAspectValue.create(AspectType.END_TIME, endTime);
    ScoringFunction endScoreFunc = 
      ScoringFunction.createStrictlyAtValue(endTAV);    
    Preference endPreference = 
      myPlanningFactory.newPreference(AspectType.END_TIME, endScoreFunc );

    Vector preferenceVector = new Vector(2);
    preferenceVector.addElement(startPreference);
    preferenceVector.addElement(endPreference);

    reportTask.setPreferences(preferenceVector.elements());
    
    reportTask.setVerb(getReportVerb(roles)); 

    return reportTask;
  }
  
  protected Verb getReportVerb(Collection roles) {
    return Constants.Verb.Report;
  }

  protected Asset getAsset(String className, String itemIdentification,
                           String typeIdentification, String agentName) {

    Asset asset = myPlanningFactory.createAsset(className);
  	
    ((NewTypeIdentificationPG)asset.getTypeIdentificationPG()).setTypeIdentification(typeIdentification);

    NewItemIdentificationPG itemIdProp = 
      (NewItemIdentificationPG)asset.getItemIdentificationPG();
    itemIdProp.setItemIdentification(itemIdentification);
    itemIdProp.setNomenclature(itemIdentification);
    
    NewClusterPG cpg = (NewClusterPG)asset.getClusterPG();
    cpg.setMessageAddress(MessageAddress.getMessageAddress(agentName));
    
    Asset saved = (Asset) myOtherAssets.get(asset.getKey());
    if (saved == null) {
      myOtherAssets.put(asset.getKey(), asset);
      saved = asset;
    }
    return saved;
  }

  public long parseDate(String dateString) throws ParseException {
    return myDateFormat.parse(dateString).getTime();
  }

  protected Object parseExpr(String type, String arg) {
    int i;

    type = type.trim();
    arg = arg.trim();

    if ((i = type.indexOf("<")) >= 0) {
      int j = type.lastIndexOf(">");
      String ctype = type.substring(0, i).trim();
      String etype = type.substring(i + 1, j).trim();
      Collection c = null;
      if (ctype.equals("Collection") || ctype.equals("List")) {
        c = new ArrayList();
      } else {
        throw new RuntimeException("Unparsable collection type: "+type);
      }

      String[] l = org.cougaar.util.CSVUtility.parse(arg);
      for (int ix = 0; ix < l.length; ix++) {
        c.add(parseExpr(etype, l[ix]));
      }
      return c;
    } else if ((i = type.indexOf("/")) >= 0) {
      String m = type.substring(0, i).trim();
      String mt = type.substring(i + 1).trim();
      double qty = Double.valueOf(arg).doubleValue();
      return createMeasureObject(m, qty, mt);
    } else {
      Class cl = findClass(type);

      try {
        if (cl.isInterface()) {
          if (TimeSpan.class.isAssignableFrom(cl)) {
            String[] svs =  org.cougaar.util.CSVUtility.parse(arg);
            long startTime = getDefaultStartTime();
            long endTime = getDefaultEndTime();
            for (int ix = 0; ix < svs.length; ix++) {
              String ss = svs[ix];

              int eq = ss.indexOf('=');
              if (eq < 0) {
                throw new IllegalArgumentException("Missing \"=\" in " + ss);
              }
              String slotname = ss.substring(0, eq).trim();
              String vspec = ss.substring(eq + 1).trim();
              try {
                long time  = parseDate(vspec);
                if (slotname.equals("startTime")) {
                  startTime = time;
                } else if (slotname.equals("endTime")) {
                  endTime = time;
                }
              } catch (ParseException pe) {
              }
            }
            return new TrivialTimeSpan(startTime, endTime);
          } else {
            // interface means try the COF
            return parseWithCOF(cl, arg);
          }
        } else {
          Class ac = getArgClass(cl);
          Object[] args = {arg};
          Constructor cons = Reflect.getConstructor(ac,stringArgSpec);
          if (cons != null) {
            // found a constructor - use it
            return cons.newInstance(args);
          } else {
            Method fm = Reflect.getMethod(ac, "create", stringArgSpec);
            if (fm == null) {
              String n = ac.getName();
              // remove the package prefix
              n = n.substring(n.lastIndexOf('.') + 1).trim();
              fm = Reflect.getMethod(ac, "create"+n, stringArgSpec);
              if (fm == null) 
                fm = Reflect.getMethod(ac, "get"+n, stringArgSpec);
            }
            if (fm == null) {
              throw new RuntimeException("Couldn't figure out how to construct "+type);
            }
            return fm.invoke(null,args);
          }
        }
      } catch (Exception e) {
	myLogger.error("Exception constructing "+type+" from \""+arg+"\":");
        e.printStackTrace();
        throw new RuntimeException("Construction problem "+e);
      }
    }
  }
  
  protected AssetDataReader getAssetDataReader() {
    return myAssetInitializerService.getAssetDataReader();
  }

  private static Class[] stringArgSpec = {String.class};

  private static Class[][] argClasses = {{Integer.TYPE, Integer.class},
                                         {Double.TYPE, Double.class},
                                         {Boolean.TYPE, Boolean.class},
                                         {Float.TYPE, Float.class},
                                         {Long.TYPE, Long.class},
                                         {Short.TYPE, Short.class},
                                         {Byte.TYPE, Byte.class},
                                         {Character.TYPE, Character.class}};

                                     
  private static Class getArgClass(Class c) {
    if (! c.isPrimitive()) return c;
    for (int i = 0; i < argClasses.length; i++) {
      if (c == argClasses[i][0])
        return argClasses[i][1];
    }
    throw new IllegalArgumentException("Class "+c+" is an unknown primitive.");
  }

  protected String getType(String type) {
    int i;
    if ((i = type.indexOf("<")) > -1) { // deal with collections 
//      int j = type.lastIndexOf(">");
      return getType(type.substring(0, i).trim()); // deal with measures
    } else if ((i = type.indexOf("/")) > -1) {
      return getType(type.substring(0, i).trim());
    } else {
      return type;
    }
  }
    

  protected Object parseWithCOF(Class cl, String val) {
    String name = cl.getName();
    int dot = name.lastIndexOf('.');
    if (dot != -1) 
      name = name.substring(dot + 1).trim();

    try {
      // lookup method on ldmf
      Object o = callFactoryMethod(name);

      String[] svs = org.cougaar.util.CSVUtility.parse(val);
      // svs should be a set of strings like "slot=value" or "slot=type value"
      for (int i = 0; i < svs.length; i++) {
        String ss = svs[i];
        if (myLogger.isDebugEnabled()) {
          myLogger.debug("Processing \"" + ss + "\"");
        }

        int eq = ss.indexOf('=');
        if (eq < 0) {
          throw new IllegalArgumentException("Missing \"=\" in " + ss);
        }
        String slotname = ss.substring(0, eq).trim();
        String vspec = ss.substring(eq + 1).trim();
        
        int spi = vspec.indexOf(' ');
        Object v;
        if (spi == -1) {
          v = vspec;
        } else {
          String st = vspec.substring(0, spi).trim();
          String sv = vspec.substring(spi + 1).trim();
          v = parseExpr(st, sv);
        }
        callSetMethod(o, slotname, v);
      }
      return o;
    } catch (Exception e) {
      myLogger.error("Exception parsing " + val, e);
      return null;
    }
  }

  private Object callFactoryMethod(String ifcname) {
    // look up a zero-arg factory method in the ldmf
    String newname = "new"+ifcname;
    
    DomainService domainService = 
      (DomainService) getDelegate().getServiceBroker().getService(this, DomainService.class, null);

    if (domainService == null) {
      throw new RuntimeException ("Unable to get DomainService");
    }

    List factories = domainService.getFactories();
    for (Iterator i = factories.iterator(); i.hasNext(); ) {
      try {
        Class ldmfc = i.next().getClass();
        Method fm = ldmfc.getMethod(newname, nullClassList);
	// Documentation says first arg is ignored for static methods,
	// which it is ?always?
        return fm.invoke(myPlanningFactory, nullArgList);
      } catch (NoSuchMethodException nsme) {
        // This is okay - just try the next factory
      } catch (Exception e) { 
	myLogger.error("Problem loading Domain Factory", e);
      }
    }
      
    throw new RuntimeException ("Couldn't find a factory method for "+ifcname);
  }

  private static final Class nullClassList[] = {};
  private static final Object nullArgList[] = {};

  private void callSetMethod(Object o, String slotname, Object value) {
    Class oc = o.getClass();
    String setname = "set"+slotname;
    Class vc = value.getClass();

    try {
      Method ms[] = Reflect.getMethods(oc);
      for (int i = 0; i<ms.length; i++) {
        Method m = ms[i];
        if (setname.equals(m.getName())) {
          Class mps[] = m.getParameterTypes();
          if (mps.length == 1 &&
              mps[0].isAssignableFrom(vc)) {
            Object args[] = {value};
            m.invoke(o, args);
            return;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Couldn't find set"+slotname+" for "+o+", value "+value + ". Typo in slot name: " + slotname + "?");
    }

    throw new RuntimeException("Couldn't find set"+slotname+" for "+o+", value "+value);
  }

  /**
   * Returns the integer value for the appropriate
   * unitOfMeasure field in the measureClass
   */
  protected int getMeasureUnit(String measureClass, String unitOfMeasure) {
    try {
      String fullClassName = "org.cougaar.planning.ldm.measure." + measureClass;
      Field f = Class.forName(fullClassName).getField(unitOfMeasure);
      return f.getInt(null);
    } catch (Exception e) {
      myLogger.error("Exception " + e + 
		     ": for measure unit: " + unitOfMeasure);
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Returns a measure object which is an instance of className and has
   * a quantity of unitOfMeasure
   */
  protected Object createMeasureObject(String className, double quantity, String unitOfMeasure) {
    try {
      Class classObj = Class.forName("org.cougaar.planning.ldm.measure." + className);
      String methodName = "new" + className;
      Class parameters[] = {double.class, int.class};
      Method meth = classObj.getMethod(methodName, parameters);
      Object arguments[] = {new Double(quantity), new Integer(getMeasureUnit(className, unitOfMeasure))};
      return meth.invoke(classObj, arguments); // static method call
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static HashMap classes;
  protected static Collection packages = new ArrayList();

  static {
    // initialize packages:
    packages.add("org.cougaar.planning.ldm.measure");
    packages.add("org.cougaar.planning.ldm.plan");
    packages.add("org.cougaar.planning.ldm.asset");
    packages.add("org.cougaar.planning.ldm.oplan");

    packages.add("java.lang");  // extras for fallthrough
    packages.add("java.util");

    // initialize the classmap with some common ones
    classes = new HashMap();

    classes.put("MessageAddress", MessageAddress.class);

    // precache some builtins
    classes.put("long", Long.TYPE);
    classes.put("int", Integer.TYPE);
    classes.put("integer", Integer.TYPE);
    classes.put("boolean", Boolean.TYPE);
    classes.put("float", Float.TYPE);
    classes.put("double", Double.TYPE);
    // and some java.lang
    classes.put("Double", Double.class);
    classes.put("String", String.class);
    classes.put("Integer", Integer.class);
    // and some java.util
    classes.put("Collection", Collection.class);
    classes.put("List", List.class);
    classes.put("TimeSpan", TimeSpan.class);
                                           
    // COUGAAR-specific stuff will be looked for
  }

  private Class findClass(String name) {
    synchronized (classes) {
      Class c = (Class) classes.get(name);
      // try the cache
      if (c != null) return c;

      for (Iterator i = packages.iterator(); i.hasNext();) {
        String pkg = (String) i.next();
        try {                   // Oh so ugly!
          c = Class.forName(pkg+"."+name);
          if (c != null) {        // silly
            classes.put(name, c);
            return c;
          }
        } catch (ClassNotFoundException e) {}; // sigh
      }
      throw new RuntimeException("Could not find a class for '"+name+"'.");
    }
  }

  /**
   * Creates and calls the appropriate "setter" method for the property
   * which is of type className.
   */
  protected void callSetter(NewPropertyGroup pg, 
			    String setterName, String type, 
			    Object []arguments) {
    Class parameters[] = new Class[1];
    
    try {
      parameters[0] = findClass(type);
      Method meth = findMethod(pg.getClass(), setterName, parameters);
      if (meth == null) {
        StringBuffer msg = new StringBuffer();
        msg.append("Method not found: ");
        msg.append(setterName);
        msg.append("(");
        for (int i = 0; i < parameters.length; i++) {
          if (i > 0) msg.append(", ");
          msg.append(parameters[i].getName());
        }
        msg.append(")");
        myLogger.error(msg.toString());
        return;
      }
      meth.invoke(pg, arguments);
    } catch (Exception e) {
      myLogger.error("Exception: callSetter("+pg.getClass().getName()+", "+setterName+", "+type+", "+arguments+" : " + e);
      e.printStackTrace();
    }
  }

  private static Method findMethod(Class c, String name, Class params[]) {
    Method ms[] = Reflect.getMethods(c);
    int pl = params.length;
    for (int i = 0; i < ms.length; i++) {
      Method m = ms[i];
      if (name.equals(m.getName())) {
        Class mps[] = m.getParameterTypes();
        if (mps.length == pl) {
          int j;
          for (j = 0; j < pl; j++) {
            if (!(mps[j].isAssignableFrom(params[j]))) 
              break;            // j loop
          }
          if (j==pl)            // all passed
            return m;
        }
      }
    }
    return null;
  }

  public static final String ATTRIBUTE_DELIMITER = "=";
  public static final String VALUE_DELIMITER = ",";
  public static final String KEYWORD_DELIMITER = ":";

  protected void addParamRelationships() {
    Collection params = getDelegate().getParameters();    

    for (Iterator iterator = params.iterator();
	 iterator.hasNext();) {
      String param = (String) iterator.next();
      
      addParamRelationship(param);
    }
  }

  public static final String RELATIONSHIP_KEY = "Relationship";

  protected void addParamRelationship(String param) {
    StringTokenizer tokenizer = 
      new StringTokenizer(param, KEYWORD_DELIMITER + ATTRIBUTE_DELIMITER + VALUE_DELIMITER, true);

    if (myLogger.isDebugEnabled()) {
      myLogger.debug("addParamRelationship: param = " + param + 
		     " - tokenizer count = " + tokenizer.countTokens());
    }

    String nextAttribute = getToken(tokenizer, KEYWORD_DELIMITER);

    if (!nextAttribute.equals(RELATIONSHIP_KEY)) {
      if (myLogger.isDebugEnabled()) {
	myLogger.debug("addParamRelationship: skipping param - " + param);
      }
      return;
    } 
    
    String messageAddress = null;
    String typeIdentification = null;
    String itemIdentification=null;
    String role = "";
    long start = getDefaultStartTime();
    long end = getDefaultEndTime();
    
    while (tokenizer.hasMoreTokens()) {

      nextAttribute = getToken(tokenizer, ATTRIBUTE_DELIMITER);
      
      if (myLogger.isDebugEnabled()) {
	myLogger.debug("addParamRelelationship: nextAttribute " + nextAttribute);
      }

      if (nextAttribute == null) {
	myLogger.error("Unrecognized attribute " + 
		       nextAttribute + " in " + param + 
		       ". No relationship added.");
	return;
      } else if (nextAttribute.equals("MessageAddress")) {
	messageAddress = getToken(tokenizer, VALUE_DELIMITER);
      } else if (nextAttribute.equals("ItemIdentification")) {
	itemIdentification = getToken(tokenizer, VALUE_DELIMITER);
      } else if (nextAttribute.equals("TypeIdentification")) { 
	typeIdentification = getToken(tokenizer, VALUE_DELIMITER);
      } else if (nextAttribute.equals("Role")) { 
	role =getToken(tokenizer, VALUE_DELIMITER);
      } else if (nextAttribute.equals("StartTime")) { 
	String timeString = getToken(tokenizer, VALUE_DELIMITER);
	if (myLogger.isDebugEnabled())
	  myLogger.debug("trying to parse timeString: " + timeString + " from " + param);
	try {
	  if (timeString != null)
	    start = parseDate(timeString);
	  else
	    start = getDefaultStartTime();
	} catch (java.text.ParseException pe) {
	  myLogger.error("Unable to parse start date from " + param +  
			 ". Start time defaulting to " + 
			 getDefaultStartTime());
	}
      } else if (nextAttribute.equals("EndTime")) { 
	String timeString = getToken(tokenizer, VALUE_DELIMITER);
	try {
	  if (timeString != null)
	    end = parseDate(timeString);
	  else
	    end = getDefaultEndTime();
	} catch (java.text.ParseException pe) {
	  myLogger.error("Unable to parse end date from " + param +  
			 ". Start time defaulting to " + 
			 getDefaultEndTime());
	}
      } else {
	myLogger.error("Unrecognized attribute " + 
		       nextAttribute + " in " + param + 
		       ". No relationship added.");
	return;
      }
    }

    if ((messageAddress == null) ||
	(typeIdentification == null) ||
	(itemIdentification == null) ||
	(role == null)) {
      myLogger.error("Incomplete relationship specification - " + param +
		     ". No relationship added.");
    } else {
      addRelationship(typeIdentification, itemIdentification, messageAddress,
		      role, start, end);
    }
  }

  private String getToken(StringTokenizer tokenizer, String delim) {
    String token = null;
    if (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken(delim);
      if (tokenizer.hasMoreTokens() &&
	  !(tokenizer.nextToken().equals(delim))) {
	token = null;
      }
    }

    return token;
  }

  
  private static class TrivialTimeSpan implements TimeSpan {
    long myStart;
    long myEnd;

    public TrivialTimeSpan(long start, long end) {
      myStart = start;
      myEnd = end;
    }

    public long getStartTime() {
      return myStart;
    }

    public long getEndTime() {
      return myEnd;
    }
  }

  private class AssetDataCallbackImpl implements AssetDataCallback {
    public ConfigFinder getConfigFinder() {
      return AssetDataPlugin.this.getConfigFinder();
    }
    public void createMyLocalAsset(String assetClassName) {
      AssetDataPlugin.this.createMyLocalAsset(assetClassName);
    }
    public boolean hasMyLocalAsset() {
      return (myLocalAsset != null);
    }
    public NewPropertyGroup createPropertyGroup(String propertyName) throws Exception {
      return AssetDataPlugin.this.createPropertyGroup(propertyName);
    }
    public Object parseExpr(String dataType, String value) {
      return AssetDataPlugin.this.parseExpr(dataType, value);
    }
    public long parseDate(String dateString) throws ParseException {
      return AssetDataPlugin.this.parseDate(dateString);
    }
    public String getType(String type) {
      return AssetDataPlugin.this.getType(type);
    }
    public void callSetter(NewPropertyGroup pg, String setterName,
                           String type, Object[] arguments)
    {
      AssetDataPlugin.this.callSetter(pg, setterName, type, arguments);
    }
    public void setLocationSchedule(String latStr, String lonStr) {
      AssetDataPlugin.this.setLocationSchedule(latStr, lonStr);
    }
    public long getDefaultStartTime() {
      return AssetDataPlugin.this.getDefaultStartTime();
    }
    public long getDefaultEndTime() {
      return AssetDataPlugin.this.getDefaultEndTime();
    }
    public void addPropertyToAsset(PropertyGroup pg) {
      AssetDataPlugin.this.addPropertyToAsset(pg);
    }

    public void addRelationship(String typeId, String itemId,
                                String otherAgentId, String roleName,
                                long start, long end)
    {
      AssetDataPlugin.this.addRelationship(typeId, itemId, otherAgentId, roleName, start, end);
    }
  }

  private static class LocalAssetInfo implements java.io.Serializable {
    private Object myKey = null;
    private String myClassName = "";

    public LocalAssetInfo(Asset localAsset, String className) {
      myKey = localAsset.getKey();
      myClassName = className;
    }

    public LocalAssetInfo() {
    }

    public Object getKey() {
      return myKey;
    }

    public void setKey(Object key) {
      myKey = key;
    }

    public String getClassName() {
      return myClassName;
    }

    public void setClassName(String className) {
      myClassName = className;
    }
  }   
}




