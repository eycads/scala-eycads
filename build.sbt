
name := "scala-eycads"
version := "0.1"
scalaVersion := "2.13.6"

lazy val global = project
  .in(file("."))
  .settings(
    settings,
    Keys.`package` := file(""),
    packageBin in Global := file(""),
    packagedArtifacts := Map(),
    publish := (()),
    publishLocal := (())
  )
  .aggregate(
    protobuf
  )

lazy val settings =
  commonSettings

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)

val cleanProtocols = taskKey[Unit]("Cleans the protobuf generated code")
lazy val protobuf = project
  .settings(
    name := "protobuf",
    settings,
    libraryDependencies ++= sdkDependencies,
    PB.protocVersion := dependencies.protoV,
    cleanProtocols in Compile := {
      (PB.targets in Compile).value
        .flatMap(target => {
          val outputPath = target.outputPath
          if (outputPath.exists()) {
            outputPath.listFiles.toList
          } else {
            List()
          }
        })
        .foreach(IO.delete)
      println(
        "Cleaned protocols for module " + moduleName.in(Compile).value + " in project directory " + baseDirectory
          .in(Compile)
          .value
      )
    },
    PB.targets in Compile := {
      scalapb.gen() -> (sourceManaged in Compile).value
      Seq(
        AlonaScalaPbCodeGenerator(
          sLog.value,
          file((baseDirectory in Compile).value.getParent + "/protobuf/src/main/protocols" + "/com/eycads/sdk/protocols/ScalaPbManifest.scala"),
        ) -> file((baseDirectory in Compile).value.getParent + "/protobuf/src/main/protocols"),
      )
    },
    PB.protoSources in Compile :=
      Seq(
        file("protobuf/src/main/protobuf"),
      ),
    PB.generate in Compile := {
      (cleanProtocols in Compile).value
      (PB.generate in Compile).value
    },
    unmanagedSourceDirectories in Compile += (baseDirectory in Compile).value.getParentFile / "/protobuf/src/main/protocols",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "1.18.1-1" % "protobuf",
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "1.18.1-1"
    ),
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  )

lazy val `alona-sdk` = project
  .settings(
    name := "alona-sdk",
    logBuffered := false,
    settings,
    libraryDependencies ++= sdkDependencies,
  ).dependsOn(protobuf)

lazy val `account-server` = project
  .settings(
    name := "account-server",
    //    PB.protoSources in Compile := Seq(file("protobuf/sdk"), file("protobuf/user")),
    settings,
    libraryDependencies ++= serverDependencies,
  )
  .dependsOn(`alona-sdk`)

lazy val dependencies =
  new {
    val akkaV = "2.6.12"
    val akkaHttpV = "10.1.11"
    val scalaTestV = "3.0.8"
    val zioV = "1.0.3"
    val catsV = "2.0.0"
    val circeV = "0.14.1"
    val kafkaV = "2.4.0"
    val protoV = "3.14.0"

    val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaV
    val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaV
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaV
    val akkaStreamTyped = "com.typesafe.akka" %% "akka-stream-typed" % akkaV
    val akkaClusterTyped = "com.typesafe.akka" %% "akka-cluster-typed" % akkaV
    val akkaClusterShardingTyped = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaV
    val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaV
    val akkaSerializationJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % akkaV
    val akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % akkaV
    val akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaV
    val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpV
    val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV
    val akkaStreamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.3"
    val akkaStreamS3 = ("com.lightbend.akka" %% "akka-stream-alpakka-s3" % "2.0.2")

    val zio = "dev.zio" %% "zio" % zioV
    val zioStreams = "dev.zio" %% "zio-streams" % zioV
    val zioInteropJava = "dev.zio" %% "zio-interop-java" % "1.1.0.0-RC6"
    val catsEffect = "org.typelevel" %% "cats-effect" % catsV
    val catsCore = "org.typelevel" %% "cats-core" % catsV
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
    val typeSafeConfig = "com.typesafe" % "config" % "1.4.0"
    val kafkaSerdeCirce = "io.github.azhur" %% "kafka-serde-circe" % "0.4.0"
    val akkaHttpCirceJson = "de.heikoseeberger" %% "akka-http-circe" % "1.38.2"

    val circeCore = "io.circe" %% "circe-core" % circeV
    val circeParser = "io.circe" %% "circe-parser" % circeV
    val circeGeneric = "io.circe" %% "circe-generic" % circeV
    val circeGenericExtras = "io.circe" %% "circe-generic-extras" % "0.12.2"

    val scalaTest = "org.scalatest" %% "scalatest" % scalaTestV
    val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaV
    val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV
    val junit = "junit" % "junit" % "4.10"

    val levelDB = "org.iq80.leveldb" %% "leveldb" % "0.7"
    val levelDBJni = "org.fusesource.leveldbjni" %% "leveldbjni-all" % "1.8"

    val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.4"

    val kafkaClient = "org.apache.kafka" % "kafka-clients" % kafkaV
    val elastic4sClient = "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.7.0"

    val cassandraDatastaxJavaCore = "com.datastax.oss" % "java-driver-core" % "4.8.0"
    val awsJavaSDK = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.923"

  }

lazy val sdkDependencies = Seq(
  dependencies.akkaHttp,
  dependencies.akkaActorTyped,
  dependencies.akkaStreamTyped,
  dependencies.akkaPersistenceTyped,
  dependencies.akkaClusterTyped,
  dependencies.akkaClusterShardingTyped,
  dependencies.akkaStreamKafka,
  dependencies.akkaStreamS3,
  dependencies.akkaHttpSprayJson,
  dependencies.circeCore,
  dependencies.circeGeneric,
  dependencies.circeGenericExtras,
  dependencies.zio,
  dependencies.kafkaClient,
  dependencies.elastic4sClient,
  dependencies.awsJavaSDK,
  dependencies.cassandraDatastaxJavaCore,
  dependencies.scalaLogging,
)

lazy val serverDependencies = Seq(
  dependencies.akkaHttp,
  dependencies.akkaActorTyped,
  dependencies.akkaStreamTyped,
  dependencies.akkaClusterTyped,
  dependencies.akkaClusterShardingTyped,
  dependencies.akkaRemote,
  dependencies.akkaPersistenceTyped,
  dependencies.akkaPersistenceQuery,
  dependencies.kafkaSerdeCirce,
  dependencies.akkaHttpCirceJson,
  dependencies.circeCore,
  dependencies.circeParser,
  dependencies.circeGeneric,
  dependencies.circeGenericExtras,
  dependencies.zio,
  dependencies.akkaPersistenceCassandra,
  dependencies.kafkaClient,
  dependencies.scalaLogging,
  dependencies.logbackClassic
)