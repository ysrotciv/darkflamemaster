package space.yangshuai.darkflamemaster

import com.typesafe.scalalogging.Logger
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.crawler.{IP66Crawler, KuaiDaiLiCrawler}
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/24.
  */
object Scheduler {

  def main(args: Array[String]): Unit = {

    val logger = Logger("Scheduler")

    while (true) {
      val size = ProxyManager.checkAllProxies()
      logger.info(s"There are $size valid proxies.")

      logger.info("Begin to crawl www.kuaidaili.com...")
      KuaiDaiLiCrawler.start()
      logger.info("Begin to crawl www.66ip.cn...")
      IP66Crawler.start()

      val seconds = 10 * 60
      logger.info(s"Next loop start at: ${Utils.futureTime(seconds)}")
      Thread.sleep(1000l * seconds)
    }

  }

}
