package ammrunner

class NoMatchingScalaAmmoniteVersionsFound(val foundScalaVersions: Seq[String])
  extends AmmoniteFetcherException("No matching scala and Ammonite versions found")
