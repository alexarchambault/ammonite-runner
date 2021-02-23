package ammrunner.cli

import java.io._

object TestUtil {
  def outputOf(cmd: Seq[String]): String = {
    // stuff in scala.sys.process should allow to do that in a straightforward way
    // not using it here to circumvent https://github.com/scala/bug/issues/9824

    val b = new ProcessBuilder(cmd: _*)
    b.redirectOutput(ProcessBuilder.Redirect.PIPE)
    b.redirectError(ProcessBuilder.Redirect.INHERIT)
    val p = b.start()

    // Closing stdin so that sbt doesn't wait indefinitely for input
    p.getOutputStream.close()

    // inspired by https://stackoverflow.com/a/16714180/3714539
    val reader = new BufferedReader(new InputStreamReader(p.getInputStream))
    val builder = new StringBuilder
    var line: String = null
    while ({ line = reader.readLine(); line != null }) {
      builder.append(line)
      builder.append(sys.props("line.separator"))
    }
    val result = builder.toString
    val retCode = p.waitFor()
    if (retCode != 0)
      sys.error(s"Error running ${cmd.mkString(" ")} (return code: $retCode)")
    result
  }
}
