[![Build Status](https://travis-ci.org/seblm/djspiewak-thread-pools.svg?branch=master)](https://travis-ci.org/seblm/djspiewak-thread-pools)

Source code to understand [Thread Pools](https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c).

![thread pools](https://typelevel.org/cats-effect/img/concurrency-thread-pools.png)

From [cats-effect documentation](https://typelevel.org/cats-effect/concurrency/basics.html#choosing-thread-pool)

 1. (Seb) Start with a basic HTTPServer (default HTTP server thread):
 
    ```scala
    import java.net.InetSocketAddress

    import com.sun.net.httpserver.HttpServer
    import infrastructure.log.Log
    
    object ThreadPoolsHttpServer extends App with Log {

      val defaultBacklog = 0
      val server = HttpServer.create(new InetSocketAddress(8080), defaultBacklog)
    
      server.start()
      log.info("server is started, main thread will stop")

    }
    ```

    Then invoke the server

    ```bash
    $ curl --silent --include localhost:8080 | head -n 1
    HTTP/1.1 404 Not Found
    ```

 2. (Martin) Add a default route:

    ```scala
    import com.sun.net.httpserver.HttpExchange
    
    server.createContext("/", (exchange: HttpExchange) ⇒ {
      exchange.sendResponseHeaders(200, 0)
      exchange.close()
    }
    ```

    Then invoke the server
    ```bash
    $ curl --silent --include localhost:8080 | head -n 1
    HTTP/1.1 200 OK
    ```

 3. (Seb) Measure the thing with some blocking IO:

    ```scala
    import com.sun.net.httpserver.{HttpExchange, HttpServer}
    import infrastructure.log.Log
    import infrastructure.web.PerformanceResults

    import scala.util.Random
    
    object ThreadPoolsHttpServer extends App with Log with PerformanceResults {

      // ...
      installPerformanceResultsTo(server)

      server.createContext("/", (exchange: HttpExchange) ⇒ {
        measure(s"handle ${exchange.getRequestURI.getQuery}") {
          Thread.sleep(Random.nextInt(40) + 80)
          exchange.sendResponseHeaders(200, 0)
          exchange.close()
        }
      }

      // ...
    }
    ```

    Then invoke the server

    ```bash
    src/scripts/test.sh
    ```

    Start web server

    ```bash
    cd src/main/webapp
    python -m SimpleHTTPServer
    ```

    And go to [performance results web page](http://localhost:8000).

    We can see that requests are stacked, performing blocking io and cpu bounded tasks on non blocking io polling is
    bad.

 4. (Martin) Add computation and measure all the thing:

    ```scala
    import com.sun.net.httpserver.HttpExchange
    import domain.Fibonacci
    import infrastructure.log.Log
    import infrastructure.web.PerformanceResults

    import scala.util.Random

    object ThreadPoolsHttpServer extends App with Log with PerformanceResults with Fibonacci {

      // ...

      server.createContext("/", (exchange: HttpExchange) ⇒ {
        measure(s"🚫 ${exchange.getRequestURI.getQuery}") {
          Thread.sleep(Random.nextInt(40) + 80)
        }
        measure(s"🔥 ${exchange.getRequestURI.getQuery}") {
          fibonacci(Random.nextInt(1) + 37)
        }
        measure(s"↗️") {
          exchange.sendResponseHeaders(200, 0)
          exchange.close()
        }
      }

      // ...
    }
    ```

    ```bash
    src/scripts/test.sh
    ```


 5. (Seb) Creates Thread pools and dispatch:

    ```scala
    import java.util.concurrent.Executors.{newCachedThreadPool, newFixedThreadPool, newSingleThreadExecutor}
    
    import com.sun.net.httpserver.HttpExchange
    
    import scala.concurrent.JavaConversions._
    import scala.util.Random

    // ...

    server.setExecutor(newSingleThreadExecutor())
    val nonBlockingIOPolling = server.getExecutor()  // pool-1-thread-1
    val blockingIOThreadPool = newCachedThreadPool() // pool-2-thread-*
    val cpuBoundThreadPool = newFixedThreadPool(4)   // pool-3-thread-*

    // ...

    server.createContext("/", (exchange: HttpExchange) ⇒ {
      measure("↘️") {
        Future {
          measure(s"🚫 ${exchange.getRequestURI.getQuery}") {
            Thread.sleep(Random.nextInt(40) + 80)
          }
        }(blockingIOThreadPool).map { _ ⇒
          measure(s"🔥 ${exchange.getRequestURI.getQuery}") {
            fibonacci(Random.nextInt(1) + 39)
          }
        }(cpuBoundThreadPool).onComplete { _ ⇒
          measure("↗️") {
            exchange.sendResponseHeaders(200, 0)
            exchange.close()
          }
        }(nonBlockingIOPolling)
      }
    }
    ```

 6. (Martin) cats-effect

    ```scala
    import cats.effect.IO
    import scala.util.Try

    // ...

    server.createContext("/", (exchange: HttpExchange) ⇒ {
      val program = for {
        _ <- IO.shift(blockingIOThreadPool)
        _ <- IO {
          measure(s"🚫 ${exchange.getRequestURI.getQuery}") {
            Thread.sleep(Random.nextInt(40) + 80)
          }
        }
        _ <- IO.shift(cpuBoundThreadPool)
        _ <- IO {
          measure(s"🔥 ${exchange.getRequestURI.getQuery}") {
            fibonacci(Random.nextInt(1) + 37)
          }
        }
        _ <- IO.shift(nonBlockingIOPolling)
        _ <- IO {
          measure("↗️") {
            exchange.sendResponseHeaders(200, 0)
            exchange.close()
          }
        }
      } yield ()

      measure("↘️") {
        program.unsafeRunAsync(_.fold(_ ⇒ (), identity))
      }
    })
    ```

 7. (Seb) Gatling

    Show scenario `performance.LoadTest`.

    Then start sbt task `gatling:test`.

    Visit [gatling reports](http://localhost:8000/gatling).

    Optional: use jvisualvm to show CPU usage and threads.
