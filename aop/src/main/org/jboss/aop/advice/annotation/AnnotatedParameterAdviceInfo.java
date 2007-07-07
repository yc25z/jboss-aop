package org.jboss.aop.advice.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jboss.aop.AspectManager;
import org.jboss.aop.advice.AdviceMethodProperties;
import org.jboss.aop.advice.annotation.AdviceMethodFactory.ReturnType;

/**
 * Information about an advice method whose parameters should annotated according to
 * <code>ParameterAnnotationRule</code>s.
 * 
 * @author Flavia Rainone
 */
class AnnotatedParameterAdviceInfo extends AdviceInfo
{
   // the annotated parameter types
   private ParameterAnnotationType paramTypes[];
   // the context dependent annotated parameter types
   private ParameterAnnotationType contextParamTypes[];
   // muttually exclusive context parameter rules
   private int[][] mutuallyExclusive;
   // compusloriness rules: for each subarray, the first element is the precondition;
   // the following elements are the annotations whose use is compulsory given
   // that precondition is present among the annotated parameters
   private int[][] compulsory;
   
   /**
    * Creates an annotated parameter advice info.
    * 
    * @param properties        the properties to which <code>method</code> must
    *                          comply with
    * @param method            the advice method
    * @param rules             the annnotated parameter rules this method should
    *                          comply with
    * @param contextRules      second priority rules this method should comply with
    * @param mutuallyExclusive a list of mutually exclusive context parameter rules
    * @param compulsory        a list of the annotated parameters whose use is
    *                          compulsory only if the precondition annotation is
    *                          present among the annotated parameters.
    * 
    * @throws ParameterAnnotationRuleException thrown when the advice method does not
    *         comply with a parameter annotation rule.
    */
   public AnnotatedParameterAdviceInfo(AdviceMethodProperties properties,
         Method method, ParameterAnnotationRule[] rules,
         ParameterAnnotationRule[] contextRules, int[][] mutuallyExclusive,
         int[][] compulsory)
      throws ParameterAnnotationRuleException
   {
      super(method, 0);
      this.paramTypes = createParameterAnnotationTypes(rules);
      this.contextParamTypes = createParameterAnnotationTypes(contextRules);
      this.mutuallyExclusive = mutuallyExclusive;
      this.compulsory = compulsory;
      this.applyRules(properties);
   }
      
