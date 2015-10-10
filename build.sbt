name := "FoldOut"

organization := "com.roundeights"

version := "0.1.1"

scalaVersion := "2.11.7"

// Compiler flags
scalacOptions ++= Seq("-deprecation", "-feature")

// Repositories in which to find dependencies
resolvers ++= Seq(
    "Specs Repository" at "http://oss.sonatype.org/content/repositories/releases",
    "RoundEights" at "http://maven.spikemark.net/roundeights"
)

publishTo := Some("Spikemark" at "https://spikemark.herokuapp.com/maven/roundeights")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// Application dependencies
libraryDependencies ++= Seq(
    "com.roundeights" %% "scalon" % "0.2",
    "com.roundeights" %% "hasher" % "1.2.0",
    "com.ning" % "async-http-client" % "1.9+",
    "org.slf4j" % "slf4j-simple" % "1.+",
    "commons-codec" % "commons-codec" % "1.+",
    "org.specs2" %% "specs2" % "2.3.+" % "test"
)

