package domain

trait Fibonacci {

  def fibonacci(iteration: Int): Long = iteration match {
    case 0 ⇒ 0
    case 1 ⇒ 1
    case i ⇒ fibonacci(i - 2) + fibonacci(i - 1)
  }

}
