package threadpools

import java.lang.Thread.currentThread
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.logging.Logger

import chrono.ChronoManager
import com.sun.net.httpserver.{HttpExchange, HttpServer}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
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
  val blockingIOThreadPool: ExecutionContextExecutorService = Executors.newCachedThreadPool()
  server.createContext(
    "/threadpools", { exchange: HttpExchange =>
      val threadName = currentThread().getName
      chronoManager.start()

      val blockingIOResult = Future.apply {
        chronoManager.start()
        blockIO()
        chronoManager.stop()
      }(blockingIOThreadPool)

      blockingIOResult.andThen{
        case Success(_) =>
          chronoManager.start(threadName)
          exchange.sendResponseHeaders(200, 0)
          exchange.close()

          println("coucou")
          chronoManager.stop(threadName)
          chronoManager.generate()
      }(blockingIOThreadPool) // TODO A changer


      chronoManager.stop()
      ()
    }
  )

  private def blockIO(): Unit = {
    Thread.sleep(5000)
  }

  server.start()
  log.info("server is started, main thread will stop")
}
