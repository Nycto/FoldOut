name := "FoldOut"

scalaVersion := "2.10.0-RC5"

version := "0.1"

// Compiler flags
scalacOptions ++= Seq("-deprecation", "-feature")

// Repositories in which to find dependencies
resolvers ++= Seq(
    "Specs Repository" at "http://oss.sonatype.org/content/repositories/releases"
)

// Application dependencies
libraryDependencies ++= Seq(
    "com.ning" % "async-http-client" % "1.7.9",
    "commons-codec" % "commons-codec" % "1.7",
    "org.specs2" %% "specs2" % "1.12.3" % "test"
)

