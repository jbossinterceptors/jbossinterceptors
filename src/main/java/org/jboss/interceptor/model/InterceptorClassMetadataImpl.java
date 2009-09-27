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

package org.jboss.interceptor.model;

import org.jboss.interceptor.InterceptorException;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptor.util.ReflectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptorClassMetadataImpl implements InterceptorClassMetadata
{

   private Log log = LogFactory.getLog(InterceptorClassMetadataImpl.class);

   private Class<?> interceptorClass;

   private Map<InterceptionType, List<Method>> methodMap = new HashMap<InterceptionType, List<Method>>();

   public InterceptorClassMetadataImpl(Class<?> interceptorClass)
   {
      this.interceptorClass = interceptorClass;

      Class<?> currentClass = interceptorClass;

      Set<MethodHolder> foundMethods = new HashSet<MethodHolder>();
      do
      {
         Set<InterceptionType> detectedInterceptorTypes = new HashSet<InterceptionType>();

         for (Method method : currentClass.getDeclaredMethods())
         {
            for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes())
            {
               if (InterceptionUtils.isInterceptorMethod(interceptionType, method))
               {
                  if (methodMap.get(interceptionType) == null)
                     methodMap.put(interceptionType, new LinkedList<Method>());
                  if (detectedInterceptorTypes.contains(interceptionType))
                     throw new InterceptorMetadataException("Same interception type cannot be specified twice on the same class");
                  else
                     detectedInterceptorTypes.add(interceptionType);
                  // add method in the list - if it is there already, it means that it has been added by a subclass
                  ReflectionUtils.ensureAccessible(method);
                  if (!foundMethods.contains(new MethodHolder(method)))
                  {
                     methodMap.get(interceptionType).add(0, method);
                  }
               }
            }
            foundMethods.add(new MethodHolder(method));
         }
         currentClass = currentClass.getSuperclass();
      } while (!Object.class.equals(currentClass));
   }

   public Class<?> getInterceptorClass()
   {
      return interceptorClass;
   }

   public List<Method> getInterceptorMethods(InterceptionType interceptionType)
   {
      List<Method> methods = methodMap.get(interceptionType);
      return methods == null ? Collections.EMPTY_LIST : methods;
   }

   private class MethodHolder
   {
      private String methodName;

      private Class<?>[] parameterTypes;


      MethodHolder(Method method)
      {
         this.methodName = method.getName();
         this.parameterTypes = method.getParameterTypes();
      }

      @Override
      public boolean equals(Object o)
      {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         MethodHolder that = (MethodHolder) o;

         if (methodName != null ? !methodName.equals(that.methodName) : that.methodName != null)
            return false;
         if (!Arrays.equals(parameterTypes, that.parameterTypes))
            return false;

         return true;
      }

      @Override
      public int hashCode()
      {
         int result = methodName != null ? methodName.hashCode() : 0;
         result = 31 * result + (parameterTypes != null ? Arrays.hashCode(parameterTypes) : 0);
         return result;
      }
   }
}
