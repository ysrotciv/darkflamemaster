package space.yangshuai.darkflamemaster.crawler.ip66

import java.net.SocketTimeoutException

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive
import org.jsoup.HttpStatusException
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/26.
  */
class PageActor extends Actor {
  import PageActor._

  override def receive: Receive = {
    case SchedulerActor.RequestMessage(url, proxy) =>
      logger.info(s"Begin to crawl $url with $proxy")
      try {
        val trs = Utils.commonRequest(url, proxy)
          .getElementById("footer")
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
        case e: HttpStatusException =>
          sender ! ConnectionError(url, proxy)
          logger.error(url, e)
        case e: SocketTimeoutException =>
          sender ! ConnectionError(url, proxy)
          logger.error(url, e)
        case e: Throwable =>
          sender ! Failed
          logger.error(url, e)
      }
  }
}

object PageActor {
  val props: Props = Props[PageActor]
  object Success
  object Failed
  case class ConnectionError(url: String, proxy: (String, Int))
}
