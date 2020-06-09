addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")
addSbtPlugin("com.github.alexarchambault.tmp" % "sbt-mima-plugin" % "0.7.1-SNAPSHOT")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.3")
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.12")

resolvers += Resolver.sonatypeRepo("snapshots")
