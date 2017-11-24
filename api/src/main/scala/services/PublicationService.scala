package services

import java.time.Instant

import models.Publication

import scala.concurrent.{ExecutionContext, Future}

object PublicationService {


  def findAll (from : Option[String] = None, to : Option[String] = None, author : Option[String] = None) (implicit ec : ExecutionContext) : Future[List[Publication]] = {
    // TODO
    val fromInstant = from.map(Instant.parse)
    Future.successful(List.empty)
  }

  def findById (id : String) (implicit ec : ExecutionContext) : Future[Option[Publication]] = {
    // TODO
    Future.successful(None)
  }
}
