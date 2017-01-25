package space.yangshuai.darkflamemaster.crawler

import java.net.SocketTimeoutException

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.jsoup.{HttpStatusException, Jsoup}
import org.jsoup.select.Elements
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/24.
  */
object KuaidailiCrawler extends Crawler {

  private val HOME_PAGE = "http://www.kuaidaili.com/free/inha/"
  private val logger = Logger("kuaidaili")

  override def crawl(): Unit = {

    val conf = ConfigFactory.load()
    val userAgent = conf.getString("user_agent")
    val proxies = ProxyManager.getProxies
    val result = if (requestHomePage(userAgent, proxies)) "success" else "failed"
    logger.info(s"crawl homepage $result")

    var pageNumber = 2
    var emptyTimes = 0
    while (true) {
      logger.info(s"Begin to crawl page: $pageNumber")
      val url = HOME_PAGE + pageNumber
      var exists = false

      try {

        val trs: Elements = request(userAgent, url)

        //if current page exists a valid proxy
        for ( i <- 0 until trs.size()) {
          val tds = trs.get(i).getElementsByTag("td")
          val host = tds.get(0).text
          val port = tds.get(1).text
          if (ProxyManager.addNewProxy(s"$host:$port")) exists = true
        }


      } catch {
        case e: HttpStatusException =>
          ProxyManager.updateProxy()
          logger.error(url, e)
        case e: SocketTimeoutException =>
          ProxyManager.updateProxy()
          logger.error(url, e)
        case e: Throwable =>
          logger.error(url, e)
      } finally {
        //if specific number of continuous pages doesn't have valid proxy, then return
        if (!exists) {
          emptyTimes += 1
          if (emptyTimes >= 5)
            return
        } else {
          emptyTimes = 0
        }
        logger.info(s"Empty times: $emptyTimes")
        pageNumber += 1
      }
    }

  }

  /**
    * Request home page, use proxies in ssdb first, use local ip if all of them failed.
    * @param userAgent user agent read from application.conf
    * @param proxies proxies read from ssdb
    * @return if request successful
    */
  private def requestHomePage(userAgent: String, proxies: List[(String, Int)]): Boolean = {
    proxies.exists(proxy => {
      try {
        val doc = Jsoup.connect(HOME_PAGE)
          .userAgent(userAgent)
          .proxy(proxy._1, proxy._2)
          .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
          .header("Accept-Encoding", "gzip, deflate, sdch")
          .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2,es;q=0.2")
          .header("Cache-Control", "max-age=0")
          .header("Connection", "keep-alive")
          .header("Host", "www.kuaidaili.com")
          .header("Cookie", "channelid=0; sid=1485073248119755; _ga=GA1.2.825227426.1485074036; _gat=1")
          .timeout(10000).get()
        val trs = doc.getElementById("list").getElementsByTag("tbody").first.getElementsByTag("tr")
        for ( i <- 0 until trs.size()) {
          val tds = trs.get(i).getElementsByTag("td")
          val host = tds.get(0).text
          val port = tds.get(1).text
          ProxyManager.addNewProxy(s"$host:$port")
        }
        return true
      } catch {
        case _: Exception =>
          false
      }
    })

    for (_ <- 0 until 5) {
      try {
        val doc = Jsoup.connect(HOME_PAGE)
          .userAgent(userAgent)
          .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
          .header("Accept-Encoding", "gzip, deflate, sdch")
          .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2,es;q=0.2")
          .header("Cache-Control", "max-age=0")
          .header("Connection", "keep-alive")
          .header("Host", "www.kuaidaili.com")
          .header("Cookie", "channelid=0; sid=1485073248119755; _ga=GA1.2.825227426.1485074036; _gat=1")
          .timeout(10000).get()
        val trs = doc.getElementById("list").getElementsByTag("tbody").first.getElementsByTag("tr")
        for ( i <- 0 until trs.size()) {
          val tds = trs.get(i).getElementsByTag("td")
          val host = tds.get(0).text
          val port = tds.get(1).text
          ProxyManager.addNewProxy(s"$host:$port")
        }
        return true
      } catch {
        case _: Exception =>
      }
    }

    false
  }

  /**
    * Request the given page with the given user agent.
    * @param userAgent user agent read from application.conf
    * @param url the url to be requested
    * @return Jsoup elements which represent the proxies
    */
  private def request(userAgent: String, url: String): Elements = {
    val (host, port) = ProxyManager.getProxy
    val doc = Jsoup.connect(url)
      .userAgent(userAgent)
      .proxy(host, port)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
      .header("Accept-Encoding", "gzip, deflate, sdch")
      .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2,es;q=0.2")
      .header("Cache-Control", "max-age=0")
      .header("Connection", "keep-alive")
      .header("Host", "www.kuaidaili.com")
      .header("Cookie", "channelid=0; sid=1485073248119755; _ga=GA1.2.825227426.1485074036; _gat=1")
      .timeout(20000).get()
    try {
      doc.getElementById("list").getElementsByTag("tbody").first.getElementsByTag("tr")
    } catch {
      case e: NullPointerException =>
        logger.error(doc.toString, e)
        new Elements()
    }

  }

}
