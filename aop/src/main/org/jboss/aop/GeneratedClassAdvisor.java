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
package org.jboss.aop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.jboss.aop.advice.AdviceBinding;
import org.jboss.aop.advice.AspectDefinition;
import org.jboss.aop.advice.InterceptorFactory;
import org.jboss.aop.advice.GeneratedAdvisorInterceptor;
import org.jboss.aop.advice.PrecedenceSorter;
import org.jboss.aop.instrument.ConByConJoinPointGenerator;
import org.jboss.aop.instrument.ConByMethodJoinPointGenerator;
import org.jboss.aop.instrument.ConstructionJoinPointGenerator;
import org.jboss.aop.instrument.ConstructorJoinPointGenerator;
import org.jboss.aop.instrument.FieldJoinPointGenerator;
import org.jboss.aop.instrument.JoinPointGenerator;
import org.jboss.aop.instrument.MethodByConJoinPointGenerator;
import org.jboss.aop.instrument.MethodByMethodJoinPointGenerator;
import org.jboss.aop.instrument.MethodJoinPointGenerator;
import org.jboss.aop.joinpoint.Joinpoint;
import org.jboss.aop.joinpoint.MethodJoinpoint;
import org.jboss.aop.pointcut.PointcutMethodMatch;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import gnu.trove.TLongObjectHashMap;

