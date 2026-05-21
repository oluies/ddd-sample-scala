package se.citerus.dddsample

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Spring Boot entry point. Translation of the upstream Java
 * `se.citerus.dddsample.Application` class.
 *
 * Component scanning starts at this class's package and walks down through
 * `se.citerus.dddsample.*`, so any `@Configuration` / `@RestController` /
 * `@Service` / `@Repository` we add in later phases gets picked up
 * automatically. The Java reference uses an explicit `@Import` listing the
 * config classes; we don't, because component scan covers it.
 */
@SpringBootApplication
class Application

object Application:
  def main(args: Array[String]): Unit =
    val _ = SpringApplication.run(classOf[Application], args*)
