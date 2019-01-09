package infrastructure.web

import java.nio.file.{Files, Paths}
import java.time.{Duration, ZoneId}
import java.time.format.DateTimeFormatter

import chrono.{ChronoManager, FinishedMeasure}
import com.sun.net.httpserver.{HttpExchange, HttpServer}

import scala.io.Source

trait PerformanceResults {

  val chronoManager: ChronoManager = new ChronoManager()

  def measure[T](label: String)(f: ⇒ T): T = {
    chronoManager.start(label)
    val result = f
    chronoManager.stop()
    result
  }

  def installPerformanceResultsTo(server: HttpServer): Unit = server.createContext(
    "/generate", { exchange: HttpExchange ⇒
      generate()
      exchange.sendResponseHeaders(200, 0)
      exchange.close()
    }
  )

  private def generate(): Unit = {
    val measuresByThread = chronoManager.clear()
    val total = computeTotalElapsedDuration(measuresByThread.values.flatten.toList)
    val threads = measuresByThread.keySet
    val data = threads.zipWithIndex.map { case (thread, index) ⇒
      measuresByThread(thread).map(measureToJson(index)).mkString(",\n")
    }.mkString("[\n", ",\n", "\n                ]")
    val source = Source.fromResource("template.html").mkString
      .replaceAll("""\$\{total\}""", total.getOrElse(0).toString)
      .replaceAll("""\$\{threads\}""", threads.toList.map(renameThreadPools).mkString("['", "', '", "']"))
      .replaceAll("""\$\{data\}""", data)
    Files.write(Paths.get("src", "main", "webapp", "index.html"), source.getBytes)
  }

  private def computeTotalElapsedDuration(measures: List[FinishedMeasure]): Option[Long] = for {
    start ← measures.map(_.start.start).sorted.headOption
    end ← measures.map(_.end).sorted.lastOption
  } yield {
    Duration.between(start, end).toMillis
  }

  private def measureToJson(index: Int)(measure: FinishedMeasure): String = {
    val x = measure.start.start.atZone(paris).format(toIso)
    val x2 = measure.end.atZone(paris).format(toIso)
    val label = measure.start.label
    s"                    {x: $x, x2: $x2, y: $index, label: '$label'}"
  }

  private def renameThreadPools(name: String): String = name match {
    case "Thread-2" ⇒ "Default HttpServer thread"
    case poolMatcher("1", _) ⇒ "Non-blocking IO polling"
    case poolMatcher("2", threadNumber) ⇒ s"Blocking IO $threadNumber"
    case poolMatcher("3", threadNumber) ⇒ s"CPU Bound $threadNumber"
    case threadName ⇒ threadName
  }

  private val paris = ZoneId.of("Europe/Paris")
  private val toIso = DateTimeFormatter.ofPattern("'Date.UTC('y, M, d, H, m, s, SSS)")
  private val poolMatcher = """pool-(\d+)-thread-(\d+)""".r

}
