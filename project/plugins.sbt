addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.19.0")

addSbtPlugin("io.github.johnhungerford" % "sbt-jsbundler" % "0.0.5")

libraryDependencies ++= Seq(
  "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.0.0",
  "org.scala-js" %% "scalajs-env-selenium" % "1.1.1",
)
