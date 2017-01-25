package space.yangshuai.darkflamemaster

import com.typesafe.scalalogging.Logger
import space.yangshuai.darkflamemaster.common.Utils
import space.yangshuai.darkflamemaster.crawler.KuaidailiCrawler
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by rotciv on 2017/1/24.
  */
object Scheduler {

  def main(args: Array[String]): Unit = {

    val logger = Logger("Scheduler")

    while (true) {
      logger.info("Begin to check existing proxies...")
      val size = ProxyManager.checkAllProxies()
      logger.info(s"There are $size valid proxies.")

      logger.info("Begin to crawl www.kuaidaili.com.")
      KuaidailiCrawler.crawl()

      val seconds = 10 * 60
      logger.info(s"Next loop start at: ${Utils.futureTime(seconds)}")
      Thread.sleep(1000l * seconds)
    }

  }

}
