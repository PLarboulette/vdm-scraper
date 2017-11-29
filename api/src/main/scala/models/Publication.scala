package models

import org.mongodb.scala.bson.collection.Document

case class Publication(id : String, content : Option[String], date : Option[String], author : Option[String])

object Publication {


  case class PublicationsOutput (count : Int, posts : List[Publication])

  case class PublicationOutput (post : Option[Publication])


  implicit def toDocument (publication : Publication) : Document = {
    Document(
      "_id" -> publication.id,
      "content" -> publication.content,
      "date" -> publication.date,
      "author" -> publication.author
    )
  }

  implicit def toPublication (document : Document) : Publication = {
    Publication(
      document.get("_id").map(_.asString().getValue).getOrElse(""),
      document.get("content").map(_.asString().getValue),
      document.get("date").map(_.asString().getValue),
      document.get("author").map(_.asString().getValue)
    )
  }

}
