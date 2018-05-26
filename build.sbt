lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "name.lemerdy.sebastian",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "djspiewak-thread-pools"
  )
