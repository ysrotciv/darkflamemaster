package space.yangshuai.darkflamemaster.crawler.youdaili

import java.net.SocketTimeoutException

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.Logger
import org.jsoup.HttpStatusException
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.crawler.youdaili.SchedulerActor.HomePageRequest


/**
  * Created by rotciv on 2017/1/26.
  */
class HomePageActor extends Actor {
  import HomePageActor._

  val logger: Logger = Logger[HomePageActor]

  override def receive: Receive = {
    case HomePageRequest(url, proxy) =>
      try {
        val firstPage = Utils.commonRequest(url, proxy)
          .getElementsByClass("chunlist").first()
          .getElementsByTag("a").first().attr("href")
        sender ! SchedulerActor.URLMessage(firstPage)
      } catch {
        case e: HttpStatusException =>
          logger.error(url, e)
          sender ! ConnectionError(url, proxy)
        case e: SocketTimeoutException =>
          logger.error(url, e)
          sender ! ConnectionError(url, proxy)
        case e: Exception =>
          logger.error(url, e)
          sender() ! Failed
      }

  }

}

object HomePageActor {
  val props: Props = Props[HomePageActor]
  case class ConnectionError(url: String, proxy: (String, Int))
  object Failed
}
