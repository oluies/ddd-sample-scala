package se.citerus.dddsample.infrastructure.persistence.jpa

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Spring config enabling JPA repository + entity scanning under this
 * package. Without this, `@SpringBootApplication`'s default classpath
 * scan picks up the `@Entity` / `JpaRepository` interfaces, but
 * `@DataJpaTest` slices the context and needs the explicit hints.
 *
 * Active by default once present on the classpath. To run the app
 * without JPA, exclude this config from component scanning *or* keep
 * the `application.properties` autoconfig excludes; tests opt in via
 * `@TestPropertySource(properties = "spring.autoconfigure.exclude=")`.
 */
@Configuration
@EntityScan(basePackageClasses = Array(classOf[CargoEntity]))
@EnableJpaRepositories(basePackageClasses = Array(classOf[CargoEntityRepository]))
class JpaConfig
