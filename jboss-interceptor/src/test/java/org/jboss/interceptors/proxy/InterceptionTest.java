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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.Serializable;

import org.jboss.interceptor.model.InterceptionModelBuilder;
import org.jboss.interceptor.model.InterceptionModel;
import org.jboss.interceptor.proxy.DirectClassInterceptionHandlerFactory;
import org.jboss.interceptor.registry.InterceptorRegistry;
import org.jboss.interceptor.util.InterceptionUtils;

import org.junit.Assert;
import org.junit.Test;

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

   private String[] expectedLoggedValuesWithGlobalsIgnored = {
         "org.jboss.interceptors.proxy.InterceptionTest$MyFirstInterceptor_postConstruct",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_getName",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_preDestroy"
   };

   private String[] expectedLoggedValuesOnSerialization = {
         "org.jboss.interceptors.proxy.FootballTeam_prePassivating",
         "org.jboss.interceptors.proxy.FootballTeam_postActivating",
         "org.jboss.interceptors.proxy.InterceptionTest$MyFirstInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_getName",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.InterceptionTest$MySecondInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.InterceptionTest$MyFirstInterceptor_aroundInvokeAfter",
   };
   private InterceptionModel<Class<?>, Class<?>> interceptionModel;
   private InterceptorRegistry<Class<?>, Class<?>> interceptorRegistry;

   public void resetLogAndSetupClassesForMethod() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("getName")).with(MyFirstInterceptor.class, MySecondInterceptor.class);
      builder.interceptPostConstruct().with(MyFirstInterceptor.class);
      builder.interceptPreDestroy().with(MySecondInterceptor.class);
      interceptionModel = builder.build();
      this.interceptorRegistry = new InterceptorRegistry<Class<?>, Class<?>>();
      this.interceptorRegistry.registerInterceptionModel(FootballTeam.class, interceptionModel);

   }

   public void resetLogAndSetupClassesGlobally() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAll().with(MyFirstInterceptor.class, MySecondInterceptor.class);
      interceptionModel = builder.build();
      this.interceptorRegistry = new InterceptorRegistry<Class<?>, Class<?>>();
      this.interceptorRegistry.registerInterceptionModel(FootballTeam.class, interceptionModel);

   }

   public void resetLogAndSetupClassesMixed() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);
      builder.interceptAll().with(MyFirstInterceptor.class);
      builder.interceptPreDestroy().with(MySecondInterceptor.class);
      builder.interceptAroundInvoke(FootballTeam.class.getMethod("getName")).with(MySecondInterceptor.class);
      interceptionModel = builder.build();
      this.interceptorRegistry = new InterceptorRegistry<Class<?>, Class<?>>();
      this.interceptorRegistry.registerInterceptionModel(FootballTeam.class, interceptionModel);

   }

   public void resetLogAndSetupClassesWithGlobalsIgnored() throws Exception
   {
      InterceptorTestLogger.reset();
      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);
      builder.interceptAll().with(MyFirstInterceptor.class);
      builder.interceptPreDestroy().with(MySecondInterceptor.class);
      builder.interceptAroundInvoke(FootballTeam.class.getMethod("getName")).with(MySecondInterceptor.class);
      builder.ignoreGlobalInterceptors(FootballTeam.class.getMethod("getName"));
      interceptionModel = builder.build();
      this.interceptorRegistry = new InterceptorRegistry<Class<?>, Class<?>>();
      this.interceptorRegistry.registerInterceptionModel(FootballTeam.class, interceptionModel);

   }


   @Test
   public void testInterceptionWithMethodRegisteredInterceptors() throws Exception
   {
      resetLogAndSetupClassesForMethod();
      FootballTeam proxy = InterceptionUtils.proxifyInstance(new FootballTeam(TEAM_NAME), FootballTeam.class, interceptorRegistry, new DirectClassInterceptionHandlerFactory());
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValues, logValues);      
   }

   @Test
   public void testInterceptionWithGlobalInterceptors() throws Exception
   {
      resetLogAndSetupClassesGlobally();
      FootballTeam proxy = InterceptionUtils.proxifyInstance(new FootballTeam(TEAM_NAME), FootballTeam.class, interceptorRegistry, new DirectClassInterceptionHandlerFactory());
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
   }

   @Test
   public void testInterceptionWithMixedInterceptors() throws Exception
   {
      resetLogAndSetupClassesMixed();
      FootballTeam proxy = InterceptionUtils.proxifyInstance(new FootballTeam(TEAM_NAME), FootballTeam.class, interceptorRegistry, new DirectClassInterceptionHandlerFactory());
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValues, logValues);

   }

   @Test
   public void testInterceptionWithGlobalsIgnored() throws Exception
   {
      resetLogAndSetupClassesWithGlobalsIgnored();
      FootballTeam proxy = InterceptionUtils.proxifyInstance(new FootballTeam(TEAM_NAME), FootballTeam.class, interceptorRegistry, new DirectClassInterceptionHandlerFactory());
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValuesWithGlobalsIgnored, logValues);
   }


   @Test
   public void testInterceptionWithSerializedProxy() throws Exception
   {
      resetLogAndSetupClassesForMethod();
      FootballTeam proxy = InterceptionUtils.proxifyInstance(new FootballTeam(TEAM_NAME), FootballTeam.class, interceptorRegistry, new DirectClassInterceptionHandlerFactory());
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new ObjectOutputStream(baos).writeObject(proxy);
      proxy = (FootballTeam) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValuesOnSerialization, logValues);
   }

   private String iterateAndDisplay(Object[] logValues)
   {
      StringBuffer buffer = new StringBuffer();
      for (Object logValue : logValues)
      {
         buffer.append(logValue.toString()).append("\n");
      }
      return buffer.toString();
   }


   public static class MyFirstInterceptor implements Serializable
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

