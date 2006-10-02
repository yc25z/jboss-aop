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

import java.util.ArrayList;

public class Driver
{
   private static void caller()
   {
      System.out.println("--- caller is calling new ArrayList(); ---");
      ArrayList list = new ArrayList();
      System.out.println("--- caller is calling list.size(); ---");
      list.size();
   }
   private static void anotherCaller()
   {
      System.out.println("--- anotherCaller is calling new ArrayList(); ---");
      ArrayList list = new ArrayList();
      System.out.println("--- anotherCaller is calling list.size(); ---");
      list.size();
   }
   public static void main(String[] args)
   {
      System.out.println("--- main is calling new ArrayList(); ---");
      ArrayList list = new ArrayList();
      System.out.println("--- main is calling list.size(); ---");
      list.size();

      caller();
      anotherCaller();
   }
}
