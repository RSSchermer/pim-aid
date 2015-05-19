name := "pim-aid"

version := "1.0.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.webjars"                   %% "webjars-play"        % "2.2.2",
  "com.typesafe.play"             %% "play-slick"          % "0.8.0",
  "com.adrianhurt"                %% "play-bootstrap3"     % "0.4",
  "com.rockymadden.stringmetric"  %% "stringmetric-core"   % "0.27.3",
  "com.github.tototoshi"          %% "scala-csv"           % "1.1.2",
  "postgresql"                    %  "postgresql"          % "9.1-901-1.jdbc4",
  "net.sourceforge.htmlcleaner"   %  "htmlcleaner"         % "2.10",
  "com.github.rsschermer"         %% "entitytled-core"     % "0.5.0",
  "org.webjars"                   %  "codemirror"          % "5.0",
  "org.webjars"                   %  "font-awesome"        % "4.3.0",
  "org.webjars"                   %  "lodash"              % "2.4.1",
  "org.webjars"                   %  "angularjs"           % "1.3.0",
  "org.webjars"                   %  "restangular"         % "1.3.1",
  "org.scalatestplus"             %% "play"                % "1.1.0"   % "test"
)

fork in Test := false

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  javaOptions in Test += "-Dconfig.file=conf/test.conf"
)

includeFilter in (Assets, LessKeys.less) := "*.less"
