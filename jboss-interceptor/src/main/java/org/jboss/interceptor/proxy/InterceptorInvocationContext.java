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

import org.jboss.interceptor.InterceptorException;

import javax.interceptor.InvocationContext;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptorInvocationContext implements InvocationContext
{

   private Map<String, Object> contextData = new HashMap<String, Object>();

   private Method method;

   private Object[] parameters;
   
   private Object target;

   private InterceptionChain interceptionChain;

   private Object timer;

   public InterceptorInvocationContext(InterceptionChain interceptionChain, Object target, Method targetMethod, Object[] parameters)
   {
      this.interceptionChain = interceptionChain;
      this.method = targetMethod;
      this.parameters = parameters;
      this.target = target;
      this.timer = null;
   }

   public InterceptorInvocationContext(InterceptionChain interceptionChain, Object target, Method targetMethod, Object timer)
   {
      this.interceptionChain = interceptionChain;
      this.method = targetMethod;
      this.timer = timer;
      this.target = target;
      this.parameters = null;
   }

   public Map<String, Object> getContextData()
   {
      return contextData;
   }

   public Method getMethod()
   {
      return method;
   }

   public Object[] getParameters()
   {
      return parameters;
   }

   public Object getTarget()
   {
      return target;
   }

   public Object proceed() throws Exception
   {
      try
      {
         return interceptionChain.invokeNext(this);
      }
      catch (Exception e)
      {
         throw e;
      }
      catch (Throwable t)
      {
         throw new InterceptorException(t);
      }
   }

   public void setParameters(Object[] params)
   {
      if (method != null)
      {
         // there is no requirement to do anything if params is null
         // but this is theoretically possible only if the target method has no arguments
         int newParametersCount = params == null? 0 : params.length;
         if (method.getParameterTypes().length != newParametersCount)
            throw new IllegalArgumentException("Wrong number of parameters: method has " + method.getParameterTypes().length
                  + ", attempting to set " + newParametersCount + (params != null?"": " (argument was null)"));
         if (params != null)
         {
            for (int i=0; i<params.length; i++)
            {
               Class<?> parameterClass = method.getParameterTypes()[i];
               if (params[i] != null)
               {
                  if (!method.getParameterTypes()[i].isAssignableFrom(params[i].getClass()))
                  {
                     throw new IllegalArgumentException("Incompatible parameter: " + params[i] + " (expected type was " + method.getParameterTypes()[i].getName() + ")");
                  }
               }
               else
               {
                  if (method.getParameterTypes()[i].isPrimitive())
                  {
                     throw new IllegalArgumentException("Trying to set a null value on a " + method.getParameterTypes()[i].getName());
                  }
               }
            }
            this.parameters = params;
         }
      }
      else
      {
         throw new IllegalStateException("Illegal invocation to setParameters() during lifecycle invocation");
      }
   }

   public Object getTimer()
   {
      return timer;
   }

}
