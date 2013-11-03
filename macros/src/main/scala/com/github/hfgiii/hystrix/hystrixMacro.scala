package com.github.hfgiii.hystrix

import scala.reflect.macros.Context
import scala.language.experimental.macros

object hystrixMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def reportInvalidAnnotationTarget() {
      c.error(c.enclosingPosition, "This annotation can only be used on vals")
    }

    // From MacWire ...
    def typeCheckExpressionOfType(typeTree: Tree): Type = {
      val someValueOfTypeString = reify {
        def x[T](): T = throw new Exception
        x[String]()
      }

      val Expr(Block(stats, Apply(TypeApply(someValueFun, _), someTypeArgs))) = someValueOfTypeString

      val someValueOfGivenType = Block(stats, Apply(TypeApply(someValueFun, List(typeTree)), someTypeArgs))
      val someValueOfGivenTypeChecked = c.typeCheck(someValueOfGivenType)
      someValueOfGivenTypeChecked.tpe
    }

    def computeType(tpt: Tree): Type = {
      if (tpt.tpe != null) {
        tpt.tpe
      } else {
        val calculatedType = c.typeCheck(tpt.duplicate, silent = true, withMacrosDisabled = true).tpe
        val result = if (tpt.tpe == null) calculatedType else tpt.tpe

        if (result == NoType) {
          typeCheckExpressionOfType(tpt)
        } else {
          result
        }
      }
    }

    // ... until here
    def falbackValue(retType:Type)  = {
      val strName = retType.typeSymbol.name.decoded
      if (strName == "Int")   q"""0"""
      else if (strName == "String") q""" "Huh?"  """
      else  q"""null.asInstanceOf[$retType]"""
    }

    def addDelegateMethods(valDef: ValDef, addToClass: ClassDef , companionObject: ModuleDef) = {
      def allMethodsInDelegate = computeType(valDef.tpt).declarations
      val ModuleDef(_,_, Template(_,_,companionBody)) = companionObject
      val ClassDef(mods, name, tparams, Template(parents, self, body)) = addToClass

      /*** The FallbackFunctions and CacheKeyFunctions trait definition ***/
      def hystrixTraitBody(parentName:String):List[Tree] =
        companionBody.flatMap {
          case ClassDef(_,_,_,Template(parents,_,body)) if parents.exists {
            case Ident(name) => parentName == name.decoded
          } => body

          case _ => List[Tree]()
        }

      // TODO  - better filtering - allow overriding  for all method selections
      // TODO - use quasiquote extraction in case expressions
      val existingMethods =
        body.flatMap {
          case DefDef(_, n, _, _, _, _) => Some(n)    //case q"""def $n[${_}](...${_}):${_} = ${_}""" => Some(n)
          case _                        => None
        }.toSet

      def isAnExistingMethod(n:Name):Boolean = existingMethods.contains(n)
      def bodyMethodFilter(defdef:Name => Boolean):List[Tree]  =
        body.filter {
          case    DefDef(_,n, _, _, _, _) =>  defdef(n)
          case    _                       =>   false
        }

      /**    Find and save the class PARAMACCESSOR declaration.
        **   Compilation will fail otherwise. It must be re-inserted into the generated wrapper class
        **/
      val PARAMACCESSOR = scala.reflect.internal.Flags.PARAMACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
      val paramaccessor = body.filter {
        case ValDef(mods,_,_,_)  => mods.hasFlag(PARAMACCESSOR )
        case _                   => false
      }

      /***  Remove methods  from wrapper class inherited from wrapped class - except for helper methods and definitely not the constructor (<init>) ***/
      val newbod   = bodyMethodFilter(n => !isAnExistingMethod (n) || n.decoded == "<init>")

      /**  Gather up the fallback methods - methods declared in wrapper class with the same signatures as the wrapped trait **/
      val fallbacks  = hystrixTraitBody("FallbackFunctions")
      val cacheKeys  = hystrixTraitBody("CacheKeyFunctions")

      /**  Try and find a fallback method body if declared. If not return a constant value depending on return type  **/
      def getFallback(name:Name,returnType:Type) = {
        val fallbackCandidate =
          fallbacks.filter(defdef => {
            val  DefDef(_,n, _, _, rt, _) = defdef
            //TODO - check the method parameter types to check for overloaded methods
            n == name  //&& rt.tpe == returnType
          })

        if(fallbackCandidate.size == 1)  {
          val   DefDef(_,_, _, _, _, rhs) =  fallbackCandidate.head
          rhs
        }
        else
          falbackValue(returnType)
      }

      def getCacheKey(name:Name,returnType:Type) = {
        val cacheKeyCandidate =
          cacheKeys.filter(defdef => {
            val  DefDef(_,n, _, _, rt, _) = defdef
            //TODO - check the method parameter types to check for overloaded methods
            n == name  //&& rt.tpe == returnType
          })

        if(cacheKeyCandidate.size == 1)  {
          val   DefDef(_,n, _, _, _, _) =  cacheKeyCandidate.head
          n.decoded + s"_${returnType.typeSymbol.name.decodedName}"
        }
        else
          null
      }

      val methodsToAdd = allMethodsInDelegate

      val newMethods = for {
        methodToInstrument <- methodsToAdd
      } yield {
        val methodSymbol = methodToInstrument.asMethod
        val vparamss = methodSymbol.paramss.map(_.map {
          paramSymbol =>
            q"""val ${paramSymbol.name.toTermName}:${paramSymbol.typeSignature} """
        })
        /****
             def wrappedMethodN(args:In *):Out =
                new HystrixCommand[Out](SettingsInstance) {
                    override protected def getFallback:Out = fallback() //() => Out
                    override protected def run:Out               = unwrappedInstance.unwrappedMethodN(args)
             }.queue.get
          ***/

        val himport          = q"""import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy._ """
        val args             =  methodSymbol.paramss.flatMap(_.map(param => Ident(param.name)))
        val methodInvocation = q"""println("OMG!!"); ${valDef.name}.${methodSymbol.name}(..$args)"""
        val returnType       = methodSymbol.returnType
        val group            = name.decoded
        val mName            = methodSymbol.name
        val command          = methodSymbol.name.decoded
        val hystrix          = q"""$himport; new com.netflix.hystrix.HystrixCommand[$returnType](HystrixConfigurator($group,$command,THREAD)) {
                                                             hystrix =>
                                                             com.netflix.hystrix.strategy.concurrency.HystrixRequestContext.initializeContext
                                                             override protected def getFallback():$returnType = {println("INSIDE FALLBACK!!!!!!!!!!!!!") ; ${getFallback(mName,returnType)}}
                                                             override protected def run():$returnType = $methodInvocation
                                                             override protected def getCacheKey():String = ${getCacheKey(mName,returnType)}
                                                             }.execute"""
        // TODO - multi params list
        val newret = q"""def ${methodSymbol.name.toTermName} (...$vparamss):$returnType = $hystrix """

        //     println(newret)
        newret
      }

      val classdef =  ClassDef(mods, name, tparams, Template(parents, self,  paramaccessor ++ newbod ++ newMethods))

      println(classdef)

      classdef
    }

    val inputs = annottees.map(_.tree).toList
    val (_, expandees) = inputs match {
      case (param: ValDef) :: (enclosing: ClassDef) :: (companionObject:List[ModuleDef]) => {
        val newEnclosing = addDelegateMethods(param, enclosing , companionObject.head)

        (param, newEnclosing :: companionObject)
      }
      case (param: TypeDef) :: (rest @ (_ :: _)) => reportInvalidAnnotationTarget(); (param, rest)
      case _ => reportInvalidAnnotationTarget(); (EmptyTree, inputs)
    }
    val outputs = expandees
    c.Expr[Any](Block(outputs, Literal(Constant(()))))
  }


}