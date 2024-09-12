
import sbt._
import sbt.Def.setting
import sbt.Keys.scalaVersion

object Deps {

  def caseApp = "com.github.alexarchambault" %% "case-app" % "2.0.6"
  def coursierInterface = "io.get-coursier" % "interface" % "1.0.20"
  def coursierLauncher = "io.get-coursier" %% "coursier-launcher" % "2.1.10"
  def dataClass = "io.github.alexarchambault" %% "data-class" % "0.2.6"
  def macroParadise = "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
  def osLib = "com.lihaoyi" %% "os-lib" % "0.9.0"
  def utest = "com.lihaoyi" %% "utest" % "0.8.1"
}
