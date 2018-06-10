package chrono

import java.lang.Thread.currentThread
import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneId}

import scala.collection.mutable
import scala.io.Source

class ChronoManager(private val clock: Clock = Clock.systemDefaultZone()) {

  private val measuresByThread: mutable.Map[String, List[FinishedMeasure]] = mutable.Map.empty

  private val currentMeasures: mutable.Map[String, StartedMeasure] = mutable.Map.empty

  def start(threadName: String = currentThread().getName): Unit = currentMeasures.update(threadName, StartedMeasure(clock))

  def stop(threadName: String = currentThread().getName): Unit =
    currentMeasures.get(threadName).foreach { startedMeasure ⇒
      val finishedMeasures = measuresByThread.getOrElse(threadName, List.empty) :+ FinishedMeasure(startedMeasure, clock)
      measuresByThread.update(threadName, finishedMeasures)
      currentMeasures.remove(threadName)
    }

  def measures(): Map[String, List[FinishedMeasure]] = measuresByThread.toMap

  def generate(): Unit = {
    val threads = measuresByThread.keySet
    val paris = ZoneId.of("Europe/Paris")
    val toIso = DateTimeFormatter.ofPattern("'Date.UTC('y, M, d, H, m, s, SSS)")
    val data = threads.zipWithIndex.map { case (thread, index) ⇒
      measuresByThread(thread).map { measure ⇒
        s"                    {x: ${measure.start.start.atZone(paris).format(toIso)}, x2: ${measure.end.atZone(paris).format(toIso)}, y: $index}"
      }
        .mkString(",\n")
    }.mkString("[\n", ",\n", "\n                ]")
    val source = Source.fromResource("template.html").mkString
      .replaceAll("""\$\{threads\}""", threads.mkString("['", "', '", "']"))
      .replaceAll("""\$\{data\}""", data)
    Files.write(Paths.get("src", "main", "webapp", "index.html"), source.getBytes)
  }

}

sealed trait Measure

case class StartedMeasure(start: Instant) extends Measure

object StartedMeasure {

  def apply(clock: Clock): StartedMeasure = StartedMeasure(clock.instant())

}

case class FinishedMeasure(start: StartedMeasure, end: Instant) extends Measure

object FinishedMeasure {

  def apply(start: StartedMeasure, clock: Clock): FinishedMeasure = FinishedMeasure(start, clock.instant())

}
