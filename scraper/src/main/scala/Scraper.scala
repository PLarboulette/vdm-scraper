
import java.text.SimpleDateFormat
import java.util.UUID

import com.typesafe.config.ConfigFactory
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

  val mongoClient: MongoClient = MongoClient(ConfigFactory.load().getString("database.url"))
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
    val contentOpt = processContent(article)
    println(contentOpt)
    val footerOpt = processFooter(article)
    if (contentOpt.isDefined && footerOpt.isDefined){
      val content = contentOpt.get
      val (author, date) = footerOpt.get
      Some(Data(UUID.randomUUID().toString, content, author, date))
    } else {
      None
    }
  }

  def processContent (article: Element) : Option[String] = {
    val contents = article tryExtract element(".panel-content") tryExtract element("p")
    val contentText = if (contents.exists(_.isDefined == true)) {
      // Main version, works for the major part of the publications
      (contents tryExtract text("a")).flatten.flatten
    } else {
      // Second version, for other format of HTML elements
      val elementsOpt = article tryExtract elementList(".panel-content")  tryExtract elementList("p")
      elementsOpt.map {
        _.map { opt =>
            if (opt.isDefined && opt.get.nonEmpty) opt.get.head tryExtract text ("a") else None
        }
      }.flatMap(_.find(_.isDefined)).getOrElse(None)
    }

    contentText.map(content => { if (content.trim.nonEmpty) Some(content) else None }).getOrElse(None)
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
