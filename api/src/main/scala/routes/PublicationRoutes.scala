package routes
import actors.PublicationActor.{FindAll, FindById}
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix, _}
import akka.actor.ActorRef
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import models.Publication
import akka.pattern.ask
import utils.MyJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import org.joda.time.DateTime

import scala.concurrent.duration._

object PublicationRoutes {

  val stringToDateTime = Unmarshaller.strict[String, DateTime](DateTime.parse)

  def getRoutes (publicationActorRef : ActorRef) : Route = {

    path("api" / "posts") {
      get {
        parameter("from".as(stringToDateTime).?, "to".as(stringToDateTime).?, "author".as[String].?) {(from, to, author) =>
          complete {
            implicit val timeout : Timeout = 5.seconds
            val posts = (publicationActorRef ? FindAll(from = from.map(_.toString), to = to.map(_.toString), author = author)).mapTo[List[Publication]]
            "OK"
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