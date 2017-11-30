import Scraper.Data
import akka.actor.ActorSystem
import org.mongodb.scala.bson.Document
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach, Matchers}
import scala.concurrent.Await
import scala.concurrent.duration._

class ScraperTest extends AsyncWordSpec with Matchers  with BeforeAndAfterEach {

  implicit val system: ActorSystem = ActorSystem("vdm-scraper-api")
  implicit val ec = system.dispatcher

  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27018")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper-test-scraper")
  implicit val coll: MongoCollection[Document] = database.getCollection("posts")

  private def cleanDb() = {
    Await.ready(coll.drop().toFuture(), 3 seconds)
  }

  override def beforeEach(): Unit = cleanDb()

  override def afterEach(): Unit = cleanDb()

  "the scraper" should {

    "return a list of nbElementsToScrap elements" in {
      val nbElementsToScrap = 200
      val elements = Functions.launch(nbElementsToScrap, 1, List.empty[Data])
      elements should have size nbElementsToScrap
    }
  }

}
