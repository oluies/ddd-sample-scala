ThisBuild / scalaVersion  := "3.3.4"
ThisBuild / organization  := "se.citerus"
ThisBuild / version       := "0.0.1-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")

// Java 21 baseline. Spring Boot 3.3 / Jakarta EE / Hibernate 6 all run cleanly on 21.
ThisBuild / javacOptions  ++= Seq("--release", "21")

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding", "UTF-8",
  "-explain",
  "-source:3.3"
  // -Wunused:imports / -Wvalue-discard / -Xfatal-warnings re-enable after phase 17 cleanup.
)

// ---------------------------------------------------------------------------
// Dependency versions — keep in one place so Scala Steward bumps land cleanly.
// ---------------------------------------------------------------------------
val SpringBootVersion   = "3.3.10"
val HsqldbVersion       = "2.7.4"
val CommonsLang3Version = "3.20.0"
val ScalaTestVersion    = "3.2.20"
val ScalaCheckVersion   = "1.19.0"
val MockitoVersion      = "3.2.19.0" // scalatestplus-mockito artifact version

lazy val root = (project in file("."))
  .settings(
    name := "ddd-sample-scala",

    // --- Compile-time dependencies ----------------------------------------
    // The Spring Boot starters bring in Jakarta EE 10, Hibernate 6, Jackson,
    // embedded Tomcat, etc. as transitive deps — no need to pin them
    // individually here.
    // Matches upstream `citerus/dddsample-core` pom.xml dependency set.
    libraryDependencies ++= Seq(
      "org.springframework.boot" % "spring-boot-starter-web"        % SpringBootVersion,
      "org.springframework.boot" % "spring-boot-starter-data-jpa"   % SpringBootVersion,
      "org.springframework.boot" % "spring-boot-starter-activemq"   % SpringBootVersion,
      "org.springframework.boot" % "spring-boot-starter-actuator"   % SpringBootVersion,
      "org.springframework.boot" % "spring-boot-starter-validation" % SpringBootVersion,
      "org.springframework.boot" % "spring-boot-starter-thymeleaf"  % SpringBootVersion,

      // ActiveMQ Classic 6.x (Jakarta) for in-process broker + Spring wiring.
      // Versions resolved transitively by Spring Boot's BOM.
      "org.apache.activemq" % "activemq-broker" % "6.1.5",
      "org.apache.activemq" % "activemq-spring" % "6.1.5",

      // Apache Commons utilities — null-checks, string utils, builders.
      "org.apache.commons" % "commons-lang3" % CommonsLang3Version,
      "commons-io"         % "commons-io"    % "2.18.0",

      // HSQLDB for the dev profile + integration tests (matches upstream Java).
      "org.hsqldb" % "hsqldb" % HsqldbVersion
    ),

    // --- Test dependencies -------------------------------------------------
    libraryDependencies ++= Seq(
      "org.springframework.boot" % "spring-boot-starter-test" % SpringBootVersion % Test
        exclude("org.junit.vintage", "junit-vintage-engine"),
      "org.scalatest"      %% "scalatest"       % ScalaTestVersion  % Test,
      "org.scalacheck"     %% "scalacheck"      % ScalaCheckVersion % Test,
      "org.scalatestplus"  %% "scalacheck-1-18" % MockitoVersion    % Test,
      "org.scalatestplus"  %% "mockito-5-12"    % MockitoVersion    % Test
    ),

    // --- Compiler + test plumbing -----------------------------------------
    Test / fork              := true,
    Test / parallelExecution := false,            // Spring contexts don't share well

    // Surefire-style test reports for CI
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),

    scalacOptions += "-language:noAutoTupling"
  )
