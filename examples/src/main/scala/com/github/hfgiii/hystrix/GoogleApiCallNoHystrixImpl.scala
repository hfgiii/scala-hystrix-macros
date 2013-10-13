package com.github.hfgiii.hystrix

import akka.actor.ActorSystem
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import scala.concurrent.Await
import scala.concurrent.duration._

class GoogleApiCallNoHystrixImpl extends GoogleApiCall {

  implicit val system = ActorSystem("simple-spray-client")
  import system.dispatcher // execution context for futures

  case class Elevation(location: Location, elevation: Double)
  case class Location(lat: Double, lng: Double)
  case class GoogleApiResult[T](status: String, results: List[T])

  object ElevationJsonProtocol extends DefaultJsonProtocol {
    implicit val locationFormat = jsonFormat2(Location)
    implicit val elevationFormat = jsonFormat2(Elevation)
    implicit def googleApiResultFormat[T :JsonFormat] = jsonFormat2(GoogleApiResult.apply[T])
  }

  override def getMountEverestHight(): String = {

    import ElevationJsonProtocol._
    import SprayJsonSupport._
    val pipeline = sendReceive ~> unmarshal[GoogleApiResult[Elevation]]
    val responseFuture = pipeline {
      Get("http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false")
    }
    val sprayResponse: GoogleApiResult[Elevation] = Await.result(responseFuture, 5 seconds)
    sprayResponse.results.head.elevation + " meters"
  }

  override def getDataFromLongRunningOperation(): String = {
    Thread.sleep(5000)
    "took 5 seconds"
  }
}
