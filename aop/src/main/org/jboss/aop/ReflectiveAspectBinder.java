/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
package org.jboss.aop;

import gnu.trove.TLongObjectHashMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.jboss.aop.advice.AdviceBinding;
import org.jboss.aop.introduction.AnnotationIntroduction;
import org.jboss.aop.pointcut.AnnotationMatcher;
import org.jboss.aop.pointcut.PointcutMethodMatch;
import org.jboss.aop.proxy.container.InstanceProxyContainer;
import org.jboss.aop.util.Advisable;
import org.jboss.util.MethodHashing;

/**
 * comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class ReflectiveAspectBinder
{
   protected Class clazz;
   protected HashSet aspects = new HashSet();
   protected HashMap methodAdvices = new HashMap();
   protected HashMap constructorAdvices = new HashMap();
   protected HashMap fieldReadAdvices = new HashMap();
   protected HashMap fieldWriteAdvices = new HashMap();
   protected Advisor advisor;
   protected boolean isInstanceContainer;
   TLongObjectHashMap methodMap = new TLongObjectHashMap();
   

   public ReflectiveAspectBinder(Class clazz, Advisor advisor)
   {
      this.clazz = clazz;
      this.advisor = advisor;
      isInstanceContainer = InstanceProxyContainer.class == advisor.getClass();
      bindMethodAdvices(clazz);
      bindConstructorAdvices();
      bindFieldAdvices();
   }

   public Class getClazz()
   {
      return clazz;
   }

   public HashSet getAspects()
   {
      return aspects;
   }

   public HashMap getMethodAdvices()
   {
      return methodAdvices;
   }

   public HashMap getConstructorAdvices()
   {
      return constructorAdvices;
   }

   public HashMap getFieldReadAdvices()
   {
      return fieldReadAdvices;
   }

   public HashMap getFieldWriteAdvices()
   {
      return fieldWriteAdvices;
   }

   public void createMethodMap(final Class superClass)
   {
      try
      {
         if (superClass == null || (superClass == Object.class && !isInstanceContainer))
         {
            return;
         }
         createMethodMap(superClass.getSuperclass());
         
         Method[] methods = (Method[]) AccessController.doPrivileged(new PrivilegedExceptionAction() 
         {
            public Object run() throws Exception
            {
               return superClass.getDeclaredMethods();
            }
         });
         for (int i = 0 ; i < methods.length ; i++)
         {
            if (!Advisable.isAdvisable(methods[i]))
            {
               continue;
            }

            long hash = MethodHashing.methodHash(methods[i]);
            methodMap.put(hash, methods[i]);
         }
      }
      catch (PrivilegedActionException e)
      {
         throw new RuntimeException(e.getException());
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
   
   protected void bindMethodAdvices(Class superClass)
   {
      createMethodMap(superClass); 
      if (methodMap != null)
      {
         Object[] methods = methodMap.getValues();
         for (int i = 0 ; i < methods.length ; i++)
         {
            bindMethodAdvice((Method)methods[i]);
         }
      }
   }

   protected void bindConstructorAdvices()
   {
      Constructor[] cons = (Constructor[]) AccessController.doPrivileged(new PrivilegedAction() 
      {
         public Object run()
         {
            return clazz.getDeclaredConstructors();
         }
      });
      for (int i = 0; i < cons.length; i++)
      {
         bindConstructorAdvice(cons[i]);
      }
   }

   protected void bindFieldAdvices()
   {
      Field[] fields = (Field[]) AccessController.doPrivileged(new PrivilegedAction() 
      {
         public Object run()
         {
            return clazz.getDeclaredFields();
         }
      });
      for (int i = 0; i < fields.length; i++)
      {
         bindFieldGetAdvice(fields[i]);
         bindFieldSetAdvice(fields[i]);
      }
   }

   protected boolean matches(AnnotationIntroduction ai, Object element)
   {
      AnnotationMatcher matcher = new AnnotationMatcher(advisor, element);
      return ((Boolean) ai.getTarget().jjtAccept(matcher, null)).booleanValue();
   }

   protected void bindMethodAdvice(Method mi)
   {
      Map repositoryBindings = advisor.getManager().getBindings();
      Iterator it = repositoryBindings.values().iterator();
      ArrayList advices = (ArrayList)methodAdvices.get(mi);
      while (it.hasNext())
      {

         AdviceBinding binding = (AdviceBinding)it.next();
         PointcutMethodMatch pmatch= binding.getPointcut().matchesExecution(advisor, mi);
         
         if (pmatch != null && pmatch.isMatch())
         {
            if (advices == null)
            {
               advices = new ArrayList();
               methodAdvices.put(mi, advices);
            }
            advices.addAll(Arrays.asList(binding.getInterceptorFactories()));
            for (int i = 0; i < binding.getInterceptorFactories().length; i++)
            {
               aspects.add(binding.getInterceptorFactories()[i].getAspect());
            }
         }
      }
   }

   protected void bindConstructorAdvice(Constructor mi)
   {
      Map repositoryBindings = advisor.getManager().getBindings();
      Iterator it = repositoryBindings.values().iterator();
      ArrayList advices = (ArrayList)constructorAdvices.get(mi);
      while (it.hasNext())
      {

         AdviceBinding binding = (AdviceBinding)it.next();
         if (binding.getPointcut().matchesExecution(advisor, mi))
         {
            if (advices == null)
            {
               advices = new ArrayList();
               constructorAdvices.put(mi, advices);
            }
            advices.addAll(Arrays.asList(binding.getInterceptorFactories()));
            for (int i = 0; i < binding.getInterceptorFactories().length; i++)
            {
               aspects.add(binding.getInterceptorFactories()[i].getAspect());
            }
         }
      }
   }

   protected void bindFieldGetAdvice(Field mi)
   {
      Map repositoryBindings = advisor.getManager().getBindings();
      Iterator it = repositoryBindings.values().iterator();
      ArrayList advices = (ArrayList)fieldReadAdvices.get(mi);
      while (it.hasNext())
      {

         AdviceBinding binding = (AdviceBinding)it.next();
         if (binding.getPointcut().matchesGet(advisor, mi))
         {
            if (advices == null)
            {
               advices = new ArrayList();
               fieldReadAdvices.put(mi, advices);
            }
            advices.addAll(Arrays.asList(binding.getInterceptorFactories()));
            for (int i = 0; i < binding.getInterceptorFactories().length; i++)
            {
               aspects.add(binding.getInterceptorFactories()[i].getAspect());
            }
         }
      }
   }

   protected void bindFieldSetAdvice(Field mi)
   {
      Map repositoryBindings = advisor.getManager().getBindings();
      Iterator it = repositoryBindings.values().iterator();
      ArrayList advices = (ArrayList)fieldWriteAdvices.get(mi);
      while (it.hasNext())
      {

         AdviceBinding binding = (AdviceBinding)it.next();
         if (binding.getPointcut().matchesSet(advisor, mi))
         {
            if (advices == null)
            {
               advices = new ArrayList();
               fieldWriteAdvices.put(mi, advices);
            }
            advices.addAll(Arrays.asList(binding.getInterceptorFactories()));
            for (int i = 0; i < binding.getInterceptorFactories().length; i++)
            {
               aspects.add(binding.getInterceptorFactories()[i].getAspect());
            }
         }
      }
   }
}
