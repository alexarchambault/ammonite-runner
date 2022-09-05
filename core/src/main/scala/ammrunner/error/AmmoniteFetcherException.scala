package ammrunner.error

abstract class AmmoniteFetcherException(msg: String = null, cause: Throwable = null)
  extends Exception(msg, cause)
