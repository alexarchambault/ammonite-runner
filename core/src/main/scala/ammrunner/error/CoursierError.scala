package ammrunner.error

class CoursierError(message: String, cause: coursierapi.error.CoursierError)
  extends AmmoniteFetcherException(message, cause)
