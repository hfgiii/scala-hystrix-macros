import sbt._
import sbt.Keys._


object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.github.hfgiii.scala-hystrix-macros",
    version := "1.0.0",
    scalacOptions ++= Seq(),
    scalaVersion := "2.10.3",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += "spray" at "http://repo.spray.io/",
    resolvers += Resolver.url("maven-remote", url("http://mvnrepository.com/")),
    addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)
  )
}

object MyBuild extends Build {
  import BuildSettings._

  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in examples,
      libraryDependencies += "com.google.code.findbugs" % "jsr305" % "2.0.1"
    )
  ) aggregate(macros, examples)

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies += "com.typesafe" % "config" % "1.0.2",
      libraryDependencies += "com.netflix.hystrix" % "hystrix-core" % "1.2.16")
  )

  lazy val examples: Project = Project(
    "examples",
    file("examples"),
    settings = buildSettings  ++ Seq(
      libraryDependencies += "com.netflix.hystrix" % "hystrix-core" % "1.2.16",
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1",
      libraryDependencies += "io.spray" % "spray-client" % "1.2-M8",
      libraryDependencies += "io.spray" %% "spray-json" % "1.2.3")
  ) dependsOn(macros)
}
