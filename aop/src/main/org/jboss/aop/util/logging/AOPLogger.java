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
package org.jboss.aop.util.logging;

import org.jboss.logging.Logger;

/**
 * A thin wrapper around the jboss logging framework, so that if a proper logger is not installed
 * we get the output redirected to System.out.
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class AOPLogger extends Logger
{
   private static final long serialVersionUID = 1L;

   protected AOPLogger(String arg0)
   {
      super(arg0);
   }
   
   public static Logger getLogger(String name)
   {
      initLogger();
      return Logger.getLogger(name);
   }

   public static Logger getLogger(String name, String suffix)
   {
      return Logger.getLogger(name + "." + suffix);
   }

   public static Logger getLogger(Class<?> clazz)
   {
      initLogger();
      return Logger.getLogger(clazz);
   }

   public static Logger getLogger(Class<?> clazz, String suffix)
   {
      return Logger.getLogger(clazz.getName() + "." + suffix);
   }
   
   
   protected static void initLogger()
   {
      if (pluginClass == org.jboss.logging.NullLoggerPlugin.class &&
            !System.getProperty("jboss.aop.logger.ignore", "false").equals("true"))
      {
         pluginClass = SystemOutLoggerPlugin.class;
         pluginClassName = SystemOutLoggerPlugin.class.getName();
      }
   }
}
