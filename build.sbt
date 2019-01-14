import Dependencies._

enablePlugins(GatlingPlugin)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "name.lemerdy.sebastian",
      scalaVersion := "2.12.8",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "djspiewak-thread-pools",
    projectDependencies += `mockito-core`,
    projectDependencies += scalatest,
    libraryDependencies += "org.typelevel" % "cats-effect_2.12" % "1.1.0",
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime,
    libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.2" % Test,
    libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.0.2" % Test,
  )
