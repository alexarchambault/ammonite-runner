package ammrunner

import utest._
import java.nio.file.Files

object AmmoniteFetcherTests extends TestSuite {

  val tests = Tests {

    "thin" - {
      val tmpDir = Files.createTempDirectory("amm-runner-tests")
      val maybeCommand = AmmoniteFetcher(Versions("2.0.4", "2.12.10"))
        .withThin(true)
        .withTmpDir(tmpDir)
        .command()
      val command = maybeCommand match {
        case Left(e) => throw e
        case Right(c) => c
      }
      val (inTmpDir, outsideTmpDir) = command.classPath.partition(f => f.toPath.startsWith(tmpDir))
      assert(inTmpDir.nonEmpty)
      Predef.assert(outsideTmpDir.isEmpty, s"Found Ammonite class path files outside $tmpDir: $outsideTmpDir")
    }

    "non-thin" - {
      val maybeCommand = AmmoniteFetcher(Versions("2.0.4", "2.12.10"))
        .withThin(false)
        .command()
      val command = maybeCommand match {
        case Left(e) => throw e
        case Right(c) => c
      }
      val cacheBase = coursierapi.Cache.create().getLocation.toPath
      val (inCache, outsideCache) = command.classPath.partition(f => f.toPath.startsWith(cacheBase))
      assert(inCache.nonEmpty)
      Predef.assert(outsideCache.isEmpty, s"Found Ammonite class path files outside $cacheBase: $outsideCache")
    }

    "scala3" - {
      "former convention" - {
        val maybeCommand = AmmoniteFetcher(Versions("2.4.1", "3.0.2"))
          .withThin(false)
          .command()
        val command = maybeCommand match {
          case Left(e) => throw e
          case Right(c) => c
        }
        val cacheBase = coursierapi.Cache.create().getLocation.toPath
        val (inCache, outsideCache) = command.classPath.partition(f => f.toPath.startsWith(cacheBase))
        assert(inCache.nonEmpty)
        Predef.assert(outsideCache.isEmpty, s"Found Ammonite class path files outside $cacheBase: $outsideCache")
      }
      "new convention" - {
        val maybeCommand = AmmoniteFetcher(Versions("2.5.4-11-4f5bf2aa", "3.1.3"))
          .withThin(false)
          .command()
        val command = maybeCommand match {
          case Left(e) => throw e
          case Right(c) => c
        }
        val cacheBase = coursierapi.Cache.create().getLocation.toPath
        val (inCache, outsideCache) = command.classPath.partition(f => f.toPath.startsWith(cacheBase))
        assert(inCache.nonEmpty)
        Predef.assert(outsideCache.isEmpty, s"Found Ammonite class path files outside $cacheBase: $outsideCache")
      }
    }

  }

}
