package com.github.hfgiii.hystrix

trait GoogleApiCall {
  def getMountEverestHight(): String
  def getDataFromLongRunningOperation(): String
}