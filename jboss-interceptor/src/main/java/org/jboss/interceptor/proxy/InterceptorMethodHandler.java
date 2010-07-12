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

import org.jboss.interceptor.util.InterceptionTypeRegistry;
import org.jboss.interceptor.spi.handler.InterceptionHandler;
import org.jboss.interceptor.spi.handler.InterceptionHandlerFactory;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptor.util.ReflectionUtils;
import org.jboss.interceptor.util.proxy.TargetInstanceProxyMethodHandler;

/**
 * @author Marius Bogoevici
 */
public class InterceptorMethodHandler extends TargetInstanceProxyMethodHandler implements Serializable
{

   private Map<Object, InterceptionHandler> interceptorHandlerInstances = new HashMap<Object, InterceptionHandler>();
   private InterceptorMetadata targetClassInterceptorMetadata;
   private InterceptionModel<Class<?>, ?> interceptionModel;

   public InterceptorMethodHandler(Object target, Class<?> targetClass, InterceptionModel<Class<?>, ?> interceptionModel, InterceptionHandlerFactory<?> interceptionHandlerFactory, InterceptorMetadata targetClassMetadata)
   {
      super(target, targetClass != null ? targetClass : target.getClass());
      if (interceptionModel == null)
      {
         throw new IllegalArgumentException("Interception model must not be null");
      }
      if (interceptionHandlerFactory == null)
      {
         throw new IllegalArgumentException("Interception handler factory must not be null");
      }


      this.interceptionModel = interceptionModel;

      for (Object interceptorReference : this.interceptionModel.getAllInterceptors())
      {
         interceptorHandlerInstances.put(interceptorReference, ((InterceptionHandlerFactory) interceptionHandlerFactory).createFor(interceptorReference));
      }
      targetClassInterceptorMetadata = targetClassMetadata;
   }

   public Object doInvoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
   {
      ReflectionUtils.ensureAccessible(thisMethod);
      if (null != proceed)
      {
         if (!org.jboss.interceptor.util.InterceptionUtils.isInterceptionCandidate(thisMethod))
         {
            return thisMethod.invoke(getTargetInstance(), args);
         }
         if (InterceptionTypeRegistry.isSupported(InterceptionType.AROUND_TIMEOUT) && thisMethod.isAnnotationPresent(InterceptionTypeRegistry.getAnnotationClass(InterceptionType.AROUND_TIMEOUT)))
         {
            return executeInterception(thisMethod, args, InterceptionType.AROUND_TIMEOUT);
         }
         else
         {
            return executeInterception(thisMethod, args, InterceptionType.AROUND_INVOKE);
         }
      }
      else
      {
         if (thisMethod.getName().equals(InterceptionUtils.POST_CONSTRUCT))
         {
            return executeInterception(null, null, InterceptionType.POST_CONSTRUCT);
         }
         else if (thisMethod.getName().equals(InterceptionUtils.PRE_DESTROY))
         {
            return executeInterception(null, null, InterceptionType.PRE_DESTROY);
         }
      }
      return null;

   }

   private Object executeInterception(Method thisMethod, Object[] args, InterceptionType interceptionType) throws Throwable
   {

      List<InterceptionHandler> interceptionHandlers = new ArrayList<InterceptionHandler>();
         List<?> interceptorList = interceptionModel.getInterceptors(interceptionType, thisMethod);
         for (Object interceptorReference : interceptorList)
         {
            interceptionHandlers.add(interceptorHandlerInstances.get(interceptorReference));
         }

      if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
      {
         interceptionHandlers.add(new DirectClassInterceptionHandler<Class<?>>(getTargetInstance(), targetClassInterceptorMetadata));
      }

      InterceptionChain chain = new InterceptionChain(interceptionHandlers, interceptionType, getTargetInstance(), thisMethod);
      return chain.invokeNext(new InterceptorInvocationContext(chain, getTargetInstance(), thisMethod, args));
   }

   private void writeObject(ObjectOutputStream objectOutputStream) throws IOException
   {
      try
      {
         executeInterception(null, null, InterceptionType.PRE_PASSIVATE);
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
         executeInterception(null, null, InterceptionType.POST_ACTIVATE);
      }
      catch (Throwable throwable)
      {
         throw new IOException("Error while deserializing class", throwable);
      }
   }

}
