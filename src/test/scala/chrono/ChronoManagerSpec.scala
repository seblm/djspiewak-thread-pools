package chrono

import java.time.{Clock, Instant}

import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class ChronoManagerSpec extends FlatSpec with Matchers with MockitoSugar {

  "ChronoManager" should "not contain any measure" in {
    val chronoManager = given_chrono_manager()

    chronoManager.start()

    chronoManager.measures() should be(empty)
  }

  it should "contain one measure" in {
    val chronoManager = given_chrono_manager()

    chronoManager.start()
    chronoManager.stop()

    chronoManager.measures() should contain only "ScalaTest-run-running-ChronoManagerSpec" → List(
      FinishedMeasure(StartedMeasure(measures(0)), measures(1)))
  }

  it should "contain two measures for the same thread" in {
    val chronoManager = given_chrono_manager()

    chronoManager.start()
    chronoManager.stop()
    chronoManager.start()
    chronoManager.stop()

    chronoManager.measures() should contain only "ScalaTest-run-running-ChronoManagerSpec" → List(
      FinishedMeasure(StartedMeasure(measures(0)), measures(1)),
      FinishedMeasure(StartedMeasure(measures(2)), measures(3)))
  }

  private val measures: List[Instant] = List(
    Instant.parse("2018-06-10T10:15:30.010Z"),
    Instant.parse("2018-06-10T10:15:30.590Z"),
    Instant.parse("2018-06-10T10:15:30.620Z"),
    Instant.parse("2018-06-10T10:15:30.780Z"))

  private def given_chrono_manager(): ChronoManager = {
    val clock: Clock = mock[Clock]
    val chronoManager = new ChronoManager(clock)
    given(clock.instant()).willReturn(measures.head, measures.tail: _*)
    chronoManager
  }
}
