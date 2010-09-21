/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.interceptors.interceptionchain;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

import javax.interceptor.InvocationContext;

import junit.framework.Assert;

import org.jboss.interceptor.proxy.DefaultInvocationContextFactory;
import org.jboss.interceptor.proxy.InterceptorInvocation;
import org.jboss.interceptor.proxy.SimpleInterceptionChain;
import org.jboss.interceptor.reader.InterceptorMetadataUtils;
import org.jboss.interceptor.reader.ReflectiveClassMetadata;
import org.jboss.interceptor.spi.context.InterceptionChain;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.model.InterceptionType;
import org.junit.Test;

/**
 * Tests that the lifecylce method invocations on interceptors
 * and target instances work as expected
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class LifecycleMethodInvocationTestCase
{
   
   /**
    * The interceptors spec allows the @PostConstruct method to have public, private, protected, or package level access. 
    * This test ensures that non-public @PostConstruct methods can be invoked during an interceptor invocation
    */
   @Test
   public void testNonPublicPostConstructMethod() throws Exception
   {
      // create the interceptor invocation for @PostConstruct
      InterceptorMetadata interceptorMetaData = InterceptorMetadataUtils.readMetadataForInterceptorClass(ReflectiveClassMetadata.of(SimpleInterceptor.class));
      SimpleInterceptor interceptor = new SimpleInterceptor();
      InterceptorInvocation<?> interceptorInvocation = new InterceptorInvocation(interceptor, interceptorMetaData, InterceptionType.POST_CONSTRUCT);
      Collection<InterceptorInvocation<?>> interceptorInvocations = new HashSet<InterceptorInvocation<?>>();
      interceptorInvocations.add(interceptorInvocation);

      // create a interception chain for the bean and the interceptors
      SimpleBean bean = new SimpleBean();
      Method postConstructMethod = bean.getClass().getDeclaredMethod("onConstruct", null);
      InterceptionChain interceptorChain = new SimpleInterceptionChain(interceptorInvocations, InterceptionType.POST_CONSTRUCT, bean, postConstructMethod);
      DefaultInvocationContextFactory invocationCtxFactory = new DefaultInvocationContextFactory();
      InvocationContext invocationCtx = invocationCtxFactory.newInvocationContext(interceptorChain, bean, postConstructMethod, null);
      // invoke post-construct
      invocationCtx.proceed();
      
      // test post-construct invocation on the target object
      Assert.assertTrue("@PostConstruct was not invoked on " + SimpleBean.class, bean.wasPostConstructInvoked());
      // test post construct invocation on the interceptor instance
      Assert.assertTrue("@PostConstruct was not invoked on " + SimpleInterceptor.class, interceptor.wasPostConstructInvoked());
   }

   
   
}
