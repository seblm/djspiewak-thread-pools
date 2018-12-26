import Dependencies._

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
    libraryDependencies += "org.typelevel" % "cats-effect_2.12" % "1.1.0"
  )
