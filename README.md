# ammonite-runner

[![Build Status](https://travis-ci.org/alexarchambault/ammonite-runner.svg?branch=master)](https://travis-ci.org/alexarchambault/ammonite-runner)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.alexarchambault.ammonite/ammonite-runner_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.alexarchambault.ammonite/ammonite-runner_2.13)


Library and CLI to fetch and run Ammonite scripts

## Library

```scala
libraryDependencies += "io.github.alexarchambault.ammonite" %% "ammonite-runner" % "0.2.0"
```

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/io.github.alexarchambault.ammonite/ammonite-runner_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.alexarchambault.ammonite/ammonite-runner_2.13).

Example of use
```scala
import ammrunner._

val file: java.io.File = ???

val versions = VersionsOption.fromScript(file)
  // Latest matching scala / Ammonite versions if none are set in script
  .getOrElse(Versions.default())

val command = AmmoniteFetcher(versions)
  .command()
  .fold(throw _, identity)

val proc: Process = command
  .withArgs(Seq(file.getAbsolutePath))
  .runBg()

val retCode: Int = proc.waitFor()
```

## CLI

Example of use
```
$ cs launch io.github.alexarchambault.ammonite::ammonite-runner-cli:latest.release -- script.sc
```

## License

Copyright (c) 2020, Alexandre Archambault.

All files in this repository can be used either under the Apache 2.0 license.

