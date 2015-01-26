name := """pim-aid"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.2",
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "org.webjars" % "bootstrap" % "3.0.2",
  "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.3",
  "com.github.tototoshi" %% "scala-csv" % "1.1.2",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.10",
  "com.github.julien-truffaut"  %%  "monocle-core"    % "1.0.0-M1",
  "com.github.julien-truffaut"  %%  "monocle-macro"   % "1.0.0-M1"
)

fork in Test := false

lazy val root = (project in file(".")).enablePlugins(PlayScala)

includeFilter in (Assets, LessKeys.less) := "*.less"

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value
    // in Scala 2.10, quasiquotes are provided by macro paradise
    case Some((2, 10)) =>
      libraryDependencies.value ++ Seq(
        compilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full),
        "org.scalamacros" %% "quasiquotes" % "2.0.0" cross CrossVersion.binary)
  }
}
