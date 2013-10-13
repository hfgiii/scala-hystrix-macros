scala-hystrix-macros
====================

Scala macros to generate Hystrix instrumentation for Scala/Java methods. 

* The hystrix macro generates the following wrapper code around scala/java methods:

  ```
  def instrumentedMethodN(args:In *):Out =
     new HystrixCommand[Out](HystrixConfigurator("<wrapped class Name>","<wrapped/unwrapped method name>",THREAD) {
        override protected def getFallback:Out =  
           { println("INSIDE FALLBACK!!!!!!!!!!!!!") ; <fallback block> }
           
        override protected def run:Out         =          
           { "println("OMG!!"); uninstrumentedInstance.uninstrumentedMethodN(args)}
           
     }.execute
  ```


* The starting point for this implementation is the [@delegate annotation macro](https://github.com/adamw/scala-macro-aop) by Adam Warski.The differences in *hystrix* macro are
  * For the *delegate* macro, both the delegator and delegated class must extend a common interface/trait. The macro looks at method declarations that implement the base abstract method of the base trait/interface in both classes. The *delegate* macro only generates delegation methods for methods not implemented in the delegator class. The *hystrix* macro generates wrappers for all implemented methods in the delegated class inherited from the base trait. The *hystrix* macro uses the body of any method implemented in the delegator class as a *fallback block* for a method wrapped in an anonymous HystrixCommand  
     
  * Another difference lies in the *addDelegateMethods* method, chiefly in the use of *quasiquotes* in writing the trees for the Hystrix method instrumentation. 
  
* An example of the use of the *hystrix* macro is found in the [Example object](https://github.com/hfgiii/scala-hystrix-macros/blob/master/examples/src/main/scala/com/github/hfgiii/hystrix/ExampleRunner.scala). The following *implicit def* in the [*hystrix* package object](https://github.com/hfgiii/scala-hystrix-macros/blob/master/examples/src/main/scala/com/github/hfgiii/hystrix/package.scala) ,
  
  ```
  implicit def toHystrix(instance:ExampleApi):ExampleApiHystrixImpl       = new ExampleApiHystrixImpl(instance)
  implicit def toHystrix(instance:GoogleApiCall):GoogleApiCallHystrixImpl = new GoogleApiCallHystrixImpl(instance)
 
  
  ``` 
  leads to simple invocation of the *hystix* annotation macro 
  
  ```
   import hystrix._
   
   // Produces a instance of HystrixInstrumentedApi with UnInstrumentedApiImpl methods instrumented with Hystrix 
   // Typing "instrumented" as "HystrixInstrumentedApi" forces the compiler to rely on "toHystrix" to generate
   // the proper type-correct instantiation
   //
  val original :ExampleApiHystrixImpl         = new ExampleApiNoHystrixImpl
  val googleApiCall: GoogleApiCallHystrixImpl = new GoogleApiCallNoHystrixImpl 
  ```
  
  where, the *@hystrix* annotation appears in the declaration of *ExampleApiHystrixImpl* & *GoogleApiCallHystrixImpl*:

  ```
    ExampleApiHystrixImpl(@hystrix wrapped: ExampleApi) extends ExampleApi {
     ...
    }
  ``` 
  and
 
  ```
    class GoogleApiCallHystrixImpl(@hystrix wrapped: GoogleApiCall) extends GoogleApiCall {
     ...
    }
  ``` 

 Though the current *hystrix* macro implementation can provide method fallback functions, which is a significent milestone, the next versions with strive to implement:

  * Access to the *HystrixCommand* instance in the fallback block. This can provide better error logging if a runtime context does not monitor Hystrix Event Streams.
  * Compute *HystrixCommand* Settings during macro expansion not each time each time a instrumented method is invoked. 
  * Filter overloaded methods properly for all method selections/filterings.
  * Handle methods with multi-parameter lists.
  * Use quasiquote extraction in case selection and *val* declaration
  * Handle multiple trait inheritance.
  
  
