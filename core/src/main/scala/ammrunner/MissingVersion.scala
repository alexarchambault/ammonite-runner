package ammrunner

class MissingVersion(pathOpt: Option[String])
  extends AmmoniteFetcherException("Missing Scala or Ammonite version" + pathOpt.map(p => s" in $p").mkString)
