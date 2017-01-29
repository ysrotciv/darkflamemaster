package space.yangshuai.darkflamemaster.crawler.ip66

import java.net.{SocketException, SocketTimeoutException}

import akka.actor.{Actor, Props}
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/26.
  */
class PageActor extends Actor {
  import PageActor._

  private var count = 0

  override def receive: Receive = {
    case SchedulerActor.RequestMessage(url, proxy) =>
      if (count >= 10) {
        logger.error(s"Retry times run out: $url($count)")
        sender ! Failed
      } else {
        count += 1
        logger.info(s"Begin to crawl $url with $proxy")
        var doc: Document = null
        try {
          doc = Utils.commonRequest(url, proxy)
          val trs = doc.getElementById("footer")
            .getElementsByTag("tbody").first
            .getElementsByTag("tr")
          for ( i <- 1 until trs.size()) {
            val tds = trs.get(i).getElementsByTag("td")
            val host = tds.get(0).text
            val port = tds.get(1).text
            ProxyManager.addNewProxy(s"$host:$port")
          }
          sender ! Success
        } catch {
          case _: HttpStatusException =>
            sender ! ConnectionError(url, proxy)
          case _: SocketTimeoutException =>
            sender ! ConnectionError(url, proxy)
          case _: SocketException =>
            sender ! ConnectionError(url, proxy)
          case _: NullPointerException =>
            sender ! ConnectionError(url, proxy)
          case e: Throwable =>
            sender ! Failed
            logger.error(url, e)
        }
      }
  }
}

object PageActor {
  val props: Props = Props[PageActor]
  object Success
  object Failed
  case class ConnectionError(url: String, proxy: (String, Int))
}
