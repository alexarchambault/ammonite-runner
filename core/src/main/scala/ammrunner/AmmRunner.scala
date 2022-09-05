package ammrunner.cli

import java.io.File

import ammrunner.{AmmoniteFetcher, Command, Versions, VersionsOption}

object AmmRunner {

  def command(
    ammVersion: Option[String],
    scalaVersion: Option[String],
    args: Seq[String]
  ): Command = {

    val scriptPathOpt = args
      .iterator
      .filter(!_.startsWith("-"))
      .filter(_.endsWith(".sc"))
      .map(new File(_))
      .filter(_.isFile)
      .toStream
      .headOption

    val versionsOpt = VersionsOption(ammVersion, scalaVersion)
    val versions = scriptPathOpt match {
      case None =>
        versionsOpt.getOrElse(Versions.default())

      case Some(script) =>
        versionsOpt
          .orElse(VersionsOption.fromScript(script))
          .getOrElse(Versions.default())
    }
    val command = AmmoniteFetcher(versions).command() match {
      case Left(e) => throw new Exception("Error getting Ammonite class path", e)
      case Right(cmd) => cmd
    }

    command.withArgs(args)
  }
}
