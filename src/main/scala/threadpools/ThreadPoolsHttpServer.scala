package threadpools

import java.net.InetSocketAddress
import java.util.concurrent.{ExecutorService, Executors}

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import infrastructure.log.Log
import infrastructure.web.PerformanceResults

import scala.concurrent.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object ThreadPoolsHttpServer extends App with PerformanceResults with Log {

  val backlog: Int = 0
  val server: HttpServer = HttpServer.create(new InetSocketAddress(8080), backlog)
  installPerformanceResultsTo(server)

  var nonBlockingIOPolling: ExecutorService = Executors.newSingleThreadExecutor() // pool-1-thread-1
  server.setExecutor(nonBlockingIOPolling)
  val blockingIOThreadPool: ExecutionContext = Executors.newCachedThreadPool() // pool-2-thread-*

  server.createContext(
    "/threadpools", { exchange: HttpExchange ⇒
      measure("↘️") {
        Future {
          measure("🚫") {
            Thread.sleep(Random.nextInt(40) + 80)
          }
        }(blockingIOThreadPool).map { _ ⇒
          measure("↗️") {
            exchange.sendResponseHeaders(200, 0)
            exchange.close()
          }
        }(nonBlockingIOPolling)
      }
      ()
    }
  )

  server.start()
  log.info("server is started, main thread will stop")

}
