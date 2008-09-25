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
package org.jboss.aop.asintegration.core;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import javassist.ClassPool;
import javassist.scopedpool.ScopedClassPoolFactory;

import javax.management.ObjectName;

import org.jboss.aop.AspectManager;
import org.jboss.aop.AspectXmlLoader;
import org.jboss.aop.ClassLoaderValidation;
import org.jboss.aop.ClassicWeavingStrategy;
import org.jboss.aop.Deployment;
import org.jboss.aop.SuperClassesFirstWeavingStrategy;
import org.jboss.aop.advice.AdviceBinding;
import org.jboss.aop.advice.AdviceStack;
import org.jboss.aop.advice.AspectDefinition;
import org.jboss.aop.advice.InterceptorFactory;
import org.jboss.aop.annotation.PortableAnnotationElement;
import org.jboss.aop.asintegration.JBossIntegration;
import org.jboss.aop.asintegration.jboss4.JBoss4Integration;
import org.jboss.aop.hook.JDK14Transformer;
import org.jboss.aop.hook.JDK14TransformerManager;
import org.jboss.aop.instrument.InstrumentorFactory;
import org.jboss.aop.instrument.TransformerCommon;
import org.jboss.aop.introduction.InterfaceIntroduction;
import org.jboss.logging.Logger;
import org.jboss.mx.loading.HeirarchicalLoaderRepository3;
import org.jboss.mx.server.ServerConstants;
import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.system.server.ServerConfig;
import org.w3c.dom.Document;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AspectManagerServiceDelegate
{
   Logger log = Logger.getLogger(AspectManagerServiceDelegate.class);
   
   static {
      //pre-load necessary classes so that we avoid NoClassDefFoundErrors on JRockit when using the RepositoryClassloader hook
      //When AspectManager.translate() is called the first time, these classes have not been loaded yet, and this is what causes
      //JRockit to get confused
      @SuppressWarnings("unused")
      Class<?> clazz = TransformerCommon.class;
      clazz = SuperClassesFirstWeavingStrategy.class;
      clazz = ClassicWeavingStrategy.class;
      clazz = HeirarchicalLoaderRepository3.class;
   }

   public static final ObjectName DEFAULT_LOADER_REPOSITORY = ObjectNameFactory.create(ServerConstants.DEFAULT_LOADER_NAME);

   // Attributes ---------------------------------------------------

   boolean created = false;
   protected File tmpClassesDir;
   protected boolean enableTransformer = false;
   protected boolean enableLoadtimeWeaving = false;
   protected boolean suppressTransformationErrors = true;
   protected boolean suppressReferenceErrors = true;
   protected String exclude;
   protected String include;
   protected String ignore;
   protected String includedInvisibleAnnotations;
   
   /**
    * A string containing xml to be deployed first thing when this service is created
    */
   private String bootstrapXml;
   /**
    * The name of the file used to deploy the core aspects
    */
   private String baseXml = "base-aop.xml";
   
   //When running with JBoss 5 registration with MBeanServer happens after the service has been started
   boolean registerHappensAfterStart;
   boolean hasBeenStarted;
   
   /** The encapsulation of the integration */
   private JBossIntegration integration;
   
   private AtomicBoolean started = new AtomicBoolean();
   
   private AspectManager manager;
   
   
   // Static -------------------------------------------------------

   // Constructors -------------------------------------------------
   public AspectManagerServiceDelegate()
   {
      
   }

   public void setBootstrapXml(String bootstrapXml)
   {
      this.bootstrapXml = bootstrapXml;
   }
   
   public void setBaseXml(String baseXml)
   {
      this.baseXml = baseXml;
   }
   
   public void setRegisterHappensAfterStart(boolean registerHappensAfterStart)
   {
      this.registerHappensAfterStart = registerHappensAfterStart;
   }

   /**
    * This needs setting to true in AS 4
    */
   public void setAspectManagerMaintainAdvisorInterceptors(boolean maintain)
   {
      AspectManager.maintainAdvisorMethodInterceptors = maintain;
   }
   
   public void setAspectManager(AspectManager manager)
   {
      this.manager = manager;
   }
   
   public AspectManager getAspectManager()
   {
      return manager;
   }

   // Public -------------------------------------------------------

   /**
    * Get the integration
    * 
    * @return the integration
    */
   public JBossIntegration getJBossIntegration()
   {
      return integration;
   }
   
   /**
    * Set the integration
    * 
    * @param integration the integration 
    */
   public void setJBossIntegration(JBossIntegration integration)
   {
      this.integration = integration;
   }

   public ScopedClassPoolFactory createFactory() throws Exception
   {
      return initIntegration().createScopedClassPoolFactory(tmpClassesDir);
   }
   
   public ClassLoaderValidation createClassLoaderValidation()
   {
      return initIntegration();
   }

   /**
    * Initialize the integration if not alreday done so
    * 
    * @return the integration
    */
   protected JBossIntegration initIntegration()
   {
      // Default to old JBoss4 integration when not configured
      if (integration == null)
         integration = new JBoss4Integration();
      return integration;
   }
   
   public void create()
           throws Exception
   {
      initIntegration();
      if (hasBeenStarted)
      {
         return;
      }
      
      
      // Set a default tmp classes dir to the jboss tmp dir/aopclasses
      if (tmpClassesDir == null)
      {
         String jbossTmpDir = System.getProperty(ServerConfig.SERVER_TEMP_DIR);
         if (jbossTmpDir == null)
            jbossTmpDir = System.getProperty("java.io.tmpdir");
         tmpClassesDir = new File(jbossTmpDir, "aopdynclasses");
      }
      // Validate the the tmp dir exists
      if (tmpClassesDir.exists() == false && tmpClassesDir.mkdirs() == false)
         throw new FileNotFoundException("Failed to create tmpClassesDir: " + tmpClassesDir.getAbsolutePath());
      AspectManager.setClassPoolFactory(createFactory());

      AspectManager.classLoaderValidator = createClassLoaderValidation();
      // Add the tmp classes dir to our UCL classpath

      Deployment.searchClasspath = false; // turn off dynamic finding of DDs
      AspectManager.suppressTransformationErrors = suppressTransformationErrors;
      if (enableTransformer && enableLoadtimeWeaving) throw new RuntimeException("Cannot set both EnableTransformer and EnableLoadtimeWeaving");
      if (enableTransformer)
      {
         attachDeprecatedTranslator();
      }
      if (enableLoadtimeWeaving)
      {
         attachTranslator();
      }
      created = true;
      AspectManager.setClassLoaderScopingPolicy(integration.createAOPClassLoaderScopingPolicy());

      bootstrapXml();
      baseXml();
   }
   
   public void start() throws Exception
   {
      started.set(true);
   }

   public void stop()
   {
      started.set(false);
   }
   
   public void destroy()
   {
      //TODO remove baseAop stuff
      try
      {
         if (bootstrapXml != null)
         {
            InputStream in = new BufferedInputStream(new ByteArrayInputStream(bootstrapXml.getBytes()));
            try
            {
               Document doc = AspectXmlLoader.loadDocument(new BufferedInputStream(in));
               AspectXmlLoader loader = new AspectXmlLoader();
               loader.setManager(manager);
               PortableAnnotationElement.setClosingDownManager(true);
               loader.undeployXML(doc, null);
            }
            finally
            {
               if (in != null)
               {
                  in.close();
               }
            }
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException("Problem destroying AspectManager", e);
      }
   }
   
   protected void baseXml()
   {
      if (baseXml == null)
      {
         return;
      }
      baseXml(baseXml);
   }

   public void baseXml(String xml)
   {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      URL base = cl.getResource(xml);
      try
      {
         if (base != null)
         {
            AspectXmlLoader.deployXML(base);
         }
         else
         {
            log.warn("Could not find " + xml + " file in the resources of " + cl);
         }
      }
      catch (Exception e)
      {
         System.out.println("Error loading " + xml + " file" + e);
      }
   }
   
   public void bootstrapXml()
   {
      try
      {
         if (bootstrapXml != null && bootstrapXml.trim().length() > 0)
         {
            InputStream in = new BufferedInputStream(new ByteArrayInputStream(bootstrapXml.getBytes()));
            try
            {
               Document doc = AspectXmlLoader.loadDocument(new BufferedInputStream(in));
               AspectXmlLoader loader = new AspectXmlLoader();
               loader.setManager(manager);
               loader.deployXML(doc, null);
            }
            finally
            {
               if (in != null)
               {
                  in.close();
               }
            }
         }
      }
      catch (Exception e)
      {
         // AutoGenerated
         throw new RuntimeException("Problem starting AspectManager", e);
      }
   }
   
   protected void attachDeprecatedTranslator()
   {
      log.warn("EnableTransformer has been deprecated, please use EnableLoadtimeWeaving.  See docs for more details");
      initIntegration().attachDeprecatedTranslator();
   }

   protected void detachDeprecatedTranslator()
   {
      initIntegration().detachDeprecatedTranslator();
   }

   /*
    * Despite its name, this is also used for the JRockit transformer
    */
   protected void attachTranslator()
   {
      //Despite its name, this is also used for the JRockit transformer
      JDK14TransformerManager.transformer = new JDK14Transformer()
      {
         public byte[] transform(ClassLoader loader, String classname, byte[] classBytes)
         {
            try
            {
               //Make sure that we use the correct classloader, in order to get the correct domain if it is a scoped loader
               return AspectManager.instance(loader).translate(classname, loader, classBytes);
            }
            catch (Exception e)
            {
               throw new RuntimeException("Error converting " + classname + " on " + loader, e);
            }
         }
      };
   }

   protected void detachTranslator()
   {
      JDK14TransformerManager.transformer = null;
   }

   public boolean getPrune()
   {
      return AspectManager.getPrune();
   }

   public void setPrune(boolean prune)
   {
      AspectManager.setPrune(prune);
   }

   public String getExclude()
   {
      return exclude;
   }

   public void setExclude(String exclude)
   {
      this.exclude = exclude;
      ArrayList<String> list = new ArrayList<String>();
      if (exclude != null)
      {
         StringTokenizer tokenizer = new StringTokenizer(exclude, ",");
         while (tokenizer.hasMoreTokens())
         {
            list.add(tokenizer.nextToken().trim());
         }
      }
      manager.setExclude(list);
   }

   public String getInclude()
   {
      return include;
   }

   public void setInclude(String include)
   {
      this.include = include;
      ArrayList<String> list = new ArrayList<String>();
      if (include != null)
      {
         StringTokenizer tokenizer = new StringTokenizer(include, ",");
         while (tokenizer.hasMoreTokens())
         {
            list.add(tokenizer.nextToken().trim());
         }
      }
      manager.setInclude(list);
   }

   public String getIgnore()
   {
      return ignore;
   }

   public void setIgnore(String ignore)
   {
      this.ignore = ignore;
      ArrayList<String> list = new ArrayList<String>();
      if (ignore != null)
      {
         StringTokenizer tokenizer = new StringTokenizer(ignore, ",");
         while (tokenizer.hasMoreTokens())
         {
            list.add(tokenizer.nextToken().trim());
         }
      }
      manager.setIgnore(list);
   }
   
   public String getIncludedInvisibleAnnotations()
   {
      return includedInvisibleAnnotations;
   }
   
   public void setIncludedInvisibleAnnotations(String ia)
   {
      List<String> iiaList = new ArrayList<String>();
      if(ia != null)
      {
         for(String inc : ia.split(","))
           iiaList.add(inc.trim());
      }
      manager.setIncludedInvisibleAnnotations(iiaList);
   }


   /**
    * The temporary directory to which dyn class files are written
    */
   public File getTmpClassesDir()
   {
      return tmpClassesDir;
   }

   /**
    * The temporary directory to which dyn class files are written
    */
   public void setTmpClassesDir(File tmpClassesDir)
   {
      this.tmpClassesDir = tmpClassesDir;
   }

   /**
    * Set the verbosity of aop logging.  It doesn't use log4j
    */
   public boolean getVerbose()
   {
      return AspectManager.verbose;
   }

   /**
    * Set the verbosity of aop logging.  It doesn't use log4j
    */
   public void setVerbose(boolean verbose)
   {
      AspectManager.verbose = verbose;
   }

   /**
    * Use aop optimizations.  Optional just in case there is a bug
    */
   public boolean getOptimized()
   {
      return AspectManager.optimize;
   }

   /**
    * Use aop optimizations.  Optional just in case there is a bug
    */
   public void setOptimized(boolean verbose)
   {
      AspectManager.optimize = verbose;
   }

   public boolean getSuppressTransformationErrors()
   {
      return suppressTransformationErrors;
   }

   public void setSuppressTransformationErrors(boolean suppressTransformationErrors)
   {
      this.suppressTransformationErrors = suppressTransformationErrors;
      AspectManager.suppressTransformationErrors = suppressTransformationErrors;
   }

   public boolean getSuppressReferenceErrors()
   {
      return suppressReferenceErrors;
   }

   public void setSuppressReferenceErrors(boolean suppressReferenceErrors)
   {
      this.suppressReferenceErrors = suppressReferenceErrors;
      AspectManager.suppressReferenceErrors = suppressReferenceErrors;
   }

   /**
    * The temporary directory to which dyn class files are written
    */
   public boolean getEnableTransformer()
   {
      return enableTransformer;
   }

   /**
    * The temporary directory to which dyn class files are written
    */
   public String interceptorFactories()
   {
      Map<String, InterceptorFactory> factories = manager.getInterceptorFactories();
      StringBuffer buffer = new StringBuffer("");
      for (String name : factories.keySet())
      {
         buffer.append(name + "<br>");
      }
      return buffer.toString();
   }

   /**
    * The temporary directory to which dyn class files are written
    */
   public String aspectDefinitions()
   {
      Map<String, AspectDefinition> factories = manager.getAspectDefinitions();
      StringBuffer buffer = new StringBuffer("");
      for (String name : factories.keySet())
      {
         buffer.append(name + "<br>");
      }
      return buffer.toString();
   }

   public String introductions()
   {
      Map<String, InterfaceIntroduction> factories = manager.getInterfaceIntroductions();
      StringBuffer buffer = new StringBuffer("");
      for (String name : factories.keySet())
      {
         buffer.append(name + "<br>");
      }
      return buffer.toString();
   }

   public String stacks()
   {
      Map<String, AdviceStack> factories = manager.getInterceptorStacks();
      StringBuffer buffer = new StringBuffer("");
      for (String name : factories.keySet())
      {
         buffer.append(name + "<br>");
      }
      return buffer.toString();
   }

   public String bindings()
   {
      Map<String, AdviceBinding> factories = manager.getBindings();
      StringBuffer buffer = new StringBuffer("");
      for (String name : factories.keySet())
      {
         buffer.append(name + "<br>");
      }
      return buffer.toString();
   }

   public String registeredClassLoaders()
   {
      Map<ClassLoader, ClassPool> loaders = AspectManager.getRegisteredCLs();
      StringBuffer buffer = new StringBuffer("");
      for (ClassLoader loader : loaders.keySet())
      {
         buffer.append(loader + "<br>");
      }
      return buffer.toString();
   }

   public void setEnableTransformer(boolean enableTransformer)
   {
      // Testsuite uses enableTransformer, we may be testing new loadtime features though.

      if (enableLoadtimeWeaving)
      {
         log.warn("enabledLoadtimeWeaving alread set");
         return;
      }
      if (this.enableTransformer == enableTransformer) return;
      if (started.get() == true)
      {
         if (enableTransformer)
         {
            attachDeprecatedTranslator();
         }
         else
         {
            detachDeprecatedTranslator();
         }
      }
      this.enableTransformer = enableTransformer;
   }

   public boolean getEnableLoadtimeWeaving()
   {
      return enableLoadtimeWeaving;
   }

   public void setEnableLoadtimeWeaving(boolean enableTransformer)
   {
      if (this.enableLoadtimeWeaving == enableTransformer) return;
      if (started.get() == true)
      {
         if (enableTransformer)
         {
            attachTranslator();
         }
         else
         {
            detachTranslator();
         }
      }
      this.enableLoadtimeWeaving = enableTransformer;
   }

   public String getInstrumentor()
   {
      return InstrumentorFactory.getInstrumentorName();
   }

   public void setInstrumentor(String instrumentor)
   {
      InstrumentorFactory.initialise(instrumentor);
   }

   
   public Logger getLog()
   {
      return log;
   }
}