   public boolean validate(AdviceMethodProperties properties, ReturnType returnType)
   {
      for (ParameterAnnotationType paramType: paramTypes)
      {
         if (!paramType.validate(properties))
         {
            return false;
         }
      }
      
      for (ParameterAnnotationType paramType: contextParamTypes)
      {
         if (!paramType.validate(properties))
         {
            return false;
         }
      }
      
      switch (returnType)
      {
         case ANY:
            if (method.getReturnType() == void.class)
            {
               break;
            }
         case NOT_VOID:
            if (properties.getJoinpointReturnType() != void.class &&
                  method.getReturnType() != Object.class &&
                  !properties.getJoinpointReturnType().
                  isAssignableFrom(method.getReturnType()))
            {
               if (AspectManager.verbose)
               {
                  AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - return value of ");
                  AdviceMethodFactory.adviceMatchingMessage.append(method);
                  AdviceMethodFactory.adviceMatchingMessage.append(" can not be assigned to type ");
                  AdviceMethodFactory.adviceMatchingMessage.append(properties.getJoinpointReturnType());
               }
               return false;
            }
            break;
         case VOID:
            if (method.getReturnType()!= void.class)
            {
               if (AspectManager.verbose)
               {
                  AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - '");
                  AdviceMethodFactory.adviceMatchingMessage.append(method);
                  AdviceMethodFactory.adviceMatchingMessage.append("' must return void");
               }
               return false;
            }
      }
      
      for (int i = 0; i < mutuallyExclusive.length; i++)
      {
         int[] exclusiveParamTypes = mutuallyExclusive[i];
         int found = -1;
         for (int j = 0; j < exclusiveParamTypes.length; j++)
         {
            if (contextParamTypes[exclusiveParamTypes[j]].isSet())
            {
               if (found != -1)
               {
                  if (AspectManager.verbose)
                  {
                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - the use of parameter annotations ");
                     AdviceMethodFactory.adviceMatchingMessage.append(contextParamTypes[exclusiveParamTypes[found]].rule.getAnnotation());
                     AdviceMethodFactory.adviceMatchingMessage.append(" and ");
                     AdviceMethodFactory.adviceMatchingMessage.append(contextParamTypes[exclusiveParamTypes[j]].rule.getAnnotation());
                     AdviceMethodFactory.adviceMatchingMessage.append(" is mutually exclusive");
                  }
                  return false;
               }
               found = j;
            }
         }
      }
      
      if (compulsory != null)
      {
    	  for (int i = 0; i < compulsory.length; i++)
    	  {
    		  ParameterAnnotationType precondition = paramTypes[compulsory[i][0]];
    		  if (precondition.isSet())
    		  {
    			  for (int j = 1; j < compulsory[i].length; j++)
    			  {
    				  if (!paramTypes[compulsory[i][j]].isSet())
    				  {
    					  if (AspectManager.verbose)
    	                  {
    	                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - if parameter annotation ");
    	                     AdviceMethodFactory.adviceMatchingMessage.append(precondition.rule.getAnnotation());
    	                     AdviceMethodFactory.adviceMatchingMessage.append(" is used, the use of parameter annotation ");
    	                     AdviceMethodFactory.adviceMatchingMessage.append(paramTypes[compulsory[i][j]].rule.getAnnotation());
                             AdviceMethodFactory.adviceMatchingMessage.append(" is compulsory");
    	                  }
    	                  return false;
    				  }
    			  }
    		  }
    	  }
      }
      return true;
   }

   public void resetValidation()
   {
      for (int i = 0;  i < paramTypes.length; i++)
      {
         paramTypes[i].resetValidation();
      }
      for (int i = 0; i < contextParamTypes.length; i++)
      {
         contextParamTypes[i].resetValidation();
      }
   }
   
   public short getAssignabilityDegree(int annotationIndex, boolean isContextRule,
         AdviceMethodProperties properties)
   {
      if (isContextRule)
      {
         return contextParamTypes[annotationIndex].getAssignabilityDegree(properties);
      }
      return paramTypes[annotationIndex].getAssignabilityDegree(properties);
   }
   
   public void assignAdviceInfo(AdviceMethodProperties properties)
   {
      int args[] = new int[parameterTypes.length];
      for (int i = 0; i < paramTypes.length; i++)
      {
         paramTypes[i].assignParameterInfo(args);
      }
      for (int i = 0; i < contextParamTypes.length; i++)
      {
         contextParamTypes[i].assignParameterInfo(args);
      }
      properties.setFoundProperties(this.method, args);
   }
   
   /**
    * Creates a parameter annotation type array correpondent to the parameter
    * annotation rules contained in <code>rules</code>.
    * 
    * @param rules the parameter annotation rules
    * 
    * @return a parameter annotation type array correspondent to <code>rules</code>
    */
   private final ParameterAnnotationType[] createParameterAnnotationTypes(
         ParameterAnnotationRule[] rules)
   {
      ParameterAnnotationType[] types = new ParameterAnnotationType[rules.length];
      // create appropriate annotated parameter types for each AP rule
      for (int i = 0; i < rules.length; i++)
      {
         if (rules[i].isSingleEnforced())
         {
            types[i] = new SingleParameterType(rules[i]);
         }
         else
         {
            types[i] = new MultipleParameterType(rules[i],
                  method.getParameterTypes().length);
         }
      }
      return types;
   }
   
