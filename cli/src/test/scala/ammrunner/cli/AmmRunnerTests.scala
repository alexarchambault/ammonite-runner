package ammrunner.cli

import utest._

object AmmRunnerTests extends TestSuite {

  val launcherPath = sys.props("ammrunner.launcher")

  val tests = Tests {

    val isWindows = System.getProperty("os.name")
      .toLowerCase(java.util.Locale.ROOT)
      .contains("windows")

    def runTest(
      scalaVer: String,
      ammVer: String,
      fork: Boolean = false,
      extraArgs: Seq[String] = Nil
    ): Unit = {
      val code = """println(s"Scala ${scala.util.Properties.versionNumberString} Ammonite ${ammonite.Constants.version}")"""
      val codeArg =
        if (isWindows) "\\\"" + code.replace("\"", "\\\\\\\"") + "\\\""
        else code
      val forkArg = if (fork) Seq("--fork") else Nil
      val output = TestUtil.outputOf(Seq(launcherPath, "--amm", ammVer, "--scala", scalaVer) ++ forkArg ++ Seq("-c", codeArg) ++ extraArgs).trim
      val expectedOutput = s"Scala $scalaVer Ammonite $ammVer"
      assert(output == expectedOutput)
    }

    test("default") {
      test("amm 2.0.4") {
        test("scala 2.13.1") - runTest("2.13.1", "2.0.4")
      }
      test("amm 2.3.8-4-88785969") {
        test("scala 2.13.4") - runTest("2.13.4", "2.3.8-4-88785969")
      }
      test("amm 2.3.8-32-64308dc3") {
        test("scala 2.12.13") - runTest("2.12.13", "2.3.8-32-64308dc3")
      }
      test("amm 2.3.8-36-1cce53f3") {
        test("scala 2.12.13") - runTest("2.12.13", "2.3.8-36-1cce53f3")
        test("scala 2.13.4") - runTest("2.13.4", "2.3.8-36-1cce53f3")
      }
      test("amm 2.3.8-36-1cce53f3 thin") {
        test("scala 2.12.13") - runTest("2.12.13", "2.3.8-36-1cce53f3", extraArgs = Seq("--thin"))
        test("scala 2.13.4") - runTest("2.13.4", "2.3.8-36-1cce53f3", extraArgs = Seq("--thin"))
      }
    }

    test("fork") {
      test("amm 2.0.4") {
        test("scala 2.13.1") - runTest("2.13.1", "2.0.4", fork = true)
      }
      test("amm 2.3.8-4-88785969") {
        test("scala 2.13.4") - runTest("2.13.4", "2.3.8-4-88785969", fork = true)
      }
      test("amm 2.3.8-32-64308dc3") {
        test("scala 2.12.13") - runTest("2.12.13", "2.3.8-32-64308dc3", fork = true)
      }
      test("amm 2.3.8-36-1cce53f3") {
        test("scala 2.12.13") - runTest("2.12.13", "2.3.8-36-1cce53f3", fork = true)
        test("scala 2.13.4") - runTest("2.13.4", "2.3.8-36-1cce53f3", fork = true)
      }
      test("amm 2.3.8-36-1cce53f3 thin") {
        test("scala 2.12.13") - runTest("2.12.13", "2.3.8-36-1cce53f3", fork = true, extraArgs = Seq("--thin"))
        test("scala 2.13.4") - runTest("2.13.4", "2.3.8-36-1cce53f3", fork = true, extraArgs = Seq("--thin"))
      }
    }

  }

}