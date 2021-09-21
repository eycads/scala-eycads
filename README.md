## Protobuf Generation

Module: `protobuf`

Features:
* Generate scala protobuf classes and objects based on Google's protocol buffers (https://developers.google.com/protocol-buffers) and `com.thesamet:sbt-protoc`
* Generate ScalaPBManifest Object using `AlonaScalaPbCodeGenerator`, this manifest is used to for custom deserialization

Commands:
```sbt
sbt
project protobuf
compile
```

## SDK

Module: `alona-sdk`

### ZIO [Effect Library]
ZIO implementations (https://zio.dev/) - effect type for pure, asynchronous and concurrent programming based on Haskell's IO. All this library are wrapped in ZIO:
* Actor `com.eycads.alona.sdk.zio.module.Actor` - actor-based system for event sourcing application
  * `com.eycads.alona.sdk.zio.impl.actor.AkkaActor` - Akka Actor implementation
* Cassandra `com.eycads.alona.sdk.zio.module.Cassandra` - cassandra client libraries for Cassandra NoSQL database
  * `com.eycads.alona.sdk.zio.impl.cassandra.DatastaxCassandra` - Datastax cassandra client implementation
* ElasticSearch `com.eycads.alona.sdk.zio.module.ElasticSearch` - elastic search client libraries for Elastic NoSQL database and engine
  * `com.eycads.alona.sdk.zio.impl.elasticsearch.ElasticNativeRestApi` - ElasticSearch native REST Api calls for elastic search intefacing, see https://www.elastic.co/guide/en/elasticsearch/reference/current/rest-apis.html
* FileIO `com.eycads.alona.sdk.zio.module.FileIO` - file IO operations, reading and writing to file
  * `com.eycads.alona.sdk.zio.impl.fileIO.ScalaFileIO` - scala core file IO implementation
* Generator `com.eycads.alona.sdk.zio.module.Generator` - generates timestamps and uuids
  * `com.eycads.alona.sdk.zio.impl.generator.ScalaGenerator` - scala core implementations that will generate uuid and timestamps
* Hash `com.eycads.alona.sdk.zio.module.Hash` - generate hashes and checking
  * `com.eycads.alona.sdk.zio.impl.hash.PBKDF2Hash` - pbkdf2 hash for password hashing with salts
* HttpClient `com.eycads.alona.sdk.zio.module.HttpClient` - http client for external api calls
  * `com.eycads.alona.sdk.zio.impl.http.AkkaHttpClient` - Akka Http client implementation
* Kafka `com.eycads.alona.sdk.zio.module.Kafka` - client for Kafka (needs to be revamped since the implementation was pre-ZLayer era on ZIO)
  * `com.eycads.alona.sdk.zio.impl.kafka.ApacheKafka` - apache client library
* Logger `com.eycads.alona.sdk.zio.module.Logger` - logger
  * `com.eycads.alona.sdk.zio.impl.logger.ScalaLogger` - com.typesafe scala logging
* ObjectStorage `com.eycads.alona.sdk.zio.module.ObjectStorage` - client for cloud object storage such as S3
  * `com.eycads.alona.sdk.zio.impl.objectstorage.AlpakkaS3` - Alpakka S3 client
  
Usage (HttpClient - this uses actor system, since the implementation is AkkaHttpClient):
```scala
    val myRuntime = Runtime.default

    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "HttpClientModuleTest")
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val liveHttpClient: Layer[Throwable, HttpClient] = AkkaHttpClient.createLive()

    val serverEnv: Layer[Throwable, HttpClientEnv] = liveHttpClient ++ AkkaHttpClient.liveDependency

    def execute(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse] = {
      for {
        res <- HttpClient.executeRequest(httpRequest)
      } yield res
    }

    val response: HttpResponse = myRuntime.unsafeRun(execute(HttpRequest(
      HttpMethods.HEAD,
      "http://localhost:9200/service"
    )).provideCustomLayer(serverEnv))

    val discarded: DiscardedEntity = response.discardEntityBytes()
    discarded.future.onComplete { done => println("Entity discarded completely!") }
```

### Akka Type Safe Event Sourced Persistent Actor Abstraction
Added layer of abstraction for akka event sourced persistence actor, effects are handled in ZIO including replies and 
raising events.

* EventSourcedActorBase `com.eycads.alona.sdk.actor.EventSourcedActorBase`

Actor relation mechanism for actor based system, basically this will create relationship between actors and process states
according to that relationship.

* EventSourcedActorRelation `com.eycads.alona.sdk.actor.EventSourcedActorRelation`

Motivation: 
* More control and state transition strategy between actors, for example you have a Business Account Actor and that has 
User Actor tied to it, then the Business Actor was put to a Suspended state, you don't want the User Actors to still 
received messages on Active state, so basically you want also to make User Actors to be Suspended also. 
* As long as relationships are defined, you can do whatever process you need to do based on the relationship.

Usage: can be seen on `account-server` module

### Common UniqueConstraintActor
This is a common UniqueConstraintActor `com.eycads.alona.sdk.actor.common.UniqueConstraintActor`, this will have its own 
sharding strategy and implementation. This actor will act as unique constraint validator of a value on actor-based system 
layer depending on the unique configuration being set on `com.eycads.alona.sdk.actor.common.UniqueConstraints`.

Currently, this only supports messages:
* AddUniqueConstraint - add a unique constraint
* RemoveUniqueConstraint - remove a constraint
* CheckUniqueConstraint - check a unique constraint if exist

Design:
* it uses the `EventSourcedActorBase` for the actor
* messages are protobuf generated classes
* effects are being handled in `zio`

Usage:
* For example, you want to have a unique constraints on `userName` on actor-system layer to avoid duplicate users, 
so one way would be using this `UniqueConstraintActor` and create your own config, then it would work out of the box for you.