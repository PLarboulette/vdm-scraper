package utils

import models.Post
import models.Post.{PostOutput, PostsOutput}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats extends DefaultJsonProtocol  {
  implicit val publicationFormat: RootJsonFormat[Post] = jsonFormat4(Post.apply)
  implicit val publicationsOutPut : RootJsonFormat[PostsOutput] = jsonFormat2(PostsOutput.apply)
  implicit val publicationOutPut : RootJsonFormat[PostOutput] = jsonFormat1(PostOutput.apply)
}