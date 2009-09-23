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

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptionModelImpl<T> implements InterceptionModel<T>
{

   private Map<InterceptionType, List<Class<?>>> lifecycleInterceptors = new HashMap<InterceptionType, List<Class<?>>>();

   private Map<InterceptionType, Map<Method, List<Class<?>>>> methodBoundInterceptors = new HashMap<InterceptionType, Map<Method, List<Class<?>>>>();

   private Set<Class<?>> allInterceptors = new LinkedHashSet<Class<?>>();

   private T interceptedEntity;

   public InterceptionModelImpl(T interceptedEntity)
   {
      this.interceptedEntity = interceptedEntity;
   }

   public List<Class<?>> getInterceptors(InterceptionType interceptionType, Method method)
   {
      if (interceptionType.isLifecycleCallback() && method != null)
         throw new IllegalArgumentException("On a lifecycle callback, associated metod must be null");

      if (!interceptionType.isLifecycleCallback() && method == null)
         throw new IllegalArgumentException("Around-invoke and around-timeout interceptors are defined for a given method");

      if (interceptionType.isLifecycleCallback())
      {
         return lifecycleInterceptors.get(interceptionType);
      } else
      {
         if (methodBoundInterceptors.containsKey(interceptionType))
            return methodBoundInterceptors.get(interceptionType).get(method);
         else
            return null;
      }
   }

   public Set<Class<?>> getAllInterceptors()
   {
      return Collections.unmodifiableSet(allInterceptors);
   }

   public T getInterceptedEntity()
   {
      return this.interceptedEntity;
   }

   public void appendInterceptors(InterceptionType interceptionType, Method method, Class<?>... interceptors)
   {
      if (interceptionType.isLifecycleCallback())
      {
         List<Class<?>> interceptorsList = lifecycleInterceptors.get(interceptionType);
         if (interceptorsList == null)
         {
            interceptorsList = new ArrayList<Class<?>>();
            lifecycleInterceptors.put(interceptionType, interceptorsList);
         }
         appendInterceptorClassesToList(interceptionType, interceptorsList, interceptors);
      } else
      {
         if (null == methodBoundInterceptors.get(interceptionType))
         {
            methodBoundInterceptors.put(interceptionType, new HashMap<Method, List<Class<?>>>());
         }
         List<Class<?>> interceptorsList = methodBoundInterceptors.get(interceptionType).get(method);
         if (interceptorsList == null)
         {
            interceptorsList = new ArrayList<Class<?>>();
            methodBoundInterceptors.get(interceptionType).put(method, interceptorsList);
         }
         appendInterceptorClassesToList(interceptionType, interceptorsList, interceptors);
      }
      for (Class<?> interceptorClass: interceptors )
          allInterceptors.add(interceptorClass);
   }

   private void appendInterceptorClassesToList(InterceptionType interceptionType, List<Class<?>> interceptorsList, Class<?>... interceptors)
   {
      for (Class<?> clazz: interceptors)
         if (interceptorsList.contains(clazz))
            throw new InterceptorException("Duplicate interceptor class definition when binding" + clazz.getName() + " on " + interceptionType.name());
      else
            interceptorsList.add(clazz);
   }

}
