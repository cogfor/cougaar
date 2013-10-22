/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.IsInstanceOf;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/**
 * This class provides support for plugins that wish to use annotations to
 * create and manage blackboard IncrementalSubscriptions.
 */
public abstract class AnnotatedSubscriptionsPlugin
      extends ParameterizedPlugin {
   private final Map<String, IncrementalSubscription> subscriptions = new HashMap<String, IncrementalSubscription>();
   private final List<SubscriptionInvoker> subscriptionInvokers = new ArrayList<SubscriptionInvoker>();
   private final Map<String,QueryRunner> queryRunners = new HashMap<String, QueryRunner>();

   @Override
   protected void execute() {
      for (SubscriptionInvoker invoker : subscriptionInvokers) {
         invoker.execute();
      }
   }

   /**
    * Run the query with the given id and context
    * 
    * @param <T> the expected query result type.
    * @param id name of the query.
    * @param type The expected query result class.
    * @param queryContext optional data that preserves context between iterations.
    * @return the blackboard query result, or an empty list if the id doesn't match any known query.
    */
   public <T> Collection<T> runQuery(String id, Class<T> type, Object... queryContext) {
      QueryRunner runner = queryRunners.get(id);
      if (runner != null) {
         return runner.execute(type, queryContext);
      } else {
         log.warn("\"" + id + "\" is not the name of a query");
         return Collections.emptyList();
      }
   }
   
   @Override
   protected void setupSubscriptions() {
      Class<? extends AnnotatedSubscriptionsPlugin> klass = getClass();
      // Method[] methods = klass.getMethods();
      createSubsscriptionInvokers(klass);
      createQueryRunners(klass);
   }

   private void createQueryRunners(Class<? extends AnnotatedSubscriptionsPlugin> klass) {
      Collection<Method> methods = Cougaar.getAnnotatedMethods(klass, Cougaar.Query.class);
      for (Method method : methods) {
         // Use the type of the first arg as an implicit 'isa'
         Class<?>[] parameterTypes = method.getParameterTypes();
         if (parameterTypes.length == 0) {
            String message =
                  "@Query method" + method.getName() + " of class " + getClass().getName()
                        + " has the wrong number of arguments (should be 1)";
            log.error(message);
            throw new IllegalArgumentException(message);
         }
         Cougaar.Query annotation = method.getAnnotation(Cougaar.Query.class);
         QueryRunner runner = new QueryRunner(method, annotation);
         String queryName = annotation.name();
         if ("".equals(queryName)) {
            // use the method name
            queryName = method.getName();
         }
         queryRunners.put(queryName, runner);
      }
   }

   private void createSubsscriptionInvokers(Class<? extends AnnotatedSubscriptionsPlugin> klass) {
      Collection<Method> methods = Cougaar.getAnnotatedMethods(klass, Cougaar.Execute.class);
      for (Method method : methods) {
         Cougaar.Execute annotation = method.getAnnotation(Cougaar.Execute.class);
         String id;
         String when = annotation.when();
         if (!Cougaar.NO_VALUE.equals(when)) {
            id = when;
         } else {
            // Use the type of the first arg as an implicit 'isa'
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length < 1 || parameterTypes.length > 2) {
               String message =
                     "@Execute method" + method.getName() + " of class " + getClass().getName()
                           + " has the wrong number of arguments (should be 1 or 2)";
               log.error(message);
               throw new IllegalArgumentException(message);
            }
            if (parameterTypes.length == 2) {
               // ensure 2nd arg is IncrementalSubscription
               Class<?> cls = parameterTypes[1];
               if (!IncrementalSubscription.class.isAssignableFrom(cls)) {
                  String message =
                        "@Execute method" + method.getName() + " of class " + getClass().getName()
                              + " has an invalid second argument (should be IncrementalSubscription)";
                  log.error(message);
                  throw new IllegalArgumentException(message);
               }
            }
            id = parameterTypes[0].getName();
         }
         IncrementalSubscription subscription = subscriptions.get(id);
         if (subscription != null) {
            SubscriptionInvoker invoker = new SubscriptionInvoker(method, annotation, subscription);
            subscriptionInvokers.add(invoker);
         } else {
            SubscriptionInvoker invoker = new SubscriptionInvoker(method, annotation);
            subscriptions.put(id, invoker.sub);
            subscriptionInvokers.add(invoker);
         }
      }
   }

   public <T> IncrementalSubscription<T> getSubscription(String id, Class<T> type) {
      return subscriptions.get(id);
   }

   @SuppressWarnings("deprecation")
   private boolean isTesterMethod(Method method, String name, Class<?> argClass) {
      if (method.isAnnotationPresent(Cougaar.Predicate.class)) {
         Cougaar.Predicate pred = method.getAnnotation(Cougaar.Predicate.class);
         if (!name.equals(pred.when())) {
            return false;
         }
      } else if (!name.equals(method.getName())) {
         return false;
      }
      Class<?>[] paramTypes = method.getParameterTypes();
      if (paramTypes.length != 1) {
         return false;
      }
      if (!argClass.isAssignableFrom(paramTypes[0])) {
         return false;
      }
      Class<?> returnType = method.getReturnType();
      return returnType == Boolean.class || returnType == boolean.class;
   }

   private UnaryPredicate createPredicate(Method method, String testerMethodName) {
      Class<?> pluginClass = AnnotatedSubscriptionsPlugin.this.getClass();
      Method[] methods = pluginClass.getMethods();
      Class<?> argClass = method.getParameterTypes()[0];
      
      if (Cougaar.NO_VALUE.equals(testerMethodName)) {
         // Implicit instanceof if no 'when'
        return new IsInstanceOf(argClass);
      }
      
      Method testerMethod = null;
      for (Method candidate : methods) {
         if (isTesterMethod(candidate, testerMethodName, argClass)) {
            testerMethod = candidate;
            break;
         }
      }
      if (testerMethod == null) {
         String message =
               "@Execute method" + method.getName() + " of class " + getClass().getName()
                     + " references unknown predicate method " + testerMethodName + " (" + argClass + ")";
         log.error(message);
         throw new IllegalArgumentException(message);
      }
      final Method finalTesterMethod = testerMethod;
      final Class<?> testerArgClass = testerMethod.getParameterTypes()[0];
      UnaryPredicate predicate = new UnaryPredicate() {
         private static final long serialVersionUID = 1L;
   
         public boolean execute(Object o) {
            if (!testerArgClass.isAssignableFrom(o.getClass())) {
               return false;
            }
            try {
               return (Boolean) finalTesterMethod.invoke(AnnotatedSubscriptionsPlugin.this, o);
            } catch (Exception e) {
               log.error("Test failed", e);
               return false;
            }
         }
   
      };
      return predicate;
   }

   private class QueryRunner<T> {
      private final Method method;
      private final UnaryPredicate predicate;
      private final String id;

      QueryRunner(Method method, Cougaar.Query annotation) {
         this.method = method;
         this.id = annotation.name();
         this.predicate = createPredicate(method, annotation.where());
      }
      
      Collection <T> execute(Class<T> type, Object...context) {
         Object[] completeArgs = new Object[context.length+1];
         System.arraycopy(context, 0, completeArgs, 1, context.length);
         Collection matches = blackboard.query(predicate);
         List<T> results = new ArrayList<T>();
         for (Object match : matches) {
            try {
               T typedMatch = type.cast(match);
               results.add(typedMatch);
               completeArgs[0] = typedMatch;
               method.invoke(AnnotatedSubscriptionsPlugin.this, completeArgs);
            } catch (ClassCastException e) {
               log.error("query result " + match + " from query \"" +id+ "\" is not the expected type: " + e.getMessage());
            } catch (IllegalArgumentException e) {
               log.error("Failed to invoke annotated method", e);
            } catch (IllegalAccessException e) {
               log.error("Failed to invoke annotated method", e);
            } catch (InvocationTargetException e) {
               log.error("Failed to invoke annotated method", e);
            }
         }
         return results;
      }
   }

   /**
    * Keeps the state of one IncrementalSubscription created via annotations.
    */
   private class SubscriptionInvoker {
      /**
       * The subscription itself.
       */
      private final IncrementalSubscription sub;

      /**
       * The annotated method which will be invoked on the plugin, passing in
       * each element of an IncrementalSubscription collection in turn as an
       * argument.
       */
      private final Method method;

      /**
       * The set of operations, as given in the annotation, which determines
       * which ncrementalSubscription collection are relevant.
       */
      private final Subscribe.ModType[] ops;

      public SubscriptionInvoker(Method method, Cougaar.Execute annotation, IncrementalSubscription sub) {
         this.method = method;
         this.ops = annotation.on();
         this.sub = sub;
      }

      public SubscriptionInvoker(Method method, Cougaar.Execute annotation) {
         this.method = method;
         this.ops = annotation.on();
         this.sub = createIncrementalSubscription(annotation);

      }

      private IncrementalSubscription createIncrementalSubscription(Cougaar.Execute annotation) {
         String when = annotation.when();
         UnaryPredicate predicate = createPredicate(method, when);
         return blackboard.subscribe(predicate);
      }

      private Collection<?> getCollection(Subscribe.ModType op) {
         switch (op) {
            case ADD:
               return sub.getAddedCollection();

            case CHANGE:
               return sub.getChangedCollection();

            case REMOVE:
               return sub.getRemovedCollection();

            default:
               return null;
         }
      }

      public void execute() {
         if (sub == null || !sub.hasChanged()) {
            // failed to make a proper subscription, or no changes
            return;
         }
         boolean includeSubscription = method.getParameterTypes().length == 2;
         for (Subscribe.ModType op : ops) {
            Collection<?> objects = getCollection(op);
            if (objects != null) {
               for (Object object : objects) {
                  try {
                     AnnotatedSubscriptionsPlugin plugin = AnnotatedSubscriptionsPlugin.this;
                     if (includeSubscription) {
                        method.invoke(plugin, object, sub);
                     } else {
                        method.invoke(plugin, object);
                     }
                  } catch (Exception e) {
                     log.error("Failed to invoke annotated method", e);
                  }
               }
            }
         }
      }
   }

}
