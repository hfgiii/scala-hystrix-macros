package com.github.hfgiii.hystrix

import scala.annotation.StaticAnnotation

import scala.language.experimental.macros

trait FallbackFunctions
trait CacheKeyFunctions

class hystrix extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro hystrixMacro.impl
}
