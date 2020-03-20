package ammrunner

import java.io.{File, InputStream, OutputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Files

import coursierapi.{Cache, Dependency, Fetch, Logger, ResolutionParams}
import coursier.launcher.{BootstrapGenerator, ClassLoaderContent, ClassPathEntry}

import scala.collection.JavaConverters._
import scala.io.{BufferedSource, Codec}

object AmmRunner {

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

  def versions(script: Iterator[String]): VersionsOption = {

    // TODO Read Ammonite options via the header, too

    val versionsMap = header(script)
      .map(_.stripPrefix("//"))
      .flatMap(_.split(","))
      .map(_.trim)
      .filter(s => s.startsWith("scala ") || s.startsWith("ammonite "))
      .map(_.split("\\s+", 2))
      .collect {
        case Array(k, v) => k -> v
      }
      .toMap

    VersionsOption(
      versionsMap.get("ammonite"),
      versionsMap.get("scala")
    )
  }

  def versions(script: File, charset: Charset): VersionsOption = {

    var s: BufferedSource = null
    val header0 = try {
      s = scala.io.Source.fromFile(script)(new Codec(charset))
      header(s.getLines()).toVector
    } finally {
      if (s != null)
        s.close()
    }

    versions(header0.iterator)
  }

  def versions(script: File): VersionsOption =
    // FIXME Use the default charset instead?
    versions(script, StandardCharsets.UTF_8)

  def command(versions: Versions): Either[AmmRunnerException, (Seq[File], String)] =
    command(versions, interpOnly = false)

  def command(versions: Versions, interpOnly: Boolean): Either[AmmRunnerException, (Seq[File], String)] = {

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

    def createFetcher(scalaVersion: String): Fetch =
      Fetch.create()
        .withCache(
          Cache.create()
            .withLogger(Logger.progressBars())
        )
        .withResolutionParams(
          ResolutionParams.create()
            .withScalaVersion(scalaVersion)
        )
        .withMainArtifacts()
        .addClassifiers("sources")

    def maybeResult(fetcher: Fetch) =
      try {
        Right(fetcher.fetchResult())
      } catch {
        case e: coursierapi.error.CoursierError =>
          Left(new CoursierError(s"Error fetching Ammonite ${versions.ammoniteVersion} for scala ${versions.scalaVersion}", e))
      }

    val mainClass = "ammonite.Main" // Get from META-INF/MANIFEST… if it's there?

    sys.props.get("amm-runner.coursier-fetch-cache") match {
      case Some(path) =>
        System.err.println(s"Fetch cache: $path")
        val fetcher = createFetcher(versions.scalaVersion)
          .addDependencies(mainDep)
          .withFetchCacheIKnowWhatImDoing(new File(path))
        maybeResult(fetcher)
          .map(res => (res.getFiles.asScala.toVector, mainClass))

      case None =>

        val apiFetcher = createFetcher(versions.scalaVersion)
          .addDependencies(apiDep)

        for {
          apiRes <- maybeResult(apiFetcher)
          apiDepVersions = apiRes
            .getDependencies
            .asScala
            .toVector
            .map(dep => dep.getModule -> dep.getVersion)
          fetcher = {
            val fetcher0 = createFetcher(versions.scalaVersion)
              .addDependencies(mainDep)
            fetcher0
              .withResolutionParams(
                fetcher0.getResolutionParams
                  .forceVersions(apiDepVersions.toMap.asJava)
              )
          }
          res <- maybeResult(fetcher)
        } yield {
          val apiUrls = apiRes.getArtifacts.asScala.toVector.map(_.getKey.getUrl)
          val mainUrls = res.getArtifacts.asScala.toVector.map(_.getKey.getUrl).filterNot(apiUrls.toSet)
          val sharedContent = ClassLoaderContent(apiUrls.map(u => ClassPathEntry.Url(u)))
          val mainContent = ClassLoaderContent(mainUrls.map(u => ClassPathEntry.Url(u)))
          val params = coursier.launcher.Parameters.Bootstrap(Seq(sharedContent, mainContent), mainClass)
            .withDeterministic(true)
            .withPreambleOpt(None)
          val tmpFile = Files.createTempFile("ammonite-" + versions.ammoniteVersion, ".jar")
          Runtime.getRuntime.addShutdownHook(
            new Thread {
              setDaemon(true)
              override def run(): Unit =
                Files.deleteIfExists(tmpFile)
            }
          )
          BootstrapGenerator.generate(params, tmpFile)

          (Seq(tmpFile.toFile), "coursier.bootstrap.launcher.Launcher") // FIXME Get mainClass from coursier-launcher lib
        }
    }
  }

  def command(script: File): Either[AmmRunnerException, (Seq[File], String)] = {

    val versionsOpt = versions(script).versions

    versionsOpt match {
      case None => Left(new MissingVersion(Some(script.getAbsolutePath)))
      case Some(versions) => command(versions)
    }
  }

  def run(versions: Versions, args: Seq[String], fork: Boolean): Either[AmmRunnerException, Unit] =
    command(versions).map {
      case (classPath, mainClass) =>
        Launch.launch(classPath, mainClass, args, fork)
    }

  def run(versions: Versions, args: Seq[String]): Either[AmmRunnerException, Unit] =
    run(versions, args, fork = false)

  def run(script: File, args: Seq[String]): Either[AmmRunnerException, Unit] = {

    val versionsOpt = versions(script).versions

    versionsOpt match {
      case None => Left(new MissingVersion(Some(script.getAbsolutePath)))
      case Some(versions) => run(versions, args)
    }
  }

  def runBg(
    versions: Versions,
    args: Array[String],
    mapBuilder: ProcessBuilder => ProcessBuilder
  ): Either[AmmRunnerException, Process] =
    command(versions).map {
      case (classPath, mainClass) =>
        Launch.launchBg(classPath, mainClass, args, mapBuilder)
    }

  def runBg(script: File, args: Array[String]): Either[AmmRunnerException, Process] = {

    val versionsOpt = versions(script).versions

    versionsOpt match {
      case None => Left(new MissingVersion(Some(script.getAbsolutePath)))
      case Some(versions) => runBg(versions, args, identity)
    }
  }

}
