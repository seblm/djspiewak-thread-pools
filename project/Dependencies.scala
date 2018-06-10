import sbt._

object Dependencies {

  lazy val `mockito-core` = "org.mockito" % "mockito-core" % "2.18.3" % Test
  lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % Test

}
