name := "vdm-scraper"

version := "0.1"

scalaVersion := "2.12.4"

lazy val api = project.in(file("api"))
  .settings(
    name := "vdm-api",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.10",
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.typesafe.akka" %% "akka-actor"  % "2.5.4",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
      "ch.megard" %% "akka-http-cors" % "0.2.2",
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    )
  )

lazy val scraper = project.in(file("scraper"))
  .settings(
    name := "vdm-scraper",
    libraryDependencies ++= Seq(

    )
  )
