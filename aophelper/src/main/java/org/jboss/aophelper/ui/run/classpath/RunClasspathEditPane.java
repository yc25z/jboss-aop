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
package org.jboss.aophelper.ui.run.classpath;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jboss.aophelper.annotation.AopHelperAction;
import org.jboss.aophelper.core.Action;
import org.jboss.aophelper.core.AopOption;
import org.jboss.aophelper.core.AopState;

/**
 * A RunClasspathEditPane.
 * 
 * @author <a href="stalep@gmail.com">Stale W. Pedersen</a>
 * @version $Revision: 1.1 $
 */
public class RunClasspathEditPane extends JPanel
{

   private JButton addButton;
   private JButton removeButton;
   
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   public RunClasspathEditPane()
   {
      init();
   }


   private void init()
   {
//      setLayout(new BorderLayout());
      addButton = new JButton("Add to Classpath");
      removeButton = new JButton("Remove from Classpath");
      
      
      ActionListener action = new ActionListener() 
      { 
         public void actionPerformed(ActionEvent event) 
         {
            if(event.getSource() == addButton) 
            {
               System.out.println("We need to add");
               add();

            }
            else if(event.getSource() == removeButton)
            {
               System.out.println("remove");
               remove();

            }
         }
       };
       addButton.addActionListener(action);
       removeButton.addActionListener(action);
       
       add(addButton); //, BorderLayout.CENTER);
       add(removeButton); //, BorderLayout.CENTER);

   }
   
   @AopHelperAction(
         action=Action.ADD, 
         state=AopState.RUN, 
         option=AopOption.CLASSPATH)
   public void add()
   {
   }
   
   @AopHelperAction(
         action=Action.REMOVE, 
         state=AopState.RUN, 
         option=AopOption.CLASSPATH)
   public void remove()
   {
      
   }
}
