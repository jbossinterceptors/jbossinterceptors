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

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import org.jboss.interceptor.reader.InterceptorMetadataUtils;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;
import org.jboss.interceptor.util.InterceptionTypeRegistry;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptor.util.ReflectionUtils;

/**
 * @author Marius Bogoevici
 */
public class SubclassingInterceptorMethodHandler implements MethodHandler,Serializable
{

   private Map<ClassMetadata<?>, Object> interceptorHandlerInstances = new HashMap<ClassMetadata<?>, Object>();
   private InterceptorMetadata targetClassInterceptorMetadata;
   private InterceptionModel<ClassMetadata<?>, ClassMetadata> interceptionModel;
   private Object targetInstance;

   private static MethodHandler DEFAULT_METHOD_HANDLER = new MethodHandler() {

        public Object invoke(Object self, Method m,
                             Method proceed, Object[] args)
            throws Exception
        {
            return proceed.invoke(self, args);
        }
   };

   public SubclassingInterceptorMethodHandler(Object target, ClassMetadata<?> targetClass, InterceptionModel<ClassMetadata<?>, ClassMetadata> interceptionModel, InterceptorInstantiator<ClassMetadata<?>, ?> interceptorInstantiator, InterceptorMetadata targetClassMetadata)
   {
      this.targetInstance = target;
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
         interceptorHandlerInstances.put(interceptorReference, (interceptorInstantiator).createFor(interceptorReference));
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
         if (InterceptionTypeRegistry.isSupported(InterceptionType.AROUND_TIMEOUT) && thisMethod.isAnnotationPresent(InterceptionTypeRegistry.getAnnotationClass(InterceptionType.AROUND_TIMEOUT)))
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

      List<ClassMetadata> interceptorList = interceptionModel.getInterceptors(interceptionType, thisMethod);
      Collection<InterceptorInvocation<?>> interceptorInvocations = new ArrayList<InterceptorInvocation<?>>();

      for (ClassMetadata<?> interceptorReference : interceptorList)
      {
         interceptorInvocations.add(new InterceptorInvocation(interceptorHandlerInstances.get(interceptorReference), InterceptorMetadataUtils.readMetadataForInterceptorClass(interceptorReference), interceptionType));
      }

      if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
      {
         interceptorInvocations.add(new InterceptorInvocation(self, targetClassInterceptorMetadata, interceptionType));
      }

      InterceptionChain chain = new InterceptionChain(interceptorInvocations, interceptionType, self,proceedingMethod);
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
         if (((ProxyObject)targetInstance).getHandler() == null)
         {
            ((ProxyObject)targetInstance).setHandler(DEFAULT_METHOD_HANDLER); 
         }
         executeInterception(targetInstance, null, null, null, InterceptionType.POST_ACTIVATE);
      }
      catch (Throwable throwable)
      {
         throw new IOException("Error while deserializing class", throwable);
      }
   }

}