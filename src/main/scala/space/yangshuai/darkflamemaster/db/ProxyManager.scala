package space.yangshuai.darkflamemaster.db

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.jsoup.Jsoup
import org.nutz.ssdb4j.SSDBs
import space.yangshuai.darkflamemaster.crawler.KuaidailiCrawler
import space.yangshuai.darkflamemaster.exception.DFMProxyExhaustedException

/**
  * Created by rotciv on 2017/1/24.
  */
object ProxyManager {

  private val logger = Logger("ProxyManager")

  private val TEST_URLS = Array[String]("http://www.baidu.com", "http://www.163.com", "http://so.com")
  private val proxyQueue = collection.mutable.Queue[(String, Int)]()
  private var currentProxy: (String, Int) = _

  private val conf = ConfigFactory.load()
  private val host = conf.getString("ssdb_host")
  private val port = conf.getInt("ssdb_port")
  private val timeout = conf.getInt("ssdb_timeout")
  private val ssdb = SSDBs.pool(host, port, timeout, null)

  private val PROXY_LIVE = "proxy_live"

  private val DEFAULT_SCORE = 0
  private val DEFAULT_TTL = 24 * 60 * 60
  private val IP_REGEX = "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))"

  /**
    * Add a new proxy to "proxy_live"
    * @param proxy a string represents proxy for example "123.123.123.123:8080"
    * @return return true if and only the new proxy is valid and not exists before
    */
  def addNewProxy(proxy: String): Boolean = {
    if (checkProxy(proxy))
      ssdb.zset(PROXY_LIVE, proxy, DEFAULT_SCORE).ok()
    else
      false
  }

  /**
    * Check if the proxy can access one of the websites in ${TEST_URLS}
    * @param proxy a string represents proxy for example "123.123.123.123:8080"
    * @return checking result
    */
  def checkProxy(proxy: String): Boolean = {
    if (ssdb.exists(proxy).asInt() > 0) {
      logger.info(s"$proxy already lose efficacy")
      return false
    }

    if (ssdb.zexists(PROXY_LIVE, proxy).asInt() > 0) {
      logger.info(s"$proxy already exists in $PROXY_LIVE")
      return false
    }

    val arr = proxy.split(":")
    if (arr == null || arr.size != 2) {
      logger.info(s"$proxy FAILED")
      return false
    }

    val host = arr(0)
    val port = arr(1).toInt

    val result = TEST_URLS.exists(url => {
      try {
        Jsoup.connect(url).timeout(10000).proxy(host, port).execute().statusCode() == 200
      } catch {
        case _: Exception =>
          false
      }
    })

    if (result) {
      logger.info(s"$proxy SUCCESS")
      true
    } else {
      ssdb.zdel(PROXY_LIVE, proxy)
      ssdb.setx(proxy, "", DEFAULT_TTL)
      logger.info(s"$proxy FAILED")
      false
    }
  }

  /**
    * Check all the proxies in "proxy_live", save the invalid ones as key-value format and add ttl for 24 hours.
    * @return the number of valid proxies in "proxy_live"
    */
  def checkAllProxies(): Int = {
    val list = ssdb.zscan(PROXY_LIVE, "", "", "", -1).datas
    val iterator = list.iterator()
    while (iterator.hasNext) {
      val proxy = new String(iterator.next())
      checkProxy(proxy)
      iterator.next()
    }
    ssdb.zsize(PROXY_LIVE).asInt()
  }

  /**
    * Get all the proxies in "proxy_live".
    * @return
    */
  def getProxies: List[(String, Int)] = {
    import scala.collection.JavaConversions._
    val list = ssdb.zscan(PROXY_LIVE, "", "", "", -1).datas
    list.toList.map(bytes => {
      val arr = new String(bytes).split(":")
      if (arr == null || arr.length != 2 || !validPort(arr(1)) || !validHost(arr(0)))
        null
      else
        (arr(0), arr(1).toInt)
    }).filter(_ != null)
  }

  /**
    * Check if the string represents a valid host.
    * @param host host string
    * @return
    */
  private def validHost(host: String): Boolean = host.matches(IP_REGEX)

  /**
    * Check if the string represents a valid port.
    * @param port port string
    * @return
    */
  private def validPort(port: String): Boolean = {
    if (!port.matches("\\d+")) return false
    if (port.toInt < 0 || port.toInt > 65535) return false
    true
  }

  def updateProxy(): Unit = {
    if (proxyQueue.isEmpty) {
      proxyQueue ++= getProxies
    }
    if (proxyQueue.isEmpty) throw DFMProxyExhaustedException()
    currentProxy = proxyQueue.dequeue()
    logger.info(s"change proxy to $currentProxy")
  }

  def getProxy: (String, Int) = {
    if (currentProxy == null)
      updateProxy()
    currentProxy
  }

  def main(args: Array[String]): Unit = {
    checkAllProxies()
    KuaidailiCrawler.crawl()
    ssdb.close()
  }

}
