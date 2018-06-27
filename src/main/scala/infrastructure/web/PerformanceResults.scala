package infrastructure.web

import java.nio.file.{Files, Paths}
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import chrono.ChronoManager
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
      ()
    }
  )

  private def generate(): Unit = {
    val measuresByThread = chronoManager.clear()
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
