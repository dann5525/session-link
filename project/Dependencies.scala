import sbt._

object Dependencies {

  object V {
    val tessellation = "2.8.1"
    val decline = "2.4.1"
    // Updated http4s version that supports forAsync with EmberClientBuilder
    val http4s = "0.23.23"
    val bouncyCastle = "1.70"
    val cats = "2.9.0"
    val circe = "0.14.3"
    val web3j = "4.8.7" // Added Web3j version
  }

  def tessellation(artifact: String): ModuleID = "org.constellation" %% s"tessellation-$artifact" % V.tessellation

  def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % V.http4s

  def decline(artifact: String = ""): ModuleID =
    "com.monovore" %% {
      if (artifact.isEmpty) "decline" else s"decline-$artifact"
    } % V.decline

  object Libraries {
    val tessellationNodeShared = tessellation("node-shared")
    val tessellationCurrencyL0 = tessellation("currency-l0")
    val tessellationCurrencyL1 = tessellation("currency-l1")

    val declineCore = decline()
    val declineEffect = decline("effect")
    val declineRefined = decline("refined")

    val http4sCore = http4s("core")
    val http4sDsl = http4s("dsl")
    val http4sServer = http4s("ember-server")
    val http4sClient = http4s("ember-client")
    val http4sCirce = http4s("circe")

        // New Libraries
    val bouncyCastle = "org.bouncycastle" % "bcprov-jdk15on" % V.bouncyCastle
    val catsCore = "org.typelevel" %% "cats-core" % V.cats
    val circeCore = "io.circe" %% "circe-core" % V.circe
    val circeGeneric = "io.circe" %% "circe-generic" % V.circe
    val circeParser = "io.circe" %% "circe-parser" % V.circe
    val web3jCore = "org.web3j" % "core" % V.web3j // Correctly placed Web3j dependency
  }

  // Scalafix rules
  val organizeImports = "com.github.liancheng" %% "organize-imports" % "0.5.0"

  object CompilerPlugin {

    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % "0.3.1"
    )

    val kindProjector = compilerPlugin(
      ("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full)
    )

    val semanticDB = compilerPlugin(
      ("org.scalameta" % "semanticdb-scalac" % "4.7.1").cross(CrossVersion.full)
    )
  }
}
