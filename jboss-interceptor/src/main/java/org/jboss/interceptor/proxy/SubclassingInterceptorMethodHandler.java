package org.jboss.interceptor.proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.util.proxy.MethodHandler;
import org.jboss.interceptor.model.InterceptionModel;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.InterceptionTypeRegistry;
import org.jboss.interceptor.model.InterceptorMetadata;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptor.util.ReflectionUtils;

/**
 * @author Marius Bogoevici
 */
public class SubclassingInterceptorMethodHandler implements MethodHandler,Serializable
{

   private Map<Object, InterceptionHandler> interceptorHandlerInstances = new HashMap<Object, InterceptionHandler>();
   private InterceptorMetadata targetClassInterceptorMetadata;
   private List<InterceptionModel<Class<?>, ?>> interceptionModels;
   private Object targetInstance;

   public SubclassingInterceptorMethodHandler(Object targetInstance, List<InterceptionModel<Class<?>, ?>> interceptionModels, List<InterceptionHandlerFactory<?>> interceptionHandlerFactories, InterceptorMetadata targetClassMetadata)
   {
      this.targetInstance = targetInstance;
      if (interceptionModels == null)
      {
         throw new IllegalArgumentException("Interception model must not be null");
      }

      if (interceptionHandlerFactories == null)
      {
         throw new IllegalArgumentException("Interception handler factory must not be null");
      }

      if (interceptionModels.size() != interceptionHandlerFactories.size())
      {
         throw new IllegalArgumentException("For each interception model, an interception factory must be provided");
      }

      this.interceptionModels = interceptionModels;

      for (int i = 0; i < interceptionModels.size(); i++)
      {
         for (Object interceptorReference : this.interceptionModels.get(i).getAllInterceptors())
         {
            interceptorHandlerInstances.put(interceptorReference, ((InterceptionHandlerFactory) interceptionHandlerFactories.get(i)).createFor((Object) interceptorReference));
         }
      }
      targetClassInterceptorMetadata = targetClassMetadata;
   }

   public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
   {
      ReflectionUtils.ensureAccessible(thisMethod);
      if (thisMethod.getDeclaringClass().equals(LifecycleMixin.class))
      {
         if (thisMethod.getName().equals(InterceptionUtils.POST_CONSTRUCT))
         {
            return executeInterception(self, null, null, null, InterceptionType.POST_CONSTRUCT);
         }
         else if (thisMethod.getName().equals(InterceptionUtils.PRE_DESTROY))
         {
            return executeInterception(self, null, null, null, InterceptionType.PRE_DESTROY);
         }
      }
      else
      {
         if (!InterceptionUtils.isInterceptionCandidate(thisMethod))
         {
            return proceed.invoke(self, args);
         }
         if (InterceptionTypeRegistry.supportsTimeoutMethods() && thisMethod.isAnnotationPresent(InterceptionTypeRegistry.getAnnotationClass(InterceptionType.AROUND_TIMEOUT)))
         {
            return executeInterception(self, thisMethod, thisMethod, args, InterceptionType.AROUND_TIMEOUT);
         }
         else
         {
            return executeInterception(self, thisMethod, thisMethod, args, InterceptionType.AROUND_INVOKE);
         }
      }
      return null;

   }

   private Object executeInterception(Object self, Method proceedingMethod, Method thisMethod, Object[] args, InterceptionType interceptionType) throws Throwable
   {
      List<InterceptionHandler> interceptionHandlers = new ArrayList<InterceptionHandler>();
      for (InterceptionModel interceptionModel : interceptionModels)
      {
         List<?> interceptorList = interceptionModel.getInterceptors(interceptionType, thisMethod);
         for (Object interceptorReference : interceptorList)
         {
            interceptionHandlers.add(interceptorHandlerInstances.get(interceptorReference));
         }
      }

      if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
      {
         interceptionHandlers.add(new DirectClassInterceptionHandler<Class<?>>(self, targetClassInterceptorMetadata));
      }

      InterceptionChain chain = new InterceptionChain(interceptionHandlers, interceptionType,self, proceedingMethod, args);
      return chain.invokeNext(new InterceptorInvocationContext(chain, self, thisMethod, args));
   }

   private void writeObject(ObjectOutputStream objectOutputStream) throws IOException
   {
      try
      {
         executeInterception(targetInstance, null, null, null, InterceptionType.PRE_PASSIVATE);
         objectOutputStream.defaultWriteObject();
      }
      catch (Throwable throwable)
      {
         throw new IOException("Error while serializing class", throwable);
      }
   }

   private void readObject(ObjectInputStream objectInputStream) throws IOException
   {
      try
      {
         objectInputStream.defaultReadObject();
         executeInterception(targetInstance, null, null, null, InterceptionType.POST_ACTIVATE);
      }
      catch (Throwable throwable)
      {
         throw new IOException("Error while deserializing class", throwable);
      }
   }

}