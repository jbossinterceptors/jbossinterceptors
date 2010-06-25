package org.jboss.interceptor.proxy.javassist;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javassist.util.proxy.MethodHandler;

/**
 * @author Marius Bogoevici
 */
public class CompositeHandler implements MethodHandler, Serializable
{

   private List<MethodHandler> methodHandlers;

   private Class<?> ignoredSuperclass = Object.class;

   private boolean enabled = false;

   public CompositeHandler(List<MethodHandler> methodHandlers)
   {
      this.methodHandlers = new ArrayList<MethodHandler>();
      this.methodHandlers.addAll(methodHandlers);
   }

   private static ThreadLocal<Integer> currentHandlerIndex = new ThreadLocal<Integer>();

   public void setIgnoredSuperclass(Class<?> ignoredSuperclass)
   {
      this.ignoredSuperclass = ignoredSuperclass;
   }

   private void setEnabled(boolean enabled)
   {

   }

   public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
   {
      if (thisMethod.getDeclaringClass().equals(Object.class))
         return proceed.invoke(self);
      boolean isOuter = false;
      if (currentHandlerIndex.get() == null)
      {
         isOuter = true;
         currentHandlerIndex.set(0);
      }
      else
      {
         currentHandlerIndex.set(currentHandlerIndex.get() + 1);
      }
      try
      {
         if (currentHandlerIndex.get() < methodHandlers.size())
         {
            return methodHandlers.get(currentHandlerIndex.get()).invoke(self, thisMethod, proceed, args);
         }
         else
         {
            if (proceed != null)
               return proceed.invoke(self, args);
         }
      }
      finally
      {
         if (isOuter)
         {
            currentHandlerIndex.set(null);
         }
      }
      return null;
   }
}
