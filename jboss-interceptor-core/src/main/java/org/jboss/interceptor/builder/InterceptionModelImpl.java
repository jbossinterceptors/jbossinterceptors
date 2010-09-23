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

package org.jboss.interceptor.builder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.interceptor.proxy.InterceptorException;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionType;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */

class InterceptionModelImpl<T> implements BuildableInterceptionModel<T>
{

   private Map<InterceptionType, List<InterceptorMetadata>> globalInterceptors = new HashMap<InterceptionType, List<InterceptorMetadata>>();

   private Map<InterceptionType, Map<MethodReference, List<InterceptorMetadata>>> methodBoundInterceptors = new HashMap<InterceptionType, Map<MethodReference, List<InterceptorMetadata>>>();

   private Set<MethodReference> methodsIgnoringGlobals = new HashSet<MethodReference>();

   private Set<InterceptorMetadata> allInterceptors = new LinkedHashSet<InterceptorMetadata>();

   private T interceptedEntity;

   public InterceptionModelImpl(T interceptedEntity)
   {
      this.interceptedEntity = interceptedEntity;
   }

   public List<InterceptorMetadata> getInterceptors(InterceptionType interceptionType, Method method)
   {
      if (interceptionType.isLifecycleCallback() && method != null)
      {
         throw new IllegalArgumentException("On a lifecycle callback, the associated method must be null");
      }

      if (!interceptionType.isLifecycleCallback() && method == null)
      {
         throw new IllegalArgumentException("Around-invoke and around-timeout interceptors are defined for a given method");
      }

      if (interceptionType.isLifecycleCallback())
      {
         if (globalInterceptors.containsKey(interceptionType))
         {
            return globalInterceptors.get(interceptionType);
         }
      }
      else
      {
         ArrayList<InterceptorMetadata> returnedInterceptors = new ArrayList<InterceptorMetadata>();
         if (!methodsIgnoringGlobals.contains(methodHolder(method)) && globalInterceptors.containsKey(interceptionType))
         {
            returnedInterceptors.addAll(globalInterceptors.get(interceptionType));
         }
         if (methodBoundInterceptors.containsKey(interceptionType) && methodBoundInterceptors.get(interceptionType).containsKey(methodHolder(method)))
         {
            returnedInterceptors.addAll(methodBoundInterceptors.get(interceptionType).get(methodHolder(method)));
         }
         return returnedInterceptors;
      }
      return Collections.EMPTY_LIST;
   }

   public Set<InterceptorMetadata> getAllInterceptors()
   {
      return Collections.unmodifiableSet(allInterceptors);
   }

   public T getInterceptedEntity()
   {
      return this.interceptedEntity;
   }

   public void setIgnoresGlobals(Method method, boolean ignoresGlobals)
   {
      if (ignoresGlobals)
      {
         methodsIgnoringGlobals.add(methodHolder(method));
      }
      else
      {
         methodsIgnoringGlobals.remove(methodHolder(method));
      }
   }

   public void appendInterceptors(InterceptionType interceptionType, Method method, InterceptorMetadata... interceptors)
   {
      if (null == method)
      {
         List<InterceptorMetadata> interceptorsList = globalInterceptors.get(interceptionType);
         if (interceptorsList == null)
         {
            interceptorsList = new ArrayList<InterceptorMetadata>();
            globalInterceptors.put(interceptionType, interceptorsList);
         }
         appendInterceptorClassesToList(interceptionType, interceptorsList, interceptors);
      }
      else
      {
         if (null == methodBoundInterceptors.get(interceptionType))
         {
            methodBoundInterceptors.put(interceptionType, new HashMap<MethodReference, List<InterceptorMetadata>>());
         }
         List<InterceptorMetadata> interceptorsList = methodBoundInterceptors.get(interceptionType).get(methodHolder(method));
         if (interceptorsList == null)
         {
            interceptorsList = new ArrayList<InterceptorMetadata>();
            methodBoundInterceptors.get(interceptionType).put(methodHolder(method), interceptorsList);
         }
         if (globalInterceptors.containsKey(interceptionType))
         {
            validateDuplicateInterceptors(interceptionType, globalInterceptors.get(interceptionType), interceptors);
         }
         appendInterceptorClassesToList(interceptionType, interceptorsList, interceptors);
      }
      allInterceptors.addAll(Arrays.asList(interceptors));
   }

   private void appendInterceptorClassesToList(InterceptionType interceptionType, List<InterceptorMetadata> interceptorsList, InterceptorMetadata... interceptors)
   {
      validateDuplicateInterceptors(interceptionType, interceptorsList, interceptors);
      interceptorsList.addAll(Arrays.asList(interceptors));
   }

   private void validateDuplicateInterceptors(InterceptionType interceptionType, List<InterceptorMetadata> interceptorsList, InterceptorMetadata... interceptors)
   {
      for (InterceptorMetadata interceptor : interceptors)
      {
         if (interceptorsList.contains(interceptor))
         {
            if (interceptionType != null)
            {
               throw new InterceptorException("Duplicate interceptor class definition when binding" + interceptor + " on " + interceptionType.name());
            }
         }
      }
   }

   private static MethodReference methodHolder(Method method)
   {
      return MethodReference.of(method, true);
   }

}
