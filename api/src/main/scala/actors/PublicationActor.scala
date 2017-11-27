package actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.immutable.Document
import services.PublicationService

class PublicationActor () (implicit coll : MongoCollection[Document]) extends Actor with ActorLogging {

  import PublicationActor._
  import context._

  override def receive: PartialFunction[Any, Unit] = {

    case FindAll(from, to , author) =>
      PublicationService.findAll(from, to, author) pipeTo sender

    case FindById(id) =>
      PublicationService.findById(id) pipeTo sender

    case CleanDB() =>
      println("Hello")
      PublicationService.cleanDb()
  }
}

object PublicationActor {

  def props () (implicit coll : MongoCollection[Document]) = Props(new PublicationActor() (coll))

  case class FindAll (from : Option[Long] = None, to : Option[Long] = None, author : Option[String] = None)
  case class FindById (id : String)
  case class CleanDB()

}

