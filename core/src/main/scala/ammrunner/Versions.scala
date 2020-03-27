package ammrunner

import coursierapi.Module
import dataclass.data

import scala.collection.JavaConverters._

@data class Versions(
  ammoniteVersion: String,
  scalaVersion: String
)

object Versions {

  def default(): Versions =
    default(coursierapi.Versions.create())

  def default(coursierVersions: => coursierapi.Versions): Versions = {

    val scalaVersions = coursierVersions
      .withModule(Module.of("org.scala-lang", "scala-library"))
      .versions()
      .getListings
      .asScala
      .reverseIterator
      .flatMap(_.getValue.getAvailable.asScala.reverseIterator)
      .filter(v => v.startsWith("2.1") || v.startsWith("3."))
      .toVector

    val it = scalaVersions.iterator.flatMap { scalaVersion =>
      coursierVersions
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
        throw new NoMatchingScalaAmmoniteVersionsFound(scalaVersions)
      }
  }

}
