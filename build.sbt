name := """pim-aid"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.webjars"                   %%  "webjars-play"        % "2.2.2",
  "com.typesafe.play"             %%  "play-slick"          % "0.8.0",
  "com.adrianhurt"                %%  "play-bootstrap3"     % "0.4",
  "com.rockymadden.stringmetric"  %%  "stringmetric-core"   % "0.27.3",
  "com.github.tototoshi"          %%  "scala-csv"           % "1.1.2",
  "postgresql"                    %   "postgresql"          % "9.1-901-1.jdbc4",
  "net.sourceforge.htmlcleaner"   %   "htmlcleaner"         % "2.10",
  "com.github.rsschermer"         %%  "entitytled-core"     % "0.2.0",
  "org.webjars"                   %   "codemirror"          % "5.0"
)

fork in Test := false

lazy val root = (project in file(".")).enablePlugins(PlayScala)

includeFilter in (Assets, LessKeys.less) := "*.less"