   /**
    * Applies all parameter annotation rules to the advice method parameters.
    * 
    * @param properties the properties which the searched advice method must comply
    *                   with
    * 
    * @throws ParameterAnnotationRuleException thrown when the advice method does not
    *         comply with a parameter annotation rule.
    */
   private void applyRules(AdviceMethodProperties properties) throws ParameterAnnotationRuleException
   {
      Annotation[][] paramAnnotations = method.getParameterAnnotations();
      ParameterAnnotationType typeFound;
      boolean nullifyRank = false;
      for (int i = 0; i < paramAnnotations.length; i++)
      {
         typeFound = null;
         for (Annotation annotation: paramAnnotations[i])
         {
            // no valid annotation found for parameter i yet
            if (typeFound == null)
            {
               typeFound = findAnnotationType(annotation, i);
            }
            else
            {
               if (findAnnotationType(annotation, i) != null)
               {
                  if (AspectManager.verbose)
                  {
                     throw new ParameterAnnotationRuleException("\n[warn] -parameter " + i  +
                           " of method " + method +  " contains more than one valid annotation");
                  }
                  else
                  {
                     throw new ParameterAnnotationRuleException(null);
                  }
               }
            }
         }
         if (typeFound == null)
         {
            if (AspectManager.verbose)
            {
               if (paramAnnotations[i].length == 0)
               {
                  throw new ParameterAnnotationRuleException("\n[warn] -parameter "
                        + i  + " of method " + method +  " is not annotated");
               }
               throw new ParameterAnnotationRuleException("\n[warn] -parameter "
                     + i  + " of method " + method +  " is not annotated correctly" +
                     "\n[warn]  Expecting one of: " + getDescription(paramTypes) +
                     getDescription(contextParamTypes));
            }
            // no need to say the reason a rule's been broken
            throw new ParameterAnnotationRuleException(null);
         }
         // this happens when target or caller are nulls
         // in this case, this advice should have the smallest rank, since
         // any other advice is preferable (in case of overloaded advices)
         nullifyRank = nullifyRank || (typeFound.rule.lowerRankGrade(properties));
      }
      if (nullifyRank)
      {
         rank = 0;
      }
   }
   
   private String getDescription(ParameterAnnotationType[] types)
   {
      StringBuffer buffer = new StringBuffer();
      for (int i = 1; i < types.length; i++)
      {
         buffer.append("\n          ");
         buffer.append(types[i]);
      }
      return buffer.toString();
   }

   /**
    * Searches for an annotation <code>Annotation</code> on parameter annotation
    * rules.
    * 
    * @param annotation the parameter annotation to be searched on the parameter
    *                   annotation rules
    * @param i          the number of the advice parameter that is annotated with
    *                   <code>annotation</code>
    * 
    * @return           the parameter type if there is a rule correspondent to
    *                   <code>annotation</code>; <code>null</code> otherwise
    * 
    * @throws ParameterAnnotationRuleException if a parameter annotation is found
    *         more than once and the annotation rule forbides multiple occurences
    */
   private final ParameterAnnotationType findAnnotationType(Annotation annotation,
         int i) throws ParameterAnnotationRuleException
   {
      
      for (int j = 0; j < paramTypes.length; j++)
      {
         // found
         if (paramTypes[j].applies(annotation, i))
         {
            return paramTypes[j];
         }
      }
      for (int j = 0; j < contextParamTypes.length; j++)
      {
         // found
         if (contextParamTypes[j].applies(annotation, i))
         {
            return contextParamTypes[j];
         }
      }
      return null;
   }
      
   /**
    * Contains validation data concerning a parameter annotation rule.
    */
   abstract class ParameterAnnotationType
   {
      ParameterAnnotationRule rule;
      
      public ParameterAnnotationType(ParameterAnnotationRule rule)
      {
         this.rule = rule;
      }
      
