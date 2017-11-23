import javax.swing.text.AbstractDocument.Content

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupDocument
import org.jsoup.nodes.DocumentType
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element

object Scraper extends App {

  println("Hello world, I'm the scraper ! ")

  case class Data (content : Option[String], date : Option[Long], author : Option[Content])

  launch(1, List.empty[Data], test = true)

  def launch (indexPage : Int, previousResult : List[Data], test : Boolean) : Boolean = {

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
    true
  }

  def processContent (article: Element) : String = {
    val content = article tryExtract element(".panel-content")
    ""
  }

  def processFooter (footer : Element) : String = {
    val text_center = footer tryExtract elementList(".text-center")
    val div = text_center tryExtract element("div")
    val div2 = div tryExtract text ("div")
    println(div2.getOrElse("No line for author"))
    ""
  }
}
