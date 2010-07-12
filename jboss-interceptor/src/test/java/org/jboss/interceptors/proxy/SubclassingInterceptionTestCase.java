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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import javassist.util.proxy.ProxyObject;
import org.jboss.interceptor.builder.InterceptionModelBuilder;
import org.jboss.interceptor.registry.InterceptorMetadataRegistry;
import org.jboss.interceptor.registry.SimpleInterceptorMetadataRegistry;
import org.jboss.interceptor.proxy.DirectClassInterceptionHandlerFactory;
import org.jboss.interceptor.proxy.InterceptorProxyCreator;
import org.jboss.interceptor.proxy.InterceptorProxyCreatorImpl;
import org.jboss.interceptor.proxy.javassist.CompositeHandler;
import org.jboss.interceptor.reader.ReflectiveClassMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.util.InterceptionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class SubclassingInterceptionTestCase
{
   private static final String TEAM_NAME = "Ajax Amsterdam";

   private String[] expectedLoggedValues = {
         "org.jboss.interceptors.proxy.FirstInterceptor_postConstruct",
         "org.jboss.interceptors.proxy.Team_postConstruct",
         "org.jboss.interceptors.proxy.FootballTeam_postConstruct",
         "org.jboss.interceptors.proxy.FirstInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.SecondInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_getName",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.SecondInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.FirstInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.SecondInterceptor_preDestroy"
   };

   private String[] expectedLoggedValuesWithGlobalsIgnored = {
         "org.jboss.interceptors.proxy.FirstInterceptor_postConstruct",
         "org.jboss.interceptors.proxy.Team_postConstruct",
         "org.jboss.interceptors.proxy.FootballTeam_postConstruct",
         "org.jboss.interceptors.proxy.SecondInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_getName",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.SecondInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.SecondInterceptor_preDestroy"
   };

   private String[] expectedLoggedValuesOnSerialization = {
         "org.jboss.interceptors.proxy.FirstInterceptor_postConstruct",
         "org.jboss.interceptors.proxy.Team_postConstruct",
         "org.jboss.interceptors.proxy.FootballTeam_postConstruct",
         "org.jboss.interceptors.proxy.FootballTeam_prePassivating",
         "org.jboss.interceptors.proxy.FootballTeam_postActivating",
         "org.jboss.interceptors.proxy.FirstInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.SecondInterceptor_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeBefore",
         "org.jboss.interceptors.proxy.FootballTeam_getName",
         "org.jboss.interceptors.proxy.FootballTeam_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.SecondInterceptor_aroundInvokeAfter",
         "org.jboss.interceptors.proxy.FirstInterceptor_aroundInvokeAfter",
   };

   private String[] expectedLoggedValuesWhenRaw = {
         "org.jboss.interceptors.proxy.FootballTeam_getName",
   };

   private Map<Class<?>, InterceptionModel<?,?>> interceptionModelRegistry;

   private DirectClassInterceptionHandlerFactory interceptionHandlerFactory;

   private InterceptorMetadataRegistry interceptorMetadataRegistry;

   @Before
   public void setUp()
   {
      interceptorMetadataRegistry = new SimpleInterceptorMetadataRegistry();
      interceptionHandlerFactory = new DirectClassInterceptionHandlerFactory(interceptorMetadataRegistry);
   }

   public void resetLogAndSetupClassesForMethod() throws Exception
   {
      InterceptorTestLogger.reset();
      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);
      builder.interceptAroundInvoke(FootballTeam.class.getMethod("getName")).with(FirstInterceptor.class, SecondInterceptor.class);
      builder.interceptPostConstruct().with(FirstInterceptor.class);
      builder.interceptPreDestroy().with(SecondInterceptor.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel;
      interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

   }

   public void resetLogAndSetupClassesGlobally() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAll().with(FirstInterceptor.class, SecondInterceptor.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel;
      interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

   }

   public void resetLogAndSetupClassesMixed() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);
      builder.interceptAll().with(FirstInterceptor.class);
      builder.interceptPreDestroy().with(SecondInterceptor.class);
      builder.interceptAroundInvoke(FootballTeam.class.getMethod("getName")).with(SecondInterceptor.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel;
      interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

   }

   public void resetLogAndSetupClassesWithGlobalsIgnored() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);
      builder.interceptAll().with(FirstInterceptor.class);
      builder.interceptPreDestroy().with(SecondInterceptor.class);
      builder.interceptAroundInvoke(FootballTeam.class.getMethod("getName")).with(SecondInterceptor.class);
      builder.ignoreGlobalInterceptors(FootballTeam.class.getMethod("getName"));
      InterceptionModel<Class<?>, Class<?>> interceptionModel;
      interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

   }


   @Test
   public void testInterceptionWithMethodRegisteredInterceptors() throws Exception
   {
      resetLogAndSetupClassesForMethod();
      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValues, logValues);
      assertRawObject(proxy);
   }

   @Test
   public void testInterceptionWithGlobalInterceptors() throws Exception
   {
      resetLogAndSetupClassesGlobally();
      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      assertRawObject(proxy);
   }

   @Test
   public void testInterceptionWithMixedInterceptors() throws Exception
   {
      resetLogAndSetupClassesMixed();
      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValues, logValues);
      assertRawObject(proxy);
   }

   @Test
   public void testInterceptionWithGlobalsIgnored() throws Exception
   {
      resetLogAndSetupClassesWithGlobalsIgnored();
      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      InterceptionUtils.executePostConstruct(proxy);
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      InterceptionUtils.executePredestroy(proxy);
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValuesWithGlobalsIgnored, logValues);
      assertRawObject(proxy);
   }


   @Test
   @Ignore
   public void testInterceptionWithSerializedProxy() throws Exception
   {
      resetLogAndSetupClassesForMethod();
      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      InterceptionUtils.executePostConstruct(proxy);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new ObjectOutputStream(baos).writeObject(proxy);
      proxy = (FootballTeam) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
      Assert.assertEquals(TEAM_NAME, proxy.getName());
      Object[] logValues = InterceptorTestLogger.getLog().toArray();
      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValuesOnSerialization, logValues);
      Assert.assertTrue(((ProxyObject)proxy).getHandler() instanceof CompositeHandler);
      assertRawObject(proxy);
   }


   @Test
   public void testSerialization() throws Exception
   {
      FootballTeam proxy = new FootballTeam("Ajax Amsterdam");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new ObjectOutputStream(baos).writeObject(proxy);
      proxy = (FootballTeam) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
      Assert.assertNotNull(proxy);
   }


   @Test
   public void testMethodParameterOverriding() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echo", String.class)).with(ParameterOverridingInterceptor.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(42, proxy.echo("1"));
   }

   @Test
   public void testMethodParameterOverridingWithPrimitive() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echoInt", int.class)).with(ParameterOverridingInterceptorWithInteger.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(42, proxy.echoInt(1));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testMethodParameterOverridingWithObject() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echoLongAsObject", Long.class)).with(ParameterOverridingInterceptorWithInteger.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(new Long(42), proxy.echoLongAsObject(1l));
   }

   @Test
   public void testMethodParameterOverridingWithObjectSucceed() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echoLongAsObject", Long.class)).with(ParameterOverridingInterceptorWithLong.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(new Long(42), proxy.echoLongAsObject(1l));
   }

   @Test
   public void testMethodParameterOverridingWithPrimitiveWidening() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echoLong", long.class)).with(ParameterOverridingInterceptorWithInteger.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(42, proxy.echoLong(1));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testMethodParameterOverridingWithPrimitiveNarrowing() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echoInt", int.class)).with(ParameterOverridingInterceptorWithLong.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(42, proxy.echoInt(1));
   }

   @Test
   public void testMethodParameterOverridingWithArray() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echoObjectArray", Object[].class)).with(ParameterOverridingInterceptorWithLongArray.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(new Long[]{42l}, proxy.echoObjectArray(new Object[]{}));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testMethodParameterOverridingWithArrayOnString() throws Exception
   {
      InterceptorTestLogger.reset();

      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echoStringArray", String[].class)).with(ParameterOverridingInterceptorWithLongArray.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      this.interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      Assert.assertEquals(new Long[]{42l}, proxy.echoStringArray(new String[]{}));
   }

   @Test
   public void testMethodParameterOverridingWithSubclass() throws Exception
   {
      InterceptorTestLogger.reset();


      InterceptionModelBuilder<Class<?>, Class<?>> builder = InterceptionModelBuilder.newBuilderFor(FootballTeam.class, (Class) Class.class);

      builder.interceptAroundInvoke(FootballTeam.class.getMethod("echo2", ValueBearer.class)).with(ParameterOverridingInterceptor2.class);
      InterceptionModel<Class<?>, Class<?>> interceptionModel = builder.build();
      this.interceptionModelRegistry = new HashMap<Class<?>, InterceptionModel<?,?>>();
      interceptionModelRegistry.put(FootballTeam.class, interceptionModel);

      FootballTeam proxy = createAdvisedInstance(FootballTeam.class, TEAM_NAME);
      proxy.doNothing();
      Assert.assertEquals(42, proxy.echo2(new ValueBearerImpl(1)));
   }

   public void assertRawObject(FootballTeam proxy)
   {
//      InterceptorTestLogger.reset();
//      FootballTeam rawInstance = InterceptionUtils.getRawInstance(proxy);
//      Assert.assertEquals(TEAM_NAME, rawInstance.getName());
//      Object[] logValues = InterceptorTestLogger.getLog().toArray();
//      Assert.assertArrayEquals(iterateAndDisplay(logValues), expectedLoggedValuesWhenRaw, logValues);logValues
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

   private <T> T createAdvisedInstance(Class<? extends T> targetClass, Object... args) throws Exception
   {
       return createAdvisedSubclassedInstance(targetClass, args);
   }

   private <T> T createAdvisedSubclassedInstance(Class<? extends T> targetClass, Object... args)
   {
      InterceptorProxyCreator ipc = new InterceptorProxyCreatorImpl(interceptionHandlerFactory, ((InterceptionModel<Class<?>, ?>) interceptionModelRegistry.get(targetClass)));
      return ipc.createProxyFromClass(targetClass, new Class<?>[]{String.class},args, interceptorMetadataRegistry.getInterceptorClassMetadata(ReflectiveClassMetadata.of(targetClass), true));
   }

}