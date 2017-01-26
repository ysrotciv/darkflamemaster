package space.yangshuai.darkflamemaster.common

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
  * Created by rotciv on 2017/1/25.
  */
object Utils {

  private val conf = ConfigFactory.load()
  val userAgent: String = conf.getString("user_agent")
  private val IP_REGEX = "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))"

  def futureTime(seconds: Int): String = {
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.format(new Date(new Date().getTime + 1000l * seconds))
  }

  def commonRequest(url: String, proxy: (String, Int))(implicit headerHost: String, timeout: Int): Document = {
    val request = Jsoup.connect(url).userAgent(Utils.userAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
      .header("Accept-Encoding", "gzip, deflate, sdch")
      .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2,es;q=0.2")
      .header("Cache-Control", "max-age=0")
      .header("Connection", "keep-alive")
      .header("Host", headerHost)
      .timeout(timeout)

    if (proxy == null)
      request.get()
    else
      request.proxy(proxy._1, proxy._2).get()
  }

  /**
    * Check if it is a valid proxy string
    * @param proxy format of "123.123.123.123:8888"
    * @return
    */
  def validProxy(proxy: String): Boolean = {
    if (!proxy.contains(":")) return false
    val arr = proxy.split(":")
    if (arr.length != 2) return false
    validHost(arr(0)) && validPort(arr(1))
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

}
