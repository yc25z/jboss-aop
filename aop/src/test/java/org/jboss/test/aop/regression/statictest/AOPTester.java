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
package org.jboss.test.aop.regression.statictest;


import org.jboss.test.aop.AOPTestWithSetup;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests that static is used correctly
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public class AOPTester extends AOPTestWithSetup
{
   public static Test suite()
   {
      TestSuite suite = new TestSuite("AOPTester");
      suite.addTestSuite(AOPTester.class);
      return suite;
   }

   public AOPTester(String name)
   {
      super(name);
   }

   public void testRegression()
   {
      MemberOnly mo = new MemberOnly();
      SimpleInterceptor.called = false;
      mo.memberMethod();
      if (SimpleInterceptor.called != true) throw new RuntimeException("MemberOnly did not get intercepted");
      SimpleInterceptor.called = false;
      MemberOnly.staticMethod();
      if (SimpleInterceptor.called == true) throw new RuntimeException("MemberOnly static method DID get intercepted");

      StaticOnly so = new StaticOnly();
      SimpleInterceptor.called = false;
      so.memberMethod();
      if (SimpleInterceptor.called == true) throw new RuntimeException("StaticOnly member method got intercepted!");
      SimpleInterceptor.called = false;
      StaticOnly.staticMethod();
      if (SimpleInterceptor.called == false) throw new RuntimeException("StaticOnly static method DID NOT get intercepted");

   }

}