      /**
       * Verifies if <code>parameterAnnotation</code> is of this type, and, if it is,
       * sets the parameter index information.
       * 
       * @param parameterAnnotation the parameter annotation
       * @param parameterIndex      the parameter index
       * @return <code>true</code> if <code>parameterAnnotation</code> is of this type
       * @throws ParameterAnnotationRuleException if the parameter annotation has more
       *         than one occurrence and this is forbidden by the annotation rule
       */
      public final boolean applies(Annotation parameterAnnotation, int parameterIndex)
         throws ParameterAnnotationRuleException
      {
         if (parameterAnnotation.annotationType() == rule.getAnnotation())
         {
            setIndex(parameterIndex, parameterAnnotation);
            return true;
         }
         return false;
      }

      /**
       * Validates the occurences of this parameter type, according to the annotaion rule
       * and to <code>properties</code>.
       * 
       * @param properties contains information about the queried method
       * @return <code>true</code> if the occurrences of this parameter type are all valid
       */
      public final boolean validate(AdviceMethodProperties properties)
      {
         if (rule.isMandatory() && !isSet())
         {
            if (AspectManager.verbose)
            {
               AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - mandatory parameter annotation ");
               AdviceMethodFactory.adviceMatchingMessage.append(rule.getAnnotation());
               AdviceMethodFactory.adviceMatchingMessage.append(" not found on method ");
               AdviceMethodFactory.adviceMatchingMessage.append(method);
            }
            return false;
         }
         return internalValidate(properties);
      }

      public String toString()
      {
         return rule.getAnnotation().toString();
      }
      
      /**
       * Records that the parameter identified by <code>parameterIndex</code> is of
       * this type.
       * @param parameterIndex the index of the parameter
       * @param annotation     the annotation used on parameter <code>parameterIndex
       *                       </code>        
       * @throws ParameterAnnotationRuleException if the parameter annotation has more
       *         than one occurrence and this is forbidden by the annotation rule
       */
      public abstract void setIndex(int parameterIndex,
            Annotation annotation) throws ParameterAnnotationRuleException;

      /**
       * Returns <code>true</code> if there is a parameter of this type.
       */
      public abstract boolean isSet();
      
      /**
       * Validates the occurences of this parameter type, according to the annotation rule
       * and to <code>properties</code>.
       * 
       * @param properties contains information about the queried method
       * @return <code>true</code> if the occurrences of this parameter type are all valid
       */
      public abstract boolean internalValidate(AdviceMethodProperties properties);
      
      /**
       * Resets all advice method properties information that has been set during
       * validation.
       */
      public abstract void resetValidation();
      
      /**
       * Returns the sum of the assignability degrees of every paramater of this type.
       * 
       * @param properties       contains information about the queried advice method
       * @return                 the assignability degree if this parameter type on the
       *                         advice method
       */
      public abstract short getAssignabilityDegree(AdviceMethodProperties properties);
      
      /**
       * Assigns information regarding all occurences of this parameter type on the
       * advice method to <code>args</code>.
       * 
       * @param args array containing information of parameter type occurrences
       */
      public abstract void assignParameterInfo(int[] args);
   }

   /**
    * A parameter type whose annotation can occur only once in an advice method.
    */
   class SingleParameterType extends ParameterAnnotationType
   {
      int index;
      
      public SingleParameterType(ParameterAnnotationRule rule)
      {
         super(rule);
         this.index = -1;
      }
      
      public final void setIndex(int parameterIndex, Annotation annotation)
         throws ParameterAnnotationRuleException
      {
         if (this.index != -1)
         {
            if (AspectManager.verbose)
            {
               throw new ParameterAnnotationRuleException("\n[warn] - found more than "
                     + "one occurence of " + rule.getAnnotation().getName() +
                     " on parameters of advice" + method);  
            }
            throw new ParameterAnnotationRuleException(null);
         }
         this.index = parameterIndex;
         rank += rule.getRankGrade();
      }

