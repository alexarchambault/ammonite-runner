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
  fork: Boolean = false
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
}
