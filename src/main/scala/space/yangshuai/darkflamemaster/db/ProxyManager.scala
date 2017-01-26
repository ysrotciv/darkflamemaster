package space.yangshuai.darkflamemaster.db

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.jsoup.Jsoup
import org.nutz.ssdb4j.SSDBs
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.crawler.ip66.IP66Crawler
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
    * @param ifNew if the proxy is new, opposite to the ones in "proxy_live"
    * @return checking result
    */
  def checkProxy(proxy: String, ifNew: Boolean = true): Boolean = {
    if (!Utils.validProxy(proxy)) return false

    if (ssdb.exists(proxy).asInt() > 0) {
      logger.info(s"$proxy already lose efficacy")
      return false
    }

    if (ifNew && ssdb.zexists(PROXY_LIVE, proxy).asInt() > 0) {
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
    logger.info("Begin to check existing proxies...")
    val list = ssdb.zscan(PROXY_LIVE, "", "", "", -1).datas
    val iterator = list.iterator()
    while (iterator.hasNext) {
      val proxy = new String(iterator.next())
      checkProxy(proxy, ifNew = false)
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
      val proxy = new String(bytes)
      val arr = proxy.split(":")
      if (arr == null || arr.length != 2 || Utils.validProxy(proxy))
        null
      else
        (arr(0), arr(1).toInt)
    }).filter(_ != null)
  }

  def updateProxy(proxy: (String, Int) = null): (String, Int) = {
    if (proxyQueue.isEmpty) {
      proxyQueue ++= getProxies
    }
    if (proxyQueue.isEmpty) throw DFMProxyExhaustedException()
    while (true) {
      if (proxyQueue.isEmpty) return null
      currentProxy = proxyQueue.dequeue()
      if (proxy == null || !proxy.equals(currentProxy)) return currentProxy
    }
    currentProxy
  }

  def getProxy: (String, Int) = {
    if (currentProxy == null) {
      try {
        updateProxy()
      } catch {
        case _: DFMProxyExhaustedException =>
          return null
      }
    }
    currentProxy
  }

  def main(args: Array[String]): Unit = {
//    checkAllProxies()
    IP66Crawler.start()
    ssdb.close()
  }

}
