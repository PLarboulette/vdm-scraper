package services

import java.time.Instant
import java.time.temporal.{ChronoField, ChronoUnit}

import models.Publication
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PublicationServiceTest extends AsyncWordSpec with Matchers with BeforeAndAfterEach {


  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27018")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper-test-services")
  implicit val coll: MongoCollection[Document] = database.getCollection("publications")

  private def cleanDb() = {
    Await.ready(coll.drop().toFuture(), 3 seconds)
  }

  override def beforeEach(): Unit = cleanDb()

  override def afterEach(): Unit = cleanDb()

  // Instant.now().plusMillis()   index * 3 600 000  là dedans pour rajouter des heures
//  Instant.now().plus(4, ChronoUnit.HOURS)

  def insertData (nbElementsToInsert : Int): List[Future[Completed]] = {
    val publicationsToInsert = (1 to nbElementsToInsert).map(index => Publication(s"$index", Some(s"Content $index"), Some(index * 10.toLong), Some(s"Author$index")))
    val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
    listDocuments.map { coll.insertOne(_).toFuture()}
  }

  "findAll" should {
    "return nothing" in {
      PublicationService.findAll().map { publications => publications shouldBe empty }
    }

    "return a list of publications sorted by date" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PublicationService.findAll().map {
            results =>
              results should have size nbElementsToInsert
              results.head.date.getOrElse(0L) should be > results.last.date.getOrElse(0L)
          }
        }
    }

    "return a list of publications (1 item) filtered by author" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PublicationService.findAll(author = Some("Author3")).map {
            results =>
              results should have size 1
              results.head.author.getOrElse("") should equal ("Author3")
          }
        }
    }

    "return a list of publications (many items) filtered by author" in {

      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(index * 10.toLong), Some(if(index < 3) "Pierre" else "Unicorn")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}

      Future.sequence(futureRes).flatMap { _ =>
          PublicationService.findAll(author = Some("Pierre")).map {
            results =>
              results should have size 2
              results.head.author.getOrElse("") should equal ("Pierre")
          }
        }
    }


    "return a list of publications filtered by from timestamp" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PublicationService.findAll(from = Some(12)).map {
            results =>
              results should have size 4
              results.head.author.getOrElse("") should equal ("Author5")
              results.head.date.getOrElse(0) should equal (50)
          }
        }
    }

    "return a list of publications filtered by to timestamp" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PublicationService.findAll(to = Some(45)).map {
            results =>
              results should have size 4
              results.head.author.getOrElse("") should equal ("Author4")
              results.head.date.getOrElse(0) should equal (40)
          }
        }
    }

    "return a list of publications filtered by to timestamp and author" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PublicationService.findAll(to = Some(45), author = Some("Author4")).map {
            results =>
              results should have size 1
              results.head.author.getOrElse("") should equal ("Author4")
              results.head.date.getOrElse(0) should equal (40)
          }
        }
    }


    "return a list of publications filtered by from timestamp and to timestamp" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PublicationService.findAll(from = Some(12), to = Some(45)).map {
            results =>
              results should have size 3
              results.head.author.getOrElse("") should equal ("Author4")
              results.head.date.getOrElse(0) should equal (40)
          }
        }
    }

    "return a list of publications filtered by from timestamp, to timestamp and author" in {

      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(index * 10.toLong), Some(if(index < 3) "Pierre" else "Dark Vador")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}

      Future.sequence(futureRes)
        .flatMap { _ =>
          PublicationService.findAll( from = Some(12), to = Some(45), author = Some("Pierre")).map {
            results =>
              results should have size 1
              results.head.author.getOrElse("") should equal ("Pierre")
              results.head.date.getOrElse(0) should equal (20)
          }
        }
    }
  }

  "findById" should {

    "return a publication" in {
      Future.sequence(insertData(5))
        .flatMap { _ =>
          PublicationService.findById("1").map {
            publicationOpt =>
              publicationOpt shouldBe defined
          }
        }
    }

    "return nothing" in {
      Future.sequence(insertData(5))
        .flatMap { _ =>
          PublicationService.findById("15").map {
            publicationOpt =>
              publicationOpt shouldBe empty
          }
        }
    }
  }


}
