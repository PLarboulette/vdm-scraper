package database

import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import scala.concurrent.{ExecutionContext, Future}

object MongoHelper {

   def find[T](fn : Document => T, filter : Option[Bson])(implicit coll : MongoCollection[Document], ec : ExecutionContext): Future[List[T]] = {
     for {
      documents <- filter.map(filters => coll.find(filters)).getOrElse(coll.find()).toFuture().recoverWith { case e : Throwable => Future.failed(e)}
    } yield documents.toList.map(fn)
  }

  def findById[T](id : String, fn : Document => T)(implicit coll : MongoCollection[Document], ec: ExecutionContext): Future[Option[T]] = {
    for {
      documents <- coll.find(equal("_id", id)).toFuture().recoverWith{
        case e : Throwable => Future.failed(e)
      }
    } yield documents.toList.map(fn).headOption
  }

  def cleanDb[T] () (implicit coll: MongoCollection[Document], ec: ExecutionContext): Future[Completed] = {
    coll.drop().toFuture()
  }

}
