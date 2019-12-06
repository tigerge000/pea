import Dependencies._

lazy val pea = Project("pea", file("."))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(
    peaCommon % "compile->compile;test->test",
    peaDubbo % "compile->compile;test->test",
    peaGrpc % "compile->compile;test->test",
  ).aggregate(peaCommon, peaDubbo, peaGrpc)

// pea-app dependencies
val gatlingVersion = "3.2.1"
val gatling = "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion exclude("io.gatling", "gatling-app")
val gatlingCompiler = "io.gatling" % "gatling-compiler" % gatlingVersion
val curator = "org.apache.curator" % "curator-recipes" % "2.12.0"
val oshiCore = "com.github.oshi" % "oshi-core" % "4.0.0"

libraryDependencies ++= Seq(gatling, gatlingCompiler, curator, oshiCore) ++ appPlayDeps
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test

// pea-common
lazy val peaCommon = subProject("pea-common")
  .settings(libraryDependencies ++= commonDependencies)

// pea-dubbo dependencies, specify javassist and jbossnetty deps because of coursier dep resolve problems
val dubbo = "com.alibaba" % "dubbo" % "2.6.5" excludeAll(ExclusionRule(organization = "org.springframework"), ExclusionRule(organization = "org.javassist"), ExclusionRule(organization = "org.jboss.netty"))
val dubboJavassist = "org.javassist" % "javassist" % "3.21.0-GA"
val dubboJbossNetty = "org.jboss.netty" % "netty" % "3.2.5.Final"
val dubboSpring = "org.springframework" % "spring-context" % "4.3.10.RELEASE" % Test
lazy val peaDubbo = subProject("pea-dubbo")
  .settings(libraryDependencies ++= Seq(
    gatling, dubbo, curator, dubboJavassist, dubboJbossNetty, dubboSpring
  ))

// pea-grpc
val grpcVersion = "1.22.2" // override 1.8, com.trueaccord.scalapb.compiler.Version.grpcJavaVersion
val grpcNetty = "io.grpc" % "grpc-netty" % grpcVersion
val scalapbRuntime = "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
// Override the version that scalapb depends on. This adds an explicit dependency on
// protobuf-java. This will cause sbt to evict the older version that is used by
// scalapb-runtime.
val protobuf = "com.google.protobuf" % "protobuf-java" % "3.7.0"
lazy val peaGrpc = subProject("pea-grpc")
  .settings(libraryDependencies ++= Seq(
    gatling, grpcNetty, scalapbRuntime, protobuf
  ))

// options: https://github.com/thesamet/sbt-protoc
PB.protoSources in Compile := Seq(
  baseDirectory.value / "test/protobuf"
)
PB.targets in Compile := Seq(
  scalapb.gen(grpc = true) -> baseDirectory.value / "test-generated"
)
unmanagedSourceDirectories in Compile += baseDirectory.value / "test-generated"
sourceGenerators in Compile -= (PB.generate in Compile).taskValue

coverageEnabled := false
