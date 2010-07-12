package org.jboss.interceptor.registry;

import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;


/**
 * Acts as a repository of
 * 
 * @author Marius Bogoevici
 */
public interface InterceptorMetadataRegistry
{
   InterceptorMetadata getInterceptorClassMetadata(ClassMetadata interceptorClass);

   InterceptorMetadata getInterceptorClassMetadata(ClassMetadata interceptorClass, boolean isInterceptorTargetClass);

}
