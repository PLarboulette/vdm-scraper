package routes
import java.time.Instant

import actors.PublicationActor.{FindAll, FindById}
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix, _}
import akka.actor.ActorRef
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import models.Publication
import akka.pattern.ask
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import utils.JsonFormats._

import scala.concurrent.duration._


object PublicationRoutes {


  def getRoutes (publicationActorRef : ActorRef) : Route = {

    implicit val stringToDateTime: Unmarshaller[String, Instant ] = Unmarshaller.strict[String, Instant](Instant.parse)

    path("api" / "posts") {
      get {
        parameter("from".as[Instant] ?, "to".as[Instant] ?, "author".as[String] ?) {(from, to, author) =>
          complete {
            implicit val timeout : Timeout = 5.seconds
            val posts = (publicationActorRef ? FindAll(from = from.map(_.toEpochMilli), to = to.map(_.toEpochMilli), author = author)).mapTo[List[Publication]]
            posts
          }
        }
      }
    } ~ pathPrefix("api" / "posts" / Segment ) { id =>
      get {
        implicit val timeout: Timeout = 5.seconds
        val post = (publicationActorRef ? FindById(id)).mapTo[Option[Publication]]
        complete(post)
      }
    }
  }
}