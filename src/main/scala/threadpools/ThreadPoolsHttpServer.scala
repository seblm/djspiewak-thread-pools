package threadpools

import java.lang.Runtime.getRuntime
import java.net.InetSocketAddress
import java.util.concurrent.Executors.{newCachedThreadPool, newFixedThreadPool, newSingleThreadExecutor}

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import domain.Fibonacci
import infrastructure.log.Log
import infrastructure.web.PerformanceResults

import scala.concurrent.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object ThreadPoolsHttpServer extends App with Log with PerformanceResults with Fibonacci {

  val backlog: Int = 0
  val server: HttpServer = HttpServer.create(new InetSocketAddress(8080), backlog)
  installPerformanceResultsTo(server)

  server.setExecutor(newSingleThreadExecutor())
  val nonBlockingIOPolling: ExecutionContext = server.getExecutor() // pool-1-thread-1
  val blockingIOThreadPool: ExecutionContext = newCachedThreadPool() // pool-2-thread-*
  val cpuBoundThreadPool: ExecutionContext = newFixedThreadPool(getRuntime.availableProcessors()) // pool-3-thread-*

  server.createContext("/", (exchange: HttpExchange) ⇒ {
    measure("↘️") {
      Future {
        measure("🚫") {
          Thread.sleep(Random.nextInt(40) + 80)
        }
      }(blockingIOThreadPool).map { _ ⇒
        measure("🔥") {
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
