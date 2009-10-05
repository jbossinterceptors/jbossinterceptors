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
import javassist.util.proxy.ProxyObject;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.InterceptorClassMetadata;
import org.jboss.interceptor.registry.InterceptorRegistry;
import org.jboss.interceptor.registry.InterceptorClassMetadataRegistry;
import static org.jboss.interceptor.util.InterceptionUtils.isAroundInvokeInterceptionCandidate;
import org.jboss.interceptor.InterceptorException;

import javax.interceptor.AroundInvoke;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.*;

import sun.reflect.ReflectionFactory;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptorProxyCreatorImpl<I> implements InterceptorProxyCreator
{
   public static final String POST_CONSTRUCT = "lifecycle_mixin_$$_postConstruct";
   public static final String PRE_DESTROY = "lifecycle_mixin_$$_preDestroy";

   private InterceptorRegistry<Class<?>, I> interceptorRegistry;

   private InterceptionHandlerFactory<I> interceptionHandlerFactory;

   public InterceptorProxyCreatorImpl(InterceptorRegistry<Class<?>, I> interceptorRegistry, InterceptionHandlerFactory<I> interceptionHandlerFactory)
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

      InstanceProxifyingMethodHandler instanceProxifyingMethodHandler = new InstanceProxifyingMethodHandler(target, proxyClass, interceptorRegistry);
      proxyFactory.setHandler(instanceProxifyingMethodHandler);

      try
      {
         //return (T) proxyFactory.create(constructorTypes, constructorArguments);

         Class<T> clazz = proxyFactory.createClass();
         ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();
         Constructor<T> c = reflectionFactory.newConstructorForSerialization(clazz, Object.class.getDeclaredConstructor());
         T proxyObject = c.newInstance();
         ((ProxyObject)proxyObject).setHandler(instanceProxifyingMethodHandler);
         return proxyObject;
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

   public MethodHandler createInstanceProxifyingMethodHandler(final Object target, Class<?> proxyClass)
   {
      return new InstanceProxifyingMethodHandler(target, proxyClass, interceptorRegistry);
   }


   private static ThreadLocal<Stack<Method>> interceptionStack = new ThreadLocal<Stack<Method>>();
   
   private class InstanceProxifyingMethodHandler implements MethodHandler
   {
      private final Object target;

      private InterceptorRegistry<Class<?>, I> registry;
      private Map<I, InterceptionHandler> interceptorHandlerInstances = new HashMap<I, InterceptionHandler>();
      private Class<?> targetClazz;
      private InterceptorClassMetadata targetClassInterceptorMetadata;


      public InstanceProxifyingMethodHandler(Object target, Class<?> targetClass, InterceptorRegistry<Class<?>, I> registry)
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

         for (I interceptorClazz : registry.getInterceptionModel(this.targetClazz).getAllInterceptors())
         {
            interceptorHandlerInstances.put(interceptorClazz, interceptionHandlerFactory.createFor(interceptorClazz));
         }
         targetClassInterceptorMetadata = InterceptorClassMetadataRegistry.getRegistry().getInterceptorClassMetadata(targetClazz);
         //interceptorHandlerInstances.put(targetClazz, interceptionHandlerFactory.createFor(i));
      }

      public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
      {
         if (getInterceptionStack().contains(thisMethod))
            return thisMethod.invoke(target, args);
         try
         {
            getInterceptionStack().push(thisMethod);

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
         } finally
         {
            getInterceptionStack().remove(thisMethod);
         }


      }

      private Stack<Method> getInterceptionStack()
      {
         if (interceptionStack.get() == null)
            interceptionStack.set(new Stack<Method>());
         return interceptionStack.get();
      }


      private Object executeInterception(Method thisMethod, Object[] args, InterceptionType interceptionType) throws Exception
      {
         List<I> interceptorClasses = registry.getInterceptionModel(targetClazz).getInterceptors(interceptionType, thisMethod);
         //assume the list is immutable

         List<InterceptionHandler> interceptionHandlers = new ArrayList<InterceptionHandler>();
         for (I interceptorReference : interceptorClasses)
         {
            interceptionHandlers.add(interceptorHandlerInstances.get(interceptorReference));
         }

         if (targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
         {
            interceptionHandlers.add(new DirectClassInterceptionHandler<Class<?>>(targetClazz));
         }

         InterceptionChain chain = new InterceptionChain(interceptionHandlers, interceptionType, target, thisMethod, args);
         return chain.invokeNext(new InterceptorInvocationContext(chain, target, thisMethod, args));
      }
   }


   private class AutoProxifiedMethodHandler implements MethodHandler
   {
      private InterceptorRegistry<Class<?>, I> registry;
      private Map<I, InterceptionHandler> interceptorHandlerInstances = new HashMap<I, InterceptionHandler>();
      private Class<?> targetClazz;
      private InterceptorClassMetadata targetClassInterceptorMetadata;


      public AutoProxifiedMethodHandler(Class<?> targetClazz, InterceptorRegistry<Class<?>, I> registry)
      {
         if (targetClazz == null)
            throw new IllegalArgumentException("Target class must not be null");

         this.targetClazz = targetClazz;
         this.registry = registry;

         for (I interceptorClazz : registry.getInterceptionModel(this.targetClazz).getAllInterceptors())
         {
            interceptorHandlerInstances.put(interceptorClazz, interceptionHandlerFactory.createFor(interceptorClazz));
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

         List<I> interceptorClasses = registry.getInterceptionModel(targetClazz).getInterceptors(interceptionType, thisMethod);
         List<InterceptionHandler> interceptionHandlers = new ArrayList<InterceptionHandler>();
         for (I interceptorReference : interceptorClasses)
         {
            interceptionHandlers.add(interceptorHandlerInstances.get(interceptorReference));
         }

         if (targetClassInterceptorMetadata.getInterceptorMethods(interceptionType) != null && !targetClassInterceptorMetadata.getInterceptorMethods(interceptionType).isEmpty())
         {
            interceptionHandlers.add(new DirectClassInterceptionHandler<Class<?>>(targetClazz));
         }
         InterceptionChain chain = new InterceptionChain(interceptionHandlers, interceptionType, self, proceed, args);
         return chain.invokeNext(new InterceptorInvocationContext(chain, self, proceed, args));
      }


   }

   public interface LifecycleMixin
   {
      public void lifecycle_mixin_$$_postConstruct();

      public void lifecycle_mixin_$$_preDestroy();
   }
}


