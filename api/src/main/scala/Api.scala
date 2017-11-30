import actors.PostActor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}
import routes.PostRoutes
import scala.concurrent.ExecutionContextExecutor
import com.typesafe.config.ConfigFactory

object Api extends App {

  println("Hello world, I'm the API ! ")

  implicit val system: ActorSystem = ActorSystem("vdm-scraper-api")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val mongoClient: MongoClient = MongoClient(ConfigFactory.load().getString("database.url"))
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper")
  implicit val coll: MongoCollection[Document] = database.getCollection("posts")

  val postActor = system.actorOf(PostActor.props(), "PostActor")

  val route = PostRoutes.getRoutes(postActor)

  val server = "0.0.0.0"
  val port = 8080
  val bindingFuture = Http().bindAndHandle(route, server, port)

  println(s"Server listening on $server:$port")
}
