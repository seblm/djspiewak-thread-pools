[![Build Status](https://travis-ci.org/seblm/djspiewak-thread-pools.svg?branch=master)](https://travis-ci.org/seblm/djspiewak-thread-pools)

Source code to understand [Thread Pools](https://gist.github.com/djspiewak/46b543800958cf61af6efa8e072bfd5c).

 1. (Seb) Start with a basic HTTPServer (default HTTP server thread):
 
    ```scala
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
    object ThreadPoolsHttpServer extends App with Log with PerformanceResults {

      // ...
      installPerformanceResultsTo(server)

      server.createContext("/", (exchange: HttpExchange) ⇒ {
        measure("handle") {
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

    And go to performance results web page.

    We can see that requests are stacked, performing blocking io and cpu bounded tasks on non blocking io polling is
    bad.

 4. (Martin) Add computation and measure all the thing:

    ```scala
    object ThreadPoolsHttpServer extends App with Log with PerformanceResults with Fibonacci {

      // ...

      server.createContext("/", (exchange: HttpExchange) ⇒ {
        measure("🚫") {
          Thread.sleep(Random.nextInt(40) + 80)
        }
        measure("🔥") {
          fibonacci(Random.nextInt(1) + 37)
        }
        measure("↗️") {
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
    import scala.concurrent.JavaConversions._

    // ...

    server.setExecutor(newSingleThreadExecutor())
    val nonBlockingIOPolling = server.getExecutor()  // pool-1-thread-1
    val blockingIOThreadPool = newCachedThreadPool() // pool-2-thread-*
    val cpuBoundThreadPool = newFixedThreadPool(2)   // pool-3-thread-*

    // ...

    server.createContext("/", (exchange: HttpExchange) ⇒ {
      measure("↘️") {
        Future {
          measure("🚫") {
            Thread.sleep(Random.nextInt(40) + 80)
          }
        }(blockingIOThreadPool).map { _ ⇒
          measure("🔥") {
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

 6. Gatling
 7. cats-effect
