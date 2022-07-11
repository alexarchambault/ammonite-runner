package ammrunner

import java.io.{File, InputStream, OutputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, FileSystemException, Path}

import coursierapi.{Cache, Dependency, Fetch, Logger, ResolutionParams, Version}
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
  transformFetch: Option[Fetch => Fetch] = None,
  @since
  thin: Boolean = true
) {

  def withTmpDir(tmpDir: Path): AmmoniteFetcher =
    withTmpDir(Some(tmpDir))

  def command(): Either[AmmoniteFetcherException, Command] = {

    def createFetcher(): Fetch = {
      val cache = Cache.create()
      if (progressBars)
        cache.withLogger(Logger.progressBars())
      val fetch = Fetch.create()
        .withCache(cache)
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

    lazy val fullDeps = {
      // ideally, we'd rather only resolve things (that is, get dependency graph via POM files),
      // instead of fetching JARs too, but the coursier Java API (coursierapi.*) doesn't allow it for now.
      val res = maybeResult {
        createFetcher()
          .addDependencies(Dependency.of("com.lihaoyi", "ammonite_" + versions.scalaVersion, versions.ammoniteVersion))
      }
      res.fold(ex => throw ex, identity).getDependencies.asScala.toVector
    }

    def ammoniteDep(name: String) = {
      def default = Dependency.of(
        "com.lihaoyi",
        s"ammonite-${name}_" + versions.scalaVersion,
        versions.ammoniteVersion
      )
      if (versions.scalaVersion.startsWith("2."))
        default
      else {
        val ammDeps = fullDeps.filter { dep =>
          val name0 = dep.getModule.getName
          dep.getModule.getOrganization == "com.lihaoyi" &&
          name0.startsWith("ammonite-") || name0.startsWith("ammonite_")
        }
        ammDeps
          .find { dep =>
            dep.getModule.getName.contains(s"-${name}_")
          }
          .getOrElse {
            sys.error(s"Module ammonite-$name not found (available modules: ${ammDeps.sortBy(_.getModule.getName)})")
          }
      }
    }

    val compilerDeps =
      if (Version.compare(versions.ammoniteVersion, "2.3.8-32-64308dc3") < 0)
        Nil
      else
        Seq(ammoniteDep("compiler"))

    val mainDeps =
      if (interpOnly)
        Seq(ammoniteDep("interp")) ++ compilerDeps
      else {
        val main = Dependency.of(
          "com.lihaoyi",
          "ammonite_" + versions.scalaVersion,
          versions.ammoniteVersion
        )
        Seq(main)
      }

    def apiDeps =
      Seq(ammoniteDep(if (interpOnly) "interp-api" else "repl-api")) ++ compilerDeps

    val mainClass = if (Version.compare(versions.ammoniteVersion, "2.5.0") < 0)
      "ammonite.Main" else "ammonite.AmmoniteMain" // Get from META-INF/MANIFEST… if it's there?

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
