package routes

import java.time.Instant
import actors.PostActor.{CleanDB, FindAll, FindById}
import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.pattern.ask
import akka.util.Timeout
import models.Post
import models.Post.{PostOutput, PostsOutput}
import utils.JsonFormats._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object PostRoutes {

  def getRoutes (postActorRef : ActorRef) : Route = {

    implicit val stringToDateTime: Unmarshaller[String, Instant ] = Unmarshaller.strict[String, Instant](Instant.parse)

    path("api" / "posts") {
      get {
        parameter("from".as[Instant] ?, "to".as[Instant] ?, "author".as[String] ?) {(from, to, author) =>
          complete {
            implicit val timeout : Timeout = 5.seconds
            val publicationsFuture = (postActorRef ? FindAll(from = from.map(_.toString), to = to.map(_.toString), author = author)).mapTo[List[Post]]
            publicationsFuture.map {
              publications =>PostsOutput(publications.size, publications)
            }
          }
        }
      }
    } ~ pathPrefix("api" / "posts" / Segment ) { id =>
      get {
        complete {
          implicit val timeout: Timeout = 5.seconds
          val publicationFuture = (postActorRef ? FindById(id)).mapTo[Option[Post]]
          publicationFuture.map {
              case Some(publiation) => PostOutput(Some(publiation))
              case None => PostOutput(None)
            }
          }
        }
    } ~ path("admin" / "clean") {
      get {
        complete {
          implicit val timeout : Timeout = 5.seconds
          postActorRef ! CleanDB()
          "DB Cleaned !"
        }
      }
    }
  }
}