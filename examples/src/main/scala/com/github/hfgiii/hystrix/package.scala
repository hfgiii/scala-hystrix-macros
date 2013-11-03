package com.github.hfgiii

import com.typesafe.config.{ConfigFactory, Config}
import com.netflix.hystrix.{HystrixThreadPoolProperties, HystrixCommandKey, HystrixCommandProperties}
import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.HystrixCommandGroupKey.Factory._

package object hystrix {

  implicit def toHystrix(instance:ExampleApi):ExampleApiHystrixImpl       = new ExampleApiHystrixImpl(instance)  {}
  implicit def toHystrix(instance:GoogleApiCall):GoogleApiCallHystrixImpl = new GoogleApiCallHystrixImpl(instance) {}

  private implicit class HystrixConfig(config:Config) {
    def command   (key:String):String = config.getString("client_api.hystrix.command." + key)
    def threadPool(key:String):String = config.getString("client_api.hystrix.thread_pool." + key)
  }

  implicit def strToBool(tf:String):Boolean =
    tf.toLowerCase match {
      case "true" => true
      case  _       => false
    }

  implicit def strToInt(istr:String):Int = Integer.parseInt(istr)

  private lazy val cfg:Config = ConfigFactory.load

  object HystrixConfigurator {
    def apply(group:String,command:String,isolationStrategy:HystrixCommandProperties.ExecutionIsolationStrategy):Setter =
      Setter.
        withGroupKey(asKey(group)).
        andCommandKey(HystrixCommandKey.Factory.asKey(command)).
        andCommandPropertiesDefaults(HystrixCommandProperties.
        Setter().
        withExecutionIsolationStrategy(isolationStrategy).
        withCircuitBreakerEnabled(cfg.command("circuit_breaker")).
        withExecutionIsolationThreadTimeoutInMilliseconds(cfg.command("thread_timeout")).
        withFallbackEnabled(cfg.command("fallback"))).
        andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.
        Setter().
        withCoreSize(cfg.threadPool("core_size")).
        withMaxQueueSize(cfg.threadPool("max_queue_size")).
        withQueueSizeRejectionThreshold(cfg.threadPool("rejection_queue_size")))
  }

}
