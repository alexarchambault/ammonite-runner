package ammrunner.cli

import java.io.File

import ammrunner.{AmmoniteFetcher, Versions, VersionsOption}
import caseapp._
import coursierapi.Module

import scala.collection.JavaConverters._

object AmmRunner extends CaseApp[Options] {

  def exit(message: String): Nothing = {
    System.err.println(message)
    sys.exit(1)
  }

  def run(options: Options, args: RemainingArgs): Unit = {

    val scriptPathOpt = args.remaining.headOption.filter(!_.startsWith("-"))

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

      case Some(scriptPath) =>
        val script = new File(scriptPath)

        val versions = options.versionsOpt
          .orElse(VersionsOption.fromScript(script))
          .getOrElse(Versions.default())

        fetcher(versions).command() match {
          case Left(e) => throw new Exception("Error getting Ammonite class path", e)
          case Right(cmd) => cmd.withArgs(Seq(scriptPath))
        }
    }

    command.exec()
  }
}

abstract class AmmRunner
