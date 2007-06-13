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
package org.jboss.test.aop.stress.batf;

import org.jboss.test.aop.stress.AbstractScenario;
import org.jboss.test.aop.stress.ScenarioTest;

/**
 * Primarily to make sure I got everything right for the generated advisors
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 61751 $
 */
public class BeforeAfterThrowingFinallyVersusAroundTestCase extends ScenarioTest
{
   
   public static void main(String[] args)
   {
      junit.textui.TestRunner.run(BeforeAfterThrowingFinallyVersusAroundTestCase.class);
   }

   
   public BeforeAfterThrowingFinallyVersusAroundTestCase(String name) throws Exception
   {
      super(name);
   }

   public void testAroundWithNoExceptionScenario() throws Exception
   {
      POJO pojo = new POJO();
      pojo.methodWithAroundNoExceptions();
      assertTrue(SimpleAspect.before);
      assertTrue(SimpleAspect.after);
      assertFalse(SimpleAspect.throwing);
      assertFalse(SimpleAspect.finaly);
      getRunner().executeScenario(new AroundWithNoExceptionsScenario());
   }
   
   private class AroundWithNoExceptionsScenario extends AbstractScenario
   {
      POJO pojo = new POJO();

      public void execute(int thread, int loop) throws Exception
      {
         pojo.methodWithAroundNoExceptions();
      }
   }

   public void testBeforeAfterThrowingFinallyWithNoExceptionScenario() throws Exception
   {
      POJO pojo = new POJO();
      pojo.methodWithBeforeAfter();
      assertTrue(SimpleAspect.before);
      assertTrue(SimpleAspect.after);
      assertFalse(SimpleAspect.throwing);
      assertFalse(SimpleAspect.finaly);
      getRunner().executeScenario(new BeforeAfterThrowingFinallyWithNoExceptionsScenario());
   }
   
   private class BeforeAfterThrowingFinallyWithNoExceptionsScenario extends AbstractScenario
   {
      POJO pojo = new POJO();
      public void execute(int thread, int loop) throws Exception
      {
         pojo.methodWithBeforeAfter();
      }
   }

   public void testAroundWithExceptionScenario() throws Exception
   {
      boolean exception = false;
      try
      {
         POJO pojo = new POJO();
         pojo.methodWithAroundExceptions();
         assertTrue(SimpleAspect.before);
         assertFalse(SimpleAspect.after);
         assertTrue(SimpleAspect.throwing);
         assertTrue(SimpleAspect.finaly);
      }
      catch (Exception e)
      {
         exception = true;
      }
      
      assertTrue(exception);
      getRunner().executeScenario(new AroundWithExceptionsScenario());
   }
   
   private class AroundWithExceptionsScenario extends AbstractScenario
   {
      POJO pojo = new POJO();
      public void execute(int thread, int loop) throws Exception
      {
         try
         {
            pojo.methodWithAroundExceptions();
         }
         catch(RuntimeException e) 
         {
            
         }
      }
   }


   public void testBeforeAfterThrowingFinallyWithExceptionScenario() throws Exception
   {
      boolean exception = false;
      try
      {
         POJO pojo = new POJO();
         pojo.methodWithBeforeThrowingFinally();
         assertTrue(SimpleAspect.before);
         assertFalse(SimpleAspect.after);
         assertTrue(SimpleAspect.throwing);
         assertTrue(SimpleAspect.finaly);
      }
      catch (Exception e)
      {
         exception = true;
      }
      
      assertTrue(exception);
      getRunner().executeScenario(new BeforeAfterThrowingFinallyWithExceptionScenario());
   }
   
   private class BeforeAfterThrowingFinallyWithExceptionScenario extends AbstractScenario
   {
      POJO pojo = new POJO();
      public void execute(int thread, int loop) throws Exception
      {
         try
         {
            pojo.methodWithBeforeThrowingFinally();
         }
         catch (RuntimeException e)
         {
         }
      }
   }
}
