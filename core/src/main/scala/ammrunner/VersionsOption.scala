package ammrunner

import dataclass.data

@data class VersionsOption(
  ammoniteVersion: Option[String],
  scalaVersion: Option[String]
) {
  def versions: Option[Versions] =
    for {
      amm <- ammoniteVersion
      scala <- scalaVersion
    } yield Versions(amm, scala)

  def orElse(other: => VersionsOption): VersionsOption = {
    lazy val other0 = other
    var v = this
    if (ammoniteVersion.isEmpty && other0.ammoniteVersion.nonEmpty)
      v = v.withAmmoniteVersion(other0.ammoniteVersion)
    if (scalaVersion.isEmpty && other0.scalaVersion.nonEmpty)
      v = v.withScalaVersion(other0.scalaVersion)
    v
  }

  def getOrElse(versions: => Versions): Versions = {
    lazy val versions0 = versions
    val ammVer = ammoniteVersion.getOrElse(versions0.ammoniteVersion)
    val scalaVer = scalaVersion.getOrElse(versions0.scalaVersion)
    Versions(ammVer, scalaVer)
  }
}
