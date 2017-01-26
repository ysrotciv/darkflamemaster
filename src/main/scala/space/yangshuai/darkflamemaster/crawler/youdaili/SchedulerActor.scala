package space.yangshuai.darkflamemaster.crawler.youdaili

import akka.actor.{Actor, ActorRef, Props}
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/26.
  */
class SchedulerActor extends Actor {
  import SchedulerActor._

  private val HOME_PAGE = "http://www.youdaili.net/Daili/guonei/"
  private var scheduler: ActorRef = _
  private var pageCount: Int = -1

  override def receive: Receive = {
    case Start =>
      scheduler = sender
      val actor = YouDaiLiCrawler.system.actorOf(HomePageActor.props, "home_page_actor")
      logger.info("Begin to crawl home page...")
      actor ! HomePageRequest(HOME_PAGE, ProxyManager.getProxy)
    case URLMessage(url) =>
      val actor = YouDaiLiCrawler.system.actorOf(ProxyPageActor.props, "first_page_actor")
      logger.info("Begin to crawl first page...")
      actor ! ProxyPageRequest(url, ProxyManager.getProxy, homePage = true)
    case HomePageActor.ConnectionError(url, proxy) =>
      if (proxy == null) {
        scheduler ! "Failed during crawling home page."
      } else {
        val currentProxy = ProxyManager.getProxy
        if (currentProxy equals proxy) {
          sender ! HomePageRequest(url, ProxyManager.updateProxy())
        } else {
          sender ! HomePageRequest(url, currentProxy)
        }
      }
    case HomePageActor.Failed =>
      scheduler ! "Failed during crawling home page."
    case ProxyPageActor.PageNumberMessage(firstPageURL, pageNumber) =>
      if (pageNumber < 2) scheduler ! "Failed during crawling first page."
      pageCount = pageNumber - 1
      if (pageNumber >= 2) {
        for (i <- 2 to pageNumber) {
          val url = s"${firstPageURL.split(".html")(0)}_$i.html"
          val actor = YouDaiLiCrawler.system.actorOf(ProxyPageActor.props, url)
          actor ! ProxyPageRequest(url, ProxyManager.getProxy)
        }
      }
    case ProxyPageActor.ConnectionError(url, proxy, isHomePage) =>
      if (proxy == null) {
        if (pageCount != -1) pageCount -= 1
        if (pageCount <= 0) scheduler ! "complete"
      } else {
        val currentProxy = ProxyManager.getProxy
        if (currentProxy equals proxy) {
          sender ! ProxyPageRequest(url, ProxyManager.updateProxy(), isHomePage)
        } else {
          sender ! ProxyPageRequest(url, currentProxy, isHomePage)
        }
      }
    case ProxyPageActor.Failed(url) =>
      logger.error(s"Failed during crawling $url.")
      pageCount -= 1
      if (pageCount <= 0) scheduler ! "complete"
    case ProxyPageActor.Success =>
      pageCount -= 1
      if (pageCount <= 0) scheduler ! "complete"
  }

}

object SchedulerActor {
  val props: Props = Props[SchedulerActor]
  object Start
  case class HomePageRequest(url: String, proxy: (String, Int))
  case class ProxyPageRequest(url: String, proxy: (String, Int), homePage: Boolean = false)
  case class URLMessage(url: String)
}
