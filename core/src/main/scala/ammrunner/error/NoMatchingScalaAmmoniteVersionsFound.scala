package ammrunner.error

class NoMatchingScalaAmmoniteVersionsFound(val foundScalaVersions: Seq[String])
  extends AmmoniteFetcherException("No matching scala and Ammonite versions found")
