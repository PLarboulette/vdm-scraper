package actors

import actors.PublicationActor.FindAll
import akka.actor.{Actor, ActorLogging, Props}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.immutable.Document
import services.PublicationService
import akka.pattern.pipe

class PublicationActor () (implicit coll : MongoCollection[Document]) extends Actor with ActorLogging {


  import context._

  override def receive: PartialFunction[Any, Unit] = {

    case FindAll(from, to , author) =>
      PublicationService.findAll(from, to, author) pipeTo sender
      println(s"Find All $from / $to / $author")
  }
}

object PublicationActor {

  def props () (implicit coll : MongoCollection[Document]) = Props(new PublicationActor() (coll))

  case class FindAll (from : Option[Long] = None, to : Option[Long] = None, author : Option[String] = None)
  case class FindById (id : String)

}

