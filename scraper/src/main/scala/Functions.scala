import java.text.SimpleDateFormat
import Scraper.Data
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList, text}
import java.util.{Locale, UUID}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.mongodb.scala.{Completed, Document, MongoCollection}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Functions {

  /**
    * Launch the scraper !
    * @param maxElements the max number of elements to get
    * @param indexPage the index of the page to scrap
    * @param previousResult the previous list of results
    * @param coll
    * @param ec
    * @return the list of data scraped
    */
  def launch (maxElements  : Int, indexPage : Int, previousResult : List[Data]) (implicit coll : MongoCollection[Document], ec : ExecutionContext) : List[Data] = {
    val browser = JsoupBrowser()
    val results = processPage(browser, indexPage).map {
      item => if (previousResult.exists(elem => elem.author == item.author && elem.date == item.date && elem.content == item.content )) None else Some(item)
    }.filter(_.isDefined).map(_.get) ::: previousResult

    if (results.size >= maxElements) {
      val temp = results.take(maxElements)
      writeElement(temp)
      temp
    } else {
      launch(maxElements, indexPage + 1, results)
    }
  }

  /**
    * Process one page on VDM
    * @param browser Browser item
    * @param indexPage the index of the page
    * @param coll
    * @param ec
    * @return A list of case classes Data which contains all the data of the different posts
    */
  def processPage (browser : Browser, indexPage : Int)(implicit coll : MongoCollection[Document], ec : ExecutionContext) : List[Data] = {
    val page = browser.get(s"http://www.viedemerde.fr/?page=$indexPage")
    val items = page tryExtract elementList ("article") tryExtract elementList (".art-panel") tryExtract elementList (".col-xs-12")
    items.map {_.flatMap {_.map {_.flatMap {_.map {_.map { step6 =>processArticle(step6)
    }}.getOrElse(List())}}.getOrElse(List())}}.getOrElse(List())
      .filter(_.isDefined).map(_.get)
  }

  /**
    * Write element in DB
    * @param data the data to write
    * @param coll
    * @param ec
    * @return A future of Completed which indicates the status of the operation
    */
  def writeElement (data : List[Data]) (implicit coll : MongoCollection[Document], ec : ExecutionContext) : Future[Completed] = {
    MongoHelper.createMany[Data](data, MongoHelper.toDocument)
  }

  /**
    *
    * @param post the post to parse
    * @param coll
    * @param ec
    * @return return an option of case class Data which contains the expected data
    */
  def processArticle (post : Element) (implicit coll : MongoCollection[Document], ec : ExecutionContext) : Option[Data] = {
    val contentOpt = processContent(post)
    val footerOpt = processFooter(post)
    if (contentOpt.isDefined && footerOpt.isDefined){
      val content = contentOpt.get
      val (author, date) = footerOpt.get
      Some(Data(UUID.randomUUID().toString, content, author, date))
    } else {
      None
    }
  }

  /**
    * Extract content of the post
    * @param content The HTML element to parse
    * @param coll
    * @param ec
    * @return an option of String with the content extracted
    */
  def processContent (content: Element) (implicit coll : MongoCollection[Document], ec : ExecutionContext) : Option[String] = {
    val contents = content tryExtract element(".panel-content") tryExtract element("p")
    val contentText = if (contents.exists(_.isDefined == true)) {
      // Main version, works for the major part of the posts
      (contents tryExtract text("a")).flatten.flatten
    } else {
      // Second version, for other format of HTML elements
      val elementsOpt = content tryExtract elementList(".panel-content")  tryExtract elementList("p")
      elementsOpt.map {
        _.map { opt =>
          if (opt.isDefined && opt.get.nonEmpty) opt.get.head tryExtract text ("a") else None
        }
      }.flatMap(_.find(_.isDefined)).getOrElse(None)
    }
    contentText.map(content => { if (content.trim.nonEmpty) Some(content) else None }).getOrElse(None)
  }

  /**
    * Extract data of footer (author, date)
    * @param footer the footer element
    * @return an option of tuple which contains (author, date)
    */
  def processFooter (footer : Element)(implicit coll : MongoCollection[Document], ec : ExecutionContext) : Option[(String, String)] = {
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
  def extractDataForLine (line : String)(implicit coll : MongoCollection[Document], ec : ExecutionContext) : Option[(String, String)] = {
    Try {
      val data = line.split("/")
      val author = data(0).split("Par")(1).replace("-", "").trim
      (author, new SimpleDateFormat("E d MMMM yyyy k:m", Locale.FRANCE).parse(data(1).trim).toInstant.toString)
    } match {
      case Success(date2) => Some(date2)
      case Failure(e) =>
        None
    }
  }
}
