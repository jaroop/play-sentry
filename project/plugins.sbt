
resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.3")
