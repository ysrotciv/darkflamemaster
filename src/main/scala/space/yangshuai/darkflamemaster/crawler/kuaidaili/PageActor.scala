package space.yangshuai.darkflamemaster.crawler.kuaidaili

import java.net.{SocketException, SocketTimeoutException}

import akka.actor.{Actor, Props}
import org.jsoup.HttpStatusException
import space.yangshuai.darkflamemaster.common.{Messages, Utils}
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by yang
  * Created on 28/01/2017.
  */
class PageActor extends Actor {

  private var count = 0
  private val cookie = "channelid=0; sid=1485599520828435; _gat=1; Hm_lvt_7ed65b1cc4b810e9fd37959c9bb51b31=1485525092; Hm_lpvt_7ed65b1cc4b810e9fd37959c9bb51b31=1485603228; _ga=GA1.2.1271044218.1485071409"

  override def receive: Receive = {
    case Messages.RequestMessage(url, proxy) =>
      if (count >= 1) {
        sender ! Messages.Failed
      } else {
        count += 1
        try {
          val doc = Utils.commonRequest(url, proxy, cookie)
          val trs = doc.getElementById("list")
            .getElementsByTag("tbody").first
            .getElementsByTag("tr")
          for ( i <- 0 until trs.size()) {
            val tds = trs.get(i).getElementsByTag("td")
            val host = tds.get(0).text
            val port = tds.get(1).text
            ProxyManager.addNewProxy(s"$host:$port")
          }
          sender ! Messages.Start
        } catch {
          case e: HttpStatusException =>
            sender ! Messages.ConnectionError(url, proxy)
            logger.error("", e)
          case _: SocketTimeoutException =>
            sender ! Messages.ConnectionError(url, proxy)
          case _: SocketException =>
            sender ! Messages.ConnectionError(url, proxy)
          case _: NullPointerException =>
            sender ! Messages.ConnectionError(url, proxy)
          case e: Throwable =>
            sender ! Messages.Failed
            logger.error(url, e)
        }
      }
  }

}

object PageActor {
  val props: Props = Props[PageActor]
}
