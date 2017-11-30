package actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.immutable.Document
import services.PostService

class PostActor()(implicit coll : MongoCollection[Document]) extends Actor with ActorLogging {

  import PostActor._
  import context._

  override def receive: PartialFunction[Any, Unit] = {

    case FindAll(from, to , author) =>
      PostService.findAll(from, to, author) pipeTo sender

    case FindById(id) =>
      PostService.findById(id) pipeTo sender

    case CleanDB() =>
      PostService.cleanDb()
  }
}

object PostActor {

  def props () (implicit coll : MongoCollection[Document]) = Props(new PostActor() (coll))

  case class FindAll (from : Option[String] = None, to : Option[String] = None, author : Option[String] = None)
  case class FindById (id : String)
  case class CleanDB()

}

