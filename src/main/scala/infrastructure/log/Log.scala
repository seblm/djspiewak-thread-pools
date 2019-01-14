package infrastructure.log

import org.slf4j.{Logger, LoggerFactory}

trait Log {

  val log: Logger = LoggerFactory.getLogger("threadpools")

}
