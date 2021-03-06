/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors. 
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
package org.jboss.test.aop.extender;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.ConstructionInvocation;
import org.jboss.aop.joinpoint.Invocation;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@SuppressWarnings({"unchecked"})
public class ConstructionInterceptor implements Interceptor
{
   public static ArrayList interceptions = new ArrayList();
   public String getName()
   {
      return this.getClass().getName();
   }

   public Object invoke(Invocation invocation) throws Throwable
   {
      ConstructionInvocation ci = (ConstructionInvocation)invocation;
      Constructor ctor = ci.getConstructor();
      Class clazz = ctor.getDeclaringClass();
      interceptions.add(clazz);
      
      return ci.invokeNext();
   }

}
