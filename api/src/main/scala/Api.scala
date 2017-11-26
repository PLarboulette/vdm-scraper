import actors.PublicationActor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}
import routes.PublicationRoutes

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

object Api extends App {

  println("Hello world, I'm the API ! ")

  implicit val system: ActorSystem = ActorSystem("vdm-scraper-api")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper")
  implicit val coll: MongoCollection[Document] = database.getCollection("publications")

  val publicationActor = system.actorOf(PublicationActor.props(), "PublicationActor")

  val route = PublicationRoutes.getRoutes(publicationActor)

  val server = "0.0.0.0"
  val port = 8080
  val bindingFuture = Http().bindAndHandle(route, server, port)

  println(s"Server listening on $server:$port")
}
