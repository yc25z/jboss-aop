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
package org.jboss.test.aop.rebuildingchain;

/**
 * A SynchThread.
 * 
 * @author <a href="stale.pedersen@jboss.org">Stale W. Pedersen</a>
 * @version $Revision: 1.1 $
 */
public class SyncThread extends Thread
{
   private static volatile boolean status = false;
   private volatile boolean done = false;

   @Override
   public void run()
   {

         for(int i=0; i < 30; i++)
         {
         checkStatus();
         
         if(isDone())
            return;
         
      }
   }
   
   private void checkStatus()
   {
      System.out.println("ST checking status...");
      if(getStatus() == false)
      {
         RebuildingChainTestCase.setTestFailed();
         throw new RuntimeException("Status failed!!!!");
      }
   }

   public static boolean getStatus()
   {
      return status;
   }
   
   public static void setStatus(boolean b)
   {
      status = b;
   }
   
   public void setDone(boolean b)
   {
      done = b;
   }
   
   private boolean isDone()
   {
      return done;
   }
}
