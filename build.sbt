
inThisBuild(List(
  organization := "io.github.alexarchambault.ammonite",
  homepage := Some(url("https://github.com/alexarchambault/ammonite-runner")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "",
      url("https://github.com/alexarchambault")
    )
  )
))

lazy val isAtLeastScala213 = Def.setting {
  import Ordering.Implicits._
  CrossVersion.partialVersion(scalaVersion.value).exists(_ >= (2, 13))
}

val scala213 = "2.13.1"
val scala212 = "2.12.11"
val scala211 = "2.11.12"

lazy val shared = Def.settings(
  scalaVersion := scala213,
  crossScalaVersions := Seq(scala213, scala212, scala211),
  libraryDependencies ++= {
    if (isAtLeastScala213.value) Nil
    else Seq(compilerPlugin(Deps.macroParadise))
  },
  scalacOptions ++= {
    if (isAtLeastScala213.value) Seq("-Ymacro-annotations")
    else Nil
  }
)


lazy val core = project
  .settings(
    name := "ammonite-runner",
    shared,
    libraryDependencies ++= Seq(
      Deps.coursierInterface,
      Deps.coursierLauncher,
      Deps.dataClass % Provided,
      Deps.svm % Provided,
      Deps.utest.value % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val cli = project
  .dependsOn(core)
  .enablePlugins(GraalVMNativeImagePlugin, PackPlugin)
  .settings(
    name := "ammonite-runner-cli",
    shared,
    libraryDependencies += Deps.caseApp,
    graalVMNativeImageOptions += "--no-server"
  )

skip.in(publish) := true
scalaVersion := scala213
crossScalaVersions := Nil
