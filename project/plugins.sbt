// xsbt-web-plugin: WAR packaging + `Jetty / start` / `Jetty / stop`
addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.2.4")

// Code formatting (reads .scalafmt.conf at the repo root)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// Refactoring rules; we'll lean on this for Scala 3 cleanups post-migration
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.13.0")
