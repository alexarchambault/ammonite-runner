package ammrunner

import java.io.{File, InputStream, OutputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, FileSystemException, Path}

import coursierapi.{Cache, Dependency, Fetch, Logger, ResolutionParams}
import coursier.launcher.{BootstrapGenerator, ClassLoaderContent, ClassPathEntry}
import dataclass._

import scala.collection.JavaConverters._
import scala.io.{BufferedSource, Codec}

@data class AmmoniteFetcher(
  versions: Versions,
  interpOnly: Boolean = false,
  resolutionParams: ResolutionParams = ResolutionParams.create(),
  fetchCacheIKnowWhatImDoing: Option[File] = None,
  tmpDir: Option[Path] = None,
  deleteTmpFile: Boolean = true,
  fetchSources: Boolean = true,
  progressBars: Boolean = true,
  transformFetch: Option[coursierapi.Fetch => coursierapi.Fetch] = None,
  @since
  thin: Boolean = true
) {

  def withTmpDir(tmpDir: Path): AmmoniteFetcher =
    withTmpDir(Some(tmpDir))

  def command(): Either[AmmoniteFetcherException, Command] = {

    val compilerDeps =
      if (AmmoniteFetcher.compareVersions("2.3.8-32-64308dc3", versions.ammoniteVersion) <= 0)
        Seq(
          Dependency.of(
            "com.lihaoyi",
            "ammonite-compiler_" + versions.scalaVersion,
            versions.ammoniteVersion
          )
        )
      else Nil

    val mainDeps = {
      val main = Dependency.of(
        "com.lihaoyi",
        if (interpOnly)
          "ammonite-interp_" + versions.scalaVersion
        else
          "ammonite_" + versions.scalaVersion,
        versions.ammoniteVersion
      )
      Seq(main) ++ (if (interpOnly) compilerDeps else Nil)
    }

    def apiDeps = {
      val api = Dependency.of(
        "com.lihaoyi",
        if (interpOnly)
          "ammonite-interp-api_" + versions.scalaVersion
        else
          "ammonite-repl-api_" + versions.scalaVersion,
        versions.ammoniteVersion
      )
      Seq(api) ++ compilerDeps
    }

    def createFetcher(): Fetch = {
      val cache = Cache.create()
      if (progressBars)
        cache.withLogger(Logger.progressBars())
      val fetch = Fetch.create()
        .withCache(cache)
        .withResolutionParams(
          // FIXME This mutates resolutionParams in place
          resolutionParams
            .withScalaVersion(versions.scalaVersion)
        )
        .withMainArtifacts()
      if (fetchSources)
        fetch.addClassifiers("sources")
      transformFetch.fold(fetch)(_(fetch))
    }

    def maybeResult(fetcher: Fetch) =
      try {
        Right(fetcher.fetchResult())
      } catch {
        case e: coursierapi.error.CoursierError =>
          Left(new CoursierError(s"Error fetching Ammonite ${versions.ammoniteVersion} for scala ${versions.scalaVersion}", e))
      }

    val mainClass = "ammonite.Main" // Get from META-INF/MANIFEST… if it's there?

    if (fetchCacheIKnowWhatImDoing.nonEmpty || !thin) {
      val fetcher = createFetcher()
        .addDependencies(mainDeps: _*)
        .withFetchCacheIKnowWhatImDoing(fetchCacheIKnowWhatImDoing.orNull)
      maybeResult(fetcher)
        .right
        .map(res => Command(res.getFiles.asScala.toVector, mainClass))
    } else {

      val apiFetcher = createFetcher()
        .addDependencies(apiDeps: _*)

      for {
        apiRes <- maybeResult(apiFetcher).right
        res <- {
          val apiDepVersions = apiRes
            .getDependencies
            .asScala
            .toVector
            .map(dep => dep.getModule -> dep.getVersion)
          val fetcher = {
            val fetcher0 = createFetcher()
              .addDependencies(mainDeps: _*)
            fetcher0
              .withResolutionParams(
                fetcher0.getResolutionParams
                  .forceVersions(apiDepVersions.toMap.asJava)
              )
          }
          maybeResult(fetcher).right
        }
      } yield {
        val apiUrls = apiRes.getArtifacts.asScala.toVector.map(_.getKey.getUrl)
        val mainUrls = res.getArtifacts.asScala.toVector.map(_.getKey.getUrl).filterNot(apiUrls.toSet)
        val sharedContent = ClassLoaderContent(apiUrls.map(u => ClassPathEntry.Url(u)))
        val mainContent = ClassLoaderContent(mainUrls.map(u => ClassPathEntry.Url(u)))
        val params = coursier.launcher.Parameters.Bootstrap(Seq(sharedContent, mainContent), mainClass)
          .withDeterministic(true)
          .withPreambleOpt(None)
        val tmpFile = {
          val prefix = "ammonite-" + versions.ammoniteVersion
          val suffix = ".jar"
          tmpDir match {
            case None => Files.createTempFile(prefix, suffix)
            case Some(tmpDir0) => Files.createTempFile(tmpDir0, prefix, suffix)
          }
        }
        if (deleteTmpFile)
          Runtime.getRuntime.addShutdownHook(
            new Thread {
              setDaemon(true)
              override def run(): Unit =
                try Files.deleteIfExists(tmpFile)
                catch {
                  case e: FileSystemException if Command.isWindows =>
                    System.err.println(s"Ignored error while deleting temporary file $tmpFile: $e")
                }
            }
          )
        BootstrapGenerator.generate(params, tmpFile)

        Command(
          Seq(tmpFile.toFile),
          // FIXME Get mainClass from coursier-launcher lib
          "coursier.bootstrap.launcher.Launcher"
        )
      }
    }
  }

}

object AmmoniteFetcher {
  private val splitter = "[-.]".r
  // should only work fine for versions whose 4 first "elements" are made of integers,
  // such as '2.3.8-32-…' or '2.0.4', but not '2.2-M1' or '3.0.0-RC4'.
  private def compareVersions(a: String, b: String): Int = {
    def toInt(s: String): Int =
      if (s.nonEmpty && s.forall(_.isDigit)) s.toInt
      else Int.MaxValue
    def itemize(v: String): (Int, Int, Int, Int) =
      splitter.split(v) match {
        case Array(a, b, c, d, _*) => (toInt(a), toInt(b), toInt(c), toInt(d))
        case Array(a, b, c) => (toInt(a), toInt(b), toInt(c), 0)
        case Array(a, b) => (toInt(a), toInt(b), 0, 0)
        case Array(a) => (toInt(a), 0, 0, 0)
        case Array() => (0, 0, 0, 0)
      }
    Ordering[(Int, Int, Int, Int)]
      .compare(itemize(a), itemize(b))
  }
}
