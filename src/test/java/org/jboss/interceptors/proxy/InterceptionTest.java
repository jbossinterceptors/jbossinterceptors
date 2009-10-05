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

package org.jboss.interceptors.proxy;

import org.jboss.interceptor.model.InterceptionModelBuilder;
import org.jboss.interceptor.proxy.InterceptorProxyCreator;
import org.jboss.interceptor.proxy.InterceptorProxyCreatorImpl;
import org.jboss.interceptor.proxy.DirectClassInterceptionHandlerFactory;
import org.jboss.interceptor.registry.InterceptorRegistry;
import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.interceptors.proxy.FootballTeam;
import org.jboss.interceptors.proxy.InterceptorTestLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptionTest
{
   private static final String TEAM_NAME = "Ajax Amsterdam";

   private String[] expectedLoggedValues = {
         "org.jboss.interceptors.proxy.InterceptionTest$MyFirstInterceptor_postConstruct",
         "org.jboss.interceptors.proxy.InterceptionTest$MyFirstInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_getName",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.InterceptionTest$MyFirstInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_preDestroy"

   };
   private InterceptorProxyCreator interceptorProxyCreator;

   @Before
   public void resetLogAndSetupClasses() throws Exception
   {
      InterceptorTestLogger.reset();
      InterceptorRegistry<Class<?>, Class<?>> interceptorRegistry = new InterceptorRegistry<Class<?>, Class<?>>();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class)Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("getName")).with(MyFirstInterceptor.class, MySecondInterceptor.class);
      builder.interceptPostConstruct().with(MyFirstInterceptor.class);
      builder.interceptPreDestroy().with(MySecondInterceptor.class);
      interceptorRegistry.registerInterceptionModel(FootballTeam.class, builder.build());

      interceptorProxyCreator = new InterceptorProxyCreatorImpl(interceptorRegistry, new DirectClassInterceptionHandlerFactory());

   }

   @Test
   @Ignore
   public void testInterceptionWithInstrumentedClass() throws Exception
   {

      FootballTeam proxy = interceptorProxyCreator.createInstrumentedInstance(FootballTeam.class, new Class<?>[]{String.class}, new Object[]{TEAM_NAME});
      //FootballTeam proxy = interceptorProxyCreator.createProxyFromInstance(new FootballTeam(TEAM_NAME), FootballTeam.class);
      executeAssertionsOnProxy(proxy);

   }


   @Test
   public void testInterceptionWithProxifiedObject() throws Exception
   {
      FootballTeam proxy = interceptorProxyCreator.createProxyFromInstance(new FootballTeam(TEAM_NAME), FootballTeam.class);
      proxy = interceptorProxyCreator.createProxyFromInstance(proxy, FootballTeam.class);
      executeAssertionsOnProxy(proxy);

   }

   private void executeAssertionsOnProxy(FootballTeam proxy)
   {
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValues, logValues);
   }

   private String iterateAndDisplay(Object[] logValues)
   {
      StringBuffer buffer = new StringBuffer();
      for (Object logValue: logValues)
      {
         buffer.append(logValue.toString()).append("\n");
      }
      return buffer.toString();
   }


   public static class MyFirstInterceptor
   {

      @AroundInvoke
      private Object doAround(InvocationContext invocationContext) throws Exception
      {
         InterceptorTestLogger.add(MyFirstInterceptor.class, "aroundInvokeBefore");
         Object result = invocationContext.proceed();
         InterceptorTestLogger.add(MyFirstInterceptor.class, "aroundInvokeAfter");
         return result;
      }

      @PostConstruct
      public void doAfterConstruction(InvocationContext invocationContext) throws Exception
      {
         InterceptorTestLogger.add(MyFirstInterceptor.class, "postConstruct");
      }
   }

   public static class MySecondInterceptor extends MyFirstInterceptor
   {
      @AroundInvoke
      private Object doAround(InvocationContext invocationContext) throws Exception
      {
         InterceptorTestLogger.add(MySecondInterceptor.class, "aroundInvokeBefore");
         Object result = invocationContext.proceed();
         InterceptorTestLogger.add(MySecondInterceptor.class, "aroundInvokeAfter");
         return result;
      }

      @PreDestroy
      private void doneHere(InvocationContext invocationContext) throws Exception
      {
         InterceptorTestLogger.add(MySecondInterceptor.class, "preDestroy");
      }
   }
}

