package space.yangshuai.darkflamemaster

import com.typesafe.scalalogging.Logger
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
      Thread.sleep(10l * 60 * 1000)
    }

  }

}
