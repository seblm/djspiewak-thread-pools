package chrono

import java.lang.Thread.currentThread
import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, ZoneId}

import scala.collection.mutable
import scala.io.Source

class ChronoManager(private val clock: Clock = Clock.systemDefaultZone()) {

  private val measuresByThread: mutable.SortedMap[String, List[FinishedMeasure]] = mutable.TreeMap.empty

  private val currentMeasures: mutable.Map[String, StartedMeasure] = mutable.Map.empty

  def start(label: String): Unit = currentMeasures.update(currentThread().getName, StartedMeasure(label, clock))

  def stop(): Unit = {
    val threadName = currentThread().getName
    currentMeasures.get(threadName).foreach { startedMeasure ⇒
      val finishedMeasures = measuresByThread.getOrElse(threadName, List.empty) :+ FinishedMeasure(startedMeasure, clock)
      measuresByThread.update(threadName, finishedMeasures)
      currentMeasures.remove(threadName)
    }
  }

  def measures(): Map[String, List[FinishedMeasure]] = measuresByThread.toMap

  def generate(): Unit = {
    val threads = measuresByThread.keySet.toSet
    val paris = ZoneId.of("Europe/Paris")
    val toIso = DateTimeFormatter.ofPattern("'Date.UTC('y, M, d, H, m, s, SSS)")
    val data = threads.zipWithIndex.map { case (thread, index) ⇒
      measuresByThread(thread).map { measure ⇒
        s"                    {x: ${measure.start.start.atZone(paris).format(toIso)}, x2: ${measure.end.atZone(paris).format(toIso)}, y: $index, label: '${measure.start.label}'}"
      }
        .mkString(",\n")
    }.mkString("[\n", ",\n", "\n                ]")
    val source = Source.fromResource("template.html").mkString
      .replaceAll("""\$\{threads\}""", threads.map(renameThreadPools).mkString("['", "', '", "']"))
      .replaceAll("""\$\{data\}""", data)
    Files.write(Paths.get("src", "main", "webapp", "index.html"), source.getBytes)
    measuresByThread.clear
    currentMeasures.clear
  }

  private def renameThreadPools(name: String): String = {
    val poolMatcher = """pool-2-thread-(\d+)""".r
    name match {
      case "pool-1-thread-1" ⇒ "Non-blocking IO polling"
      case poolMatcher(threadNumber) ⇒ s"Blocking IO $threadNumber"
      case threadName ⇒ threadName
    }
  }

}

sealed trait Measure

case class StartedMeasure(label: String, start: Instant) extends Measure

object StartedMeasure {

  def apply(label: String, clock: Clock): StartedMeasure = StartedMeasure(label, clock.instant())

}

case class FinishedMeasure(start: StartedMeasure, end: Instant) extends Measure

object FinishedMeasure {

  def apply(start: StartedMeasure, clock: Clock): FinishedMeasure = FinishedMeasure(start, clock.instant())

}
