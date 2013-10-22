/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.util.UniqueObject;

/**
 * Adds a {@link TodoSubscription} to an AnnotatedSubscriptionsPlugin. The items
 * on this subscription should be considered as work to do, in the form of a
 * Runnable, rather than as data. Work can be added with one of the executeLater
 * methods, which are modeled after Swing's invokeLater.
 * 
 * For convenience there are also predefined methods for adding work that just
 * does a single publishAdd/Remove/Change of some {@link UniqueObject}.
 */
public class TodoPlugin extends AnnotatedSubscriptionsPlugin {
    private TodoSubscription todo;
    
    // Hackery to deal with work being added before the the
    // subscription is operational.
    private final Object pendingLock = new Object();
    private List<Runnable> pendingWork = new ArrayList<Runnable>();

    /**
     * Add some work to the TodoSubscription. The actual running happens in the
     * execute thread.
     * 
     * @param work
     *            The work to be performed in the execute thread
     */
    public void executeLater(Runnable work) {
        synchronized (pendingLock) {
            if (pendingWork != null) {
                pendingWork.add(work);
                return;
            }
            todo.add(work);
        }
    }

    /**
     * Add some work to the TodoSubscription after a delay. The actual running
     * happens in the execute thread.
     * 
     * @param delayMillis
     *            How long to delay. If <= 0, the add happens immediately.
     * @param work
     *            The work to be performed in the execute thread
     *            
     * @return the Alarm used for the delay, or null if no delay.  The Alarm can be
     * cancelled by the caller to prevent the work from happening.
     */
    public Alarm executeLater(long delayMillis, Runnable work) {
        if (delayMillis <= 0) {
            executeLater(work);
            return null;
        } else {
            Alarm alarm = new TodoAlarm(System.currentTimeMillis() + delayMillis, work);
            getAlarmService().addRealTimeAlarm(alarm);
            return alarm;
        }
    }

    @Override
   protected void execute() {
        super.execute();
        if (todo.hasChanged()) {
            @SuppressWarnings("unchecked")
            Collection<Runnable> items = todo.getAddedCollection();
            for (Runnable item : items) {
                item.run();
            }
        }
    }

    @Override
   protected void setupSubscriptions() {
        super.setupSubscriptions();
        todo = new TodoSubscription("me");
        blackboard.subscribe(todo);
        
        // Add early work, if any
        synchronized (pendingLock) {
            todo.addAll(pendingWork);
            // now disable pending work
            pendingWork = null;
        }
    }

    // Convenience methods for simple work that does a single
    // publishAdd/Remove/Change, with or without a delay (and
    // in the case of Change, with or without change reports).

    protected void publishAddLater(UniqueObject object) {
        publishAddLater(0, object);
    }

    protected Alarm publishAddLater(long delay, final UniqueObject object) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishAdd(object);
            }
        });
    }

    protected void publishRemoveLater(UniqueObject object) {
        publishRemoveLater(0, object);
    }

    protected Alarm publishRemoveLater(long delay, final UniqueObject object) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishRemove(object);
            }
        });
    }

    protected void publishChangeLater(UniqueObject object) {
        publishChangeLater(0, object);
    }

    protected Alarm publishChangeLater(long delay, final UniqueObject object) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishChange(object);
            }
        });
    }

    protected void publishChangeLater(UniqueObject object, Collection<ChangeReport> changeReports) {
        publishChangeLater(0, object, changeReports);
    }

    protected Alarm publishChangeLater(long delay,
                                       final UniqueObject object,
                                       final Collection<ChangeReport> changeReports) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishChange(object, changeReports);
            }
        });
    }

    private final class TodoAlarm extends AlarmBase {
        private final Runnable item;

        public TodoAlarm(long futureTime, Runnable item) {
            super(futureTime);
            this.item = item;
        }

        @Override
      public void onExpire() {
            todo.add(item);
        }
    }
}
