import sbt._

object Dependencies {

  lazy val `mockito-core` = "org.mockito" % "mockito-core" % "2.23.4" % Test
  lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % Test

}
