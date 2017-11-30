
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.{ Document, MongoClient, MongoCollection, MongoDatabase}

object Scraper extends App {

  println("Hello world, I'm the scraper ! ")

  implicit val system: ActorSystem = ActorSystem("vdm-scraper-api")
  implicit val ec = system.dispatcher

  val mongoClient: MongoClient = MongoClient(ConfigFactory.load().getString("database.url"))
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper")
  implicit val coll: MongoCollection[Document] = database.getCollection("posts")

  case class Data (id : String, content : String, author : String, date : String)

  MongoHelper.cleanDb().map {
    _ =>
      val elements = 200
      val list = Functions.launch(elements, 1, List.empty[Data])
      if (list.size == elements) {
        system.terminate()
      }
  }
}
