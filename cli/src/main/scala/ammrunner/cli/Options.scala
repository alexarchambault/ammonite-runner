package ammrunner.cli

import ammrunner.{Versions, VersionsOption}

import caseapp._
import caseapp.core.help.Help

final case class Options(
  @ExtraName("V")
  @HelpMessage("Force Ammonite version")
  @ValueDescription("Ammonite version")
    amm: Option[String] = None,
  @ExtraName("S")
  @HelpMessage("Force Scala version")
  @ValueDescription("Scala version")
    scala: Option[String] = None,
  fork: Boolean = Options.defaultFork
) {
  def versionsOpt: VersionsOption =
    VersionsOption(amm, scala)

  def versionOverrides(versions: VersionsOption): VersionsOption = {
    var versions0 = versions
    if (amm.nonEmpty)
      versions0 = versions0.withAmmoniteVersion(amm)
    if (scala.nonEmpty)
      versions0 = versions0.withScalaVersion(scala)
    versions0
  }
}

object Options {
  implicit val parser = Parser[Options]
  implicit val help = Help[Options]

  private def isWindows = System.getProperty("os.name")
    .toLowerCase(java.util.Locale.ROOT)
    .contains("windows")

  // not sure why, the tests don't pass on Windows without forking
  private val defaultFork = isWindows ||
    // Starting from JDK 9, AppClassLoader gets in the way of isolating
    // our JARs from those of Ammonite, so we fork by default.
    scala.util.Try(System.getProperty("java.version").takeWhile(_ != '.').toInt)
      .toOption
      .exists(_ >= 9)
}
