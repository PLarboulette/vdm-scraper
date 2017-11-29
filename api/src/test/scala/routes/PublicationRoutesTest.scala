package routes

import java.time.Instant
import java.time.temporal.ChronoUnit

import actors.PublicationActor
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{MalformedQueryParamRejection, MissingQueryParamRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import models.Publication
import models.Publication.{PublicationOutput, PublicationsOutput}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.{Completed, MongoClient, MongoCollection, MongoDatabase}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach, Matchers}
import spray.json._
import utils.JsonFormats._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}


class PublicationRoutesTest extends AsyncWordSpec with Matchers with ScalatestRouteTest  with BeforeAndAfterEach {

  implicit val systemActor: ActorSystem = ActorSystem("vdm-scraper-api-test")
  implicit val materializerTemp: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27018")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper-test-routes")
  implicit val coll: MongoCollection[Document] = database.getCollection("publications")

  val publicationActor: ActorRef = system.actorOf(PublicationActor.props(), "PublicationActor")

  val publicationRoutes: Route = PublicationRoutes.getRoutes(publicationActor)

  val now: Instant = Instant.ofEpochMilli(1511982333000L)

  implicit val publicationUnmarshaller : Unmarshaller[String, Publication] =
    Unmarshaller.strict[String, Publication](str => str.parseJson.convertTo[Publication])

  def f1 : ExecutionContext => HttpResponse => Future[PublicationOutput] = {
    implicit ec => {
      implicit response =>
        response.entity.toStrict(5.seconds).map(item => {
          item.data.utf8String.parseJson.convertTo[PublicationOutput]
        })
    }
  }

  implicit val publicationOutputUnmarshaller: Unmarshaller[HttpResponse, PublicationOutput] =
    Unmarshaller.apply[HttpResponse, PublicationOutput] (f1)

  def f2 : ExecutionContext => HttpResponse => Future[PublicationsOutput] = {
    implicit ec => {
      implicit response =>
        response.entity.toStrict(5.seconds).map(item => {
          item.data.utf8String.parseJson.convertTo[PublicationsOutput]
        })
    }
  }

  implicit val publicationsOutputUnmarshaller : Unmarshaller[HttpResponse, PublicationsOutput] =
    Unmarshaller.apply[HttpResponse, PublicationsOutput] (f2)

  private def cleanDb() = {
    Await.ready(coll.drop().toFuture(), 3 seconds)
  }

  override def beforeEach(): Unit = cleanDb()

  override def afterEach(): Unit = cleanDb()

  def insertData (nbElementsToInsert : Int): List[Future[Completed]] = {
    val publicationsToInsert = (1 to nbElementsToInsert).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author $index")))
    val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
    listDocuments.map { coll.insertOne(_).toFuture()}
  }

  "the api" should {

    "return a little message for GET requests to the /admin/clean path to indicate you that the database xas cleaned" in {
      Get("/admin/clean") ~> publicationRoutes ~> check {
        responseAs[String] shouldEqual "DB Cleaned !"
      }
    }

    "return a empty list of publications for GET requests to the /api/posts path" in {
      Get("/api/posts") ~> publicationRoutes ~> check {
        responseAs[PublicationsOutput] shouldEqual PublicationsOutput(0, List.empty)
      }
    }

    "return a non-empty list of publications for GET requests to the /api/posts path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author $index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get("/api/posts") ~> publicationRoutes ~> check {
            responseAs[PublicationsOutput] shouldEqual PublicationsOutput(5, publicationsToInsert.toList.sortBy(_.date).reverse)
          }
        }
    }

    "return a non-empty list of publications filtered by author for GET requests to the /api/posts?author=X path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get("/api/posts?author=Author1") ~> publicationRoutes ~> check {
            responseAs[PublicationsOutput] shouldEqual PublicationsOutput(1, publicationsToInsert.toList.filter(_.author.contains("Author1")))
          }
        }
    }

    "return a non-empty list of publications filtered by from for GET requests to the /api/posts?from=X path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val from = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?from=$from") ~> publicationRoutes ~> check {
            responseAs[PublicationsOutput] shouldEqual PublicationsOutput(2, publicationsToInsert.toList.filter(_.date.getOrElse("") >= from).reverse)
          }
        }
    }

    "return a non-empty list of publications filtered by to for GET requests to the /api/posts?to=X path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val to = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?to=$to") ~> publicationRoutes ~> check {
            responseAs[PublicationsOutput] shouldEqual PublicationsOutput(4, publicationsToInsert.toList.filter(_.date.getOrElse("") <= to).reverse)
          }
        }
    }

    "return a non-empty list of publications filtered by author and to for GET requests to the /api/posts?to=X&author=Y path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val to = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?to=$to&author=Author2") ~> publicationRoutes ~> check {
            responseAs[PublicationsOutput] shouldEqual PublicationsOutput(1, publicationsToInsert.toList.filter(_.date.getOrElse("") <= to).filter(_.author.getOrElse("")  == "Author2").reverse)
          }
        }
    }

    "return a empty list of publications filtered by author, from and to for GET requests to the /api/posts?from=2&to=X&author=Y path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val from = now.plus(3, ChronoUnit.DAYS).toString
      val to = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?from=$from&to=$to&author=Author2") ~> publicationRoutes ~> check {
            responseAs[PublicationsOutput] shouldEqual PublicationsOutput(0, List.empty)
          }
        }
    }


    "return a empty publication for GET requests to the /api/posts/id path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts/333") ~> publicationRoutes ~> check {
            responseAs[PublicationOutput] shouldEqual PublicationOutput(None)
          }
        }
    }

    "return a non-empty publication for GET requests to the /api/posts/id path" in {
      val publicationsToInsert = (1 to 5).map(index => Publication(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = publicationsToInsert.toList.map(Publication.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts/2") ~> publicationRoutes ~> check {
            responseAs[PublicationOutput] shouldEqual PublicationOutput(publicationsToInsert.find(_.id == "2"))
          }
        }
    }

    "return a error message for GET requests to the /api/posts?from=XInError" in {
      val test = "Coucou"
      Get(s"/api/posts?from=$test") ~> publicationRoutes ~> check {
        rejection shouldEqual   MalformedQueryParamRejection("from", s"Text '$test' could not be parsed at index 0",None)
      }
    }

    "return a error message for GET requests to the /api/posts?to=XInError" in {
      val test = "Coucou"
      Get(s"/api/posts?to=$test") ~> publicationRoutes ~> check {
        rejection shouldEqual   MalformedQueryParamRejection("to", s"Text '$test' could not be parsed at index 0",None)
      }
    }
  } // End API
}
