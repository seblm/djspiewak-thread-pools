package performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class LoadTest extends Simulation {

  private val httpProtocol = http.baseUrl("http://localhost:8080")
  private val scn = scenario("loadtest").repeat(8, "i") {
    exec(http("request ${i}").get("/?${i}"))
      .pause(session ⇒ (30 + session("i").as[Int]).milliseconds)
  }

  setUp(scn.inject(rampUsers(100).during(10.seconds))).protocols(httpProtocol)

}
