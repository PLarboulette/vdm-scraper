import Scraper.Data
import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.bson.Document

import scala.concurrent.{ExecutionContext, Future}

object MongoHelper {

  implicit def toDocument (data : Data) : Document = {
    Document(
      "_id" -> data.id,
      "content" -> data.content,
      "date" -> data.date,
      "author" -> data.author
    )
  }

  def create[T](item: T, fn : T => Document)(implicit coll : MongoCollection[Document], ec: ExecutionContext): Future[Completed] = {
    coll.insertOne(fn(item)).toFuture().recoverWith {case e : Throwable => Future.failed(e)}
  }

}
