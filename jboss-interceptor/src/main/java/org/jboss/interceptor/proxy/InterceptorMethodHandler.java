package org.jboss.interceptor.proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.interceptor.reader.InterceptorMetadataUtils;
import org.jboss.interceptor.reader.ReflectiveClassMetadata;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;
import org.jboss.interceptor.util.InterceptionTypeRegistry;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptor.util.ReflectionUtils;
import org.jboss.interceptor.util.proxy.TargetInstanceProxyMethodHandler;

/**
 * @author Marius Bogoevici
 */
public class InterceptorMethodHandler extends TargetInstanceProxyMethodHandler implements Serializable
{

   private Map<ClassMetadata<?>, Object> interceptorHandlerInstances = new HashMap<ClassMetadata<?>, Object>();
   private InterceptorMetadata targetClassInterceptorMetadata;
   private InterceptionModel<ClassMetadata<?>, ClassMetadata> interceptionModel;

   public InterceptorMethodHandler(Object target, ClassMetadata<?> targetClass, InterceptionModel<ClassMetadata<?>, ClassMetadata> interceptionModel, InterceptorInstantiator<ClassMetadata<?>, ?> interceptorInstantiator, InterceptorMetadata targetClassMetadata)
   {
      super(target, targetClass != null ? targetClass : ReflectiveClassMetadata.of(target.getClass()));
      if (interceptionModel == null)
      {
         throw new IllegalArgumentException("Interception model must not be null");
      }
      if (interceptorInstantiator == null)
      {
         throw new IllegalArgumentException("Interception handler factory must not be null");
      }


      this.interceptionModel = interceptionModel;

      for (ClassMetadata<?> interceptorReference : this.interceptionModel.getAllInterceptors())
      {
         interceptorHandlerInstances.put(interceptorReference, ((InterceptorInstantiator) interceptorInstantiator).createFor(interceptorReference));
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

      List<ClassMetadata> interceptorList = interceptionModel.getInterceptors(interceptionType, thisMethod);
      Collection<InterceptorInvocation<?>> interceptorInvocations = new ArrayList<InterceptorInvocation<?>>();

      for (ClassMetadata<?> interceptorReference : interceptorList)
      {
         interceptorInvocations.add(new InterceptorInvocation(interceptorHandlerInstances.get(interceptorReference), InterceptorMetadataUtils.readMetadataForInterceptorClass(interceptorReference), interceptionType));
      }

      if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
      {
         interceptorInvocations.add(new InterceptorInvocation(getTargetInstance(), InterceptorMetadataUtils.readMetadataForTargetClass(getTargetClass()), interceptionType));
      }

      InterceptionChain chain = new InterceptionChain(interceptorInvocations, interceptionType, getTargetInstance(), thisMethod);
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
