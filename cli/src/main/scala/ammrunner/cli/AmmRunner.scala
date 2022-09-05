package ammrunner.cli

import java.io.File

import ammrunner.{AmmoniteFetcher, Command, Versions, VersionsOption}
import caseapp._
import coursierapi.Module

import scala.collection.JavaConverters._

object AmmRunner extends CaseApp[Options] {

  override def stopAtFirstUnrecognized = true

  def exit(message: String): Nothing = {
    System.err.println(message)
    sys.exit(1)
  }

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

  def run(options: Options, args: RemainingArgs): Unit = {

    // that should probably be fixed by case-app
    val args0 =
      if (args.remaining.headOption.contains("--"))
        args.all.drop(1)
      else
        args.all

    val proc = command(options.amm, options.scala, args0).withArgs(args0).run()
    val retCode = proc.waitFor()
    if (retCode != 0)
      sys.exit(retCode)
  }
}

abstract class AmmRunner
