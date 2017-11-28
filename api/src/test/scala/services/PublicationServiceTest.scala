package services

import models.{Publication}
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PublicationServiceTest extends AsyncWordSpec with Matchers with BeforeAndAfterEach {


  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27018")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper-test")
  implicit val coll: MongoCollection[Document] = database.getCollection("players")

  private def cleanDb() = {
    Await.ready(coll.drop().toFuture(), 3 seconds)
  }

  override def beforeEach(): Unit = cleanDb()

  override def afterEach(): Unit = cleanDb()


  "find" should {

    "return nothing" in {
      PublicationService.findAll().map { publications => publications shouldBe empty }
    }

    "return a list of publications sorted by date" in {

      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some((Math.random() * 158000).toLong), Some(s"Author $index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)

      val futuresRes = listDocuments.map { coll.insertOne(_).toFuture()}

      listDocuments should have size listDocuments.size
      Future.sequence(futuresRes)
        .flatMap { _ =>
          PublicationService.findAll().map {
            results =>
              results should have size listDocuments.size
          }
        }

    }
  }

  // Pense à tester tous les from, to, author

}
