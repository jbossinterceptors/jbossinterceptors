/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual
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

package org.jboss.interceptor.reader;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.MethodMetadata;
import org.jboss.interceptor.util.ArrayIterator;
import org.jboss.interceptor.util.ImmutableIteratorWrapper;

/**
 * @author Marius Bogoevici
 */
public class ReflectiveClassMetadata implements ClassMetadata, Serializable
{

   private Class<?> clazz;

   private ReflectiveClassMetadata(Class<?> clazz)
   {
      this.clazz = clazz;
   }

   public static ClassMetadata of(Class<?> clazz)
   {
      return new ReflectiveClassMetadata(clazz);
   }

   public String getClassName()
   {
      return clazz.getName();
   }

   public Iterable<MethodMetadata> getDeclaredMethods()
   {
      return new Iterable<MethodMetadata>()
      {
         public Iterator<MethodMetadata> iterator()
         {
             return new ImmutableIteratorWrapper<Method>(new ArrayIterator(ReflectiveClassMetadata.this.clazz.getDeclaredMethods()))
             {
                @Override
                protected MethodMetadata wrapObject(Method method)
                {
                   return ReflectiveMethodMetadata.of(method);
                }
             };
         }
      };     
   }

   public Class<?> getJavaClass()
   {
      return clazz;
   }   

   public ClassMetadata getSuperclass()
   {
      Class<?> superClass = clazz.getSuperclass();
      return superClass == null? null : new ReflectiveClassMetadata(superClass);
   }

}
