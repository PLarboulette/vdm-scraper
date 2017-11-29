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
  implicit val coll: MongoCollection[Document] = database.getCollection("publications")


  private def cleanDb() = {
    Await.ready(coll.drop().toFuture(), 3 seconds)
  }

  override def beforeEach(): Unit = cleanDb()

  override def afterEach(): Unit = cleanDb()

  "the scraper" should {

    "return a list of 200 elements" in {
      val elements = Functions.launch(200, 1, List.empty[Data], test = true)
      elements should have size 200
    }
  }

}
