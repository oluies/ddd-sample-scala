package se.citerus.dddsample.config

import com.pathfinder.api.GraphTraversalService
import com.pathfinder.internal.{GraphDAO, GraphDAOStub, GraphTraversalServiceImpl}
import org.springframework.context.annotation.{Bean, Configuration}

import se.citerus.dddsample.domain.model.cargo.{CargoFactory, CargoRepository}
import se.citerus.dddsample.domain.model.handling.HandlingEventFactory
import se.citerus.dddsample.domain.model.location.LocationRepository
import se.citerus.dddsample.domain.model.voyage.VoyageRepository
import se.citerus.dddsample.domain.service.RoutingService
import se.citerus.dddsample.infrastructure.routing.ExternalRoutingService

/**
 * Spring wiring for the framework-free domain factories and the routing /
 * pathfinder adapters.
 *
 * `CargoFactory` and `HandlingEventFactory` live in `domain.*` and stay
 * annotation-free (Decision D1). We construct them here as `@Bean`s
 * instead of dropping `@Component` on the domain class.
 *
 * The pathfinder lives in its own `com.pathfinder.*` package — also kept
 * out of the dddsample component scan, wired up here.
 */
@Configuration
class DomainConfig:

  @Bean
  def cargoFactory(
      locationRepository: LocationRepository,
      cargoRepository: CargoRepository
  ): CargoFactory =
    new CargoFactory(locationRepository, cargoRepository)

  @Bean
  def handlingEventFactory(
      cargoRepository: CargoRepository,
      voyageRepository: VoyageRepository,
      locationRepository: LocationRepository
  ): HandlingEventFactory =
    new HandlingEventFactory(cargoRepository, voyageRepository, locationRepository)

  @Bean
  def graphDAO: GraphDAO = new GraphDAOStub

  @Bean
  def graphTraversalService(graphDAO: GraphDAO): GraphTraversalService =
    new GraphTraversalServiceImpl(graphDAO)

  @Bean
  def routingService(
      graphTraversalService: GraphTraversalService,
      locationRepository: LocationRepository,
      voyageRepository: VoyageRepository
  ): RoutingService =
    new ExternalRoutingService(graphTraversalService, locationRepository, voyageRepository)
