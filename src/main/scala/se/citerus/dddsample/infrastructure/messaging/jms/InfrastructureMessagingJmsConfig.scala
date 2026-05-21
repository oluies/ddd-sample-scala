package se.citerus.dddsample.infrastructure.messaging.jms

import jakarta.jms.{
  Connection,
  ConnectionFactory,
  Destination,
  MessageConsumer,
  QueueConnection,
  Session
}
import java.util.List as JList

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.command.ActiveMQDestination
import org.springframework.beans.factory.annotation.{Qualifier, Value}
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.core.{JmsOperations, JmsTemplate}

import se.citerus.dddsample.application.{
  ApplicationEvents,
  CargoInspectionService,
  HandlingEventService
}

/**
 * Spring config wiring up ActiveMQ-backed queues, listener container, and
 * five `MessageConsumer` beans (one per queue) — three with the simple
 * logging listener, two with the real cargo / handling consumers.
 */
@EnableJms
@Configuration
class InfrastructureMessagingJmsConfig:

  @Value("${brokerUrl}")
  private var brokerUrl: String = uninitialized

  @Bean(value = Array("cargoHandledConsumer"), destroyMethod = "close")
  def cargoHandledConsumer(
      session: Session,
      @Qualifier("cargoHandledQueue") destination: Destination,
      cargoInspectionService: CargoInspectionService
  ): MessageConsumer =
    val c = session.createConsumer(destination)
    c.setMessageListener(new CargoHandledConsumer(cargoInspectionService))
    c

  @Bean(value = Array("handlingEventRegistrationAttemptConsumer"), destroyMethod = "close")
  def handlingEventRegistrationAttemptConsumer(
      session: Session,
      @Qualifier("handlingEventRegistrationAttemptQueue") destination: Destination,
      handlingEventService: HandlingEventService,
      @Qualifier("jmsObjectMapper") objectMapper: ObjectMapper
  ): MessageConsumer =
    val c = session.createConsumer(destination)
    c.setMessageListener(
      new HandlingEventRegistrationAttemptConsumer(handlingEventService, objectMapper)
    )
    c

  @Bean(value = Array("misdirectedCargoConsumer"), destroyMethod = "close")
  def misdirectedCargoConsumer(
      session: Session,
      @Qualifier("misdirectedCargoQueue") destination: Destination
  ): MessageConsumer =
    loggingConsumer(session, destination)

  @Bean(value = Array("deliveredCargoConsumer"), destroyMethod = "close")
  def deliveredCargoConsumer(
      session: Session,
      @Qualifier("deliveredCargoQueue") destination: Destination
  ): MessageConsumer =
    loggingConsumer(session, destination)

  @Bean(value = Array("rejectedRegistrationAttemptsConsumer"), destroyMethod = "close")
  def rejectedRegistrationAttemptsConsumer(
      session: Session,
      @Qualifier("rejectedRegistrationAttemptsQueue") destination: Destination
  ): MessageConsumer =
    loggingConsumer(session, destination)

  private def loggingConsumer(session: Session, destination: Destination): MessageConsumer =
    val c = session.createConsumer(destination)
    c.setMessageListener(new SimpleLoggingConsumer)
    c

  @Bean(Array("cargoHandledQueue"))
  def cargoHandledQueue: Destination = createQueue("CargoHandledQueue")

  @Bean(Array("misdirectedCargoQueue"))
  def misdirectedCargoQueue: Destination = createQueue("MisdirectedCargoQueue")

  @Bean(Array("deliveredCargoQueue"))
  def deliveredCargoQueue: Destination = createQueue("DeliveredCargoQueue")

  @Bean(Array("handlingEventRegistrationAttemptQueue"))
  def handlingEventRegistrationAttemptQueue: Destination = createQueue(
    "HandlingEventRegistrationAttemptQueue"
  )

  @Bean(Array("rejectedRegistrationAttemptsQueue"))
  def rejectedRegistrationAttemptsQueue: Destination = createQueue(
    "RejectedRegistrationAttemptsQueue"
  )

  @Bean
  def listenerContainerFactory(
      jmsConnectionFactory: ConnectionFactory
  ): DefaultJmsListenerContainerFactory =
    val factory = new DefaultJmsListenerContainerFactory
    factory.setConnectionFactory(jmsConnectionFactory)
    factory.setConcurrency("1-1")
    factory

  @Bean
  def jmsConnectionFactory(): ConnectionFactory =
    val factory = new ActiveMQConnectionFactory(brokerUrl)
    // All five queues now carry only TextMessage payloads (tracking-id
    // strings + the JSON-serialized HandlingEventRegistrationAttempt), so no
    // class names cross the wire and Java deserialization can't be reached
    // even if an attacker gained broker access. Empty list = ObjectMessage
    // deserialization rejects every class.
    factory.setTrustedPackages(JList.of())
    factory

  /**
   * Dedicated Jackson `ObjectMapper` for JMS payloads. Registers the Scala
   * module so `Option`, case classes, and Scala 3 enums round-trip cleanly,
   * and the JSR-310 module so `java.time.Instant` serialises as ISO-8601.
   * Kept distinct from Spring's REST-facing `ObjectMapper` so behavioural
   * changes to one don't surprise the other.
   */
  @Bean(Array("jmsObjectMapper"))
  def jmsObjectMapper: ObjectMapper =
    val mapper = new ObjectMapper
    mapper.registerModule(new JavaTimeModule)
    mapper.registerModule(DefaultScalaModule)
    mapper

  @Bean
  def jmsOperations(jmsConnectionFactory: ConnectionFactory): JmsOperations =
    new JmsTemplate(jmsConnectionFactory)

  @Bean(destroyMethod = "close")
  def connection(connectionFactory: ConnectionFactory): Connection =
    val qc: QueueConnection =
      connectionFactory.asInstanceOf[ActiveMQConnectionFactory].createQueueConnection()
    qc.start()
    qc

  @Bean
  def session(connection: Connection): Session =
    connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

  @Bean
  def applicationEvents(
      jmsOperations: JmsOperations,
      @Qualifier("cargoHandledQueue") cargoHandledQueue: Destination,
      @Qualifier("misdirectedCargoQueue") misdirectedCargoQueue: Destination,
      @Qualifier("deliveredCargoQueue") deliveredCargoQueue: Destination,
      @Qualifier(
        "rejectedRegistrationAttemptsQueue"
      ) rejectedRegistrationAttemptsQueue: Destination,
      @Qualifier(
        "handlingEventRegistrationAttemptQueue"
      ) handlingEventRegistrationAttemptQueue: Destination,
      @Qualifier("jmsObjectMapper") objectMapper: ObjectMapper
  ): ApplicationEvents =
    new JmsApplicationEventsImpl(
      jmsOperations,
      cargoHandledQueue,
      misdirectedCargoQueue,
      deliveredCargoQueue,
      rejectedRegistrationAttemptsQueue,
      handlingEventRegistrationAttemptQueue,
      objectMapper
    )

  private def createQueue(name: String): Destination =
    ActiveMQDestination.createDestination(name, ActiveMQDestination.QUEUE_TYPE)

  private inline def uninitialized: Null = null
