package org.cougaar.core.mobility.plugin;

import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ParameterizedPlugin;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

public class MoveAgentLoopTestPlugin extends ParameterizedPlugin {
    protected MessageAddress localAgent;
    protected MessageAddress localNode;
    protected DomainService domain;
    protected NodeIdentificationService nodeIdService;
    protected MobilityFactory mobilityFactory;
    private LoggingService log;
    private AlarmService alarmService;
    
    private static final long MOVE_PERIOD = 40000; // 40 seconds
    // remove uid:
    protected static final String REMOVE_UID_PARAM = "removeUID";
    protected String removeUID;
    
    // ticket options:
    protected static final String MOBILE_AGENT_PARAM = "mobileAgent";
    protected MessageAddress mobileAgent;
    protected static final String ORIGIN_NODE_PARAM = "originNode";
    protected MessageAddress originNode;
    protected static final String DEST_NODE_PARAM = "destNode";
    protected MessageAddress destNode;
    protected static final String IS_FORCE_RESTART_PARAM = "isForceRestart";
    protected boolean isForceRestart;
    
    
    protected static final UnaryPredicate AGENT_CONTROL_PRED =
        new UnaryPredicate() {
        /**
          * 
          */
         private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
            return (o instanceof AgentControl);
        }
    };
    
  
    @Override
   public void load() {
        super.load();
        ServiceBroker sb = getServiceBroker();
        log = sb.getService(this, LoggingService.class, null);
        NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
        localNode = nis.getMessageAddress();
        domain = sb.getService(this, DomainService.class, null);
        mobilityFactory = (MobilityFactory) domain.getFactory("mobility");
        alarmService = sb.getService(this, AlarmService.class, null);
    }
    // release services:
    @Override
   public void unload() {
        super.unload();
        ServiceBroker sb = getServiceBroker();
        if (domain != null) {
            sb.releaseService(
                    this, DomainService.class, domain);
            domain = null;
        }
        if (nodeIdService != null) {
            sb.releaseService(
                    this, NodeIdentificationService.class, nodeIdService);
            nodeIdService = null;
        }
        if (log != null) {
            sb.releaseService(
                    this, LoggingService.class, log);
            log = null;
        }
        if (alarmService != null) {
            sb.releaseService(
                    this, AlarmService.class, alarmService);
            alarmService = null;
        }
        
    }
    @Override
   public void start() {
        mobileAgent = MessageAddress.getMessageAddress(getParameter(MOBILE_AGENT_PARAM,"Source1"));
        originNode= MessageAddress.getMessageAddress(getParameter(ORIGIN_NODE_PARAM,"NODE1"));        
        destNode= MessageAddress.getMessageAddress(getParameter(DEST_NODE_PARAM,"NODE2"));
        isForceRestart= "true".equals(getParameter(IS_FORCE_RESTART_PARAM ,"false"));
        if (log.isInfoEnabled())
            log.info("Loaded MoveAgentLoop Plugin" +
                    " mobileAgent=" + mobileAgent +
                    " originNode="+ originNode +
                    " destNode=" + destNode +
                    " isForceRestart=" + isForceRestart); 
        if(log.isInfoEnabled()) 
            log.info("Started Agent Move timer");
        alarmService.addRealTimeAlarm(new Mover(MOVE_PERIOD)); 
        super.start();
    }
    
    protected Collection queryAgentControls() {
        Collection ret = null;
        try {
            blackboard.openTransaction();
            ret = blackboard.query(AGENT_CONTROL_PRED);
        } finally {
            blackboard.closeTransactionDontReset();
        }
        return ret;
    }
    
    protected AgentControl queryAgentControl(final UID uid) {
        if (uid == null) {
            throw new IllegalArgumentException("null uid");
        }
        UnaryPredicate pred = new UnaryPredicate() {
            /**
          * 
          */
         private static final long serialVersionUID = 1L;

            public boolean execute(Object o) {
                return 
                ((o instanceof AgentControl) &&
                        (uid.equals(((AgentControl) o).getUID())));
            }
        };
        AgentControl ret = null;
        try {
            blackboard.openTransaction();
            Collection c = blackboard.query(pred);
            if ((c != null) && (c.size() >= 1)) {
                ret = (AgentControl) c.iterator().next();
            }
        } finally {
            blackboard.closeTransactionDontReset();
        }
        return ret;
    }
    
    protected AgentControl createAgentControl(
            UID ownerUID, 
            MessageAddress target, 
            AbstractTicket ticket) {
        if (mobilityFactory == null) {
            throw new RuntimeException(
            "Mobility factory (and domain) not enabled");
        }
        AgentControl ac = 
            mobilityFactory.createAgentControl(
                    ownerUID, target, ticket);
        return ac;
    }
    
    protected AgentControl createMoveAgentControl(
            UID ownerUID, 
            MessageAddress orginNodeAddr,
            MessageAddress destNodeAddr,
            MessageAddress mobileAgentAddr,
            boolean isForceRestart){
        if (mobilityFactory == null) {
            throw new RuntimeException(
            "Mobility factory (and domain) not enabled");
        }
        AbstractTicket ticket = new MoveTicket(
                null,
                mobileAgentAddr,
                orginNodeAddr,
                destNodeAddr,
                isForceRestart);
        AgentControl ac = 
            mobilityFactory.createAgentControl(
                    ownerUID, orginNodeAddr, ticket);
        
        return ac;

    }
    
    protected void addAgentControl(AgentControl ac) {
        try {
            blackboard.openTransaction();
            blackboard.publishAdd(ac);
        } finally {
            blackboard.closeTransactionDontReset();
        }
    }
    
    protected void removeAgentControl(AgentControl ac) {
        try {
            blackboard.openTransaction();
            blackboard.publishRemove(ac);
        } finally {
            blackboard.closeTransaction();
        }
    }
    
    private class Mover implements Alarm {
        private long expirationTime;
        private long period;
        private boolean expired = false;
        private boolean forward=true;
                
        public Mover(long period) {
            super();
            this.period = period;
            this.expirationTime = System.currentTimeMillis()+period;
        }
        
        private void moveForward() {
            AgentControl forwardAgentControl=createMoveAgentControl(null,
                    originNode,destNode,mobileAgent,isForceRestart);
            addAgentControl(forwardAgentControl);
            forward=false;
        }

        private void moveBackward() {
         AgentControl backwardAgentControl=createMoveAgentControl(null,
                    destNode,originNode,mobileAgent,isForceRestart);
         addAgentControl(backwardAgentControl);
         forward=true;
        }

        
        public long getExpirationTime() {
            return expirationTime;
        }

        public boolean hasExpired() {
            return expired;
        }

        public boolean cancel() {
            boolean was = expired;  
            expired = true;
            return was;
        }
        
        public void restart() {
            this.expirationTime = System.currentTimeMillis()+period;
            expired = false;
            alarmService.addRealTimeAlarm(this);
        }
        
        public void expire() {
            if (log.isInfoEnabled())
                log.info("JAZ Move timer fired forward=" + forward);
            if(forward) moveForward();
            else moveBackward();
            restart();
            }
    }
    
    @Override
   protected void setupSubscriptions() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
   protected void execute() {
        // TODO Auto-generated method stub
        
    }
    
}
