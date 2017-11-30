package routes

import java.time.Instant
import java.time.temporal.ChronoUnit

import actors.PostActor
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{MalformedQueryParamRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import models.Post
import models.Post.{PostOutput, PostsOutput}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.{Completed, MongoClient, MongoCollection, MongoDatabase}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach, Matchers}
import spray.json._
import utils.JsonFormats._
import models.{Post => PostModel}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}


class PostRoutesTest extends AsyncWordSpec with Matchers with ScalatestRouteTest  with BeforeAndAfterEach {

  implicit val systemActor: ActorSystem = ActorSystem("vdm-scraper-api-test")
  implicit val materializerTemp: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27018")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper-test")
  implicit val coll: MongoCollection[Document] = database.getCollection("posts")

  val postActor: ActorRef = system.actorOf(PostActor.props(), "PostActor")

  val postRoutes: Route = PostRoutes.getRoutes(postActor)

  val now: Instant = Instant.ofEpochMilli(1511982333000L)

  implicit val postUnmarshaller : Unmarshaller[String, PostModel] =
    Unmarshaller.strict[String, Post](str => str.parseJson.convertTo[PostModel])

  def f1 : ExecutionContext => HttpResponse => Future[PostOutput] = {
    implicit ec => {
      implicit response =>
        response.entity.toStrict(5.seconds).map(item => {
          item.data.utf8String.parseJson.convertTo[PostOutput]
        })
    }
  }

  implicit val postOutputUnmarshaller: Unmarshaller[HttpResponse, PostOutput] = Unmarshaller.apply[HttpResponse, PostOutput] (f1)

  def f2 : ExecutionContext => HttpResponse => Future[PostsOutput] = {
    implicit ec => {
      implicit response =>
        response.entity.toStrict(5.seconds).map(item => {
          item.data.utf8String.parseJson.convertTo[PostsOutput]
        })
    }
  }

  implicit val postsOutputUnmarshaller : Unmarshaller[HttpResponse, PostsOutput] = Unmarshaller.apply[HttpResponse, PostsOutput] (f2)

  private def cleanDb() = {
    Await.ready(coll.drop().toFuture(), 3 seconds)
  }

  override def beforeEach(): Unit = cleanDb()

  override def afterEach(): Unit = cleanDb()

  def insertData (nbElementsToInsert : Int): List[Future[Completed]] = {
    val postsToInsert = (1 to nbElementsToInsert).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author $index")))
    val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
    listDocuments.map { coll.insertOne(_).toFuture()}
  }

  "the api" should {

    "return a little message for GET requests to the /admin/clean path to indicate you that the database xas cleaned" in {
      Get("/admin/clean") ~> postRoutes ~> check {
        responseAs[String] shouldEqual "DB Cleaned !"
      }
    }

    "return a empty list of posts for GET requests to the /api/posts path" in {
      Get("/api/posts") ~> postRoutes ~> check {
        responseAs[PostsOutput] shouldEqual PostsOutput(0, List.empty)
      }
    }

    "return a non-empty list of posts for GET requests to the /api/posts path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author $index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get("/api/posts") ~> postRoutes ~> check {
            responseAs[PostsOutput] shouldEqual PostsOutput(5, postsToInsert.toList.sortBy(_.date).reverse)
          }
        }
    }

    "return a non-empty list of posts filtered by author for GET requests to the /api/posts?author=X path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get("/api/posts?author=Author1") ~> postRoutes ~> check {
            responseAs[PostsOutput] shouldEqual PostsOutput(1, postsToInsert.toList.filter(_.author.contains("Author1")))
          }
        }
    }

    "return a non-empty list of posts filtered by from for GET requests to the /api/posts?from=X path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val from = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?from=$from") ~> postRoutes ~> check {
            responseAs[PostsOutput] shouldEqual PostsOutput(2, postsToInsert.toList.filter(_.date.getOrElse("") >= from).reverse)
          }
        }
    }

    "return a non-empty list of posts filtered by to for GET requests to the /api/posts?to=X path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val to = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?to=$to") ~> postRoutes ~> check {
            responseAs[PostsOutput] shouldEqual PostsOutput(4, postsToInsert.toList.filter(_.date.getOrElse("") <= to).reverse)
          }
        }
    }

    "return a non-empty list of posts filtered by author and to for GET requests to the /api/posts?to=X&author=Y path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val to = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?to=$to&author=Author2") ~> postRoutes ~> check {
            responseAs[PostsOutput] shouldEqual PostsOutput(1, postsToInsert.toList.filter(_.date.getOrElse("") <= to).filter(_.author.getOrElse("")  == "Author2").reverse)
          }
        }
    }

    "return a empty list of posts filtered by author, from and to for GET requests to the /api/posts?from=2&to=X&author=Y path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      val from = now.plus(3, ChronoUnit.DAYS).toString
      val to = now.plus(4, ChronoUnit.DAYS).toString
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts?from=$from&to=$to&author=Author2") ~> postRoutes ~> check {
            responseAs[PostsOutput] shouldEqual PostsOutput(0, List.empty)
          }
        }
    }


    "return a empty posts for GET requests to the /api/posts/id path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts/333") ~> postRoutes ~> check {
            responseAs[PostOutput] shouldEqual PostOutput(None)
          }
        }
    }

    "return a non-empty posts for GET requests to the /api/posts/id path" in {
      val postsToInsert = (1 to 5).map(index => PostModel(s"$index", Some(s"Content $index"), Some(now.plus(index, ChronoUnit.DAYS).toString), Some(s"Author$index")))
      val listDocuments = postsToInsert.toList.map(PostModel.toDocument)
      val futureRes = listDocuments.map { coll.insertOne(_).toFuture()}
      Future.sequence(futureRes)
        .flatMap { _ =>
          Get(s"/api/posts/2") ~> postRoutes ~> check {
            responseAs[PostOutput] shouldEqual PostOutput(postsToInsert.find(_.id == "2"))
          }
        }
    }

    "return a error message for GET requests to the /api/posts?from=XInError" in {
      val test = "Coucou"
      Get(s"/api/posts?from=$test") ~> postRoutes ~> check {
        rejection shouldEqual   MalformedQueryParamRejection("from", s"Text '$test' could not be parsed at index 0",None)
      }
    }

    "return a error message for GET requests to the /api/posts?to=XInError" in {
      val test = "Coucou"
      Get(s"/api/posts?to=$test") ~> postRoutes ~> check {
        rejection shouldEqual   MalformedQueryParamRejection("to", s"Text '$test' could not be parsed at index 0",None)
      }
    }
  } // End API
}
