package ammrunner

import java.io.File
import java.net.URLClassLoader
import java.lang.reflect.{InvocationTargetException, Modifier}
import java.util.Locale

import dataclass._

import scala.annotation.tailrec

@data class Command(
  classPath: Seq[File],
  mainClass: String,
  args: Seq[String] = Nil,
  @since("0.2.6")
  jvmArgs: Option[Seq[String]] = None
) {
  import Command._

  def runBg(): Process =
    runBg(identity)
  def runBg(mapBuilder: ProcessBuilder => ProcessBuilder): Process =
    Jvm.fork(jvmArgs.getOrElse(Nil), classPath, mainClass, args, mapBuilder)

  def withJvmArgs(args: Seq[String]): Command =
    withJvmArgs(Some(args))
  def addJvmArgs(args: String*): Command =
    withJvmArgs(Some(jvmArgs.getOrElse(Seq.empty[String]) ++ args))

  def exec(): Unit = exec(forceFork = false)
  def exec(forceFork: Boolean): Unit =
    if (isNativeImage)
      Graalvm.launch(jvmArgs.getOrElse(Nil), classPath, mainClass, args)
    else if (forceFork) {
      val p = Jvm.fork(Nil, classPath, mainClass, args, identity)
      val retCode = p.waitFor()
      sys.exit(retCode)
    } else
      Jvm.launch(classPath, mainClass, args)
}

object Command {

  private[ammrunner] lazy val isWindows: Boolean =
    Option(System.getProperty("os.name"))
      .map(_.toLowerCase(Locale.ROOT))
      .exists(_.contains("windows"))

  private lazy val windowsPathExtensions =
    Option(System.getenv("pathext"))
      .toSeq
      .flatMap(_.split(File.pathSeparator).toSeq)

  def javaPath: String = {
    // No evecvp in com.oracle.svm.core.posix.headers.Unistd, so we're handling the path lookup logic ourselves

    val pathDirs = Option(System.getenv("PATH"))
      .toSeq
      .flatMap(_.split(File.pathSeparatorChar))
    val pathExtensions =
      if (isWindows) windowsPathExtensions else Seq("")

    val it = for {
      dir <- pathDirs.iterator
      ext <- pathExtensions.iterator
      file = new File(new File(dir), "java" + ext)
      if file.exists()
    } yield file

    if (it.hasNext)
      it.next().getAbsolutePath
    else
      throw new Exception("java executable not found")
  }

  def isNativeImage: Boolean =
    sys.props
      .get("org.graalvm.nativeimage.imagecode")
      .contains("runtime")


  private object Graalvm {
    import com.oracle.svm.core.headers.Errno
    import com.oracle.svm.core.posix.headers.Unistd
    import org.graalvm.nativeimage.c.`type`.CTypeConversion

    def execv(argc: String, argv: Seq[String]): Unit = {

      if (java.lang.Boolean.getBoolean("debug.execv"))
        System.err.println(s"Running\n$argc\n$argv\n")

      val argc0 = CTypeConversion.toCString(argc)
      val argv0 = CTypeConversion.toCStrings(argv.toArray)

      Unistd.execv(argc0.get(), argv0.get())
      val err = Errno.errno()
      val desc = CTypeConversion.toJavaString(Errno.strerror(err))
      throw new Exception(s"Error running $argc ${argv.mkString(" ")}: $desc")
    }

    def launch(
      jvmArgs: Seq[String],
      classpath: Seq[File],
      mainClass: String,
      args: Seq[String]
    ): Unit = {
      val args0 = Seq("java") ++ // not actually used
        jvmArgs ++
        Seq(
          "-cp", classpath.map(_.getAbsolutePath).mkString(File.pathSeparator),
          mainClass
        ) ++
        args
      execv(javaPath, args0)
    }
  }

  private object Jvm {

    def baseLoader: ClassLoader = {

      @tailrec
      def rootLoader(cl: ClassLoader): ClassLoader = {
        val par = cl.getParent
        if (par == null)
          cl
        else
          rootLoader(par)
      }

      rootLoader(ClassLoader.getSystemClassLoader)
    }


    def launch(classpath: Seq[File], mainClass: String, args: Seq[String]): Unit = {
      val cl = new URLClassLoader(classpath.map(_.toURI.toURL).toArray, baseLoader)
      val cls = cl.loadClass(mainClass) // throws ClassNotFoundException
      val method = cls.getMethod("main", classOf[Array[String]]) // throws NoSuchMethodException
      method.setAccessible(true)
      val isStatic = Modifier.isStatic(method.getModifiers)
      assert(isStatic)

      val thread = Thread.currentThread()
      val prevLoader = thread.getContextClassLoader
      try {
        thread.setContextClassLoader(cl)
        method.invoke(null, args.toArray)
      } catch {
        case e: InvocationTargetException =>
          throw Option(e.getCause).getOrElse(e)
      } finally {
        thread.setContextClassLoader(prevLoader)
      }
    }

    def fork(
      jvmArgs: Seq[String],
      classpath: Seq[File],
      mainClass: String,
      args: Seq[String],
      mapBuilder: ProcessBuilder => ProcessBuilder
    ): Process = {
      val cmd = Seq(javaPath) ++
        jvmArgs ++
        Seq(
          "-cp", classpath.map(_.getAbsolutePath).mkString(File.pathSeparator),
          mainClass
        ) ++
        args
      val builder = mapBuilder(new ProcessBuilder(cmd: _*).inheritIO())
      builder.start()
    }
  }

}
