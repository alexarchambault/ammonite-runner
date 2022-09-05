package ammrunner

import java.io.File

import dataclass._

import scala.util.Properties

@data class Command(
  classPath: Seq[File],
  mainClass: String,
  args: Seq[String] = Nil,
  @since("0.2.6")
  jvmArgs: Option[Seq[String]] = None
) {
  import Command._

  def run(): Process =
    run(identity)
  def run(mapBuilder: ProcessBuilder => ProcessBuilder): Process =
    Jvm.run(jvmArgs.getOrElse(Nil), classPath, mainClass, args, mapBuilder)

  def withJvmArgs(args: Seq[String]): Command =
    withJvmArgs(Some(args))
  def addJvmArgs(args: String*): Command =
    withJvmArgs(Some(jvmArgs.getOrElse(Seq.empty[String]) ++ args))

  def command: Seq[String] =
    Jvm.command(jvmArgs.getOrElse(Nil), classPath, mainClass, args)
}

object Command {

  private object Jvm {

    def command(
      jvmArgs: Seq[String],
      classpath: Seq[File],
      mainClass: String,
      args: Seq[String]
    ): Seq[String] = {
      val ext = if (Properties.isWin) ".exe" else ""
      Seq("java" + ext) ++
        jvmArgs ++
        Seq(
          "-cp", classpath.map(_.getAbsolutePath).mkString(File.pathSeparator),
          mainClass
        ) ++
        args
    }

    def run(
      jvmArgs: Seq[String],
      classpath: Seq[File],
      mainClass: String,
      args: Seq[String],
      mapBuilder: ProcessBuilder => ProcessBuilder
    ): Process = {
      val cmd = command(jvmArgs, classpath, mainClass, args)
      val builder = mapBuilder(new ProcessBuilder(cmd: _*).inheritIO())
      builder.start()
    }
  }

}
