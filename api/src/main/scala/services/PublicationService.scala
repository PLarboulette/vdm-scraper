package services

import database.MongoHelper
import models.Publication
import org.bson.conversions.Bson
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters.{equal, _}
import org.mongodb.scala.{Completed, MongoCollection}

import scala.concurrent.{ExecutionContext, Future}

object PublicationService {


  def findAll (from : Option[Long] = None, to : Option[Long] = None, author : Option[String] = None) (implicit coll : MongoCollection[Document], ec : ExecutionContext) : Future[List[Publication]] = {

    def addFilter (filter : Option[Bson], old : Bson): Bson = {
      filter.map (value => Filters.and(old, value)).getOrElse(Filters.and(old))
    }

    val fromFilter = addFilter(from.map {value => gt("date", value)}, Filters.and())
    val toFilter  = addFilter(to.map {value => lt("date", value)}, fromFilter)
    val allFilters = addFilter(author.map(value => equal("author", value)), toFilter)
    val finalFilters = if(from.isEmpty && to.isEmpty && author.isEmpty) None else Some(allFilters)

    for {
      publications <- MongoHelper.find[Publication](Publication.toPublication, finalFilters)
    } yield  {
      publications.sortBy(_.date).reverse
    }
  }

  def findById (id : String) (implicit ec : ExecutionContext) : Future[Option[Publication]] = {
    // TODO
    Future.successful(None)
  }

  def cleanDb () (implicit coll : MongoCollection[Document], ec : ExecutionContext) : Future[Completed] = {
    MongoHelper.cleanDb[Publication]()
  }
}
