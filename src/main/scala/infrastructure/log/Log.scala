package infrastructure.log

import java.util.logging.Logger

trait Log {

  System.setProperty("java.util.logging.config.file", "src/main/resources/logging.properties")

  val log: Logger = Logger.getLogger("threadpools")

}
