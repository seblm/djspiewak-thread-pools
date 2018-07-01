package domain

import org.scalatest.{FlatSpec, Matchers}

class FibonacciSpec extends FlatSpec with Matchers {

  "fibonacci" should "return 21 at iteration 8" in {
    val fibonacci = new Fibonacci {}

    val result = fibonacci.fibonacci(8)

    result should be(21)
  }

}
