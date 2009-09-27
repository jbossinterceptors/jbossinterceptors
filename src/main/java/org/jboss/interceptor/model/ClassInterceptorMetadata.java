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

import org.jboss.interceptor.proxy.InterceptorException;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class ClassInterceptorMetadata implements InterceptorMetadata
{

   private Class<?> interceptorClass;

   private Map<InterceptionType, List<Method>> methodMap = new HashMap<InterceptionType, List<Method>>();

   public ClassInterceptorMetadata(Class<?> interceptorClass)
   {
      this.interceptorClass = interceptorClass;

      Class<?> currentClass = interceptorClass;


      Set<String> foundMethodNames = new HashSet<String>();
      do
      {
         Set<InterceptionType> detectedInterceptorTypes = new HashSet<InterceptionType>();
         for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes())
         {
            for (Method method : currentClass.getDeclaredMethods())
            {
               if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(InvocationContext.class) && method.getAnnotation(InterceptionTypeRegistry.getAnnotationClass(interceptionType)) != null)
               {
                  if (methodMap.get(interceptionType) == null)
                     methodMap.put(interceptionType, new LinkedList<Method>());
                  if (detectedInterceptorTypes.contains(interceptionType))
                     throw new InterceptorException("Same interception type cannot be specified twice on the same class");
                  else
                     detectedInterceptorTypes.add(interceptionType);
                  // add method in the list - if it is there already, it means that it has been added by a subclass
                  ensureAccessible(method);
                  if (!foundMethodNames.contains(method.getName()))
                  {
                     methodMap.get(interceptionType).add(method);
                     foundMethodNames.add(method.getName());
                  }
               }
            }
         }
         currentClass = currentClass.getSuperclass();
      } while (currentClass != null);
   }

   public static void ensureAccessible(Method method)
   {
      if (!method.isAccessible())
      {
         method.setAccessible(true);
      }
   }

   public Class<?> getInterceptorClass()
   {
      return interceptorClass;
   }

   public List<Method> getInterceptorMethods(InterceptionType interceptionType)
   {
      return methodMap.get(interceptionType);
   }

}
