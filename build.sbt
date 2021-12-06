name := "gh_contributors"
 
version := "1.0" 
      
lazy val `gh_contributors` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.7"

libraryDependencies ++= Seq(
  ehcache,
  ws,
  guice,
  "org.typelevel" %% "cats-core" % "2.6.1", // for aggregating contributions at contributor level
  "dev.zio" %% "zio" % "1.0.12", // to cancel other parallel processes if one fails
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test"
)

scalacOptions ++= Seq(
  "-Xfatal-warnings"
)