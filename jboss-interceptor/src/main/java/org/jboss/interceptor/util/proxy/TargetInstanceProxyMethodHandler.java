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

package org.jboss.interceptor.util.proxy;

import java.lang.reflect.Method;
import java.io.Serializable;

import javassist.util.proxy.MethodHandler;

/**
 * @author Marius Bogoevici
 */
public abstract class TargetInstanceProxyMethodHandler<T> implements MethodHandler, Serializable
{
   private T targetInstance;

   private Class<? extends T> targetClass;
   
   public TargetInstanceProxyMethodHandler(T targetInstance, Class<? extends T> targetClass)
   {
      this.targetInstance = targetInstance;
      this.targetClass = targetClass;
   }

   public final Object invoke(Object proxyInstance, Method interceptedMethod, Method proceed, Object[] args) throws Throwable
   {
      if (interceptedMethod.getDeclaringClass().equals(TargetInstanceProxy.class))
      {
         if (interceptedMethod.getName().equals("getTargetInstance"))
         {
            return this.getTargetInstance();
         }
         else if (interceptedMethod.getName().equals("getTargetClass"))
         {
            return this.getTargetClass();
         }
         else
         {
            // we shouldn't arrive here
            return null;
         }
      }
      else
      {
         return doInvoke(proxyInstance, interceptedMethod, proceed, args);
      }
   }

   /**/
   protected abstract Object doInvoke(Object proxyInstance, Method interceptedMethod, Method proceedingMethod, Object[] invocationArguments) throws Throwable;

   public T getTargetInstance()
   {
      return targetInstance;
   }

   public Class<? extends T> getTargetClass()
   {
      return targetClass;
   }
}

