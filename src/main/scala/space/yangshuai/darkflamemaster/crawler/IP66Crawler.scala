package space.yangshuai.darkflamemaster.crawler

import java.net.SocketTimeoutException

import com.typesafe.scalalogging.Logger
import org.jsoup.{HttpStatusException, Jsoup}
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/25.
  */
object IP66Crawler extends Crawler{

  val logger = Logger("66IP")

  override def start(): Unit = {

    val proxy = ProxyManager.getProxy

    for (i <- 1 to 26) {
      val url = s"http://www.66ip.cn/areaindex_$i/1.html"
      logger.info(s"Begin to crawl $url")
      try {
        val trs = request(url, proxy)
        for ( i <- 1 until trs.size()) {
          val tds = trs.get(i).getElementsByTag("td")
          val host = tds.get(0).text
          val port = tds.get(1).text
          ProxyManager.addNewProxy(s"$host:$port")
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
      }
    }

  }

  def request(url: String, proxy: (String, Int)): Elements = {

    var doc: Document = null
    val request = Jsoup.connect(url).userAgent(Utils.userAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
      .header("Accept-Encoding", "gzip, deflate, sdch")
      .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2,es;q=0.2")
      .header("Cache-Control", "max-age=0")
      .header("Connection", "keep-alive")
      .header("Host", "www.66ip.cn")
      .timeout(10000)

    if (proxy == null)
      doc = request.get()
    else
      doc = request.proxy(proxy._1, proxy._2).get()

    try {
      doc.getElementById("footer").getElementsByTag("tbody").first.getElementsByTag("tr")
    } catch {
      case e: NullPointerException =>
        logger.error(doc.toString, e)
        new Elements()
    }
  }

}
