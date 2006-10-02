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
package org.jboss.aop.instrument;


import org.jboss.aop.classpool.AOPClassPool;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;

/**
 * Comment
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class OptimizedConstructionInvocations extends
      OptimizedBehaviourInvocations
{

   protected static void addCopy(ClassPool pool, CtClass invocation, CtClass[] params) throws Exception
   {
      CtClass methodInvocation = pool.get("org.jboss.aop.joinpoint.ConstructionInvocation");
      CtMethod template = methodInvocation.getDeclaredMethod("copy");
   
   
      String code =
      "{ " +
      "   " + invocation.getName() + " wrapper = new " + invocation.getName() + "(this.interceptors, this.constructor); " +
      "   wrapper.metadata = this.metadata; " +
      "   wrapper.currentInterceptor = this.currentInterceptor; ";
      for (int i = 0; i < params.length; i++)
      {
         code += "   wrapper.arg" + i + " = this.arg" + i + "; ";
      }
      code += "   return wrapper; }";
   
      CtMethod copy = CtNewMethod.make(template.getReturnType(), "copy", template.getParameterTypes(), template.getExceptionTypes(), code, invocation);
      copy.setModifiers(template.getModifiers());
      invocation.addMethod(copy);
   
   }

   /**
    * Returns the name of the optimized Invocation class.
    * @param declaringClazz the class that contains the constructor.
    * @param constructorIndex the index of the constructor.
    * @return the name of the optimized Invocation class.
    */
   protected static String getOptimizedInvocationClassName(CtClass declaringClazz, int constructorIndex)
   {
      return declaringClazz.getName() + constructorIndex + "OptimizedConstructionInvocation";
   }

   protected static String createOptimizedInvocationClass(Instrumentor instrumentor, CtClass clazz, CtConstructor con, int index) throws Exception
   {
      AOPClassPool pool = (AOPClassPool) instrumentor.getClassPool();
      CtClass conInvocation = pool.get("org.jboss.aop.joinpoint.ConstructionInvocation");
      CtClass untransformable = pool.get("org.jboss.aop.instrument.Untransformable");
   
      String className = getOptimizedInvocationClassName(clazz, index);
      boolean makeInnerClass = !Modifier.isPublic(con.getModifiers());
   
      CtClass invocation = makeInvocationClassNoCtors(pool, makeInnerClass, clazz, className, conInvocation);
      
      CtConstructor template = null;
      CtConstructor[] tcons = conInvocation.getDeclaredConstructors();
      for (int i = 0; i < tcons.length; i++)
      {
         if (tcons[i].getParameterTypes().length == 2)
         {
            template = tcons[i];
            break;
         }
      }
      CtConstructor icon = CtNewConstructor.make(template.getParameterTypes(), template.getExceptionTypes(), invocation);
      invocation.addConstructor(icon);
   
      CtClass[] params = con.getParameterTypes();
      for (int i = 0; i < params.length; i++)
      {
         CtField field = new CtField(params[i], "arg" + i, invocation);
         field.setModifiers(Modifier.PUBLIC);
         invocation.addField(field);
      }
   
      addGetArguments(pool, invocation, con.getParameterTypes());
      addCopy(pool, invocation, con.getParameterTypes());
      // If compile time
      TransformerCommon.compileOrLoadClass(con.getDeclaringClass(), invocation);
   
      //Return fully qualified name of class (may be an inner class)
      return invocation.getName();
   }

}
