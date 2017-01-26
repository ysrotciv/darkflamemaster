package space.yangshuai.darkflamemaster

import com.typesafe.scalalogging.Logger
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.crawler.youdaili.YouDaiLiCrawler
import space.yangshuai.darkflamemaster.crawler.KuaiDaiLiCrawler
import space.yangshuai.darkflamemaster.crawler.ip66.IP66Crawler
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/24.
  */
object Scheduler {

  def main(args: Array[String]): Unit = {

    val logger = Logger("Scheduler")

    while (true) {
      try {
        val size = ProxyManager.checkAllProxies()
        logger.info(s"There are $size valid proxies.")

        logger.info("Begin to crawl www.kuaidaili.com...")
        KuaiDaiLiCrawler.start()
        logger.info("Begin to crawl www.66ip.cn...")
        IP66Crawler.start()
        logger.info("Begin to crawl www.youdaili.net")
        YouDaiLiCrawler.start()
      } catch {
        case e: Exception => logger.error("", e)
      } finally {
        val seconds = 10 * 60
        logger.info(s"Next loop start at: ${Utils.futureTime(seconds)}")
        Thread.sleep(1000l * seconds)
      }
    }

  }

}
