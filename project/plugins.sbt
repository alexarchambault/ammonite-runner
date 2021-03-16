addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.6")
addSbtPlugin(("io.github.alexarchambault.sbt" % "sbt-compatibility" % "0.0.8").exclude("com.typesafe", "sbt-mima-plugin"))
addSbtPlugin("io.github.alexarchambault.sbt" % "sbt-eviction-rules" % "0.2.0")
addSbtPlugin("com.github.alexarchambault.tmp" % "sbt-mima-plugin" % "0.7.1-SNAPSHOT")
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.13")

resolvers += Resolver.sonatypeRepo("snapshots")
