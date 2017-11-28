package routes
import java.time.Instant

import actors.PublicationActor.{CleanDB, FindAll, FindById}
import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.pattern.ask
import akka.util.Timeout
import models.Publication
import models.Publication.{PublicationOutput, PublicationsOutput}
import utils.JsonFormats._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


object PublicationRoutes {

  def getRoutes (publicationActorRef : ActorRef) : Route = {

    implicit val stringToDateTime: Unmarshaller[String, Instant ] = Unmarshaller.strict[String, Instant](Instant.parse)

    path("api" / "posts") {
      get {
        parameter("from".as[Instant] ?, "to".as[Instant] ?, "author".as[String] ?) {(from, to, author) =>
          complete {
            implicit val timeout : Timeout = 5.seconds
            val publicationsFuture = (publicationActorRef ? FindAll(from = from.map(_.toEpochMilli), to = to.map(_.toEpochMilli), author = author)).mapTo[List[Publication]]
            publicationsFuture.map {
              publications =>PublicationsOutput(publications.size, publications)
            }
          }
        }
      }
    } ~ pathPrefix("api" / "posts" / Segment ) { id =>
      get {
        complete {
          implicit val timeout: Timeout = 5.seconds
          val publicationFuture = (publicationActorRef ? FindById(id)).mapTo[Option[Publication]]
          publicationFuture.map {
              case Some(publiation) => PublicationOutput(Some(publiation))
              case None => PublicationOutput(None)
            }
          }
        }
    } ~ path("admin" / "clean") {
      get {
        complete {
          implicit val timeout : Timeout = 5.seconds
          publicationActorRef ! CleanDB()
          "DB Cleaned !"
        }
      }
    }
  }
}