package services

import java.time.Instant
import java.time.temporal.ChronoUnit

import models.Post
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach, Matchers}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PostServiceTest extends AsyncWordSpec with Matchers with BeforeAndAfterEach {

  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27018")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper-test-services")
  implicit val coll: MongoCollection[Document] = database.getCollection("posts")

  private def cleanDb() = {
    Await.ready(coll.drop().toFuture(), 3 seconds)
  }

  override def beforeEach(): Unit = cleanDb()

  override def afterEach(): Unit = cleanDb()

  val now: Instant = Instant.ofEpochMilli(1511982333000L)

  def insertData (nbElementsToInsert : Int): List[Future[Completed]] = {
    val postsToInsert = (1 to nbElementsToInsert).map(index =>
      Post(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
    val listDocuments = postsToInsert.toList.map(Post.toDocument)
    listDocuments.map { coll.insertOne(_).toFuture()}
  }

  "findAll" should {
    "return nothing" in {
      PostService.findAll().map { posts => posts shouldBe empty }
    }

    "return a list of posts sorted by date" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PostService.findAll().map {
            results =>
              results should have size nbElementsToInsert
              results.head.date.getOrElse("") should be > results.last.date.getOrElse("")
          }
        }
    }

    "return a list of posts (1 item) filtered by author" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PostService.findAll(author = Some("Author3")).map {
            results =>
              results should have size 1
              results.head.author.getOrElse("") should equal ("Author3")
          }
        }
    }

    "return a list of posts (many items) filtered by author" in {

      val postsToInsert = (1 to 5).map(index => Post(s"$index", Some(s"Content $index"), Some(now.plus(1, ChronoUnit.DAYS).toString), Some(if(index < 3) "Pierre" else "Unicorn")))
      val listDocuments = postsToInsert.toList.map(Post.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}

      Future.sequence(futureRes).flatMap { _ =>
          PostService.findAll(author = Some("Pierre")).map {
            results =>
              results should have size 2
              results.head.author.getOrElse("") should equal ("Pierre")
          }
        }
    }


    "return a list of posts filtered by from" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PostService.findAll(from = Some(now.plus(2, ChronoUnit.DAYS).toString)).map {
            results =>
              results should have size 4
              results.head.author.getOrElse("") should equal ("Author5")
              results.head.date.getOrElse("") should equal (now.plus(5, ChronoUnit.DAYS).toString)
          }
        }
    }

    "return a list of posts filtered by to" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PostService.findAll(to = Some(now.plus(3, ChronoUnit.DAYS).toString)).map {
            results =>
              results should have size 3
              results.head.author.getOrElse("") should equal ("Author3")
              results.head.date.getOrElse("") should equal (now.plus(3, ChronoUnit.DAYS).toString)
          }
        }
    }

    "return a list of posts filtered by to and author" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PostService.findAll(to = Some(now.plus(3, ChronoUnit.DAYS).toString), author = Some("Author3")).map {
            results =>
              results should have size 1
              results.head.author.getOrElse("") should equal ("Author3")
              results.head.date.getOrElse("") should equal (now.plus(3, ChronoUnit.DAYS).toString)
          }
        }
    }


    "return a list of posts filtered by from and to" in {
      val nbElementsToInsert = 5
      Future.sequence(insertData(nbElementsToInsert))
        .flatMap { _ =>
          PostService.findAll(from = Some(now.plus(3, ChronoUnit.DAYS).toString), to = Some(now.plus(5, ChronoUnit.DAYS).toString)).map {
            results =>
              results should have size 3
              results.head.author.getOrElse("") should equal ("Author5")
              results.head.date.getOrElse("") should equal (now.plus(5, ChronoUnit.DAYS).toString)
          }
        }
    }

    "return a list of posts filtered by from, to and author" in {

      val postsToInsert = (1 to 5).map(index => Post(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(if(index < 3) "Pierre" else "Unicorn")))
      val listDocuments = postsToInsert.toList.map(Post.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}

      Future.sequence(futureRes)
        .flatMap { _ =>
          PostService.findAll( from = Some(now.plus(3, ChronoUnit.DAYS).toString), to = Some(now.plus(5, ChronoUnit.DAYS).toString), author = Some("Unicorn")).map {
            results =>
              results should have size 3
              results.head.author.getOrElse("") should equal ("Unicorn")
              results.head.date.getOrElse("") should equal (now.plus(5, ChronoUnit.DAYS).toString)
          }
        }
    }
  }

  "findById" should {

    "return a post" in {
      Future.sequence(insertData(5))
        .flatMap { _ =>
          PostService.findById("1").map {
            postOpt =>
              postOpt shouldBe defined
          }
        }
    }

    "return nothing" in {
      Future.sequence(insertData(5))
        .flatMap { _ =>
          PostService.findById("15").map {
            postOpt =>
              postOpt shouldBe empty
          }
        }
    }
  }


}
