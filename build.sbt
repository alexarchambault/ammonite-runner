
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
  ),
  sonatypeCredentialHost := "s01.oss.sonatype.org"
))

lazy val isAtLeastScala213 = Def.setting {
  import Ordering.Implicits._
  CrossVersion.partialVersion(scalaVersion.value).exists(_ >= (2, 13))
}

val scala213 = "2.13.8"
val scala212 = "2.12.16"
val scala211 = "2.11.12"

lazy val shared = Def.settings(
  sonatypeProfileName := "io.github.alexarchambault",
  scalaVersion := scala213,
  crossScalaVersions := Seq(scala213, scala212, scala211),
  libraryDependencies ++= {
    if (isAtLeastScala213.value) Nil
    else Seq(compilerPlugin(Deps.macroParadise))
  },
  scalacOptions += "-target:jvm-1.8",
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
    testFrameworks += new TestFramework("utest.runner.Framework"),
    mimaPreviousArtifacts := {
      mimaPreviousArtifacts.value.filter(!_.revision.startsWith("0.3."))
    },
    mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      Seq(
        // private methods
        ProblemFilters.exclude[DirectMissingMethodProblem]("ammrunner.Command#Graalvm.launch"),
        ProblemFilters.exclude[DirectMissingMethodProblem]("ammrunner.Command#Jvm.fork")
      )
    }
  )

lazy val cli = project
  .dependsOn(core)
  // .enablePlugins(GraalVMNativeImagePlugin)
  .enablePlugins(PackPlugin)
  .disablePlugins(MimaPlugin)
  .settings(
    name := "ammonite-runner-cli",
    shared,
    crossScalaVersions := crossScalaVersions.value.filter(!_.startsWith("2.11.")),
    libraryDependencies ++= Seq(
      Deps.caseApp,
      Deps.utest.value % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / fork := true,
    Test / javaOptions += {
      val isWindows = System.getProperty("os.name")
        .toLowerCase(java.util.Locale.ROOT)
        .contains("windows")
      val ext = if (isWindows) ".bat" else ""
      val launcher = (Compile / pack)
        .value
        .getAbsoluteFile
        ./("bin/amm-runner" + ext)
        .toString
      s"-Dammrunner.launcher=$launcher"
    },
    // graalVMNativeImageOptions += "--no-server"
  )

shared
publish / skip := true
disablePlugins(MimaPlugin)
scalaVersion := scala213
crossScalaVersions := Nil
