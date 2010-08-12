package org.jboss.interceptors.customInvocationContext;

import java.awt.im.InputMethodRequests;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.util.proxy.MethodHandler;

import javax.interceptor.InvocationContext;

import junit.framework.Assert;

import org.jboss.interceptor.builder.InterceptionModelBuilder;
import org.jboss.interceptor.proxy.DirectClassInterceptorInstantiator;
import org.jboss.interceptor.proxy.InterceptorProxyCreator;
import org.jboss.interceptor.proxy.InterceptorProxyCreatorImpl;
import org.jboss.interceptor.reader.InterceptorMetadataUtils;
import org.jboss.interceptor.reader.ReflectiveClassMetadata;
import org.jboss.interceptor.spi.context.InterceptionChain;
import org.jboss.interceptor.spi.context.InvocationContextFactory;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.junit.Test;

public class CustomInvocationContextTest
{
   
   @Test
   public void testCustomInvocationContextSupported()
   {
      
      CustomInterceptor.invocationCount = 0;
      InterceptorInstantiator<ClassMetadata<?>, Object> interceptorInstantiator = new DirectClassInterceptorInstantiator();
      InvocationContextFactory invocationContextFactory = new InvocationContextFactory()
      {
         
         public InvocationContext newInvocationContext(InterceptionChain chain, Object o, Method method, Object[] args)
         {
            // TODO Auto-generated method stub
            return new CustomInvocationContextImpl(chain, o, method, args);
         }
      };
      
      InterceptionModelBuilder<ClassMetadata<?>, ClassMetadata> builder = InterceptionModelBuilder.<ClassMetadata<?>,ClassMetadata>newBuilderFor(ReflectiveClassMetadata.of(Service.class), ClassMetadata.class);
      builder.interceptAll().with(ReflectiveClassMetadata.of(CustomInterceptor.class));
      InterceptionModel<ClassMetadata<?>, ClassMetadata> interceptionModel = builder.build();
      InterceptorProxyCreatorImpl interceptorProxyCreator = new InterceptorProxyCreatorImpl(interceptorInstantiator, invocationContextFactory, interceptionModel);
      
      Service serviceInstance = interceptorProxyCreator.createProxyFromClass(ReflectiveClassMetadata.of(Service.class), new Class<?>[]{}, new Object[]{} );
      
      serviceInstance.invoke();
      
      Assert.assertEquals(1, CustomInterceptor.invocationCount);
      Assert.assertTrue(serviceInstance.isInvoked());
      
   }

}
