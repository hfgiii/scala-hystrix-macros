package com.github.hfgiii.hystrix


class GoogleApiCallHystrixImpl(@hystrix wrapped: GoogleApiCall) extends GoogleApiCall {

  def getMountEverestHight(): String            =   "0 meters"

  def getDataFromLongRunningOperation(): String =   "took a lotta seconds - in fallback"

}