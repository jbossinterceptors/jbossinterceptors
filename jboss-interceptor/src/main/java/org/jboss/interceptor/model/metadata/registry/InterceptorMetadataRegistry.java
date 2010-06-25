package org.jboss.interceptor.model.metadata.registry;

import org.jboss.interceptor.model.metadata.reader.ClassMetadataProvider;
import org.jboss.interceptor.model.metadata.InterceptorMetadata;

/**
 * Acts as a repository of
 * 
 * @author Marius Bogoevici
 */
public interface InterceptorMetadataRegistry
{
   InterceptorMetadata getInterceptorClassMetadata(ClassMetadataProvider interceptorClass);

   InterceptorMetadata getInterceptorClassMetadata(ClassMetadataProvider interceptorClass, boolean isInterceptorTargetClass);

}
