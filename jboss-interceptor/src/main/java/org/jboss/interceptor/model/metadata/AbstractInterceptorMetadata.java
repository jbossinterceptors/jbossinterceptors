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

package org.jboss.interceptor.model.metadata;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.model.metadata.reader.ClassMetadataProvider;
import org.jboss.interceptor.model.metadata.reader.MethodMetadataProvider;
import org.jboss.interceptor.util.InterceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public abstract class AbstractInterceptorMetadata implements InterceptorMetadata, Serializable
{

   private Logger log = LoggerFactory.getLogger(AbstractInterceptorMetadata.class);
   private ClassMetadataProvider interceptorClass;
   private Map<InterceptionType, List<MethodMetadataProvider>> methodMap;
   private boolean targetClass;

   protected AbstractInterceptorMetadata(ClassMetadataProvider interceptorClass, boolean targetClass)
   {
      this.interceptorClass = interceptorClass;
      this.methodMap = InterceptionUtils.buildMethodMap(interceptorClass, targetClass);
      this.targetClass = targetClass;
   }

   public ClassMetadataProvider getInterceptorClass()
   {
      return interceptorClass;
   }

   public List<MethodMetadataProvider> getInterceptorMethods(InterceptionType interceptionType)
   {
      if (methodMap != null)
      {
         List<MethodMetadataProvider> methods = methodMap.get(interceptionType);
         return methods == null ? Collections.<MethodMetadataProvider>emptyList() : methods;
      }
      else
      {
         return Collections.<MethodMetadataProvider>emptyList();
      }
   }

   public boolean isInterceptor()
   {
      return !methodMap.keySet().isEmpty();
   }

   private Object writeReplace()
   {
      return createSerializableProxy();
   }

   protected abstract Object createSerializableProxy();

   public boolean isTargetClass()
   {
      return targetClass;
   }


}
