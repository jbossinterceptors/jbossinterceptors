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

package org.jboss.interceptors.metadata;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.metadata.InterceptorMetadata;
import org.jboss.interceptor.model.metadata.InterceptorMetadataException;
import org.jboss.interceptor.model.metadata.reader.MethodMetadataProvider;
import org.jboss.interceptor.model.metadata.reader.ReflectiveClassMetadataProvider;
import org.jboss.interceptor.model.metadata.reader.SimpleInterceptorMetadataReader;
import org.jboss.interceptor.model.metadata.registry.InterceptorMetadataRegistry;
import org.jboss.interceptor.model.metadata.registry.SimpleInterceptorMetadataRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptorClassMetadataTestCase
{

   InterceptorMetadataRegistry interceptorMetadataRegistry;

   @Before
   public void setUp()
   {
      interceptorMetadataRegistry = new SimpleInterceptorMetadataRegistry(SimpleInterceptorMetadataReader.getInstance());
   }

   @Test
   public void testInterceptorWithAllMethods()
   {
      InterceptorMetadata interceptorClassMetadata = interceptorMetadataRegistry.getInterceptorClassMetadata(ReflectiveClassMetadataProvider.of(InterceptorWithAllMethods.class));

      List<MethodMetadataProvider> postConstructMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_CONSTRUCT);
      assertEquals(true, postConstructMethods.size() == 1);
      assertEquals(postConstructMethods.get(0).getJavaMethod().getName(), "doPostConstruct");

      List<MethodMetadataProvider> preDestroyMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_DESTROY);
      assertEquals(true, preDestroyMethods.size() == 1);
      assertEquals(preDestroyMethods.get(0).getJavaMethod().getName(), "doPreDestroy");

      List<MethodMetadataProvider> aroundInvokeMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.AROUND_INVOKE);
      assertEquals(true, aroundInvokeMethods.size() == 1);
      assertEquals(aroundInvokeMethods.get(0).getJavaMethod().getName(), "doAroundInvoke");

      List<MethodMetadataProvider> postActivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_ACTIVATE);
      assertEquals(true, postActivateMethods.size() == 1);
      assertEquals(postActivateMethods.get(0).getJavaMethod().getName(), "doPostActivate");

      List<MethodMetadataProvider> prePassivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_PASSIVATE);
      assertEquals(true, prePassivateMethods.size() == 1);
      assertEquals(prePassivateMethods.get(0).getJavaMethod().getName(), "doPrePassivate");

   }

   @Test
   public void testInterceptorWithSomeMethods()
   {
      InterceptorMetadata interceptorClassMetadata = interceptorMetadataRegistry.getInterceptorClassMetadata(ReflectiveClassMetadataProvider.of(InterceptorWithSomeMethods.class));

      List<MethodMetadataProvider> postConstructMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_CONSTRUCT);
      assertEquals(true, postConstructMethods.size() == 0);

      List<MethodMetadataProvider> preDestroyMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_DESTROY);
      assertEquals(true, preDestroyMethods.size() == 1);
      assertEquals(preDestroyMethods.get(0).getJavaMethod().getName(), "doPreDestroy");

      List<MethodMetadataProvider> aroundInvokeMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.AROUND_INVOKE);
      assertEquals(true, aroundInvokeMethods.size() == 1);
      assertEquals(aroundInvokeMethods.get(0).getJavaMethod().getName(), "doAroundInvoke");

      List<MethodMetadataProvider> postActivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_ACTIVATE);
      assertEquals(true, postActivateMethods.size() == 1);
      assertEquals(postActivateMethods.get(0).getJavaMethod().getName(), "doPostActivate");

      List<MethodMetadataProvider> prePassivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_PASSIVATE);
      assertEquals(true, prePassivateMethods.size() == 0);

   }

   @Test
   public void testSimpleInheritance()
   {
      InterceptorMetadata interceptorClassMetadata = interceptorMetadataRegistry.getInterceptorClassMetadata(ReflectiveClassMetadataProvider.of(SimpleInheritanceChildInterceptor.class));

      List<MethodMetadataProvider> postConstructMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_CONSTRUCT);
      assertEquals(1, postConstructMethods.size());
      assertEquals(postConstructMethods.get(0).getJavaMethod().getName(), "doPostConstruct");

      List<MethodMetadataProvider> preDestroyMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_DESTROY);
      assertEquals(true, preDestroyMethods.size() == 0);

      List<MethodMetadataProvider> aroundInvokeMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.AROUND_INVOKE);
      assertEquals(true, aroundInvokeMethods.size() == 1);
      assertEquals(aroundInvokeMethods.get(0).getJavaMethod().getName(), "doAroundInvoke");

      List<MethodMetadataProvider> postActivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_ACTIVATE);
      assertEquals(true, postActivateMethods.size() == 0);

      List<MethodMetadataProvider> prePassivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_PASSIVATE);
      assertEquals(true, prePassivateMethods.size() == 0);

   }

   @Test
   public void testInheritanceWithAndWithoutOverriding()
   {
      InterceptorMetadata interceptorClassMetadata = interceptorMetadataRegistry.getInterceptorClassMetadata(ReflectiveClassMetadataProvider.of(OverrideChildInterceptor.class));

      List<MethodMetadataProvider> postConstructMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_CONSTRUCT);
      assertEquals(true, postConstructMethods.size() == 1);
      assertEquals(postConstructMethods.get(0).getJavaMethod().getName(), "methodOverriddenAndUsedAsInterceptor");

      List<MethodMetadataProvider> preDestroyMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_DESTROY);
      assertEquals(true, preDestroyMethods.size() == 0);

      List<MethodMetadataProvider> aroundInvokeMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.AROUND_INVOKE);
      assertEquals(true, aroundInvokeMethods.size() == 2);
      assertEquals(aroundInvokeMethods.get(0).getJavaMethod().getName(), "methodDefinedOnParentAndUsedAsInterceptor");
      assertEquals(aroundInvokeMethods.get(1).getJavaMethod().getName(), "methodDefinedOnChildAndUsedAsInterceptor");

      List<MethodMetadataProvider> postActivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.POST_ACTIVATE);
      assertEquals(true, postActivateMethods.size() == 0);


      List<MethodMetadataProvider> prePassivateMethods = interceptorClassMetadata.getInterceptorMethods(InterceptionType.PRE_PASSIVATE);
      assertEquals(true, prePassivateMethods.size() == 0);

   }

   @Test(expected = InterceptorMetadataException.class)
   public void testDuplicateAnnotations()
   {
      InterceptorMetadata interceptorClassMetadata = interceptorMetadataRegistry.getInterceptorClassMetadata(ReflectiveClassMetadataProvider.of(InterceptorWithDuplicateAnnotations.class));

   }



}
