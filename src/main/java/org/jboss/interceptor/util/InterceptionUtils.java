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

import org.jboss.interceptor.proxy.InterceptorProxyCreatorImpl;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.InterceptionTypeRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptionUtils
{
   private static final Log LOG = LogFactory.getLog(InterceptionUtils.class);

   public static void executePostConstruct(Object proxy)
   {
      if (proxy instanceof InterceptorProxyCreatorImpl.LifecycleMixin)
      {
         InterceptorProxyCreatorImpl.LifecycleMixin lifecycleMixin = (InterceptorProxyCreatorImpl.LifecycleMixin) proxy;
         lifecycleMixin.lifecycle_mixin_$$_postConstruct();
      }
   }

   public static void executePredestroy(Object proxy)
   {
      if (proxy instanceof InterceptorProxyCreatorImpl.LifecycleMixin)
      {
         InterceptorProxyCreatorImpl.LifecycleMixin lifecycleMixin = (InterceptorProxyCreatorImpl.LifecycleMixin) proxy;
         lifecycleMixin.lifecycle_mixin_$$_preDestroy();
      }
   }

   /**
    * @param method
    * @return true if the method has none of the interception type annotations, and is public and not static
    *         false otherwise
    */
   public static boolean isAroundInvokeInterceptionCandidate(Method method)
   {
      // just a provisory implementation
      int modifiers = method.getModifiers();
      for (InterceptionType interceptionType: InterceptionTypeRegistry.getSupportedInterceptionTypes())
      {
         if (method.getAnnotation(InterceptionTypeRegistry.getAnnotationClass(interceptionType)) != null)
            return true;
      }
      return Modifier.isPublic(modifiers) 
            && !Modifier.isStatic(modifiers);
   }

   /**
    *
    * @param interceptionType
    * @param method
    * @return
    */
   public static boolean isInterceptorMethod(InterceptionType interceptionType, Method method)
   {

      if (method.getAnnotation(InterceptionTypeRegistry.getAnnotationClass(interceptionType)) == null)
         return false;

      if (interceptionType.isLifecycleCallback())
      {
         if (!Void.TYPE.equals(method.getReturnType()))
         {
            LOG.warn("Method " + method.getName() + " on class " + method.getDeclaringClass().getName()
                  + " is annotated with " + interceptionType.getAnnotationClassName()
                  + " but does not have a void return type");
            return false;
         }

         Class<?>[] parameterTypes = method.getParameterTypes();

         if (parameterTypes.length > 1)
         {
            LOG.warn("Method " + method.getName() + " on class " + method.getDeclaringClass().getName()
                  + " is annotated with " + interceptionType.getAnnotationClassName()
                  + " but has more than 1 parameter");
            return false;
         }

         if (parameterTypes.length == 1 && !InvocationContext.class.equals(parameterTypes[0]))
         {
            LOG.warn("Method " + method.getName() + " on class " + method.getDeclaringClass().getName()
                  + " is annotated with " + interceptionType.getAnnotationClassName()
                  + " but does not have a " + InvocationContext.class.getName() + " parameter ");
            return false;
         }

         return true;
      } else
      {
         if (!Object.class.equals(method.getReturnType()))
         {
            LOG.warn("Method " + method.getName() + " on class " + method.getDeclaringClass().getName()
                  + " is annotated with " + interceptionType.getAnnotationClassName()
                  + " but does not return a " + Object.class.getName());
            return false;
         }

         Class<?>[] parameterTypes = method.getParameterTypes();

         if (parameterTypes.length != 1)
         {
            LOG.warn("Method " + method.getName() + " on class " + method.getDeclaringClass().getName()
                  + " is annotated with " + interceptionType.getAnnotationClassName()
                  + " but does not have exactly 1 parameter");
            return false;
         }

         if (!InvocationContext.class.equals(parameterTypes[0]))
         {
            LOG.warn("Method " + method.getName() + " on class " + method.getDeclaringClass().getName()
                  + " is annotated with " + interceptionType.getAnnotationClassName()
                  + " but does not have a " + InvocationContext.class.getName() + " parameter ");
            return false;
         }

         return true;
      }
   }
}
