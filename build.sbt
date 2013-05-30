name := "FoldOut"

organization := "com.roundeights"

version := "0.1"

scalaVersion := "2.10.1"

// Compiler flags
scalacOptions ++= Seq("-deprecation", "-feature")

// Repositories in which to find dependencies
resolvers ++= Seq(
    "Specs Repository" at "http://oss.sonatype.org/content/repositories/releases"
)

// Application dependencies
libraryDependencies ++= Seq(
    "com.roundeights" %% "scalon" % "0.1",
    "com.roundeights" %% "hasher" % "1.0.0",
    "com.ning" % "async-http-client" % "1.7.9",
    "org.slf4j" % "slf4j-simple" % "1.7.2",
    "commons-codec" % "commons-codec" % "1.7",
    "org.specs2" %% "specs2" % "1.13" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

