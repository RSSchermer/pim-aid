name := "pim-aid"

version := "1.0.0"

scalaVersion := "2.11.6"

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  // Database access
  "com.typesafe.play"       %% "play-slick"             % "1.0.0",
  "com.typesafe.play"       %% "play-slick-evolutions"  % "1.0.0",
  "com.github.rsschermer"   %% "entitytled-core"        % "0.7.2",
  "org.postgresql"          %  "postgresql"             % "9.4-1201-jdbc41",

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
  "org.webjars"   %  "select2"        % "3.5.2",

  // Testing
  "org.scalatest"       %% "scalatest"  % "2.2.4"     % "test",
  "org.scalatestplus"   %% "play"       % "1.4.0-M4"  % "test",
  "com.h2database"      %   "h2"        % "1.4.188"   % "test"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  javaOptions in Test += "-Dconfig.file=conf/test.conf"
)

includeFilter in (Assets, LessKeys.less) := "*.less"

routesImport ++= Seq(
  "binders._",
  "model.PIMAidDBContext._"
)

TwirlKeys.templateImports ++= Seq(
  "scala.concurrent.ExecutionContext",
  "model.PIMAidDBContext._"
)

fork in run := false
fork in Test := false