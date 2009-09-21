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

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptionModelImpl implements InterceptionModel
{

   private Map<InterceptionType, List<Class<?>>> lifecycleInterceptors = new HashMap<InterceptionType, List<Class<?>>>();

   private Map<InterceptionType, Map<Method, List<Class<?>>>> methodBoundInterceptors = new HashMap<InterceptionType, Map<Method, List<Class<?>>>>();

   private Set<Class<?>> allInterceptors = new LinkedHashSet<Class<?>>();


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

   public void setInterceptors(InterceptionType interceptionType, Method method, List<Class<?>> interceptors)
   {
      if (interceptionType.isLifecycleCallback())
      {
         lifecycleInterceptors.put(interceptionType, interceptors);
      } else
      {
         if (null == methodBoundInterceptors.get(interceptionType))
         {
            methodBoundInterceptors.put(interceptionType, new HashMap<Method, List<Class<?>>>());
         }
         methodBoundInterceptors.get(interceptionType).put(method, interceptors);
      }
      allInterceptors.addAll(interceptors);
   }

}
