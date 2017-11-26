package database

import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Filters.equal

import scala.concurrent.{ExecutionContext, Future}

object MongoHelper {


   def find[T](fn : Document => T)(implicit coll : MongoCollection[Document], ec : ExecutionContext): Future[List[T]] = {
    for {
      documents <- coll.find().toFuture().recoverWith { case e : Throwable => Future.failed(e)}
    } yield documents.toList.map(fn)
  }

  def findById[T](id : String, fn : Document => T)(implicit coll : MongoCollection[Document], ec: ExecutionContext): Future[Option[T]] = {
    for {
      documents <- coll.find(equal("_id", id)).toFuture().recoverWith{
        case e : Throwable => Future.failed(e)
      }
    } yield documents.toList.map(fn).headOption
  }

  def create[T](item: T, fn : T => Document)(implicit coll : MongoCollection[Document], ec: ExecutionContext): Future[Completed] = {
    coll.insertOne(fn(item)).toFuture().recoverWith {case e : Throwable => Future.failed(e)}
  }

  def update[T](id : String, newItem : T, fn : T => Document)(implicit coll : MongoCollection[Document], ec: ExecutionContext): Future[Either[String, Boolean]] = {
    coll.replaceOne(equal("_id", id), fn(newItem)).toFuture().recoverWith {
      case e : Throwable => Future.failed(e)
    }.map(result => if(result.getMatchedCount == 1) Right(true) else Left(s"No update made : $id"))
  }

  def delete[T] (id : String) (implicit coll : MongoCollection[Document], ec : ExecutionContext) : Future[Boolean] = {
    for {
      deletedItem <- coll.deleteOne(equal("_id", id)).toFuture()
        .recoverWith { case e: Throwable => Future.failed(e)}
        .map(result => if (result.getDeletedCount == 1) true else false)
    } yield deletedItem
  }

  def cleanDb[T] () (implicit coll: MongoCollection[Document], ec: ExecutionContext): Future[Completed] = {
    coll.drop().toFuture()
  }

}
