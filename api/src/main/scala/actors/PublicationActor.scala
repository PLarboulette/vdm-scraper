package actors

import actors.PublicationActor.FindAll
import akka.actor.{Actor, ActorLogging}


class PublicationActor extends Actor with ActorLogging {


  override def receive: PartialFunction[Any, Unit] = {

    case FindAll(from, to , author) =>
      println(s"Find All $from / $to / $author")
  }
}

object PublicationActor {

  case class FindAll (from : Option[String] = None, to : Option[String] = None, author : Option[String] = None)
  case class FindById (id : String)

}

