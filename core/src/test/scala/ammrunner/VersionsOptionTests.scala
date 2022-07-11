package ammrunner

import utest._

object VersionsOptionTests extends TestSuite {

  val tests = Tests {

    test("both versions") {
      val content =
        """#!/usr/bin/env amm
          |// scala 2.14.3, ammonite 4.1.2
          |val n = 2
          |""".stripMargin

      val result = VersionsOption.fromScript(content.linesIterator)
      val expected = VersionsOption(Some("4.1.2"), Some("2.14.3"))

      assert(result == expected)
    }

    test("both versions, case insensitive") {
      test {
        val content =
          """#!/usr/bin/env amm
            |// Scala 2.14.3, ammonite 4.1.2
            |val n = 2
            |""".stripMargin

        val result = VersionsOption.fromScript(content.linesIterator)
        val expected = VersionsOption(Some("4.1.2"), Some("2.14.3"))

        assert(result == expected)
      }
      test {
        val content =
          """#!/usr/bin/env amm
            |// scala 2.14.3-RC2, Ammonite 4.1.2
            |val n = 2
            |""".stripMargin

        val result = VersionsOption.fromScript(content.linesIterator)
        val expected = VersionsOption(Some("4.1.2"), Some("2.14.3-RC2"))

        assert(result == expected)
      }
      test {
        val content =
          """#!/usr/bin/env amm
            |// SCALA 2.14.3, AmmonIte 4.1.2-M1
            |val n = 2
            |""".stripMargin

        val result = VersionsOption.fromScript(content.linesIterator)
        val expected = VersionsOption(Some("4.1.2-M1"), Some("2.14.3"))

        assert(result == expected)
      }
    }

    test("only scala") {
      val content =
        """#!/usr/bin/env amm
          |// scala 2.14.3
          |val n = 2
          |""".stripMargin

      val result = VersionsOption.fromScript(content.linesIterator)
      val expected = VersionsOption(None, Some("2.14.3"))

      assert(result == expected)
    }

    test("only Ammonite") {
      val content =
        """#!/usr/bin/env amm
          |// ammonite 4.4.3
          |val n = 2
          |""".stripMargin

      val result = VersionsOption.fromScript(content.linesIterator)
      val expected = VersionsOption(Some("4.4.3"), None)

      assert(result == expected)
    }

    test("no versions") {
      val content =
        """#!/usr/bin/env amm
          |val n = 2
          |""".stripMargin

      val result = VersionsOption.fromScript(content.linesIterator)
      val expected = VersionsOption(None, None)

      assert(result == expected)
    }

  }

}