      public final boolean isSet()
      {
         return this.index != -1;
      }
      
      public final boolean internalValidate(AdviceMethodProperties properties)
      {
         if (index != -1 && !method.getParameterTypes()[index].isAssignableFrom(
               (Class)rule.getAssignableFrom(properties)))
         {
            if (AspectManager.verbose)
            {
               AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - parameter annotated with ");
               AdviceMethodFactory.adviceMatchingMessage.append(rule.getAnnotation());
               AdviceMethodFactory.adviceMatchingMessage.append(" is not assignable from expected type ");
               AdviceMethodFactory.adviceMatchingMessage.append(rule.getAssignableFrom(properties));   
               AdviceMethodFactory.adviceMatchingMessage.append(" on  method ");
               AdviceMethodFactory.adviceMatchingMessage.append(method);
            }
            return false;
         }
         return  true;
      }

      // this class doesn't set any field during validation
      public final void resetValidation() { }

      public final short getAssignabilityDegree(AdviceMethodProperties properties)
      {
         if (this.index == -1)
         {
            return -1;
         }
         return DEGREE.getAssignabilityDegree(
               method.getParameterTypes()[this.index],
               (Class) rule.getAssignableFrom(properties));
      }
      
      public final void assignParameterInfo(int[] args)
      {
         if (this.index != -1)
         {
            args[index] = rule.getProperty();
         }
      }
   }

   /**
    * A parameter type whose annotation can occur more than once in an advice method.
    */   
   class MultipleParameterType extends ParameterAnnotationType
   {
      private int[][] indexes;
      private int[] originalIndexValues; // for resetting purposes
      private int indexesLength;
      
      // maximum size is the total number of parameters
      public MultipleParameterType(ParameterAnnotationRule rule, int totalParams)
      {
         super(rule);
         this.indexes = new int[totalParams][2];
         this.indexesLength = 0;
         this.originalIndexValues = new int[totalParams];
      }
      
      public final void setIndex(int index, Annotation annotation)
         throws ParameterAnnotationRuleException
      {
         if (indexesLength == indexes.length)
         {
            throw new ParameterAnnotationRuleException("Found more @Arg annotated parameters than the number of parameters available on joinpoint");
         }
         indexes[indexesLength][0] = index;
         originalIndexValues[indexesLength] = ((Arg) annotation).index();
         indexes[indexesLength][1] = originalIndexValues[indexesLength];
         indexesLength ++;
         rank += rule.getRankGrade();
      }
      
      public final boolean isSet()
      {
         return indexesLength > 0;
      }
      
