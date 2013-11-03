package com.github.hfgiii.hystrix

object ExampleApiHystrixImpl {

    trait ExampleApiFallbackHystrix extends ExampleApi  with FallbackFunctions {
      def method1() = "hello world from method1's fallback"
      def method2(p1: String) = 41L

    }

    trait ExampleApiCacheKeyHystrix extends ExampleApi  with CacheKeyFunctions {
      def method1():String
      def method2(p1: String):Long
  }
}


abstract class ExampleApiHystrixImpl(@hystrix wrapped: ExampleApi) extends ExampleApi