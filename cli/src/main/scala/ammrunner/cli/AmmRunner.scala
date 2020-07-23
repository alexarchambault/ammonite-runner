package ammrunner.cli

import java.io.File

import ammrunner.{AmmoniteFetcher, Versions, VersionsOption}
import caseapp._
import coursierapi.Module

import scala.collection.JavaConverters._

object AmmRunner extends CaseApp[Options] {

  override def stopAtFirstUnrecognized = true

  def exit(message: String): Nothing = {
    System.err.println(message)
    sys.exit(1)
  }

  def run(options: Options, args: RemainingArgs): Unit = {

    // that should probably be fixed by case-app
    val args0 =
      if (args.remaining.headOption.contains("--"))
        args.all.drop(1)
      else
        args.all

    val scriptPathOpt = args0
      .iterator
      .filter(!_.startsWith("-"))
      .filter(_.endsWith(".sc"))
      .map(new File(_))
      .filter(_.isFile)
      .toStream
      .headOption

    def fetcher(versions: Versions) =
      AmmoniteFetcher(versions)
        .withInterpOnly(false)

    val command = scriptPathOpt match {
      case None =>
        val versions = options.versionsOpt
          .getOrElse(Versions.default())

        fetcher(versions).command() match {
          case Left(e) => throw new Exception("Error getting Ammonite class path", e)
          case Right(cmd) => cmd
        }

      case Some(script) =>
        val versions = options.versionsOpt
          .orElse(VersionsOption.fromScript(script))
          .getOrElse(Versions.default())

        fetcher(versions).command() match {
          case Left(e) => throw new Exception("Error getting Ammonite class path", e)
          case Right(cmd) => cmd
        }
    }

    command.withArgs(args0).exec()
  }
}

abstract class AmmRunner
