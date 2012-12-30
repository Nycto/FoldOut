name := "FoldOut"

scalaVersion := "2.9.2"

version := "0.1"

// append -deprecation to the options passed to the Scala compiler
scalacOptions += "-deprecation"

// Repositories in which to find dependencies
resolvers ++= Seq(
    "Specs Repository" at "http://oss.sonatype.org/content/repositories/releases"
)

// Application dependencies
libraryDependencies ++= Seq(
    "com.ning" % "async-http-client" % "1.7.9",
    "commons-codec" % "commons-codec" % "1.7",
    "org.specs2" %% "specs2" % "1.12.1" % "test"
)

