package ammrunner

import java.io.{File, InputStream, OutputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Path}

import coursierapi.{Cache, Dependency, Fetch, Logger, ResolutionParams}
import coursier.launcher.{BootstrapGenerator, ClassLoaderContent, ClassPathEntry}
import dataclass._

import scala.collection.JavaConverters._
import scala.io.{BufferedSource, Codec}

@data class AmmoniteFetcher(
  versions: Versions,
  interpOnly: Boolean = true,
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

    val mainDep = Dependency.of(
      "com.lihaoyi",
      if (interpOnly)
        "ammonite-interp_" + versions.scalaVersion
      else
        "ammonite_" + versions.scalaVersion,
      versions.ammoniteVersion
    )

    def apiDep = Dependency.of(
      "com.lihaoyi",
      if (interpOnly)
        "ammonite-interp-api_" + versions.scalaVersion
      else
        "ammonite-repl-api_" + versions.scalaVersion,
      versions.ammoniteVersion
    )

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
        .addDependencies(mainDep)
        .withFetchCacheIKnowWhatImDoing(fetchCacheIKnowWhatImDoing.orNull)
      maybeResult(fetcher)
        .right
        .map(res => Command(res.getFiles.asScala.toVector, mainClass))
    } else {

      val apiFetcher = createFetcher()
        .addDependencies(apiDep)

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
              .addDependencies(mainDep)
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
                Files.deleteIfExists(tmpFile)
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
