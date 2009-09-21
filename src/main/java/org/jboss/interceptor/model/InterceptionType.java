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

package org.jboss.interceptor.model;

import javax.interceptor.AroundInvoke;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import java.lang.annotation.Annotation;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public enum InterceptionType
{
   AROUND_INVOKE(false, AroundInvoke.class),
   //AROUND_TIMEOUT(false, AroundTimeout.class),
   POST_CONSTRUCT(true, PostConstruct.class),
   PRE_DESTROY(true, PreDestroy.class),
   POST_ACTIVATE(true, PostActivate.class),
   PRE_PASSIVATE(true, PrePassivate.class);

   private boolean lifecycleCallback;
   private Class<? extends Annotation> associatedAnnotation;

   InterceptionType(boolean lifecycleCallback, Class<? extends Annotation> associatedAnnotation)
   {
      this.lifecycleCallback = lifecycleCallback;
      this.associatedAnnotation = associatedAnnotation;
   }

   public boolean isLifecycleCallback()
   {
      return lifecycleCallback;
   }

   public Class<? extends Annotation> getAssociatedAnnotation()
   {
      return associatedAnnotation;
   }
}
