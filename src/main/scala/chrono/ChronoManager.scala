package chrono

import java.lang.Thread.currentThread
import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import scala.io.Source

case class ChronoManager(measuresByThread: Map[String, List[FinishedMeasure]] = Map.empty,
                         currentMeasures: Map[String, StartedMeasure] = Map.empty) {

  def start(threadName: String = currentThread().getName): ChronoManager =
    ChronoManager(measuresByThread, currentMeasures.updated(threadName, StartedMeasure()))

  def stop(threadName: String = currentThread().getName): ChronoManager = {
    currentMeasures.get(threadName).map { startedMeasure ⇒
      val finishedMeasures = measuresByThread.getOrElse(threadName, List.empty) :+ FinishedMeasure(startedMeasure)
      ChronoManager(measuresByThread.updated(threadName, finishedMeasures), currentMeasures)
    }.getOrElse(this)
  }

  def generate(): Unit = {
    val threads = measuresByThread.keySet
    val paris = ZoneId.of("Europe/Paris")
    val toIso = DateTimeFormatter.ofPattern("'Date.UTC('y, M, d, H, m, s, SSS)")
    val data = threads.zipWithIndex.map { case (thread, index) ⇒
      measuresByThread(thread).map { measure ⇒
        s"{x: ${measure.start.start.atZone(paris).format(toIso)}, x2: ${measure.end.atZone(paris).format(toIso)}, y: $index}"
      }
        .mkString(", ")
    }.mkString("[", ", ", "]")
    val source = Source.fromResource("template.html").mkString
      .replaceAll("""\$\{threads\}""", threads.mkString("['", "', '", "']"))
      .replaceAll("""\$\{data\}""", data)
    Files.write(Paths.get("src", "main", "webapp", "index.html"), source.getBytes)
  }

}

sealed trait Measure

case class StartedMeasure(start: Instant = Instant.now) extends Measure

case class FinishedMeasure(start: StartedMeasure, end: Instant = Instant.now) extends Measure
