package ammrunner

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Locale

import dataclass.data

import scala.io.{BufferedSource, Codec}

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

object VersionsOption {

  // Warning: has to be nilpotent, like
  //   header(header(seq.iterator).toVector.iterator).toSeq == header(seq.iterator).toSeq
  private def header(lines: Iterator[String]): Iterator[String] =
    // FIXME This doesn't accept reverse shebang stuff like
    // #!/bin/sh
    //   exec …
    // !#
    lines
      .map(_.trim)
      .filter(_.nonEmpty)
      .dropWhile(_.startsWith("#"))
      .takeWhile(_.startsWith("//"))

  def fromScript(script: Iterator[String]): VersionsOption = {

    // TODO Read Ammonite options via the header, too

    val versionsMap = header(script)
      .map(_.stripPrefix("//"))
      .flatMap(_.split(","))
      .map(_.trim)
      .filter { s =>
        val s0 = s.toLowerCase(Locale.ROOT)
        s0.startsWith("scala ") || s0.startsWith("ammonite ")
      }
      .map(_.split("\\s+", 2))
      .collect {
        case Array(k, v) => k.toLowerCase(Locale.ROOT) -> v
      }
      .toMap

    VersionsOption(
      versionsMap.get("ammonite"),
      versionsMap.get("scala")
    )
  }

  def fromScript(script: File, charset: Charset): VersionsOption = {

    var s: BufferedSource = null
    val header0 = try {
      s = scala.io.Source.fromFile(script)(new Codec(charset))
      header(s.getLines()).toVector
    } finally {
      if (s != null)
        s.close()
    }

    fromScript(header0.iterator)
  }

  def fromScript(script: File): VersionsOption =
    // FIXME Use the default charset instead?
    fromScript(script, StandardCharsets.UTF_8)

}
