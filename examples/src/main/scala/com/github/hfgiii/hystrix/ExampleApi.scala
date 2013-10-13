package com.github.hfgiii.hystrix

trait ExampleApi {
  def method1(): String
  def method2(p1: String): Long
  def method3(p1: String): Int
  def method4(p1: String, p2: Long): String
}
