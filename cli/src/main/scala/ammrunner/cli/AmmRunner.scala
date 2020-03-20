package ammrunner.cli

import java.io.File

import ammrunner.Versions
import caseapp._
import coursierapi.Module

import scala.collection.JavaConverters._

object AmmRunner extends CaseApp[Options] {

  def exit(message: String): Nothing = {
    System.err.println(message)
    sys.exit(1)
  }

  def defaultVersions(): Versions = {

    // FIXME Use a cache that doesn't try to update the listings it already has

    val scalaVersionsIt = coursierapi.Versions.create()
      .withModule(Module.of("org.scala-lang", "scala-library"))
      .versions()
      .getListings
      .asScala
      .reverseIterator
      .flatMap(_.getValue.getAvailable.asScala.reverseIterator)
      .filter(v => v.startsWith("2.1") || v.startsWith("3."))

    val it = scalaVersionsIt.flatMap { scalaVersion =>
      coursierapi.Versions.create()
        .withModule(Module.of("com.lihaoyi", "ammonite_" + scalaVersion))
        .versions()
        .getListings
        .asScala
        .map(_.getValue.getRelease)
        .headOption // FIXME Order / select highest?
        .iterator
        .map((scalaVersion, _))
    }

    it.toStream
      .headOption
      .map {
        case (s, a) =>
          Versions(a, s)
      }
      .getOrElse {
        // shouldn't happen - warn or fail loudly instead?
        Versions("1.8.2", scala.util.Properties.versionNumberString)
      }
  }

  def run(options: Options, args: RemainingArgs): Unit = {

    val scriptPathOpt = args.remaining.headOption.filter(!_.startsWith("-"))

    scriptPathOpt match {
      case None =>
        val versions = options.versionsOpt.getOrElse(defaultVersions())
        ammrunner.AmmRunner.run(versions, args.all, options.fork)

      case Some(scriptPath) =>
        val script = new File(scriptPath)

        val versions = options.versionsOpt
          .orElse(ammrunner.AmmRunner.versions(script))
          .getOrElse(defaultVersions())

        ammrunner.AmmRunner.run(versions, args.all, options.fork)
    }
  }
}

abstract class AmmRunner
