package ammrunner

import utest._

object AmmRunnerTests extends TestSuite {

  val tests = Tests {

    "both versions" - {
      val content =
        """#!/usr/bin/env amm
          |// scala 2.14.3, ammonite 4.1.2
          |val n = 2
          |""".stripMargin

      val result = AmmRunner.versions(content.linesIterator)
      val expected = VersionsOption(Some("4.1.2"), Some("2.14.3"))

      assert(result == expected)
    }

    "only scala" - {
      val content =
        """#!/usr/bin/env amm
          |// scala 2.14.3
          |val n = 2
          |""".stripMargin

      val result = AmmRunner.versions(content.linesIterator)
      val expected = VersionsOption(None, Some("2.14.3"))

      assert(result == expected)
    }

    "only Ammonite" - {
      val content =
        """#!/usr/bin/env amm
          |// ammonite 4.4.3
          |val n = 2
          |""".stripMargin

      val result = AmmRunner.versions(content.linesIterator)
      val expected = VersionsOption(Some("4.4.3"), None)

      assert(result == expected)
    }

    "no versions" - {
      val content =
        """#!/usr/bin/env amm
          |val n = 2
          |""".stripMargin

      val result = AmmRunner.versions(content.linesIterator)
      val expected = VersionsOption(None, None)

      assert(result == expected)
    }

  }

}
