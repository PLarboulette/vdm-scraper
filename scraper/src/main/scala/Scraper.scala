
import java.text.SimpleDateFormat
import java.util.UUID

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Scraper extends App {

  println("Hello world, I'm the scraper ! ")

  val mongoClient: MongoClient = MongoClient("mongodb://localhost:27017")
  val database: MongoDatabase = mongoClient.getDatabase("vdm-scraper")
  implicit val coll: MongoCollection[Document] = database.getCollection("publications")

  case class Data (id : String, content : String, author : String, date : Long)

  launch(200, 1, List.empty[Data], test = true)

  def launch (maxElements  : Int, indexPage : Int, previousResult : List[Data], test : Boolean) : Boolean = {

    val browser = JsoupBrowser()
    val results  = processPage(browser, indexPage) ::: previousResult

    /*
    if (results.size == 200) {
      // Insert in database
    } else {
      launch(indexPage +1, results, test)
    }
    */
    true
  }

  def processPage (browser : Browser, indexPage : Int) : List[Data] = {
    val page = browser.get(s"http://www.viedemerde.fr/1page=$indexPage")

    val items = page tryExtract elementList ("article") tryExtract elementList (".art-panel") tryExtract elementList (".col-xs-12")

    items.map(_.map(_.map(_.map(_.map(_.map(processArticle(_).map {writeElement}))))))
    List.empty
  }

  def writeElement (data : Data) : Future[Completed] = {
    MongoHelper.create[Data](data, MongoHelper.toDocument)
  }

  def processArticle (article : Element) : Option[Data] = {
    processContent(article)
  }

  def processContent (article: Element) : Option[Data] = {
    val contents = article tryExtract element(".panel-content") tryExtract element("p")
    val contentText = contents tryExtract text ("a")
    contentText.flatMap(
      _.flatMap(
        _.flatMap(
          content => {
            if (content.contains("VDM")) {
              processFooter(article) match {
                case Some((author, date)) =>
                  Some(Data(UUID.randomUUID().toString, content, author, date))
                case None =>
                  None
              }
            } else {
              None
            }

          }
        )
      )
    )
  }

  /**
    * Extrat data of footer (author, date)
    * @param footer the footer element
    * @return an option of tuple which contains (author, date)
    */
  def processFooter (footer : Element) : Option[(String, Long)] = {
    // Get all the divs which contains footer information (author, date ...)
    val divs = footer tryExtract elementList(".text-center") tryExtract element ("div")
    // Extract text for these divs
    val footerText = divs tryExtract text ("div")
    // Transform footerText to obtain "line" which is the real line of data (like "Par X / Le ....")
    footerText.map(_.flatMap {_.flatMap {_.map( line => extractDataForLine(line))}}).flatMap(_.headOption).getOrElse(None)
  }

  /**
    * Extract author and date from a line of data
    * @param  line of data
    * @return a tuple (String, Long) with author and date
    */
  def extractDataForLine (line : String) : Option[(String, Long)] = {
    Try {
      val data = line.split("/")
      val author = data(0).split("Par")(1).replace("-", "").trim
      (author, new SimpleDateFormat("E d MMMM yyyy k:m").parse(data(1).trim).toInstant.toEpochMilli)
    } match {
      case Success(date2) => Some(date2)
      case Failure(e) => None
    }
  }
}
