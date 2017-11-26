import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import javax.swing.text.AbstractDocument.Content

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Scraper extends App {

  println("Hello world, I'm the scraper ! ")

  case class Data (content : String, author : String, date : Long)

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

  def writeElement (data : Data) : Future[Unit] = {
    println(data)
    println("####################")
    Future.successful()
  }

  def processArticle (article : Element) : Option[Data] = {
    processContent(article)
  }

  def processContent (article: Element) : Option[Data] = {
    val content = article tryExtract element(".panel-content") tryExtract element("p")
    val contentText = content tryExtract text ("a")
    contentText.flatMap(
      _.flatMap(
        _.flatMap(
          item => {
            processFooter(article) match {
              case Some((author, date)) =>
                Some(Data(item, author, date))
              case None =>
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
      case Failure(e) =>
        println(e.getMessage)
        None
    }

  }
}
