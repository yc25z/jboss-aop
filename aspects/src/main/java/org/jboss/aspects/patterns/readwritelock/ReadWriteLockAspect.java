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
package org.jboss.aspects.patterns.readwritelock;


import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.aop.joinpoint.MethodInvocation;

/**
 * @version <tt>$Revision$</tt>
 * @author  <a href="mailto:chussenet@yahoo.com">{Claude Hussenet Independent Consultant}</a>.
 */
public class ReadWriteLockAspect
{

   private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

   public ReadWriteLockAspect()
   {

   }

   public Object readLockMethod(MethodInvocation invocation) throws Throwable
   {
      Object result=null;

      try
      {
         readWriteLock.readLock().lock();
         result= invocation.invokeNext();
      }
      finally
      {
   		readWriteLock.readLock().unlock();
      }
      return result;
   }

   public Object writeLockMethod(MethodInvocation invocation) throws Throwable
   {
      Object result = null;

      try
      {
         readWriteLock.writeLock().lock();
         result= invocation.invokeNext();
      }
      finally
      {
   		readWriteLock.writeLock().unlock();
      }
     return result;
   }

}
