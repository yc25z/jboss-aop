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
package org.jboss.test.aop.regression.jbaop264underscorefieldname;

import org.jboss.test.aop.AOPTestWithSetup;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class UnderscoreInFieldNameTestCase extends AOPTestWithSetup
{
   public static void main(String[] args)
   {
      TestRunner.run(suite());
   }

   public static Test suite()
   {
      TestSuite suite = new TestSuite("AnnotationOverrideTestCase");
      suite.addTestSuite(UnderscoreInFieldNameTestCase.class);
      return suite;
   }

   public UnderscoreInFieldNameTestCase(String name)
   {
      super(name);
   }

   public void testAnnotationOverrides()throws Exception
   {
      POJO pojo = new POJO(6, 10);
      assertTrue(TestAspect.fields.contains("_foo"));
      assertTrue(TestAspect.fields.contains("_bar"));
   }
}
