package se.citerus.dddsample.infrastructure.messaging.jms

import java.util.{List as JList}

import jakarta.jms.{Connection, ConnectionFactory, Destination, MessageConsumer, QueueConnection, Session}
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.command.ActiveMQDestination
import org.springframework.beans.factory.annotation.{Qualifier, Value}
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.core.{JmsOperations, JmsTemplate}

import se.citerus.dddsample.application.{ApplicationEvents, CargoInspectionService, HandlingEventService}

/** Spring config wiring up ActiveMQ-backed queues, listener container, and
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
      handlingEventService: HandlingEventService
  ): MessageConsumer =
    val c = session.createConsumer(destination)
    c.setMessageListener(new HandlingEventRegistrationAttemptConsumer(handlingEventService))
    c

  @Bean(value = Array("misdirectedCargoConsumer"), destroyMethod = "close")
  def misdirectedCargoConsumer(session: Session, @Qualifier("misdirectedCargoQueue") destination: Destination): MessageConsumer =
    loggingConsumer(session, destination)

  @Bean(value = Array("deliveredCargoConsumer"), destroyMethod = "close")
  def deliveredCargoConsumer(session: Session, @Qualifier("deliveredCargoQueue") destination: Destination): MessageConsumer =
    loggingConsumer(session, destination)

  @Bean(value = Array("rejectedRegistrationAttemptsConsumer"), destroyMethod = "close")
  def rejectedRegistrationAttemptsConsumer(session: Session, @Qualifier("rejectedRegistrationAttemptsQueue") destination: Destination): MessageConsumer =
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
  def handlingEventRegistrationAttemptQueue: Destination = createQueue("HandlingEventRegistrationAttemptQueue")

  @Bean(Array("rejectedRegistrationAttemptsQueue"))
  def rejectedRegistrationAttemptsQueue: Destination = createQueue("RejectedRegistrationAttemptsQueue")

  @Bean
  def listenerContainerFactory(jmsConnectionFactory: ConnectionFactory): DefaultJmsListenerContainerFactory =
    val factory = new DefaultJmsListenerContainerFactory
    factory.setConnectionFactory(jmsConnectionFactory)
    factory.setConcurrency("1-1")
    factory

  @Bean
  def jmsConnectionFactory(): ConnectionFactory =
    val factory = new ActiveMQConnectionFactory(brokerUrl)
    factory.setTrustedPackages(JList.of(
      "se.citerus.dddsample.interfaces.handling",
      "se.citerus.dddsample.domain",
      "java.util",
      "scala"
    ))
    factory

  @Bean
  def jmsOperations(jmsConnectionFactory: ConnectionFactory): JmsOperations =
    new JmsTemplate(jmsConnectionFactory)

  @Bean(destroyMethod = "close")
  def connection(connectionFactory: ConnectionFactory): Connection =
    val qc: QueueConnection = connectionFactory.asInstanceOf[ActiveMQConnectionFactory].createQueueConnection()
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
      @Qualifier("rejectedRegistrationAttemptsQueue") rejectedRegistrationAttemptsQueue: Destination,
      @Qualifier("handlingEventRegistrationAttemptQueue") handlingEventRegistrationAttemptQueue: Destination
  ): ApplicationEvents =
    new JmsApplicationEventsImpl(
      jmsOperations,
      cargoHandledQueue,
      misdirectedCargoQueue,
      deliveredCargoQueue,
      rejectedRegistrationAttemptsQueue,
      handlingEventRegistrationAttemptQueue
    )

  private def createQueue(name: String): Destination =
    ActiveMQDestination.createDestination(name, ActiveMQDestination.QUEUE_TYPE)

  private inline def uninitialized: Null = null
