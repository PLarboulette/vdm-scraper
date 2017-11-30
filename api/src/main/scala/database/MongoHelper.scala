package database

import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import scala.concurrent.{ExecutionContext, Future}

object MongoHelper {

  /**
    * Helper find
    * @param fn Function to convert Document to model
    * @param filter List of filters
    * @param coll
    * @param ec
    * @tparam T Type of Document (here Post)
    * @return a list of T (Here Post)
    */
   def find[T](fn : Document => T, filter : Option[Bson])(implicit coll : MongoCollection[Document], ec : ExecutionContext): Future[List[T]] = {
     for {
      documents <- filter.map(filters => coll.find(filters)).getOrElse(coll.find()).toFuture().recoverWith { case e : Throwable => Future.failed(e)}
    } yield documents.toList.map(fn)
  }

  /**
    * Helper findById
    * @param id the id of the asked document
    * @param fn a function to convert Document to model (here Post)
    * @param coll
    * @param ec
    * @tparam T ype of Document (here Post)
    * @return an option of T
    */
  def findById[T](id : String, fn : Document => T)(implicit coll : MongoCollection[Document], ec: ExecutionContext): Future[Option[T]] = {
    for {
      documents <- coll.find(equal("_id", id)).toFuture().recoverWith{
        case e : Throwable => Future.failed(e)
      }
    } yield documents.toList.map(fn).headOption
  }

  /**
    * Helper cleanDB
    * @param coll
    * @param ec
    * @tparam T T ype of Document (here Post)
    * @return a completed statement
    */
  def cleanDb[T] () (implicit coll: MongoCollection[Document], ec: ExecutionContext): Future[Completed] = {
    coll.drop().toFuture()
  }

}
