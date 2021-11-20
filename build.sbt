name := "gh_contributors"
 
version := "1.0" 
      
lazy val `gh_contributors` = (project in file(".")).enablePlugins(PlayScala)

      
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
      
scalaVersion := "2.13.7"

libraryDependencies ++= Seq(
  caffeine,
  ws,
  //scalatest % Test,
  guice,
  "org.typelevel" %% "cats-core" % "2.6.1" // for aggregating contributions
)

scalacOptions ++= Seq(
  "-Xfatal-warnings"
)