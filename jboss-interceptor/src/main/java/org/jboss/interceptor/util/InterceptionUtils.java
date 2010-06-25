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

package org.jboss.interceptor.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.interceptor.InvocationContext;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.jboss.interceptor.InterceptorException;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.InterceptionTypeRegistry;
import org.jboss.interceptor.model.MethodHolder;
import org.jboss.interceptor.model.metadata.InterceptorMetadataException;
import org.jboss.interceptor.model.metadata.reader.ClassMetadataProvider;
import org.jboss.interceptor.model.metadata.reader.MethodMetadataProvider;
import org.jboss.interceptor.proxy.LifecycleMixin;
import org.jboss.interceptor.util.proxy.TargetInstanceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptionUtils
{
   private static final Logger LOG = LoggerFactory.getLogger(InterceptionUtils.class);
   public static final String POST_CONSTRUCT = "lifecycle_mixin_$$_postConstruct";
   public static final String PRE_DESTROY = "lifecycle_mixin_$$_preDestroy";


   private static Class<? extends Annotation> INTERCEPTORS_ANNOTATION_CLASS = null;
   private static Class<? extends Annotation> EXCLUDE_CLASS_INTERCEPTORS_ANNOTATION_CLASS = null;

   static
   {
      try
      {
         INTERCEPTORS_ANNOTATION_CLASS = (Class<? extends Annotation>) Class.forName("javax.interceptor.Interceptors");
         EXCLUDE_CLASS_INTERCEPTORS_ANNOTATION_CLASS = (Class<? extends Annotation>) Class.forName("javax.interceptor.ExcludeClassInterceptors");
      }
      catch (ClassNotFoundException e)
      {
         //do nothing
      }
   }

   public static void executePostConstruct(Object proxy, Callable callback)
   {
      if (proxy instanceof LifecycleMixin)
      {
         LifecycleMixin lifecycleMixin = (LifecycleMixin) proxy;
         lifecycleMixin.lifecycle_mixin_$$_postConstruct();
      }
      if (callback != null)
      {
         try
         {
            callback.call();
         }
         catch (Exception e)
         {
            throw new InterceptorException(e);
         }
      }
   }

   public static void executePostConstruct(Object proxy)
   {
      executePostConstruct(proxy, null);
   }

   public static void executePredestroy(Object proxy, Callable callback)
   {
      if (proxy instanceof LifecycleMixin)
      {
         LifecycleMixin lifecycleMixin = (LifecycleMixin) proxy;
         lifecycleMixin.lifecycle_mixin_$$_preDestroy();
      }
      if (callback != null)
      {
         try
         {
            callback.call();
         }
         catch (Exception e)
         {
            throw new InterceptorException(e);
         }
      }
   }

   public static void executePredestroy(Object proxy)
   {
      executePredestroy(proxy, null);
   }

   /**
    * @param method
    * @return true if the method has none of the interception type annotations, and is public and not static
    *         false otherwise
    */
   public static boolean isInterceptionCandidate(Method method)
   {
      // just a provisory implementation - any method which is not an interceptor method
      // is an interception candidate
      if (method.getDeclaringClass().equals(Object.class))
         return false;
      int modifiers = method.getModifiers();
      if (Modifier.isStatic(modifiers))
         return false;
      for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes())
      {
         if (method.getAnnotation(InterceptionTypeRegistry.getAnnotationClass(interceptionType)) != null)
         {
            return false;
         }
      }
      return true;
   }

   /**
    * @param interceptionType
    * @param method
    * @param forTargetClass
    * @return
    */
   public static boolean isInterceptorMethod(InterceptionType interceptionType, MethodMetadataProvider method, boolean forTargetClass)
   {

      if (method.getAnnotation(InterceptionTypeRegistry.getAnnotationClass(interceptionType)) == null)
      {
         return false;
      }

      if (interceptionType.isLifecycleCallback())
      {
         if (!Void.TYPE.equals(method.getReturnType().getJavaClass()))
         {
            if (LOG.isDebugEnabled())
            {
             LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have a void return type");
            }
            return false;
         }

         Class<?>[] parameterTypes = method.getJavaMethod().getParameterTypes();

         if (forTargetClass && parameterTypes.length != 0)
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "is defined on the target class and does not have 0 arguments");
            }
            return false;
         }

         if (!forTargetClass && parameterTypes.length != 1)
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have exactly one parameter");
            }
            return false;
         }

         if (parameterTypes.length == 1 && !InvocationContext.class.equals(parameterTypes[0]))
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "its single argument is not a " + InvocationContext.class.getName());
            }
            return false;
         }

         return true;
      }
      else
      {
         if (!Object.class.equals(method.getReturnType().getJavaClass()))
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not return a " + Object.class.getName());
            }
            return false;
         }

         Class<?>[] parameterTypes = method.getJavaMethod().getParameterTypes();

         if (parameterTypes.length != 1)
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have exactly 1 parameter");
            }
            return false;
         }

         if (!InvocationContext.class.equals(parameterTypes[0]))
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have a " + InvocationContext.class.getName() + " parameter ");
            }
            return false;
         }

         return true;
      }
   }

   private static String getStandardIgnoredMessage(InterceptionType interceptionType, Method method)
   {
      return "Method " + method.getName() + " defined on class " + method.getDeclaringClass().getName()
            + " will not be used for interception, since it is not defined according to the specification. It is annotated with @"
            + interceptionType.annotationClassName() + ", but ";
   }

   public static boolean supportsEjb3InterceptorDeclaration()
   {
      return INTERCEPTORS_ANNOTATION_CLASS != null && EXCLUDE_CLASS_INTERCEPTORS_ANNOTATION_CLASS != null;
   }


   public static Class<? extends Annotation> getInterceptorsAnnotationClass()
   {
      return INTERCEPTORS_ANNOTATION_CLASS;
   }

   public static Class<? extends Annotation> getExcludeClassInterceptorsAnnotationClass()
   {
      return EXCLUDE_CLASS_INTERCEPTORS_ANNOTATION_CLASS;
   }


   public static <T> Class<T> createProxyClass(Class<T> proxyClass, boolean forSubclassing)
   {
      ProxyFactory proxyFactory = new ProxyFactory();
      if (proxyClass != null)
      {
         proxyFactory.setSuperclass(proxyClass);
         proxyFactory.setUseWriteReplace(false);
      }

      if (forSubclassing)
         proxyFactory.setInterfaces(new Class<?>[]{LifecycleMixin.class, TargetInstanceProxy.class});
      else
         proxyFactory.setInterfaces(new Class<?>[]{LifecycleMixin.class, TargetInstanceProxy.class});
      Class<T> clazz = proxyFactory.createClass();
      return clazz;
   }

   public static <T> Class<T> createProxyClassWithHandler(Class<T> proxyClass, MethodHandler methodHandler)
   {
      ProxyFactory proxyFactory = new ProxyFactory();
      proxyFactory.setUseWriteReplace(false);
      if (proxyClass != null)
      {
         proxyFactory.setSuperclass(proxyClass);
      }
      proxyFactory.setInterfaces(new Class<?>[]{LifecycleMixin.class, TargetInstanceProxy.class});
      proxyFactory.setHandler(methodHandler);
      Class<T> clazz = proxyFactory.createClass();
      return clazz;
   }

   public static Map<InterceptionType, List<MethodMetadataProvider>> buildMethodMap(ClassMetadataProvider interceptorClass, boolean isTargetClass)
   {
      Map<InterceptionType, List<MethodMetadataProvider>> methodMap = new HashMap<InterceptionType, List<MethodMetadataProvider>>();
      ClassMetadataProvider currentClass = interceptorClass;
      Set<MethodHolder> foundMethods = new HashSet<MethodHolder>();
      do
      {
         Set<InterceptionType> detectedInterceptorTypes = new HashSet<InterceptionType>();

         for (MethodMetadataProvider method : currentClass.getDeclaredMethods())
         {
            for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes())
            {
               if (isInterceptorMethod(interceptionType, method, isTargetClass))
               {
                  if (methodMap.get(interceptionType) == null)
                  {
                     methodMap.put(interceptionType, new LinkedList<MethodMetadataProvider>());
                  }
                  if (detectedInterceptorTypes.contains(interceptionType))
                  {
                     throw new InterceptorMetadataException("Same interception type cannot be specified twice on the same class");
                  }
                  else
                  {
                     detectedInterceptorTypes.add(interceptionType);
                  }
                  // add method in the list - if it is there already, it means that it has been added by a subclass
                  ReflectionUtils.ensureAccessible(method.getJavaMethod());
                  if (!foundMethods.contains(MethodHolder.of(method, false)))
                  {
                     methodMap.get(interceptionType).add(0, method);
                  }
               }
            }
            foundMethods.add(MethodHolder.of(method, false));
         }
         currentClass = currentClass.getSuperclass();
      }
      while (!Object.class.equals(currentClass.getJavaClass()));
      return methodMap;
   }
}
