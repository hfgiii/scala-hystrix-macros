package com.github.hfgiii.hystrix

class ExampleApiHystrixImpl(@hystrix wrapped: ExampleApi) extends ExampleApi {
  def method1() = "hello world from method1's fallback"
  def method2(p1: String) = 41L
}