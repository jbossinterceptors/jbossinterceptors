package org.jboss.interceptor.reader;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.interceptor.InvocationContext;

import org.jboss.interceptor.builder.MethodReference;
import org.jboss.interceptor.spi.metadata.ClassMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.interceptor.spi.metadata.InterceptorReference;
import org.jboss.interceptor.spi.metadata.MethodMetadata;
import org.jboss.interceptor.spi.model.InterceptionType;
import org.jboss.interceptor.util.InterceptionTypeRegistry;
import org.jboss.interceptor.util.InterceptorMetadataException;
import org.jboss.interceptor.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marius Bogoevici
 */
public class InterceptorMetadataUtils
{
   protected static final String OBJECT_CLASS_NAME = Object.class.getName();

   private static final Logger LOG = LoggerFactory.getLogger(InterceptorMetadataUtils.class);


   public static InterceptorMetadata readMetadataForInterceptorClass(InterceptorReference<?> interceptorReference)
   {
      return new SimpleInterceptorMetadata(interceptorReference, false, buildMethodMap(interceptorReference.getClassMetadata(), false));
   }

   public static InterceptorMetadata readMetadataForTargetClass(ClassMetadata<?> classMetadata)
   {
      return new SimpleInterceptorMetadata(ClassMetadataInterceptorReference.of(classMetadata), true, buildMethodMap(classMetadata, true));
   }

   public static boolean isInterceptorMethod(InterceptionType interceptionType, MethodMetadata method, boolean forTargetClass)
   {

      if (!method.getSupportedInterceptionTypes().contains(interceptionType))
      {
         return false;
      }

      if (interceptionType.isLifecycleCallback())
      {
         if (!Void.TYPE.equals(method.getReturnType()))
         {
            if (LOG.isWarnEnabled())
            {
              LOG.warn(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have a void return type");
            }
            return false;
         }

         Class<?>[] parameterTypes = method.getJavaMethod().getParameterTypes();

         if (forTargetClass && parameterTypes.length != 0)
         {
            if (LOG.isWarnEnabled())
            {
               LOG.warn(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "is defined on the target class and does not have 0 arguments");
            }
            return false;
         }

         if (!forTargetClass && parameterTypes.length != 1)
         {
            if (LOG.isWarnEnabled())
            {
               LOG.warn(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have exactly one parameter");
            }
            return false;
         }

         if (parameterTypes.length == 1 && !InvocationContext.class.isAssignableFrom(parameterTypes[0]))
         {
            if (LOG.isWarnEnabled())
            {
               LOG.warn(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "its single argument is not a " + InvocationContext.class.getName());
            }
            return false;
         }

         return true;
      }
      else
      {
         if (!Object.class.equals(method.getReturnType()))
         {
            if (LOG.isWarnEnabled())
            {
               LOG.warn(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not return a " + OBJECT_CLASS_NAME);
            }
            return false;
         }

         Class<?>[] parameterTypes = method.getJavaMethod().getParameterTypes();

         if (parameterTypes.length != 1)
         {
            if (LOG.isWarnEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have exactly 1 parameter");
            }
            return false;
         }

         if (!InvocationContext.class.isAssignableFrom(parameterTypes[0]))
         {
            if (LOG.isWarnEnabled())
            {
               LOG.debug(getStandardIgnoredMessage(interceptionType, method.getJavaMethod()) + "does not have a " + InvocationContext.class.getName() + " parameter ");
            }
            return false;
         }

         return true;
      }
   }

   static String getStandardIgnoredMessage(InterceptionType interceptionType, Method method)
   {
      return "Method " + method.getName() + " defined on class " + method.getDeclaringClass().getName()
            + " will not be used for interception, since it is not defined according to the specification. It is annotated with @"
            + interceptionType.annotationClassName() + ", but ";
   }

   static Map<InterceptionType, List<MethodMetadata>> buildMethodMap(ClassMetadata<?> interceptorClass, boolean forTargetClass)
   {
      Map<InterceptionType, List<MethodMetadata>> methodMap = new HashMap<InterceptionType, List<MethodMetadata>>();
      ClassMetadata<?> currentClass = interceptorClass;
      Set<MethodReference> foundMethods = new HashSet<MethodReference>();
      do
      {
         Set<InterceptionType> detectedInterceptorTypes = new HashSet<InterceptionType>();

         for (MethodMetadata method : currentClass.getDeclaredMethods())
         {
            MethodReference methodReference = MethodReference.of(method, Modifier.isPrivate(method.getJavaMethod().getModifiers()));
            if (!foundMethods.contains(methodReference))
            {
               for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes())
               {
                  if (isInterceptorMethod(interceptionType, method, forTargetClass))
                  {
                     if (methodMap.get(interceptionType) == null)
                     {
                        methodMap.put(interceptionType, new LinkedList<MethodMetadata>());
                     }
                     if (detectedInterceptorTypes.contains(interceptionType))
                     {
                        throw new InterceptorMetadataException("Same interception type cannot be specified twice on the same class");
                     }
                     else
                     {
                        detectedInterceptorTypes.add(interceptionType);
                     }
                     // add method in the list - if it is there already, it means that it has been added by a subclass
                     // final methods are treated separately, as a final method cannot override another method nor be
                     // overridden
                     ReflectionUtils.ensureAccessible(method.getJavaMethod());
                     if (!foundMethods.contains(methodReference));
                     {
                        methodMap.get(interceptionType).add(0, method);
                     }
                  }
               }
               // the method reference must be added anyway - overridden methods are not taken into consideration
               foundMethods.add(methodReference);
            }
         }
         currentClass = currentClass.getSuperclass();
      }
      while (currentClass != null && !OBJECT_CLASS_NAME.equals(currentClass.getJavaClass()));
      return methodMap;
   }
}
