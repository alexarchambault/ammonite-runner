package ammrunner

class MissingVersion(pathOpt: Option[String])
  extends AmmRunnerException("Missing Scala or Ammonite version" + pathOpt.map(p => s" in $p").mkString)
