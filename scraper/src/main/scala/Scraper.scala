import javax.swing.text.AbstractDocument.Content

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

object Scraper extends App {

  println("Hello world, I'm the scraper ! ")

  case class Data (content : Option[String], date : Option[Long], author : Option[Content])

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

    items.map(
      _.map(
        _.map(
          _.map(
            _.map(
              _.map(processArticle)
            )
          )
        )
      )
    )
    List.empty
  }

  def processArticle (article : Element) : Boolean = {
    val content = processContent(article)
    val footer = processFooter(article)
    println(footer.getOrElse("No data"))
    true
  }

  def processContent (article: Element) : String = {
    val content = article tryExtract element(".panel-content")
    "TODO"
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
    footerText.map(_.map {_.map { line => extractDataForLine(line)}}).flatMap(_.headOption.getOrElse(None))
  }

  /**
    * Extract author and date from a line of data
    * @param line the line of data
    * @return a tuple (String, Long) with author and date
    */
  def extractDataForLine (line : Option[String]) : (String, Long) = {
    ("TODO", -1)
  }


}
