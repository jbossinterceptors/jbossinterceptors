/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.interceptor.proxy;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.InterceptorClassMetadata;
import org.jboss.interceptor.registry.InterceptorRegistry;
import org.jboss.interceptor.registry.InterceptorClassMetadataRegistry;
import static org.jboss.interceptor.util.InterceptionUtils.isAroundInvokeInterceptionCandidate;
import org.jboss.interceptor.InterceptorException;

import javax.interceptor.AroundInvoke;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptorProxyCreatorImpl implements InterceptorProxyCreator
{
   public static final String POST_CONSTRUCT = "lifecycle_mixin_$$_postConstruct";
   public static final String PRE_DESTROY = "lifecycle_mixin_$$_preDestroy";

   private InterceptorRegistry<Class<?>> interceptorRegistry;

   private InterceptionHandlerFactory interceptionHandlerFactory;

   public InterceptorProxyCreatorImpl(InterceptorRegistry<Class<?>> interceptorRegistry, InterceptionHandlerFactory interceptionHandlerFactory)
   {
      this.interceptorRegistry = interceptorRegistry;
      this.interceptionHandlerFactory = interceptionHandlerFactory;
   }


   public <T> T createProxyFromInstance(final Object target, Class<T> proxyClass, Class<?>[] constructorTypes, Object[] constructorArguments)
   {
      ProxyFactory proxyFactory = new ProxyFactory();
      if (proxyClass != null)
         proxyFactory.setSuperclass(proxyClass);

      proxyFactory.setInterfaces(new Class<?>[]{LifecycleMixin.class});

      proxyFactory.setHandler(new InstanceProxifyingMethodHandler(target, proxyClass, interceptorRegistry));

      try
      {
         return (T) proxyFactory.create(constructorTypes, constructorArguments);
      } catch (Exception e)
      {
         throw new InterceptorException(e);
      }
   }

   public <T> T createInstrumentedInstance(Class<T> proxyClass, Class<?>[] constructorTypes, Object[] constructorArguments)
   {
      ProxyFactory proxyFactory = new ProxyFactory();
      if (proxyClass != null)
         proxyFactory.setSuperclass(proxyClass);


      proxyFactory.setInterfaces(new Class<?>[]{LifecycleMixin.class});

      proxyFactory.setHandler(new AutoProxifiedMethodHandler(proxyClass, interceptorRegistry));

      try
      {
         return (T) proxyFactory.create(constructorTypes, constructorArguments);
      } catch (Exception e)
      {
         throw new InterceptorException(e);
      }
   }

   public <T> T constructInstrumentedInstance(final Object target, Class<T> proxyClass, Class<?>[] constructorTypes, Object[] constructorArguments) throws IllegalAccessException, InstantiationException
   {
      ProxyFactory proxyFactory = new ProxyFactory();
      if (proxyClass != null)
         proxyFactory.setSuperclass(target.getClass());

      proxyFactory.setHandler(new InstanceProxifyingMethodHandler(target, proxyClass, interceptorRegistry));

      try
      {
         return (T) proxyFactory.create(constructorTypes, constructorArguments);
      } catch (Exception e)
      {
         throw new InterceptorException(e);
      }
   }

   public <T> T createProxyFromInstance(final Object target, Class<T> proxyClass) throws IllegalAccessException, InstantiationException
   {
      return createProxyFromInstance(target, proxyClass, new Class[0], new Object[0]);
   }


   private class InstanceProxifyingMethodHandler implements MethodHandler
   {
      private final Object target;

      private InterceptorRegistry registry;
      private Map<Class<?>, InterceptionHandler> interceptorHandlerInstances = new HashMap<Class<?>, InterceptionHandler>();
      private Class<?> targetClazz;
      private InterceptorClassMetadata targetClassInterceptorMetadata;

      public InstanceProxifyingMethodHandler(Object target, Class<?> targetClass, InterceptorRegistry<Class<?>> registry)
      {
         if (target == null)
            this.target = this;
         else
            this.target = target;

         if (targetClass != null)
            this.targetClazz = targetClass;
         else
            this.targetClazz = this.target.getClass();

         this.registry = registry;

         for (Class<?> interceptorClazz : registry.getInterceptionModel(this.targetClazz).getAllInterceptors())
         {
            interceptorHandlerInstances.put(interceptorClazz, interceptionHandlerFactory.createForClass(interceptorClazz));
         }
         targetClassInterceptorMetadata = InterceptorClassMetadataRegistry.getRegistry().getInterceptorClassMetadata(targetClazz);
         interceptorHandlerInstances.put(targetClazz, new DirectClassInterceptionHandler(target, targetClazz));
      }

      public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
      {
         if (!thisMethod.getDeclaringClass().equals(LifecycleMixin.class))
         {
            if (!isAroundInvokeInterceptionCandidate(thisMethod))
               return proceed.invoke(self, args);
            return executeInterception(thisMethod, args, InterceptionType.AROUND_INVOKE);
         } else
         {
            if (thisMethod.getName().equals(POST_CONSTRUCT))
            {
               return executeInterception(null, null, InterceptionType.POST_CONSTRUCT);
            } else if (thisMethod.getName().equals(PRE_DESTROY))
            {
               return executeInterception(null, null, InterceptionType.PRE_DESTROY);
            }
         }

         return null;
      }


      private Object executeInterception(Method thisMethod, Object[] args, InterceptionType interceptionType) throws Exception
      {
         List<Class<?>> interceptorClasses = registry.getInterceptionModel(targetClazz).getInterceptors(interceptionType, thisMethod);
         List<Class<?>> interceptorClassesForMethod = interceptorClasses == null ? new ArrayList<Class<?>>() : interceptorClasses;
         //assume the list is immutable
         if (targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
         {
            interceptorClassesForMethod = new ArrayList(interceptorClassesForMethod);
            interceptorClassesForMethod.add(targetClazz);
         }
         InterceptionChain chain = new InterceptionChain(interceptorClassesForMethod, interceptionType, target, thisMethod, args, interceptorHandlerInstances);
         return chain.invokeNext(new InterceptorInvocationContext(chain, target, thisMethod, args));
      }
   }


   private static class AutoProxifiedMethodHandler implements MethodHandler
   {
      private InterceptorRegistry registry;
      private Map<Class<?>, InterceptionHandler> interceptorHandlerInstances = new HashMap<Class<?>, InterceptionHandler>();
      private Class<?> targetClazz;
      private InterceptorClassMetadata targetClassInterceptorMetadata;


      public AutoProxifiedMethodHandler(Class<?> targetClazz, InterceptorRegistry<Class<?>> registry)
      {
         if (targetClazz == null)
            throw new IllegalArgumentException("Target class must not be null");

         this.targetClazz = targetClazz;
         this.registry = registry;

         for (Class<?> interceptorClazz : registry.getInterceptionModel(this.targetClazz).getAllInterceptors())
         {
            interceptorHandlerInstances.put(interceptorClazz, new DirectClassInterceptionHandler(interceptorClazz));
         }
         targetClassInterceptorMetadata = InterceptorClassMetadataRegistry.getRegistry().getInterceptorClassMetadata(targetClazz);
      }

      public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
      {
         // do not intercept interceptor methods
         if (!thisMethod.getDeclaringClass().equals(LifecycleMixin.class))
         {
            if (thisMethod.getAnnotation(AroundInvoke.class) != null)
               return proceed.invoke(self, args);
            return executeInterception(self, thisMethod, proceed, args, InterceptionType.AROUND_INVOKE);
         } else
         {
            if (thisMethod.getName().equals(POST_CONSTRUCT))
            {
               return executeInterception(self, null, null, null, InterceptionType.POST_CONSTRUCT);
            } else if (thisMethod.getName().equals(PRE_DESTROY))
            {
               return executeInterception(self, null, null, null, InterceptionType.PRE_DESTROY);
            }
         }

         return null;
      }

      private Object executeInterception(Object self, Method thisMethod, Method proceed, Object[] args, InterceptionType interceptionType) throws Exception
      {
         addSelfAsInterceptorHandler(self);
         List<Class<?>> interceptorClasses = registry.getInterceptionModel(targetClazz).getInterceptors(interceptionType, thisMethod);
         List<Class<?>> interceptorClassesForMethod = interceptorClasses == null ? new ArrayList<Class<?>>() : interceptorClasses;
         if (targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
         {
            interceptorClassesForMethod = new ArrayList(interceptorClassesForMethod);
            interceptorClassesForMethod.add(targetClazz);
         }
         InterceptionChain chain = new InterceptionChain(interceptorClassesForMethod, interceptionType, self, proceed, args, interceptorHandlerInstances);
         return chain.invokeNext(new InterceptorInvocationContext(chain, self, proceed, args));
      }

      private void addSelfAsInterceptorHandler(Object self)
      {
         if (!interceptorHandlerInstances.containsKey(targetClazz))
            interceptorHandlerInstances.put(targetClazz, new DirectClassInterceptionHandler(self, targetClazz));
      }

   }

   public interface LifecycleMixin
   {
      public void lifecycle_mixin_$$_postConstruct();

      public void lifecycle_mixin_$$_preDestroy();
   }
}


