import java.time.Instant

case class ChronoManager(measuresByThread: Map[String, List[FinishedMeasure]], currentMeasures: Map[String, StartedMeasure]) {

  def start(): ChronoManager = {
    val threadName = Thread.currentThread().getName
    ChronoManager(measuresByThread, currentMeasures + (threadName -> StartedMeasure(Instant.now)))
  }

  def stop(): Unit = ???

  def generate(): Unit = ???

}

sealed trait Measure

case class StartedMeasure(start: Instant) extends Measure

case class FinishedMeasure(start: StartedMeasure, endInstant: Instant) extends Measure
