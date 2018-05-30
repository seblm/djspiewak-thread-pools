package threadpools

import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.logging.Logger

import com.sun.net.httpserver.{HttpExchange, HttpServer}

object ThreadPoolsHttpServer extends App {
  System.setProperty("java.util.logging.config.file", "src/main/resources/logging.properties")
  val log: Logger = Logger.getLogger("threadpools")
  val backlog: Int = 0
  val server: HttpServer = HttpServer.create(new InetSocketAddress(8080), backlog)
  server.createContext(
    "/threadpools",
    { case t: HttpExchange =>
      log.info("receive message")
      log.info(Thread.currentThread().getName)
      Thread.sleep(5000)
      val response: String = "This is the response"
      t.sendResponseHeaders(200, response.length())
      Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
      val os: OutputStream = t.getResponseBody
      os.write(response.getBytes())
      os.close()
      ()
    }
  )
  server.start()
  log.info("server is started, main thread will stop")
}
