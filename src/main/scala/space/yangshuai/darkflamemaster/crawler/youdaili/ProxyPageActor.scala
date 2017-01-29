package space.yangshuai.darkflamemaster.crawler.youdaili

import java.net.SocketTimeoutException
import java.util.regex.Pattern

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.Logger
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.crawler.youdaili.SchedulerActor.ProxyPageRequest
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/26.
  */
class ProxyPageActor extends Actor {
  import ProxyPageActor._

  private val logger = Logger[ProxyPageActor]
  private var count = 0

  override def receive: Receive = {
    case ProxyPageRequest(url, proxy, homePage) =>
      if (count >= 10) {
        logger.error(s"Retry times run out: $url($count)")
        sender ! Failed
      } else {
        count += 1
        logger.info(s"Begin to crawl $url...")
        try {
          val doc = Utils.commonRequest(url, proxy)
          if (homePage) {
            val pageNumber = extractPageNumber(doc)
            sender ! PageNumberMessage(url, pageNumber)
          }
          extractProxies(doc.getElementsByClass("content").first.getElementsByTag("p"))
          sender ! Success
        } catch {
          case _: HttpStatusException =>
            sender ! ConnectionError(url, proxy, homePage)
          case _: SocketTimeoutException =>
            sender ! ConnectionError(url, proxy, homePage)
          case _: NullPointerException =>
            sender ! ConnectionError(url, proxy, homePage)
          case e: Exception =>
            logger.error(url, e)
            sender ! Failed
        }
      }
  }

  private def extractProxies(elements: Elements): Unit = {
    for (i <- 0 until elements.size) {
      val text = elements.get(i).text.trim()
      if (text.contains(":") && text.contains("@HTTP")) {
        ProxyManager.addNewProxy(text.split("@HTTP")(0))
      }
    }
  }

  private def extractPageNumber(doc: Document): Int = {
    val text = doc.getElementsByClass("pagebreak").first.getElementsByTag("a").first.text
    val pattern = Pattern.compile("\\d+")
    val matcher = pattern.matcher(text)
    if (matcher.find()) {
      return matcher.group(0).toInt
    }
    0
  }

}

object ProxyPageActor {
  val props: Props = Props[ProxyPageActor]
  object Success
  case class PageNumberMessage(url: String, number: Int)
  case class ConnectionError(url: String, proxy: (String, Int), homePage: Boolean)
  case class Failed(url: String)
}
