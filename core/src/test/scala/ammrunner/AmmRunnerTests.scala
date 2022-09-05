package ammrunner

import ammrunner.cli.AmmRunner
import utest._

import scala.util.Properties

object AmmRunnerTests extends TestSuite {

  val tests = Tests {

    def runTest(
      scalaVer: String,
      ammVer: String,
      extraArgs: Seq[String] = Nil,
      runtimeScalaVerOpt: Option[String] = None
    ): Unit = {
      val code = """println(s"Scala ${scala.util.Properties.versionNumberString} Ammonite ${ammonite.Constants.version}")"""
      val codeArg =
        if (Properties.isWin) "\"" + code.replace("\"", "\\\"") + "\""
        else code
      val command = AmmRunner.command(Some(ammVer), Some(scalaVer), Seq("-c", codeArg) ++ extraArgs).command
      val output = os.proc(command).call().out.trim()
      // If we are using Scala 3, this is actually spitting out 2.13 due to the
      // way it's published with 2.13/3 interop
      val outputScalaVer = runtimeScalaVerOpt.getOrElse(scalaVer)
      val expectedOutput = s"Scala $outputScalaVer Ammonite $ammVer"
      assert(output == expectedOutput)
    }

    test("amm 2.0.4") {
      test("scala 2.13.1") {
        runTest("2.13.1", "2.0.4")
      }
    }
    test("amm 2.3.8-4-88785969") {
      test("scala 2.13.4") {
        runTest("2.13.4", "2.3.8-4-88785969")
      }
    }
    test("amm 2.3.8-32-64308dc3") {
      test("scala 2.12.13") {
        runTest("2.12.13", "2.3.8-32-64308dc3")
      }
    }
    test("amm 2.3.8-36-1cce53f3") {
      test("scala 2.12.13") {
        runTest("2.12.13", "2.3.8-36-1cce53f3")
      }
      test("scala 2.13.4") {
        runTest("2.13.4", "2.3.8-36-1cce53f3")
      }
    }
    test("amm 2.3.8-36-1cce53f3 thin") {
      test("scala 2.12.13") {
        runTest("2.12.13", "2.3.8-36-1cce53f3", extraArgs = Seq("--thin"))
      }
      test("scala 2.13.4") {
        runTest("2.13.4", "2.3.8-36-1cce53f3", extraArgs = Seq("--thin"))
      }
    }

    test("scala3") {
      test("amm 2.4.1") {
        test("scala 3.0.1") {
          runTest("3.0.1", "2.4.1", runtimeScalaVerOpt = Some("2.13.6"))
        }
      }
      test("amm 2.4.1 thin") {
        test("scala 3.0.1") {
          runTest("3.0.1", "2.4.1", extraArgs = Seq("--thin"), runtimeScalaVerOpt = Some("2.13.6"))
        }
      }

      test("amm 2.5.4-11-4f5bf2aa") {
        test("scala 3.1.3") {
          runTest("3.1.3", "2.5.4-11-4f5bf2aa", runtimeScalaVerOpt = Some("2.13.8"))
        }
      }
    }
  }

}
