package org.jboss.interceptor.proxy;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import org.jboss.interceptor.model.InterceptionModel;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.InterceptionTypeRegistry;
import org.jboss.interceptor.model.InterceptorMetadata;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptor.util.ReflectionUtils;
import org.jboss.interceptor.util.proxy.TargetInstanceProxyMethodHandler;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marius Bogoevici
 */
public class SubclassingInterceptorMethodHandler implements MethodHandler,Serializable
{

   private Map<Object, InterceptionHandler> interceptorHandlerInstances = new HashMap<Object, InterceptionHandler>();
   private InterceptorMetadata targetClassInterceptorMetadata;
   private List<InterceptionModel<Class<?>, ?>> interceptionModels;
   private Object targetInstance;

   private static MethodHandler DEFAULT_METHOD_HANDLER = new MethodHandler() {

        public Object invoke(Object self, Method m,
                             Method proceed, Object[] args)
            throws Exception
        {
            return proceed.invoke(self, args);
        }
   };

   public SubclassingInterceptorMethodHandler(Object targetInstance, Class<?> targetClass, List<InterceptionModel<Class<?>, ?>> interceptionModels, List<InterceptionHandlerFactory<?>> interceptionHandlerFactories, InterceptorMetadata targetClassMetadata)
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
      if (null != proceed)
      {
         if (!InterceptionUtils.isInterceptionCandidate(thisMethod))
         {
            return proceed.invoke(self, args);
         }
         if (InterceptionTypeRegistry.supportsTimeoutMethods() && thisMethod.isAnnotationPresent(InterceptionTypeRegistry.getAnnotationClass(InterceptionType.AROUND_TIMEOUT)))
         {
            return executeInterception(self, proceed, thisMethod, args, InterceptionType.AROUND_TIMEOUT);
         }
         else
         {
            return executeInterception(self, proceed, thisMethod, args, InterceptionType.AROUND_INVOKE);
         }
      }
      else
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
         // it is reasonably safe to assume that all the fields from targetInstance
         // have already been serialized because the MethodHandler is a field of the
         // generated subclass, therefore it is deserialized after the fields of the
         // superclass, which is the actual class of targetInstance, but we need a
         // non-empty MethodHandler in order to be able to execute the methods
         // set the DEFAULT_METHOD_HANDLER temporarily and let the deserialization
         // process to overwrite it
        ((ProxyObject)targetInstance).setHandler(DEFAULT_METHOD_HANDLER);
         executeInterception(targetInstance, null, null, null, InterceptionType.POST_ACTIVATE);
      }
      catch (Throwable throwable)
      {
         throw new IOException("Error while deserializing class", throwable);
      }
   }

}