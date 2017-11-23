package routes
import actors.PublicationActor.{FindAll, FindById}
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import models.{Publication}
import akka.pattern.ask
import utils.MyJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.duration._

object PublicationRoutes {


  def getRoutes (publicationActorRef : ActorRef) : Route = {
    path("posts") {
      get {
        parameter("from".as[Option[String]], "to".as[Option[String]], "author".as[Option[String]]) {(from, to, author) =>
          implicit val timeout : Timeout = 5.seconds
          val posts = (publicationActorRef ? FindAll(from = from, to = to, author = author)).mapTo[List[Publication]]
          complete(posts)
        }
      }
    } ~ pathPrefix("posts" / Segment ) { id =>
      get {
        implicit val timeout: Timeout = 5.seconds
        val post = (publicationActorRef ? FindById(id)).mapTo[Option[Publication]]
        complete(post)
      }
    }
  }
}