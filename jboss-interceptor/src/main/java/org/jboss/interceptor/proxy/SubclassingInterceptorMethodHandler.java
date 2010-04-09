package org.jboss.interceptor.proxy;

import org.jboss.interceptor.model.InterceptionModel;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.InterceptionTypeRegistry;
import org.jboss.interceptor.model.InterceptorMetadata;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptor.util.ReflectionUtils;
import org.jboss.interceptor.util.proxy.TargetInstanceProxyMethodHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marius Bogoevici
 */
public class SubclassingInterceptorMethodHandler extends TargetInstanceProxyMethodHandler implements Serializable
{

   private Map<Object, InterceptionHandler> interceptorHandlerInstances = new HashMap<Object, InterceptionHandler>();
   private InterceptorMetadata targetClassInterceptorMetadata;
   private List<InterceptionModel<Class<?>, ?>> interceptionModels;

   public SubclassingInterceptorMethodHandler(Object target, Class<?> targetClass, List<InterceptionModel<Class<?>, ?>> interceptionModels, List<InterceptionHandlerFactory<?>> interceptionHandlerFactories, InterceptorMetadata targetClassMetadata)
   {
      super(target, targetClass != null ? targetClass : target.getClass());
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

   public Object doInvoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
   {
      ReflectionUtils.ensureAccessible(thisMethod);
      if (null != proceed)
      {
         if (!InterceptionUtils.isInterceptionCandidate(thisMethod))
         {
            return proceed.invoke(getTargetInstance(), args);
         }
         if (InterceptionTypeRegistry.supportsTimeoutMethods() && thisMethod.isAnnotationPresent(InterceptionTypeRegistry.getAnnotationClass(InterceptionType.AROUND_TIMEOUT)))
         {
            return executeInterception(proceed, thisMethod, args, InterceptionType.AROUND_TIMEOUT);
         }
         else
         {
            return executeInterception(proceed, thisMethod, args, InterceptionType.AROUND_INVOKE);
         }
      }
      else
      {
         if (thisMethod.getName().equals(InterceptionUtils.POST_CONSTRUCT))
         {
            return executeInterception(null, null, null, InterceptionType.POST_CONSTRUCT);
         }
         else if (thisMethod.getName().equals(InterceptionUtils.PRE_DESTROY))
         {
            return executeInterception(null, null, null, InterceptionType.PRE_DESTROY);
         }
      }
      return null;

   }

   private Object executeInterception(Method proceedingMethod, Method thisMethod, Object[] args, InterceptionType interceptionType) throws Throwable
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
         interceptionHandlers.add(new DirectClassInterceptionHandler<Class<?>>(getTargetInstance(), targetClassInterceptorMetadata));
      }

      InterceptionChain chain = new InterceptionChain(interceptionHandlers, interceptionType, getTargetInstance(), proceedingMethod, args);
      return chain.invokeNext(new InterceptorInvocationContext(chain, getTargetInstance(), thisMethod, args));
   }

   private void writeObject(ObjectOutputStream objectOutputStream) throws IOException
   {
      try
      {
         executeInterception(null, null, null, InterceptionType.PRE_PASSIVATE);
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
         executeInterception(null, null, null, InterceptionType.POST_ACTIVATE);
      }
      catch (Throwable throwable)
      {
         throw new IOException("Error while deserializing class", throwable);
      }
   }

}