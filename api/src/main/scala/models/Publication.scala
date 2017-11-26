package models

import org.mongodb.scala.bson.collection.Document

case class Publication(id : String, content : Option[String], date : Option[Long], author : Option[String])
{

}

object Publication {

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
      document.get("date").map(_.asDouble().getValue.toLong),
      document.get("author").map(_.asString().getValue)
    )
  }

}
