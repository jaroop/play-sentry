
val appName = "play-sentry"

val playVersion = play.core.PlayVersion.current

lazy val baseSettings = Seq(
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.12.3",
  crossScalaVersions := Seq("2.11.11", "2.12.3"),
  organization := "com.jaroop",
  resolvers ++= Seq(
    Resolver.typesafeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases")
  ),
  scalacOptions ++= scalacOptionsVersion(scalaVersion.value),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _: MavenRepository => false }
)

def scalacOptionsVersion(scalaVersion: String) = {
  Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  ) ++ (CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 12)) => Seq(
      "-Xlint:_,-unused", // Enable lint warnings except for unused imports, parameters, etc.
      "-Ywarn-extra-implicit" // Warn when more than one implicit parameter section is defined.
    )
    case _ => Nil
  })
}

lazy val core = (project in file("core"))
  .settings(
    baseSettings,
    libraryDependencies += "com.typesafe.play" %% "play" % playVersion % "provided",
    libraryDependencies += "com.typesafe.play" %% "play-cache" % playVersion % "provided",
    name := appName,
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)
  )

lazy val examples = (project in file("examples"))
  .enablePlugins(PlayScala)
  .settings(
    baseSettings,
    libraryDependencies ++= Seq(
      ehcache,
      guice
    )
  )
  .dependsOn(core)
