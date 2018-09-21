
val appName = "play-sentry"

val playVersion = play.core.PlayVersion.current
val specsVersion = "3.9.5"

lazy val baseSettings = Seq(
  version := "1.0.0",
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
  pomIncludeRepository := { _: MavenRepository => false },
  fork in Test := true,
  parallelExecution in Test := false,
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/jaroop/play-sentry")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/jaroop/play-sentry"),
      "scm:git@github.com:jaroop/play-sentry.git"
    )
  )
)

def scalacOptionsVersion(scalaVersion: String) = {
  Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
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
    name := appName,
    baseSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % playVersion % "provided",
      "com.typesafe.play" %% "play-cache" % playVersion % "provided",
      "com.typesafe.play" %% "play-test" % playVersion % "test",
      "org.specs2" %% "specs2-core" % specsVersion % "test",
      "org.specs2" %% "specs2-mock" % specsVersion % "test"
    ),
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)
  )

lazy val sentryTest = (project in file("sentry-test"))
  .settings(
    name := appName + "-test",
    baseSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % playVersion % "provided",
      "com.typesafe.play" %% "play-test" % playVersion % "provided",
      "com.typesafe.play" %% "play-specs2" % playVersion % "test",
      ehcache % "test",
      "org.specs2" %% "specs2-core" % specsVersion % "test",
      "org.specs2" %% "specs2-mock" % specsVersion % "test"
    ),
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)
  ).dependsOn(core)


lazy val examples = (project in file("examples"))
  .enablePlugins(PlayScala)
  .settings(
    baseSettings,
    libraryDependencies ++= Seq(
      ehcache,
      guice,
      "com.typesafe.play" %% "play-specs2" % playVersion % "test",
      "org.specs2" %% "specs2-core" % specsVersion % "test",
      "org.specs2" %% "specs2-mock" % specsVersion % "test"
    )
  )
  .dependsOn(core, sentryTest)

lazy val root = (project in file(".")).settings(baseSettings).aggregate(core, sentryTest)
