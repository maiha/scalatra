import sbt._
import Keys._
import scala.xml._
import com.github.siasia.WebPlugin.{webSettings, jettyPort}

object ScalatraBuild extends Build {
  val description = SettingKey[String]("description")

  val scalatraSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalatra",
    version := "2.0.0-SNAPSHOT",
    crossScalaVersions := Seq("2.9.0-1", "2.9.1.RC2", "2.9.0"),
    scalaVersion <<= (crossScalaVersions) { versions => versions.head },
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    packageOptions <<= (packageOptions, name, version, organization) map {
      (opts, title, version, vendor) =>
        opts :+ Package.ManifestAttributes(
          "Created-By" -> "Simple Build Tool",
          "Built-By" -> System.getProperty("user.name"),
          "Build-Jdk" -> System.getProperty("java.version"),
          "Specification-Title" -> title,
          "Specification-Version" -> version,
          "Specification-Vendor" -> vendor,
          "Implementation-Title" -> title,
          "Implementation-Version" -> version,
          "Implementation-Vendor-Id" -> vendor,
          "Implementation-Vendor" -> vendor
        )
    },
    pomExtra <<= (pomExtra, name, description) { (extra, name, desc) => extra ++ Seq(
      <name>{name}</name>,
      <description>{desc}</description>,
      <url>http://scalatra.org</url>,
      <licenses>
        <license>
          <name>BSD</name>
          <url>http://github.com/scalatra/scalatra/raw/HEAD/LICENSE</url>
          <distribution>repo</distribution>
        </license>
      </licenses>,
      <scm>
        <url>http://github.com/scalatra/scalatra</url>
        <connection>scm:git:git://github.com/scalatra/scalatra.git</connection>
      </scm>,
      <developers>
        <developer>
          <id>riffraff</id>
          <name>Gabriele Renzi</name>
          <url>http://www.riffraff.info</url>
        </developer>
        <developer>
          <id>alandipert</id>
          <name>Alan Dipert</name>
          <url>http://alan.dipert.org</url>
        </developer>
        <developer>
          <id>rossabaker</id>
          <name>Ross A. Baker</name>
          <url>http://www.rossabaker.com/</url>
        </developer>
        <developer>
          <id>chirino</id>
          <name>Hiram Chirino</name>
          <url>http://hiramchirino.com/blog/</url>
        </developer>
        <developer>
          <id>casualjim</id>
          <name>Ivan Porto Carrero</name>
          <url>http://flanders.co.nz/</url>
        </developer>
        <developer>
          <id>jlarmstrong</id>
          <name>Jared Armstrong</name>
          <url>http://www.jaredarmstrong.name/</url>
        </developer>
      </developers>
    )},
    publishTo <<= (version) { version: String =>
      val nexus = "http://nexus-direct.scala-tools.org/content/repositories/"
      if (version.trim.endsWith("SNAPSHOT"))
        Some("Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      else
        Some("Sonatype Nexus Release Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    }
  )

  val sonatypeSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

  object Dependencies {
    def antiXml(scalaVersion: String) = {
      val libArtifactId = scalaVersion match {
        case "2.9.1.RC2" => "anti-xml_2.9.0"
        case "2.9.0-1" => "anti-xml_2.9.0"
        case x => "anti-xml_"+x
      }
      "com.codecommit" % libArtifactId % "0.2"
    }

    val base64 = "net.iharder" % "base64" % "2.3.8"

    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1"

    val commonsIo = "commons-io" % "commons-io" % "2.0.1"

    private def jettyDep(name: String) = "org.eclipse.jetty" % name % "7.4.5.v20110725"
    val testJettyServlet = jettyDep("test-jetty-servlet")
    val jettyWebsocket = jettyDep("jetty-websocket")
    val jettyWebapp = jettyDep("jetty-webapp")

    val junit = "junit" % "junit" % "4.8.2"

    def liftJson(scalaVersion: String) = {
      val libArtifactId = scalaVersion match {
        case "2.9.1.RC2" => "lift-json_2.9.0"
        case x => "lift-json_"+x
      }
      "net.liftweb" % libArtifactId % "2.4-M3"
    }

    def scalate(scalaVersion: String) = {
      "org.fusesource.scalate" % "scalate-core" % "1.5.1"
    }

    def scalatest(scalaVersion: String) = {
      val libArtifactId = scalaVersion match {
        case "2.9.1.RC2" => "scalatest_2.9.0"
        case x => "scalatest_"+x
      }
      "org.scalatest" % libArtifactId % "1.6.1"
    }

    def specs(scalaVersion: String) = {
      val libArtifactId = scalaVersion match {
        case "2.9.1.RC2" => "specs_2.9.0"
        case "2.9.0-1" => "specs_2.9.0"
        case x => "specs_"+x
      }
      "org.scala-tools.testing" % libArtifactId % "1.6.8"
    }

    def specs2(scalaVersion: String) = {
      val libArtifactId = scalaVersion match {
        case "2.9.1.RC2" => "specs2_2.9.0-1"
        case x => "specs2_"+x
      }
      val libVersion = scalaVersion match {
        case "2.9.0" => "1.3"
        case x => "1.5"
      }
      "org.specs2" % libArtifactId % libVersion
    }

    val servletApi = "javax.servlet" % "servlet-api" % "2.5" % "provided"

    def socketioCore(version: String) = "org.scalatra.socketio-java" % "socketio-core" % version

    val testng = "org.testng" % "testng" % "6.1.1" % "optional"
  }
  import Dependencies._

  lazy val scalatraProject = Project("scalatra-project", file("."),
    settings = scalatraSettings ++ Unidoc.settings)
    .settings(
      publishArtifact in Compile := false,
      description := "A tiny, Sinatra-like web framework for Scala")
    .aggregate(scalatraCore, scalatraAuth, scalatraFileupload,
      scalatraScalate, scalatraSocketio, scalatraLiftJson, scalatraAntiXml,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
      scalatraExample)

  lazy val scalatraCore = Project("scalatra", file("core"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies := Seq(servletApi),
      description := "The core Scalatra framework")
    .testWithScalatraTest

  lazy val scalatraAuth = Project("scalatra-auth", file("auth"),
    settings = scalatraSettings)
    .settings(
       libraryDependencies := Seq(servletApi, base64),
       description := "Scalatra authentication module")
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraFileupload = Project("scalatra-fileupload", file("fileupload"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies := Seq(servletApi, commonsFileupload, commonsIo),
      description := "Commons-Fileupload integration with Scalatra")
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraScalate = Project("scalatra-scalate", file("scalate"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (scalaVersion, libraryDependencies) {
        (sv, deps) => deps ++ Seq(scalate(sv), servletApi)
      },
      description := "Scalate integration with Scalatra")
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraSocketio = Project("scalatra-socketio", file("socketio"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (version, libraryDependencies) {
        (v, deps) => deps ++ Seq(jettyWebsocket, socketioCore(v))
      },
      resolvers += sonatypeSnapshots,
      description := "Socket IO support for Scalatra"
    )
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraLiftJson = Project("scalatra-lift-json", file("lift-json"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (scalaVersion, libraryDependencies) {
        (sv, deps) => deps ++ Seq(liftJson(sv), servletApi)
      },
      description := "Lift JSON support for Scalatra"
    )
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraAntiXml = Project("scalatra-anti-xml", file("anti-xml"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (scalaVersion, libraryDependencies) {
        (sv, deps) => deps ++ Seq(antiXml(sv), servletApi)
      },
      description := "Anti-XML support for Scalatra")
    .dependsOn(scalatraCore)
    .testWithScalatraTest

  lazy val scalatraTest = Project("scalatra-test", file("test"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies := Seq(testJettyServlet),
      description := "The abstract Scalatra test framework")

  lazy val scalatraScalatest = Project("scalatra-scalatest", file("scalatest"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (scalaVersion, libraryDependencies) {
        (sv, deps) => deps ++ Seq(scalatest(sv), junit, testng)
      },
      description := "ScalaTest support for the Scalatra test framework")
    .dependsOn(scalatraTest)

  lazy val scalatraSpecs = Project("scalatra-specs", file("specs"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (scalaVersion, libraryDependencies) {
        (sv, deps) => deps ++ Seq(specs(sv))
      },
      description := "Specs support for the Scalatra test framework")
    .dependsOn(scalatraTest)

  lazy val scalatraSpecs2 = Project("scalatra-specs2", file("specs2"),
    settings = scalatraSettings)
    .settings(
      libraryDependencies <<= (scalaVersion, libraryDependencies) {
        (sv, deps) => deps ++ Seq(specs2(sv))
      },
      description := "Specs2 support for the Scalatra test framework")
    .dependsOn(scalatraTest)

  lazy val scalatraExample = Project("scalatra-example", file("example"),
    settings = scalatraSettings)
    .settings(webSettings :_*)
    .settings(
      resolvers += sonatypeSnapshots,
      libraryDependencies := Seq(servletApi, jettyWebapp % "jetty"),
      description := "Scalatra example project",
      publish := {},
      publishLocal := {})
    .dependsOn(scalatraCore, scalatraScalate, scalatraAuth, scalatraFileupload,
               scalatraSocketio)

  class RichProject(project: Project) {
    def testWithScalatraTest = {
      val testProjects = Seq(scalatraScalatest, scalatraSpecs, scalatraSpecs2)
      val testDeps = testProjects map { _ % "test" }
      project.dependsOn(testDeps : _*)
    }
  }
  implicit def project2RichProject(project: Project): RichProject = new RichProject(project)
}