/**
 * Comment
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class GeneratedClassAdvisor extends ClassAdvisor
{
   public static final String ADD_METHOD_INFO = "addMethodInfo";
   public static final String ADD_CONSTRUCTOR_INFO = "addConstructorInfo";
   public static final String ADD_CONSTRUCTION_INFO = "addConstructionInfo";
   public static final String ADD_FIELD_READ_INFO = "addFieldReadInfo";
   public static final String ADD_FIELD_WRITE_INFO = "addFieldWriteInfo";
   public static final String GET_PARENT_ADVISOR = "getParentAdvisor";

   MethodInterceptors methodInfos = new MethodInterceptors(this);
   ArrayList constructorInfos = new ArrayList();
   ArrayList constructionInfos = new ArrayList();
   ArrayList fieldReadInfos = new ArrayList();
   ArrayList fieldWriteInfos = new ArrayList();
   /** Super class methods that have been overrridden */
   ArrayList overriddenMethods = new ArrayList(); 

   ConcurrentReaderHashMap constructorJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap constructionJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap fieldReadJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap fieldWriteJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap methodJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap methodByConJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap methodByMethodJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap conByConJoinPoinGenerators = new ConcurrentReaderHashMap();
   ConcurrentReaderHashMap conByMethodJoinPoinGenerators = new ConcurrentReaderHashMap();

   boolean initialisedSuperClasses; 

   protected GeneratedClassAdvisor(String classname)
   {
      //Generated advisors will not pass in an aspectmanager
      //This will be passed in via the initialise() method
      super(classname, null);
   }

   protected void initialise(Class clazz, AspectManager manager)
   {
      super.setManager(manager);

      //Make sure that we copy across per class and per joinpoint aspects from the old advisor if it exists
      //Generated advisors get created when the class is first accessed (not loaded), meaning that there could be an exisiting advisor
      //used for mathcing already when setting up the microcontainer.
      Advisor existing = AspectManager.instance().getAnyAdvisorIfAdvised(clazz);
      if (existing != null)
      {
         this.aspects = existing.aspects;
         if (existing instanceof GeneratedClassAdvisor)
         {
            this.perClassJoinpointAspectDefinitions = ((GeneratedClassAdvisor)existing).perClassJoinpointAspectDefinitions;
         }
      }
      
      manager.initialiseClassAdvisor(clazz, this);
   }
   
   protected void handleOverriddenMethods(AdviceBinding binding)
   {
      if (overriddenMethods != null && overriddenMethods.size() > 0)
      {
         for (Iterator it = overriddenMethods.iterator() ; it.hasNext() ; )
         {
            MethodInfo info = (MethodInfo)it.next();
            Method method = info.getAdvisedMethod();
            PointcutMethodMatch match = binding.getPointcut().matchesExecution(this, method);
            
            if (match != null && match.isMatch())
            {
               adviceBindings.add(binding);
               if (AspectManager.verbose)
               {
                  System.err.println("method matched binding " + binding.getPointcut().getExpr() + " " + method.toString());     
               }
               binding.addAdvisor(this);
               pointcutResolved(info, binding, new MethodJoinpoint(method));
            }
         }
      }      
   }

   protected void resolveMethodPointcut(MethodInterceptors newMethodInterceptors, AdviceBinding binding)
   {
      super.resolveMethodPointcut(newMethodInterceptors, binding);
      handleOverriddenMethods(binding);
   }

   
   protected void addMethodInfo(MethodInfo mi)
   {
      MethodInfo old = methodInfos.getMethodInfo(mi.getHash());
      if (old != null)
      {
         overriddenMethods.add(old);
      }
      methodInfos.put(mi.getHash(), mi);
      //If we do dynamic invokes the method will need to be accessible via reflection if private/protected
      SecurityActions.setAccessible(mi.getAdvisedMethod());
   }

   protected MethodInterceptors initializeMethodChain()
   {
      //We have all the advised methods here, need to get all the others here too

      long[] keys = advisedMethods.keys();
      for (int i = 0; i < keys.length; i++)
      {
         MethodMatchInfo matchInfo = methodInfos.getMatchInfo(keys[i]);

         if (super.initialized && matchInfo != null)
         {
            matchInfo.clear();
         }

         if (matchInfo == null)
         {
            MethodInfo info = new MethodInfo();
            Method amethod = (Method) advisedMethods.get(keys[i]);
            info.setAdvisedMethod(amethod);
            info.setUnadvisedMethod(amethod);
            info.setHash(keys[i]);
            info.setAdvisor(this);
            methodInfos.put(keys[i], info);
         }
      }

      return methodInfos;
   }


   protected void addConstructorInfo(ConstructorInfo ci)
   {
      constructorInfos.add(ci);
      //If we do dynamic invokes the constructor will need to be accessible via reflection
      SecurityActions.setAccessible(ci.getConstructor());
   }

   protected ArrayList initializeConstructorChain()
   {
      if (super.initialized)
      {
         for (Iterator it = constructorInfos.iterator() ; it.hasNext() ; )
         {
            ((ConstructorInfo)it.next()).clear();
         }
      }
      return constructorInfos;
   }

   protected void addConstructionInfo(ConstructionInfo ci)
   {
      constructionInfos.add(ci);
   }

   protected ArrayList initializeConstructionChain()
   {
      if (super.initialized)
      {
         for (Iterator it = constructionInfos.iterator() ; it.hasNext() ; )
         {
            ((ConstructionInfo)it.next()).clear();
         }
      }
      return constructionInfos;
   }

   protected void addFieldReadInfo(FieldInfo fi)
   {
      fieldReadInfos.add(fi);
      //If we do dynamic invokes the field will need to be accessible via reflection
      SecurityActions.setAccessible(fi.getAdvisedField());
   }

   protected ArrayList initializeFieldReadChain()
   {
      return mergeFieldInfos(fieldReadInfos);
   }

   protected void addFieldWriteInfo(FieldInfo fi)
   {
      fieldWriteInfos.add(fi);
      //If we do dynamic invokes the field will need to be accessible via reflection
      SecurityActions.setAccessible(fi.getAdvisedField());
   }

   protected ArrayList initializeFieldWriteChain()
   {
      return mergeFieldInfos(fieldWriteInfos);
   }

   /* Creates a full list of field infos for all fields, using the ones added by
    * generated advisor for advised fields.
    */
   private ArrayList mergeFieldInfos(ArrayList advisedInfos)
   {
      ArrayList newInfos = new ArrayList(advisedFields.length);

      FieldInfo nextFieldInfo = null;
      Iterator it = advisedInfos.iterator();
      if (it.hasNext())
      {
         nextFieldInfo = (FieldInfo)it.next();
      }

      for (int i = 0 ; i < advisedFields.length ; i++)
      {
         if (nextFieldInfo != null && nextFieldInfo.getIndex() == i)
         {
            if (super.initialized)
            {
               nextFieldInfo.clear();
            }

            newInfos.add(nextFieldInfo);
            if (it.hasNext())
            {
               nextFieldInfo = (FieldInfo)it.next();
            }
            else
            {
               nextFieldInfo = null;
            }
         }
         else
         {
            FieldInfo info = new FieldInfo();
            info.setAdvisedField(advisedFields[i]);
            info.setAdvisor(this);
            info.setIndex(i);
            newInfos.add(info);
         }
      }

      return newInfos;
   }

   protected void finalizeMethodChain(MethodInterceptors newMethodInterceptors)
   {
      TLongObjectHashMap newMethodInfos = new TLongObjectHashMap();

      long[] keys = newMethodInterceptors.keys();
      for (int i = 0; i < keys.length; i++)
      {
         MethodMatchInfo matchInfo = newMethodInterceptors.getMatchInfo(keys[i]);
         matchInfo.populateBindings();

         MethodInfo info = matchInfo.getInfo();
         newMethodInfos.put(keys[i], info);

         MethodJoinPointGenerator generator = getJoinPointGenerator(info);
         finalizeChainAndRebindJoinPoint(info, generator);
      }
      methodInterceptors = newMethodInfos;
      
      //Handle the overridden methods
      if (overriddenMethods != null && overriddenMethods.size() > 0)
      {
         for (Iterator it = overriddenMethods.iterator() ; it.hasNext() ; )
         {
            MethodInfo info = (MethodInfo)it.next();

            MethodJoinPointGenerator generator = getJoinPointGenerator(info);
            finalizeChainAndRebindJoinPoint(info, generator);
         }
      }      

      
   }

   protected void finalizeFieldReadChain(ArrayList newFieldInfos)
   {
      for (int i = 0; i < newFieldInfos.size(); i++)
      {
         FieldInfo info = (FieldInfo)newFieldInfos.get(i);
         FieldJoinPointGenerator generator = getJoinPointGenerator(info);
         finalizeChainAndRebindJoinPoint(info, generator);
      }
   }

   protected void finalizeFieldWriteChain(ArrayList newFieldInfos)
   {
      for (int i = 0; i < newFieldInfos.size(); i++)
      {
         FieldInfo info = (FieldInfo)newFieldInfos.get(i);
         FieldJoinPointGenerator generator = getJoinPointGenerator(info);
         finalizeChainAndRebindJoinPoint(info, generator);
      }
   }


   protected void finalizeConstructorChain(ArrayList newConstructorInfos)
   {
      for (int i = 0; i < newConstructorInfos.size(); i++)
      {
         ConstructorInfo info = (ConstructorInfo) newConstructorInfos.get(i);
         ConstructorJoinPointGenerator generator = getJoinPointGenerator(info);
         finalizeChainAndRebindJoinPoint(info, generator);
      }
   }

   protected void finalizeConstructionChain(ArrayList newConstructionInfos)
   {
      for (int i = 0; i < newConstructionInfos.size(); i++)
      {
         ConstructionInfo info = (ConstructionInfo) newConstructionInfos.get(i);
         ConstructionJoinPointGenerator generator = getJoinPointGenerator(info);
         finalizeChainAndRebindJoinPoint(info, generator);
      }
   }

   protected void finalizeMethodCalledByMethodInterceptorChain(MethodByMethodInfo info)
   {
      MethodByMethodJoinPointGenerator generator = getJoinPointGenerator(info);
      finalizeChainAndRebindJoinPoint(info, generator);
   }

   protected void finalizeConCalledByMethodInterceptorChain(ConByMethodInfo info)
   {
      ConByMethodJoinPointGenerator generator = getJoinPointGenerator(info);
      finalizeChainAndRebindJoinPoint(info, generator);
   }

   protected void finalizeConCalledByConInterceptorChain(ConByConInfo info)
   {
      ConByConJoinPointGenerator generator = getJoinPointGenerator(info);
      finalizeChainAndRebindJoinPoint(info, generator);
   }


   protected void finalizeMethodCalledByConInterceptorChain(MethodByConInfo info)
   {
      //An extra level of indirection since we distinguish between callers of method depending on
      //where the called method is defined (sub/super interfaces)
      ConcurrentReaderHashMap map = (ConcurrentReaderHashMap)methodByConJoinPoinGenerators.get(info.getJoinpoint());
      if (map == null)
      {
         map = new ConcurrentReaderHashMap();
         methodByConJoinPoinGenerators.put(info.getJoinpoint(), map);
         map = (ConcurrentReaderHashMap)methodByConJoinPoinGenerators.get(info.getJoinpoint());
      }

      MethodByConJoinPointGenerator generator = getJoinPointGenerator(info);
      finalizeChainAndRebindJoinPoint(info, generator);
   }

   protected MethodJoinPointGenerator getJoinPointGenerator(MethodInfo info)
   {
      MethodJoinPointGenerator generator = (MethodJoinPointGenerator)methodJoinPoinGenerators.get(info.getJoinpoint());
      if (generator == null)
      {
         generator = new MethodJoinPointGenerator(this, info);
         methodJoinPoinGenerators.put(info.getJoinpoint(), generator);
      }
      return generator;
   }

   protected FieldJoinPointGenerator getJoinPointGenerator(FieldInfo info)
   {
      if (info.isRead())
      {
         FieldJoinPointGenerator generator = (FieldJoinPointGenerator)fieldReadJoinPoinGenerators.get(info.getJoinpoint());
         if (generator == null)
         {
            generator = new FieldJoinPointGenerator(this, info);
            fieldReadJoinPoinGenerators.put(info.getJoinpoint(), generator);
         }
         return generator;
      }
      else
      {
         FieldJoinPointGenerator generator = (FieldJoinPointGenerator)fieldWriteJoinPoinGenerators.get(info.getJoinpoint());
         if (generator == null)
         {
            generator = new FieldJoinPointGenerator(this, info);
            fieldWriteJoinPoinGenerators.put(info.getJoinpoint(), generator);
         }
         return generator;
      }
   }

   protected void test123()
   {

   }

   protected ConstructorJoinPointGenerator getJoinPointGenerator(ConstructorInfo info)
   {
      ConstructorJoinPointGenerator generator = (ConstructorJoinPointGenerator)constructorJoinPoinGenerators.get(info.getJoinpoint());
      if (generator == null)
      {
         generator = new ConstructorJoinPointGenerator(this, info);
         constructorJoinPoinGenerators.put(info.getJoinpoint(), generator);
      }
      return generator;
   }

   protected ConstructionJoinPointGenerator getJoinPointGenerator(ConstructionInfo info)
   {
      ConstructionJoinPointGenerator generator = (ConstructionJoinPointGenerator)constructionJoinPoinGenerators.get(info.getJoinpoint());
      if (generator == null)
      {
         generator = new ConstructionJoinPointGenerator(this, info);
         constructionJoinPoinGenerators.put(info.getJoinpoint(), generator);
      }
      return generator;
   }

   protected MethodByMethodJoinPointGenerator getJoinPointGenerator(MethodByMethodInfo info)
   {
      //An extra level of indirection since we distinguish between callers of method depending on
      //where the called method is defined (sub/super interfaces)
      ConcurrentReaderHashMap map = (ConcurrentReaderHashMap)methodByMethodJoinPoinGenerators.get(info.getJoinpoint());
      if (map == null)
      {
         map = new ConcurrentReaderHashMap();
         methodByMethodJoinPoinGenerators.put(info.getJoinpoint(), map);
         map = (ConcurrentReaderHashMap)methodByMethodJoinPoinGenerators.get(info.getJoinpoint());
      }

      MethodByMethodJoinPointGenerator generator = (MethodByMethodJoinPointGenerator)map.get(info.getCalledClass());
      if (generator == null)
      {
         generator = new MethodByMethodJoinPointGenerator(this, info);
         map.put(info.getCalledClass(), generator);
         generator = (MethodByMethodJoinPointGenerator)map.get(info.getCalledClass());
      }
      return generator;
   }

   protected ConByMethodJoinPointGenerator getJoinPointGenerator(ConByMethodInfo info)
   {
      ConByMethodJoinPointGenerator generator = (ConByMethodJoinPointGenerator)conByMethodJoinPoinGenerators.get(info.getJoinpoint());
      if (generator == null)
      {
         generator = new ConByMethodJoinPointGenerator(this, info);
         conByMethodJoinPoinGenerators.put(info.getJoinpoint(), generator);
      }
      return generator;
   }

   protected ConByConJoinPointGenerator getJoinPointGenerator(ConByConInfo info)
   {
      ConByConJoinPointGenerator generator = (ConByConJoinPointGenerator)conByConJoinPoinGenerators.get(info.getJoinpoint());
      if (generator == null)
      {
         generator = new ConByConJoinPointGenerator(this, info);
         conByConJoinPoinGenerators.put(info.getJoinpoint(), generator);
      }
      return generator;
   }

   protected MethodByConJoinPointGenerator getJoinPointGenerator(MethodByConInfo info)
   {
      //An extra level of indirection since we distinguish between callers of method depending on
      //where the called method is defined (sub/super interfaces)
      ConcurrentReaderHashMap map = (ConcurrentReaderHashMap)methodByConJoinPoinGenerators.get(info.getJoinpoint());
      if (map == null)
      {
         map = new ConcurrentReaderHashMap();
         methodByConJoinPoinGenerators.put(info.getJoinpoint(), map);
         map = (ConcurrentReaderHashMap)methodByConJoinPoinGenerators.get(info.getJoinpoint());
      }

      MethodByConJoinPointGenerator generator = (MethodByConJoinPointGenerator)map.get(info.getCalledClass());
      if (generator == null)
      {
         generator = new MethodByConJoinPointGenerator(this, info);
         map.put(info.getCalledClass(), generator);
         generator = (MethodByConJoinPointGenerator)map.get(info.getCalledClass());
      }
      return generator;
   }

   /**
    * Override default behaviour of when a pointcut is matched, populate the factories since this
    * is what is needed for generating the optimized invocation method
    */
   protected void pointcutResolved(JoinPointInfo info, AdviceBinding binding, Joinpoint joinpoint)
   {
      ArrayList curr = info.getInterceptorChain();
      if (binding.getCFlow() != null)
      {
         //TODO Handle CFlow
         InterceptorFactory[] factories = binding.getInterceptorFactories();
         for (int i = 0 ; i < factories.length ; i++)
         {
            curr.add(new GeneratedAdvisorInterceptor(factories[i], this, joinpoint, binding.getCFlowString(), binding.getCFlow()));
         }
      }
      else
      {
         InterceptorFactory[] factories = binding.getInterceptorFactories();
         for (int i = 0 ; i < factories.length ; i++)
         {
            curr.add(new GeneratedAdvisorInterceptor(factories[i], this, joinpoint));
         }
      }
   }

   private void finalizeChainAndRebindJoinPoint(JoinPointInfo info, JoinPointGenerator generator)
   {
      ArrayList list = info.getInterceptorChain();
      GeneratedAdvisorInterceptor[] factories = null;
      if (list.size() > 0)
      {
         factories = applyPrecedence((GeneratedAdvisorInterceptor[]) list.toArray(new GeneratedAdvisorInterceptor[list.size()]));
      }
      info.setInterceptors(factories);

      generator.rebindJoinpoint(info);
   }

   public String toString()
   {
      Class clazz = this.getClass();
      StringBuffer sb = new StringBuffer("CLASS: " + clazz.getName());

      Field[] fields = clazz.getFields();
      for (int i = 0 ; i < fields.length ; i++)
      {
         sb.append("\n\t" + fields[i]);
      }
      return sb.toString();
   }

   GeneratedAdvisorInterceptor[] applyPrecedence(GeneratedAdvisorInterceptor[] interceptors)
   {
      return PrecedenceSorter.applyPrecedence(interceptors, manager);
   }

   GeneratedClassAdvisor parent;
   /**
    * Generated InstanceAdvisors will set this
    */
   protected void setParentAdvisor(GeneratedClassAdvisor parent)
   {
      this.parent = parent;
   }

   /**
    * Generated instance advisors will override this and return the parent class advisor
    */
   protected GeneratedClassAdvisor getParentAdvisor()
   {
      return parent;
   }

   /**
    * If this is an instance advisor, will check with parent class advisor if the aspect
    * is already registered. If so, we should use the one from the parent advisor
    */
   public Object getPerClassAspect(AspectDefinition def)
   {
      ClassAdvisor parentAdvisor = getParentAdvisor();

      if (parentAdvisor != null)
      {
         Object aspect = parentAdvisor.getPerClassAspect(def);
         if (aspect != null) return aspect;
      }

      return super.getPerClassAspect(def);
   }

   /**
    * Generated ClassAdvisors and InstanceAdvisors will be different instances,
    * so keep track of what per_class_joinpoint aspects have been added where
    */
   ConcurrentReaderHashMap perClassJoinpointAspectDefinitions = new ConcurrentReaderHashMap();

   /**
    * If this is an instance advisor, will check with parent class advisor if the aspect
    * is already registered. If so, we should use the one from the parent advisor
    */
   public Object getPerClassJoinpointAspect(AspectDefinition def, Joinpoint joinpoint)
   {
      GeneratedClassAdvisor parentAdvisor = getParentAdvisor();

      if (parentAdvisor != null)
      {
         Object aspect = parentAdvisor.getPerClassJoinpointAspect(def, joinpoint);
         if (aspect != null)return aspect;
      }

      Map joinpoints = (Map) perClassJoinpointAspectDefinitions.get(def);
      if (joinpoints != null)
      {
         return joinpoints.get(joinpoint);
      }
      return null;
   }

   public synchronized void addPerClassJoinpointAspect(AspectDefinition def, Joinpoint joinpoint)
   {
      Map joinpoints = (Map)perClassJoinpointAspectDefinitions.get(def);
      if (joinpoints == null)
      {
         joinpoints = new ConcurrentReaderHashMap();
         perClassJoinpointAspectDefinitions.put(def, joinpoints);
      }

      if (joinpoints.get(joinpoint) == null)
      {
         joinpoints.put(joinpoint, def.getFactory().createPerJoinpoint(this, joinpoint));
      }
   }

   /**
    * @see Advisor#chainOverridingForInheritedMethods()
    */
   public boolean chainOverridingForInheritedMethods()
   {
      return true;
   }
}
