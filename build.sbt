name := "pim-aid"

version := "1.0.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  // Database access
  "com.typesafe.play"       %% "play-slick"             % "1.0.0",
  "com.typesafe.play"       %% "play-slick-evolutions"  % "1.0.0",
  "com.github.rsschermer"   %% "entitytled-core"        % "0.7.0",
  "postgresql"              %  "postgresql"             % "9.1-901-1.jdbc4",

  // Misc
  "com.adrianhurt"                %% "play-bootstrap3"    % "0.4.2",
  "com.rockymadden.stringmetric"  %% "stringmetric-core"  % "0.27.4",

  // Web Jars
  "org.webjars"   %% "webjars-play"   % "2.4.0-1",
  "org.webjars"   %  "codemirror"     % "5.0",
  "org.webjars"   %  "font-awesome"   % "4.3.0",
  "org.webjars"   %  "lodash"         % "2.4.1",
  "org.webjars"   %  "angularjs"      % "1.3.0",
  "org.webjars"   %  "restangular"    % "1.3.1",
  "org.webjars"   %  "select2"        % "3.5.2"//,

  // Testing
  //"org.scalatestplus"   %% "play"   % "1.2.0"   % "test"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  javaOptions in Test += "-Dconfig.file=conf/test.conf"
)

includeFilter in (Assets, LessKeys.less) := "*.less"

TwirlKeys.templateImports ++= Seq(
  "scala.concurrent.ExecutionContext",
  "models.meta.Profile._"
)

fork in run := true
fork in Test := false