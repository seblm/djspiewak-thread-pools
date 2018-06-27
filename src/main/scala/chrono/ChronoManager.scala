package chrono

import java.lang.Thread.currentThread
import java.time.{Clock, Instant}

import scala.collection.{SortedMap, mutable}

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

  def clear(): SortedMap[String, List[FinishedMeasure]] = {
    val measures = SortedMap(measuresByThread.toSeq: _*)
    measuresByThread.clear
    currentMeasures.clear
    measures
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
