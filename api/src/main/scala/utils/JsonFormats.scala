package utils

import models.Publication
import models.Publication.{PublicationOutput, PublicationsOutput}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


object JsonFormats extends DefaultJsonProtocol  {

  implicit val publicationFormat: RootJsonFormat[Publication] = jsonFormat4(Publication.apply)
  implicit val publicationsOutPut : RootJsonFormat[PublicationsOutput] = jsonFormat2(PublicationsOutput.apply)
  implicit val publicationOutPut : RootJsonFormat[PublicationOutput] = jsonFormat1(PublicationOutput.apply)

}


