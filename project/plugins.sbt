addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")
addSbtPlugin(("io.github.alexarchambault.sbt" % "sbt-compatibility" % "0.0.8").exclude("com.typesafe", "sbt-mima-plugin"))
addSbtPlugin("io.github.alexarchambault.sbt" % "sbt-eviction-rules" % "0.2.0")
addSbtPlugin("com.github.alexarchambault.tmp" % "sbt-mima-plugin" % "0.7.1-SNAPSHOT")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3")
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.12")

resolvers += Resolver.sonatypeRepo("snapshots")
