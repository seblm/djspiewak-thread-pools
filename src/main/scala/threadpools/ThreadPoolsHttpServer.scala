package threadpools

import java.net.InetSocketAddress
import java.util.concurrent.{ExecutorService, Executors}
import java.util.logging.Logger

import chrono.ChronoManager
import com.sun.net.httpserver.{HttpExchange, HttpServer}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.JavaConversions._

object ThreadPoolsHttpServer extends App {

  System.setProperty("java.util.logging.config.file", "src/main/resources/logging.properties")
  val log: Logger = Logger.getLogger("threadpools")
  val backlog: Int = 0
  val server: HttpServer = HttpServer.create(new InetSocketAddress(8080), backlog)
  val chronoManager: ChronoManager = new ChronoManager()
  var nonBlockingIOPolling: ExecutorService = Executors.newSingleThreadExecutor()
  server.setExecutor(nonBlockingIOPolling)
  val blockingIOThreadPool: ExecutionContext = Executors.newCachedThreadPool()
  server.createContext(
    "/threadpools", { exchange: HttpExchange ⇒
      measure(s"↘️") {
        Future {
          measure(s"🚫") {
            blockIO()
          }
        }(blockingIOThreadPool).map { _ ⇒
          measure(s"↗️") {
            exchange.sendResponseHeaders(200, 0)
            exchange.close()
          }
        }(nonBlockingIOPolling)
      }
      ()
    }
  )

  server.createContext(
    "/generate", { exchange: HttpExchange ⇒
      Future {
        chronoManager.generate()
      }(blockingIOThreadPool).map { _ =>
        exchange.sendResponseHeaders(200, 0)
        exchange.close()
      }(nonBlockingIOPolling)
      ()
    }
  )

  private def blockIO(): Unit = Thread.sleep(100)

  private def measure[T](label: String)(f: => T): T = {
    chronoManager.start(label)
    val result = f
    chronoManager.stop()
    result
  }

  server.start()
  log.info("server is started, main thread will stop")

}
