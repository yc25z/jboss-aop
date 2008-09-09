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
package org.jboss.aophelper.ui.compile;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A OutputPane.
 * 
 * @author <a href="stalep@gmail.com">Stale W. Pedersen</a>
 * @version $Revision: 1.1 $
 */
public class OutputPane extends JPanel
{
   
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   private JTextArea outputArea;
   public OutputPane()
   {
      init();
   }
   
   private void init()
   {
      CompileMediator.instance().setOutputPane(this);
      
      setBackground(Color.white);
      setLayout(new FlowLayout());
      
      outputArea = new JTextArea();
      outputArea.setText("logs from the aopcompile will be seen here");
      outputArea.setLineWrap(true);
      outputArea.setWrapStyleWord(true);
      outputArea.setEditable(false);
      JScrollPane longScroll = new JScrollPane(outputArea);
      longScroll.setVisible(true);
      longScroll.setPreferredSize(new Dimension(1020, 270));
      
      add(longScroll, BorderLayout.CENTER);

   }
   
   public void setText(String text)
   {
      outputArea.setText(text);
   }

}