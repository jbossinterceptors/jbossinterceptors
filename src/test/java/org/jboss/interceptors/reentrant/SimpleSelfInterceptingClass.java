/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual
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

package org.jboss.interceptors.reentrant;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * @author Marius Bogoevici
 */
public class SimpleSelfInterceptingClass
{
   private int tries;

   private int maxTries;

   private int interceptionsCount;

   public SimpleSelfInterceptingClass(int maxTries)
   {
      this.maxTries = maxTries;
      tries = 0;
      interceptionsCount = 0;
   }

   public int getTries()
   {
      return tries;
   }

   public int getMaxTries()
   {
      return maxTries;
   }
    
   public int getInterceptionsCount()
   {
      return interceptionsCount;
   }

   public void doSomething()
   {
      if (++tries < maxTries)
      {
         throw new RuntimeException();
      }
   }

   @AroundInvoke
   public Object intercept(InvocationContext context) throws Exception
   {

      boolean isRetriable = false;
      do {
         try
         {
            return context.proceed();
         }
         catch (Exception e)
         {
            isRetriable = interceptionsCount++ < maxTries;
         }
      } while (isRetriable);
      throw new RuntimeException("Max invocations count exceeded");
   }

}
