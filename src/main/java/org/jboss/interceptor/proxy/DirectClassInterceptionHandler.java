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
import org.jboss.interceptor.model.InterceptorClassMetadata;
import org.jboss.interceptor.registry.InterceptorClassMetadataRegistry;
import org.jboss.interceptor.InterceptorException;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class DirectClassInterceptionHandler<I> implements InterceptionHandler
{

   private final Object interceptorInstance;

   private InterceptorClassMetadata interceptorMetadata;

   private Class<?> clazz;

   public DirectClassInterceptionHandler(Object interceptorInstance, Class<?> clazz)
   {
      if (interceptorInstance == null)
         throw new IllegalArgumentException("Interceptor instance cannot be null");

      this.clazz = (clazz == null) ? interceptorInstance.getClass() : clazz;
      this.interceptorInstance = interceptorInstance;
      this.interceptorMetadata = InterceptorClassMetadataRegistry.getRegistry().getInterceptorClassMetadata(this.clazz);

   }

   public DirectClassInterceptionHandler(Class<?> simpleInterceptorClass)
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
      this.interceptorMetadata = InterceptorClassMetadataRegistry.getRegistry().getInterceptorClassMetadata(this.clazz);

   }

   public Object invoke(Object target, InterceptionType interceptionType, InvocationContext invocationContext) throws Exception
   {
      List<Method> methods = interceptorMetadata.getInterceptorMethods(interceptionType);
      if (methods != null)
      {
         DelegatingInvocationContext delegatingInvocationContext = new DelegatingInvocationContext(invocationContext, interceptorInstance, methods);
         return delegatingInvocationContext.proceed();
      } else
         throw new InterceptorException(target.toString() + " was requested to perform " + interceptionType.name() + " but no such method is defined on it");
   }


   public boolean handles(Class<?> clazz)
   {
      return this.clazz.equals(clazz);
   }

   public class DelegatingInvocationContext implements InvocationContext
   {

      private InvocationContext delegateInvocationContext;

      private Object targetObject;

      private Queue<Method> invocationQueue;

      public DelegatingInvocationContext(InvocationContext delegateInvocationContext, Object targetObject, List<Method> methods)
      {
         this.delegateInvocationContext = delegateInvocationContext;
         this.targetObject = targetObject;
         this.invocationQueue = new ConcurrentLinkedQueue<Method>(methods);
      }

      public Map<String, Object> getContextData()
      {
         return delegateInvocationContext.getContextData();
      }

      public Method getMethod()
      {
         return delegateInvocationContext.getMethod();
      }

      public Object[] getParameters()
      {
         return delegateInvocationContext.getParameters();
      }

      public Object getTarget()
      {
         return delegateInvocationContext.getTarget();
      }

      public Object proceed() throws Exception
      {
         if (!invocationQueue.isEmpty())
         {

            Method interceptorMethod = invocationQueue.remove();
            if (interceptorMethod.getParameterTypes().length == 0)
               return interceptorMethod.invoke(targetObject);
            else
               return interceptorMethod.invoke(targetObject, this);
         } else
         {
            return delegateInvocationContext.proceed();
         }
      }

      public void setParameters(Object[] params)
      {
         delegateInvocationContext.setParameters(params);
      }
   }

}

