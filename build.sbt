import com.earldouglas.xwp.JettyPlugin
import com.earldouglas.xwp.WarPlugin

ThisBuild / scalaVersion  := "3.3.4"
ThisBuild / organization  := "se.citerus"
ThisBuild / version       := "0.0.1-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")

// Java 17 baseline. Spring 5.2 / Hibernate 5.4 / CXF 3.3 still rely on
// javax.* — Java 17 is the highest version that runs them without surprises.
ThisBuild / javacOptions ++= Seq("--release", "17")

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding",
  "UTF-8",
  "-explain",
  "-source:3.3"
  // -Wunused:imports / -Wvalue-discard / -Xfatal-warnings re-enable post-migration cleanup.
)

// ---------------------------------------------------------------------------
// Dependency versions — keep in one place so Scala Steward bumps land cleanly.
// ---------------------------------------------------------------------------
val SpringVersion       = "5.2.19.RELEASE"
val CxfVersion          = "3.3.13"
val HibernateVersion    = "5.4.33"
val Slf4jVersion        = "2.0.16"
val LogbackVersion      = "1.5.32"
val JacksonVersion      = "2.18.2"
val ScalaTestVersion    = "3.2.19"
val ScalaCheckVersion   = "1.19.0"
val MockitoScalaVersion = "1.17.37"
val ActiveMqVersion     = "5.18.7"

// Custom task key — must be declared before it's referenced in settings.
lazy val bookingFacadeJar = taskKey[File](
  "Build the booking-facade.jar containing only the JAX-WS facade classes"
)

lazy val root = (project in file("."))
  .enablePlugins(WarPlugin, JettyPlugin)
  .settings(
    name := "ddd-sample-scala",

    // --- Compile-time dependencies -----------------------------------------
    libraryDependencies ++= Seq(
      // Spring 5.2 (javax.* era). Use individual modules, not the legacy
      // monolithic `org.springframework:spring` artifact (which is gone).
      "org.springframework" % "spring-core"    % SpringVersion,
      "org.springframework" % "spring-context" % SpringVersion,
      "org.springframework" % "spring-beans"   % SpringVersion,
      "org.springframework" % "spring-aop"     % SpringVersion,
      "org.springframework" % "spring-tx"      % SpringVersion,
      "org.springframework" % "spring-orm"     % SpringVersion,
      "org.springframework" % "spring-jdbc"    % SpringVersion,
      "org.springframework" % "spring-web"     % SpringVersion,
      "org.springframework" % "spring-webmvc"  % SpringVersion,
      "org.springframework" % "spring-jms"     % SpringVersion,

      // Hibernate ORM. Domain stays framework-free; mapping XML lives under
      // src/main/resources.
      "org.hibernate" % "hibernate-core" % HibernateVersion
        exclude ("org.slf4j", "slf4j-api"),

      // CXF (JAX-WS) for the booking facade
      "org.apache.cxf" % "cxf-rt-frontend-jaxws"  % CxfVersion,
      "org.apache.cxf" % "cxf-rt-transports-http" % CxfVersion,

      // ActiveMQ (JMS)
      "org.apache.activemq" % "activemq-broker" % ActiveMqVersion,
      "org.apache.activemq" % "activemq-client" % ActiveMqVersion,
      "org.apache.activemq" % "activemq-spring" % ActiveMqVersion,

      // Logging — SLF4J 2.x + Logback
      "org.slf4j"      % "slf4j-api"       % Slf4jVersion,
      "org.slf4j"      % "jcl-over-slf4j"  % Slf4jVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,

      // Commons
      "org.apache.commons" % "commons-lang3" % "3.17.0",
      "commons-io"         % "commons-io"    % "2.18.0",
      "org.apache.commons" % "commons-dbcp2" % "2.13.0",

      // In-memory DB for integration tests + sample data
      "org.hsqldb" % "hsqldb" % "2.7.4",

      // JSON (replacing nothing today — useful as we add REST endpoints)
      "com.fasterxml.jackson.core"    % "jackson-databind"     % JacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion,

      // Servlet + JSP/JSTL — WAR runtime expects these from the container,
      // hence "provided".
      "javax.servlet"     % "javax.servlet-api"     % "4.0.1" % Provided,
      "javax.servlet.jsp" % "javax.servlet.jsp-api" % "2.3.3" % Provided,
      "javax.servlet"     % "jstl"                  % "1.2",
      "opensymphony"      % "sitemesh"              % "2.4.2" % Runtime
    ),

    // --- Test dependencies -------------------------------------------------
    libraryDependencies ++= Seq(
      "org.scalatest"      %% "scalatest"       % ScalaTestVersion  % Test,
      "org.scalacheck"     %% "scalacheck"      % ScalaCheckVersion % Test,
      "org.scalatestplus"  %% "scalacheck-1-18" % "3.2.19.0"        % Test,
      "org.scalatestplus"  %% "mockito-5-12"    % "3.2.19.0"        % Test,
      "org.springframework" % "spring-test"     % SpringVersion     % Test,
      "junit"               % "junit"           % "4.13.2"          % Test,
      "com.github.sbt"      % "junit-interface" % "0.13.3"          % Test
    ),

    // --- Resolvers ---------------------------------------------------------
    resolvers ++= Seq(
      "Apache Releases" at "https://repository.apache.org/content/repositories/releases/"
    ),

    // --- Compiler + test plumbing -----------------------------------------
    Test / fork              := true,
    Test / parallelExecution := false, // Spring contexts don't share well

    // Surefire-style test reports for CI
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),

    // --- Booking facade jar (mirrors the Maven secondary artifact) --------
    bookingFacadeJar := {
      val log       = streams.value.log
      val classes   = (Compile / classDirectory).value
      val facadeDir = classes / "se/citerus/dddsample/interfaces/booking/facade"
      val out       = crossTarget.value / s"${name.value}-${version.value}-booking-facade.jar"
      val matched   = (facadeDir ** "*.class").get()
      val mappings  = matched.flatMap(f => IO.relativize(classes, f).map(f -> _))
      IO.jar(mappings, out, new java.util.jar.Manifest, Some(0L))
      log.info(s"Wrote $out (${mappings.size} classes)")
      out
    },

    // --- WAR packaging ----------------------------------------------------
    // xsbt-web-plugin (WarPlugin) re-targets `Compile / packageBin` to produce
    // a .war, so no extra wiring is needed — `sbt package` builds the WAR.
    Compile / packageBin / packageOptions += Package.ManifestAttributes(
      "Implementation-Title" -> name.value
    ),
    scalacOptions += "-language:noAutoTupling"
  )
