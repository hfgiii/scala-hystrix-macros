package com.github.hfgiii.hystrix

 object GoogleApiCallHystrixImpl {

   trait GoogleApiCallFallbackHystrix extends GoogleApiCall with FallbackFunctions {

     def getMountEverestHight(): String            =   "0 meters"

     def getDataFromLongRunningOperation(): String =   "took a lotta seconds - in fallback"
   }

   trait  GoogleApiCallCacheKeyHystrix extends GoogleApiCall with CacheKeyFunctions {

     def getMountEverestHight(): String

     def getDataFromLongRunningOperation(): String
   }

 }

abstract class GoogleApiCallHystrixImpl(@hystrix wrapped: GoogleApiCall) extends GoogleApiCall