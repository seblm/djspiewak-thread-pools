package threadpools

import java.net.InetSocketAddress
import java.util.concurrent.Executors.{newCachedThreadPool, newFixedThreadPool, newSingleThreadExecutor}

import cats.effect.IO
import com.sun.net.httpserver.{HttpExchange, HttpServer}
import domain.Fibonacci
import infrastructure.log.Log

import scala.concurrent.JavaConversions._
import scala.util.Random

object ThreadPoolsHttpServer extends App with Log with Fibonacci {

  val defaultBacklog = 0
  val server = HttpServer.create(new InetSocketAddress(8080), defaultBacklog)

  server.setExecutor(newSingleThreadExecutor())
  val nonBlockingIOPolling = server.getExecutor
  val blockingIOThreadPool = newCachedThreadPool()
  val cpuBoundThreadPool = newFixedThreadPool(Runtime.getRuntime.availableProcessors())

  server.createContext("/", { exchange: HttpExchange ⇒
    val program = for {
      _ <- IO.shift(blockingIOThreadPool)
      _ <- IO(Thread.sleep(Random.nextInt(40) + 80))
      _ <- IO.shift(cpuBoundThreadPool)
      _ <- IO(fibonacci(Random.nextInt(1) + 37))
      _ <- IO.shift(nonBlockingIOPolling)
      _ <- IO {
        exchange.sendResponseHeaders(200, 0)
        exchange.close()
      }
    } yield ()

    program.unsafeRunAsync(_ ⇒ ())
  })

  server.start()
  log.info("server is started, main thread will stop")

}
