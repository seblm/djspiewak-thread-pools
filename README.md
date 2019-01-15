[![Build Status](https://travis-ci.org/seblm/djspiewak-thread-pools.svg?branch=master)](https://travis-ci.org/seblm/djspiewak-thread-pools)

Source code to understand [Thread Pools](https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c).

![thread pools](https://typelevel.org/cats-effect/img/concurrency-thread-pools.png)

From [cats-effect documentation](https://typelevel.org/cats-effect/concurrency/basics.html#choosing-thread-pool)

 0. Prepare yourself
    1. Start web server

    ```bash
    cd src/main/webapp
    python -m SimpleHTTPServer
    ```

    2. Launch [performance results web page](http://localhost:8000)

    3. Delete gatling previous runs

    ```bash
    rm -r target/gatling/*
    ```

    4. Launch sbt shell (⇧⌘S)

    5. Prepare `curl` and `test.sh` from terminal

    6. Close slack, switch off macos notifications

    7. Start jvisualvm:

    ```bash
    /Library/Java/JavaVirtualMachines/jdk1.8.0_162.jdk/Contents/Home/bin/jvisualvm
    ``` 

 1. (Seb) Start with a basic HTTPServer (default HTTP server thread) and some computations:

    ```scala
    import java.net.InetSocketAddress

    import com.sun.net.httpserver.{HttpExchange, HttpServer}
    import domain.Fibonacci
    import infrastructure.log.Log

    import scala.util.Random

    object ThreadPoolsHttpServer extends App with Log with Fibonacci {

      private val defaultBacklog = 0
      private val server = HttpServer.create(new InetSocketAddress(8080), defaultBacklog)

      server.createContext("/", { exchange: HttpExchange ⇒
        Thread.sleep(Random.nextInt(40) + 80)
        fibonacci(Random.nextInt(1) + 37)
        exchange.sendResponseHeaders(200, 0)
        exchange.close()
      })

      server.start()
      log.info("server is started, main thread will stop")

    }
    ```

    Then invoke the server

    ```bash
    $ curl --include localhost:8080 | head -n 1
    HTTP/1.1 200 OK
    ```

 2. (Martin) Measure:

    ```scala
    import com.sun.net.httpserver.{HttpExchange, HttpServer}
    import domain.Fibonacci
    import infrastructure.log.Log
    import infrastructure.web.PerformanceResults

    import scala.util.Random

    object ThreadPoolsHttpServer extends App with Log with Fibonacci with PerformanceResults {

      // ...
      installPerformanceResultsTo(server)

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

    Then invoke the server

    ```bash
    src/scripts/test.sh
    ```

    And go to [performance results web page](http://localhost:8000).

    We can see that requests are stacked, performing blocking io and cpu bounded tasks on non blocking io polling is
    bad.

 3. (Seb) Creates Thread pools and dispatch:

    ```scala
    import java.util.concurrent.Executors.{newCachedThreadPool, newFixedThreadPool, newSingleThreadExecutor}

    import com.sun.net.httpserver.HttpExchange

    import scala.concurrent.Future
    import scala.concurrent.JavaConversions._
    import scala.util.Random

    // ...

    server.setExecutor(newSingleThreadExecutor())
    val nonBlockingIOPolling = server.getExecutor()  // pool-1-thread-1
    val blockingIOThreadPool = newCachedThreadPool() // pool-2-thread-*
    val cpuBoundThreadPool = newFixedThreadPool(4)   // pool-3-thread-*

    // ...

    server.createContext("/", { exchange: HttpExchange ⇒
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
    })
    ```

 4. (Martin) cats-effect

    ```scala
    import cats.effect.IO
    import com.sun.net.httpserver.HttpExchange

    import scala.util.Random

    // ...

    server.createContext("/", { exchange: HttpExchange ⇒
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
        program.unsafeRunAsync(_ ⇒ ())
      }
    })
    ```

 5. (Seb) Gatling

    Show scenario `performance.LoadTest`.

    Then start sbt task `gatling:test`.

    Visit [gatling reports](http://localhost:8000/gatling).

    Optional: use jvisualvm to show CPU usage and threads.
