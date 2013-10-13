package com.github.hfgiii.hystrix

object  Example extends App {

  def invokeExampleMethods(foo: com.github.hfgiii.hystrix.ExampleApi) {
    println("Method 1 result: " + foo.method1())
    println("Method 2 result: " + foo.method2("x"))
    println("Method 3 result: " + foo.method3("y"))
    println("Method 4 result: " + foo.method4("z", 10L))
  }

  def invokeGoogleApiMethods(gac: GoogleApiCall) {
    println("height of Mount Everest: " + gac.getMountEverestHight)
    println("hystrix should use fallback: " + gac.getDataFromLongRunningOperation)
  }

  val original :ExampleApiHystrixImpl         = new ExampleApiNoHystrixImpl
  val googleApiCall: GoogleApiCallHystrixImpl = new GoogleApiCallNoHystrixImpl

  invokeExampleMethods(original)

  invokeGoogleApiMethods(googleApiCall)

}