      public final boolean internalValidate(AdviceMethodProperties properties)
      {
         Class<?>[] expectedTypes = (Class<?>[]) rule.getAssignableFrom(properties);
         Class<?>[] adviceTypes = method.getParameterTypes();
         boolean[] taken = new boolean[expectedTypes.length];
         for (int i = 0; i < indexesLength; i++)
         {
            // parameter index is set on @Arg annotation
            if (indexes[i][1] != -1)
            {
               // negative index
               if (indexes[i][1] < 0)
               {
                  if (AspectManager.verbose)
                  {
                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - Negative joinpoint parameter indexes are not allowed.");
                  }
                  return false;
               }
               // wrong index
               if (indexes[i][1] >= expectedTypes.length)
               {
                  if (AspectManager.verbose)
                  {
                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - There is no joinpoint parameter with index ");
                     AdviceMethodFactory.adviceMatchingMessage.append(indexes[i][1]);
                     AdviceMethodFactory.adviceMatchingMessage.append(", since there are ");
                     AdviceMethodFactory.adviceMatchingMessage.append(expectedTypes.length == 0? "no": expectedTypes.length);
                     AdviceMethodFactory.adviceMatchingMessage.append("joinpoint parameters available");
                  }
                  return false;
               }
               // wrong type
               if (!adviceTypes[indexes[i][0]].isAssignableFrom(
                     expectedTypes[indexes[i][1]]))
               {
                  if (AspectManager.verbose)
                  {
                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - Advice parameter ");
                     AdviceMethodFactory.adviceMatchingMessage.append(indexes[i][0]);
                     AdviceMethodFactory.adviceMatchingMessage.append(", of type ");
                     AdviceMethodFactory.adviceMatchingMessage.append(adviceTypes[indexes[i][0]].getName());
                     AdviceMethodFactory.adviceMatchingMessage.append(", cannot be assigned to the value of joinpoint parameter with index ");
                     AdviceMethodFactory.adviceMatchingMessage.append(indexes[i][1]);
                     AdviceMethodFactory.adviceMatchingMessage.append(", whose type is ");
                     AdviceMethodFactory.adviceMatchingMessage.append(expectedTypes[indexes[i][1]].getName());
                  }
                  return false;
               }
               // index set more than once
               if (taken[indexes[i][1]])
               {
                  if (AspectManager.verbose)
                  {
                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - Joinpoint parameter index '");
                     AdviceMethodFactory.adviceMatchingMessage.append(indexes[i][0]);
                     AdviceMethodFactory.adviceMatchingMessage.append("' cannot be assigned to more than one '@Arg' advice parameter");
                  }
                  return false;
               }
               // mark index as set
               taken[indexes[i][1]] = true;
               continue;
            }
         }
         for (int i = 0; i < indexesLength; i++)
         {
            // parameter index is already set
            if (indexes[i][1] != -1)
            {
               continue;
            }
            boolean found = false;
            for (int j = 0; j < expectedTypes.length; j++)
            {
               if (adviceTypes[indexes[i][0]] == expectedTypes[j] && !taken[j])
               {
                  indexes[i][1] = j;
                  taken[j] = true;
                  found = true;
                  break;
               }
            }
            if (!found)
            {
               for (int j = 0; j < expectedTypes.length; j++)
               {
                  if (adviceTypes[indexes[i][0]].isAssignableFrom(expectedTypes[j]) &&
                        !taken[j])
                  {
                     indexes[i][1] = j;
                     taken[j] = true;
                     found = true;
                     break;
                  }
               }
               if (!found)
               {
                  if (AspectManager.verbose)
                  {
                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn] - not found a match for argument ");
                     AdviceMethodFactory.adviceMatchingMessage.append(adviceTypes[indexes[i][0]]);
                     AdviceMethodFactory.adviceMatchingMessage.append(" of ");
                     AdviceMethodFactory.adviceMatchingMessage.append(method);
                     AdviceMethodFactory.adviceMatchingMessage.append("\n[warn]   expected one of types:");
                     for (int j = 0; j < expectedTypes.length; j++)
                     {
                        AdviceMethodFactory.adviceMatchingMessage.append(expectedTypes[j]);
                        AdviceMethodFactory.adviceMatchingMessage.append(" ");
                     }
                  }
                  return false;
               }
            }
         }
         return true;
      }
            
      public void resetValidation()
      {
         for (int i = 0; i < indexesLength; i++)
         {
            indexes[i][1] = originalIndexValues[i];
         }
      }
      
      public short getAssignabilityDegree(AdviceMethodProperties properties)
      {
         if (indexesLength == 0)
         {
            return -1;
         }
         Class[] expectedTypes = (Class[]) rule.getAssignableFrom(properties);
         short level = 0;
         for (int i = 0; i < indexesLength; i++)
         {
            level += DEGREE.getAssignabilityDegree(method.getParameterTypes()[this.indexes[i][0]],
                  expectedTypes[this.indexes[i][1]]);
         }
         return level; 
      }
      
      public void assignParameterInfo(int args[])
      {
         for (int i = 0; i < indexesLength; i++)
         {
            args[this.indexes[i][0]] = this.indexes[i][1];
         }
      }
   }
}