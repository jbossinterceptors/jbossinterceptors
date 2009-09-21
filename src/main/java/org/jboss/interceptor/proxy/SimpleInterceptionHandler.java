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

import org.jboss.interceptor.model.InterceptionType;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class SimpleInterceptionHandler implements InterceptionHandler
{

   private final Object interceptorInstance;

   private final Map<InterceptionType, Method> interceptorMethods = new HashMap<InterceptionType, Method>();

   private Class<?> clazz;

   public SimpleInterceptionHandler(Object interceptorInstance, Class<?> clazz)
   {
      if (interceptorInstance == null)
         throw new IllegalArgumentException("Interceptor instance cannot be null");

      this.clazz = (clazz == null) ?interceptorInstance.getClass():clazz;
      this.interceptorInstance = interceptorInstance;
      for (InterceptionType interceptionType : InterceptionType.values())
      {
         for (Method method : clazz.getDeclaredMethods())
         {
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(InvocationContext.class) && method.getAnnotation(interceptionType.getAssociatedAnnotation()) != null)
            {
               interceptorMethods.put(interceptionType, method);
            }
         }
      }

   }

   public SimpleInterceptionHandler(Class<?> simpleInterceptorClass)
   {

      if (simpleInterceptorClass == null)
         throw new IllegalArgumentException("Class must not be null");

      this.clazz = simpleInterceptorClass;
      try
      {
         this.interceptorInstance = simpleInterceptorClass.newInstance();
      } catch (Exception e)
      {
         throw new InterceptorException("Cannot create interceptor instance:", e);
      }
      for (InterceptionType interceptionType : InterceptionType.values())
      {
         for (Method method : simpleInterceptorClass.getDeclaredMethods())
         {
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(InvocationContext.class) && method.getAnnotation(interceptionType.getAssociatedAnnotation()) != null)
            {
               interceptorMethods.put(interceptionType, method);
            }
         }
      }

   }

   public Object invoke(Object target, InterceptionType interceptionType, InvocationContext invocationContext)
   {
      try
      {
         return interceptorMethods.get(interceptionType).invoke(interceptorInstance, new Object[]{invocationContext});
      } catch (IllegalAccessException e)
      {
         throw new RuntimeException((e));
      } catch (InvocationTargetException e)
      {
         throw new RuntimeException(e);
      }
   }


   public boolean handles(Class<?> clazz)
   {
      return this.clazz.equals(clazz);
   }
}
