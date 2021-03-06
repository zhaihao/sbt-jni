import scala.sys.process._

val scalaVersions = Seq("2.12.4", "2.11.12", "2.10.6")
val macrosParadiseVersion = "2.1.0"

// version is derived from latest git tag
version in ThisBuild := ("git describe --always --dirty=-SNAPSHOT --match v[0-9].*" !!).tail.trim
organization in ThisBuild := "ch.jodersky"
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint"
)
licenses in ThisBuild := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause")))

lazy val root = (project in file("."))
  .aggregate(macros, plugin)
  .settings(
    publish := {},
    publishLocal := {},
    // make sbt-pgp happy
    publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo")),
    addCommandAlias("test-plugin", ";+macros/publishLocal;scripted")
  )

lazy val macros = (project in file("macros"))
  .disablePlugins(ScriptedPlugin)
  .settings(
    name := "sbt-jni-macros",
    scalaVersion := scalaVersions.head,
    crossScalaVersions := scalaVersions,
    addCompilerPlugin("org.scalamacros" % "paradise" % macrosParadiseVersion cross CrossVersion.full),
    libraryDependencies += "org.typelevel" %% "macro-compat" % "1.1.1",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )

lazy val plugin = (project in file("plugin"))
  .settings(
    name := "sbt-jni",
    sbtPlugin := true,
    publishMavenStyle := false,
    libraryDependencies += "org.ow2.asm" % "asm" % "6.0",
    // make project settings available to source
    sourceGenerators in Compile += Def.task {
      val src = s"""|/* Generated by sbt */
                    |package ch.jodersky.sbt.jni
                    |
                    |private[jni] object ProjectVersion {
                    |  final val MacrosParadise = "${macrosParadiseVersion}"
                    |  final val Macros = "${version.value}"
                    |}
                    |""".stripMargin
      val file = sourceManaged.value / "ch" / "jodersky" / "sbt" / "jni" / "ProjectVersion.scala"
      IO.write(file, src)
      Seq(file)
    }.taskValue,
    scriptedLaunchOpts := Seq(
      "-Dplugin.version=" + version.value,
      "-XX:MaxPermSize=256m", "-Xmx2g", "-Xss2m"
    )
  )
