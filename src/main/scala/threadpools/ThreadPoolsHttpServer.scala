package threadpools

import java.net.InetSocketAddress
import java.util.concurrent.Executors.{newCachedThreadPool, newFixedThreadPool, newSingleThreadExecutor}

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import domain.Fibonacci
import infrastructure.log.Log
import infrastructure.web.PerformanceResults

import scala.concurrent.JavaConversions._
import scala.concurrent.Future
import scala.util.Random

object ThreadPoolsHttpServer extends App with Log with PerformanceResults with Fibonacci {

  val defaultBacklog = 0
  val server = HttpServer.create(new InetSocketAddress(8080), defaultBacklog)
  installPerformanceResultsTo(server)

  server.setExecutor(newSingleThreadExecutor())
  val nonBlockingIOPolling = server.getExecutor
  val blockingIOThreadPool = newCachedThreadPool()
  val cpuBoundThreadPool = newFixedThreadPool(2)

  server.createContext("/", (exchange: HttpExchange) ⇒ {
    measure("↘️") {
      Future {
        measure(s"🚫 ${exchange.getRequestURI.getQuery}") {
          Thread.sleep(Random.nextInt(40) + 80)
        }
      }(blockingIOThreadPool).map { _ ⇒
        measure(s"🔥 ${exchange.getRequestURI.getQuery}") {
          fibonacci(Random.nextInt(1) + 37)
        }
      }(cpuBoundThreadPool).onComplete { _ ⇒
        measure("↗️") {
          exchange.sendResponseHeaders(200, 0)
          exchange.close()
        }
      }(nonBlockingIOPolling)
    }
  })

  server.start()
  log.info("server is started, main thread will stop")

}
