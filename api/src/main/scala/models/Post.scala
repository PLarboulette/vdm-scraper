package models

import org.mongodb.scala.bson.collection.Document

case class Post(id : String, content : Option[String], date : Option[String], author : Option[String])

object Post {

  case class PostsOutput (count : Int, posts : List[Post])
  case class PostOutput (post : Option[Post])

  implicit def toDocument (post : Post) : Document = {
    Document(
      "_id" -> post.id,
      "content" -> post.content,
      "date" -> post.date,
      "author" -> post.author
    )
  }

  implicit def toPost (document : Document) : Post = {
    Post(
      document.get("_id").map(_.asString().getValue).getOrElse(""),
      document.get("content").map(_.asString().getValue),
      document.get("date").map(_.asString().getValue),
      document.get("author").map(_.asString().getValue)
    )
  }
}
