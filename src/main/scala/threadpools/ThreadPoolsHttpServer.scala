package threadpools

import java.net.InetSocketAddress
import java.util.concurrent.{ExecutorService, Executors}
import java.util.logging.Logger

import chrono.ChronoManager
import com.sun.net.httpserver.{HttpExchange, HttpServer}

import scala.concurrent.{ExecutionContextExecutorService, Future}
import scala.concurrent.JavaConversions._
import scala.util.Success

object ThreadPoolsHttpServer extends App {
  System.setProperty("java.util.logging.config.file",
                     "src/main/resources/logging.properties")
  val log: Logger = Logger.getLogger("threadpools")
  val backlog: Int = 0
  val server: HttpServer =
    HttpServer.create(new InetSocketAddress(8080), backlog)
  val chronoManager: ChronoManager = new ChronoManager()
  var nonBlockingIOPolling: ExecutorService = Executors.newSingleThreadExecutor()
  server.setExecutor(nonBlockingIOPolling)
  val blockingIOThreadPool: ExecutionContextExecutorService = Executors.newCachedThreadPool()
  server.createContext(
    "/threadpools", { exchange: HttpExchange =>
      chronoManager.start()

      val blockingIOResult = Future {
        chronoManager.start()
        blockIO()
        chronoManager.stop()
      }(blockingIOThreadPool)

      blockingIOResult.andThen {
        case Success(_) =>
          chronoManager.start()
          exchange.sendResponseHeaders(200, 0)
          exchange.close()

          chronoManager.stop()
          chronoManager.generate()
      }(nonBlockingIOPolling)


      chronoManager.stop()
      ()
    }
  )

  private def blockIO(): Unit = {
    Thread.sleep(100)
  }

  server.start()
  log.info("server is started, main thread will stop")
}
