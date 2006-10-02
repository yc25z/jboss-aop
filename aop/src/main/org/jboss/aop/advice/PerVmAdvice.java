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
package org.jboss.aop.advice;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import org.jboss.aop.AspectManager;
import org.jboss.aop.instrument.OptimizedMethodInvocations;
import org.jboss.aop.instrument.TransformerCommon;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.Joinpoint;
import org.jboss.aop.joinpoint.MethodJoinpoint;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public class PerVmAdvice
{
   private static long counter = 0;

   public static synchronized Interceptor generateOptimized(Joinpoint joinpoint, AspectManager manager, String adviceName, AspectDefinition a) throws Exception
   {
      Object aspect = manager.getPerVMAspect(a);
      return generateInterceptor(joinpoint, aspect, adviceName);

   }

   public static Interceptor generateInterceptor(Joinpoint joinpoint, Object aspect, String adviceName) throws Exception
   {
      ClassLoader cl = aspect.getClass().getClassLoader();
      String name = "org.jboss.aop.advice." + aspect.getClass().getName() + "_z_" + adviceName + "_" + System.identityHashCode(cl);
      Class iclass = null;
      
      if (cl == null)
      {
         //The classloader will be null if loader by the booststrap classloader
         cl = Thread.currentThread().getContextClassLoader();
      }
      synchronized (PerVmAdvice.class)
      {
         try 
         {
            iclass = cl.loadClass(name);
         }
         catch(Exception e)
         {
         }

         ClassPool pool;
         try
         {
            pool = AspectManager.instance().findClassPool(cl);
         }
         catch (RuntimeException e)
         {
            // AutoGenerated
            throw new RuntimeException(e);
         }

         if (iclass == null)
         {
            Method[] methods = aspect.getClass().getMethods();
            ArrayList matches = new ArrayList();
            for (int i = 0; i < methods.length; i++)
            {
               if (methods[i].getName().equals(adviceName)) matches.add(methods[i]);
            }
      
            // todo: Need to have checks on whether the advice is overloaded and it is an argument type interception
            if (matches.size() == 1)
            {
               Method method = (Method) matches.get(0);
               if (joinpoint instanceof MethodJoinpoint)
               {
                  if (method.getParameterTypes().length == 0 || method.getParameterTypes().length > 1 || !Invocation.class.isAssignableFrom(method.getParameterTypes()[0]))
                  {
                     return generateArgsInterceptor(aspect, method, joinpoint);
                  }
               }
            }
      
//            ClassPool pool = AspectManager.instance().findClassPool(cl);
            CtClass clazz = TransformerCommon.makeClass(pool, name);
            
            // We need to know whether this Interceptor is actually advice.
            CtClass interceptorInterface = pool.get("org.jboss.aop.advice.Interceptor");
            CtClass abstractAdviceClass = pool.get("org.jboss.aop.advice.AbstractAdvice");
            clazz.setSuperclass(abstractAdviceClass);
      
            // aspect field
            CtClass aspectClass = pool.get(aspect.getClass().getName());
            CtField field = new CtField(aspectClass, "aspectField", clazz);
            field.setModifiers(javassist.Modifier.PUBLIC);
            clazz.addField(field);
            // getName()
            CtMethod getNameTemplate = interceptorInterface.getDeclaredMethod("getName");
            String getNameBody =
            "{ " +
            "   return \"" + aspect.getClass().getName() + "." + adviceName + "\"; " +
            "}";
            CtMethod getName = CtNewMethod.make(getNameTemplate.getReturnType(), "getName", getNameTemplate.getParameterTypes(), getNameTemplate.getExceptionTypes(), getNameBody, clazz);
            getName.setModifiers(javassist.Modifier.PUBLIC);
            clazz.addMethod(getName);
      
            // invoke
            CtMethod invokeTemplate = interceptorInterface.getDeclaredMethod("invoke");
            StringBuffer invokeBody = new StringBuffer();
            invokeBody.append("{  ");
            if (matches.size() > 1)
            {
               for (int i = 0; i < matches.size(); i++)
               {
                  Method advice = (Method) matches.get(i);
                  String param = advice.getParameterTypes()[0].getName();
                  invokeBody.append("   if ($1 instanceof " + param + ") return aspectField." + adviceName + "((" + param + ")$1); ");
               }
               invokeBody.append("   return (org.jboss.aop.joinpoint.Invocation)null; ");
            }
            else
            {
               Method advice = (Method) matches.get(0);
               String param = advice.getParameterTypes()[0].getName();
               invokeBody.append("return aspectField." + adviceName + "((" + param + ")$1); ");
            }
            invokeBody.append("}");
            CtMethod invoke = CtNewMethod.make(invokeTemplate.getReturnType(), "invoke", invokeTemplate.getParameterTypes(), invokeTemplate.getExceptionTypes(), invokeBody.toString(), clazz);
            invoke.setModifiers(javassist.Modifier.PUBLIC);
            clazz.addMethod(invoke);
         
            iclass = TransformerCommon.toClass(clazz, cl);
         }
      }
      Interceptor rtn = (Interceptor) iclass.newInstance();
      Field f = iclass.getField("aspectField");
      f.set(rtn, aspect);
      return rtn;
   }

   public static Interceptor generateArgsInterceptor(Object aspect, Method advice, Joinpoint joinpoint) throws Exception
   {
      Method method = ((MethodJoinpoint) joinpoint).getMethod();
      String optimized = OptimizedMethodInvocations.getOptimizedInvocationClassName(method);


      ClassPool pool = AspectManager.instance().findClassPool(aspect.getClass().getClassLoader());
      CtClass clazz = TransformerCommon.makeClass(pool, "org.jboss.aop.advice." + aspect.getClass().getName() + counter++);

      // We need to know whether this Interceptor is actually advice.
      CtClass interceptorInterface = pool.get("org.jboss.aop.advice.Interceptor");
      clazz.addInterface(interceptorInterface);

      // aspect field
      CtClass aspectClass = pool.get(aspect.getClass().getName());
      CtField field = new CtField(aspectClass, "aspectField", clazz);
      field.setModifiers(javassist.Modifier.PUBLIC);
      clazz.addField(field);
      // getName()
      CtMethod getNameTemplate = interceptorInterface.getDeclaredMethod("getName");
      String getNameBody =
      "{ " +
      "   return \"" + aspect.getClass().getName() + "." + advice.getName() + "\"; " +
      "}";
      CtMethod getName = CtNewMethod.make(getNameTemplate.getReturnType(), "getName", getNameTemplate.getParameterTypes(), getNameTemplate.getExceptionTypes(), getNameBody, clazz);
      getName.setModifiers(javassist.Modifier.PUBLIC);
      clazz.addMethod(getName);

      // invoke
      String invokeBody = null;
      if (Invocation.class.isAssignableFrom(advice.getParameterTypes()[0]))
      {
         invokeBody = getInvocationBody(optimized, advice, method);
      }
      else
      {
         invokeBody = getThreadStackBody(optimized, advice, method);
      }
      CtMethod invoke = CtNewMethod.make(invokeBody, clazz);
      invoke.setModifiers(javassist.Modifier.PUBLIC);
      clazz.addMethod(invoke);
      Class iclass = TransformerCommon.toClass(clazz);

      Interceptor rtn = (Interceptor) iclass.newInstance();
      Field f = iclass.getField("aspectField");
      f.set(rtn, aspect);
      return rtn;
   }

   private static String getThreadStackBody(String optimized, Method advice, Method method)
   {
      String invokeBody =
      "public Object invoke(org.jboss.aop.joinpoint.Invocation invocation) throws java.lang.Throwable " +
      "{  " +
      "   " + optimized + " optimized = (" + optimized + ")invocation; " +
      "   org.jboss.aop.joinpoint.CurrentInvocation.push(invocation); " +
      "   try {";
      invokeBody += "return ($w)aspectField." + advice.getName() + "(";
      Class[] adviceParams = advice.getParameterTypes();
      Class[] params = method.getParameterTypes();
      boolean first = true;
      if (adviceParams.length > 0)
      {
         int adviceParam = 0;
         for (int i = 0; i < params.length && adviceParam < adviceParams.length; i++)
         {
            if (adviceParams[adviceParam].equals(params[i]))
            {
               adviceParam++;
               if (first)
               {
                  first = false;
               }
               else
               {
                  invokeBody += ", ";
               }
               invokeBody += "optimized.arg" + i;
            }
         }
      }
      invokeBody += "); ";
      invokeBody +=
      "   } finally { org.jboss.aop.joinpoint.CurrentInvocation.pop(); }";
      invokeBody +=
      "}";
      return invokeBody;
   }

   private static String getInvocationBody(String optimized, Method advice, Method method)
   {
      String invokeBody =
      "public Object invoke(org.jboss.aop.joinpoint.Invocation invocation) throws java.lang.Throwable " +
      "{  " +
      "   " + optimized + " optimized = (" + optimized + ")invocation; " +
      "   return ($w)aspectField." + advice.getName() + "(optimized";
      Class[] adviceParams = advice.getParameterTypes();
      Class[] params = method.getParameterTypes();
      if (adviceParams.length > 0)
      {
         int adviceParam = 1;
         for (int i = 0; i < params.length && adviceParam < adviceParams.length; i++)
         {
            if (adviceParams[adviceParam].equals(params[i]))
            {
               adviceParam++;
               invokeBody += ", ";
               invokeBody += "optimized.arg" + i;
            }
         }
      }
      invokeBody += "); ";
      invokeBody +=
      "}";
      return invokeBody;
   }
}
