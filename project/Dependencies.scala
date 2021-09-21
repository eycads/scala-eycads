//object Dependencies {
//
//  lazy val sdkDependencies = Seq(
//    //  dependencies.scalaPb,
//    dependencies.akkaHttp,
//    dependencies.akkaActorTyped,
//    dependencies.akkaStreamTyped,
//    dependencies.akkaPersistenceTyped,
//    dependencies.akkaClusterTyped,
//    dependencies.akkaClusterShardingTyped,
//    dependencies.akkaStreamKafka,
//    dependencies.akkaStreamS3,
//    dependencies.akkaHttpSprayJson,
//    dependencies.circeCore,
//    dependencies.circeGeneric,
//    dependencies.circeGenericExtras,
//    dependencies.zio,
//    dependencies.kafkaClient,
//    dependencies.elastic4sClient,
//    dependencies.awsJavaSDK,
//    dependencies.cassandraDatastaxJavaCore,
//    dependencies.scalaLogging,
//  )
//
//  lazy val dependencies =
//    new {
//      val akkaV = "2.6.12"
//      val akkaHttpV = "10.1.11"
//      val scalaTestV = "3.0.8"
//      val zioV = "1.0.3"
//      val catsV = "2.0.0"
//      val circeV = "0.14.1"
//      val kafkaV = "2.4.0"
//      val protoV = "3.14.0"
//
//      val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaV
//      val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaV
//      val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaV
//      val akkaStreamTyped = "com.typesafe.akka" %% "akka-stream-typed" % akkaV
//      val akkaClusterTyped = "com.typesafe.akka" %% "akka-cluster-typed" % akkaV
//      val akkaClusterShardingTyped = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaV
//      val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaV
//      val akkaSerializationJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % akkaV
//      val akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % akkaV
//      val akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaV
//      val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpV
//      val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV
//      val akkaStreamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.3"
//      val akkaStreamS3 = ("com.lightbend.akka" %% "akka-stream-alpakka-s3" % "2.0.2")
//
//      val zio = "dev.zio" %% "zio" % zioV
//      val zioStreams = "dev.zio" %% "zio-streams" % zioV
//      val zioInteropJava = "dev.zio" %% "zio-interop-java" % "1.1.0.0-RC6"
//      val catsEffect = "org.typelevel" %% "cats-effect" % catsV
//      val catsCore = "org.typelevel" %% "cats-core" % catsV
//      val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
//      val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
//      val typeSafeConfig = "com.typesafe" % "config" % "1.4.0"
//      val kafkaSerdeCirce = "io.github.azhur" %% "kafka-serde-circe" % "0.4.0"
//      val akkaHttpCirceJson = "de.heikoseeberger" %% "akka-http-circe" % "1.38.2"
//
//      val circeCore = "io.circe" %% "circe-core" % circeV
//      val circeParser = "io.circe" %% "circe-parser" % circeV
//      val circeGeneric = "io.circe" %% "circe-generic" % circeV
//      val circeGenericExtras = "io.circe" %% "circe-generic-extras" % "0.12.2"
//
//      val scalaTest = "org.scalatest" %% "scalatest" % scalaTestV
//      val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaV
//      val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV
//      val junit = "junit" % "junit" % "4.10"
//
//      val levelDB = "org.iq80.leveldb" %% "leveldb" % "0.7"
//      val levelDBJni = "org.fusesource.leveldbjni" %% "leveldbjni-all" % "1.8"
//
//      val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.4"
//
//      val kafkaClient = "org.apache.kafka" % "kafka-clients" % kafkaV
//      val elastic4sClient = "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.7.0"
//
//      val cassandraDatastaxJavaCore = "com.datastax.oss" % "java-driver-core" % "4.8.0"
//      val awsJavaSDK = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.923"
//
//    }
//}
