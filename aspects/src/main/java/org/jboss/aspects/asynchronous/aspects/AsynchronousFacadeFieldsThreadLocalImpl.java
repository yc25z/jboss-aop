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

package org.jboss.aspects.asynchronous.aspects;

import org.jboss.aspects.asynchronous.AsynchronousTask;

/**
 * @author <a href="mailto:chussenet@yahoo.com">{Claude Hussenet Independent Consultant}</a>.
 * @version <tt>$Revision$</tt>
 */

public class AsynchronousFacadeFieldsThreadLocalImpl

implements AsynchronousFacadeFields
{

   private ThreadLocal<AsynchronousFacadeFields> threadLocal = new ThreadLocal<AsynchronousFacadeFields>();

   public AsynchronousFacadeFieldsThreadLocalImpl()
   {

      init();

   }

   private void init()
   {

      threadLocal.set(new AsynchronousFacadeFieldsImpl());

   }

   private AsynchronousFacadeFields getValue()
   {

      if (threadLocal.get() == null)

         init();

      return threadLocal.get();

   }

   public String getId()
   {

      return getValue().getId();

   }

   public void setId(String id)
   {

      getValue().setId(id);

   }

   public long getTimeout()
   {

      return getValue().getTimeout();

   }

   public void setTimeout(long timeout)
   {

      getValue().setTimeout(timeout);

   }

   public AsynchronousTask getAsynchronousTask()
   {

      return getValue().getAsynchronousTask();

   }

   public void setAsynchronousTask(AsynchronousTask asynchronousTask)
   {

      getValue().setAsynchronousTask(asynchronousTask);

   }

}
