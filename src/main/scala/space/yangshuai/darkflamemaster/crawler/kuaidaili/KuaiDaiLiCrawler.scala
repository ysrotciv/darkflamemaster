package space.yangshuai.darkflamemaster.crawler.kuaidaili

import akka.pattern.ask
import akka.util.Timeout
import space.yangshuai.darkflamemaster.common.Messages
import space.yangshuai.darkflamemaster.crawler.Crawler

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by rotciv on 2017/1/24.
  */
object KuaiDaiLiCrawler extends Crawler {

  override def start(): Unit = {
    logger.info("Begin to collect proxies on KuaiDaiLi...")
    val scheduler = system.actorOf(SchedulerActor.props, "KuaiDaiLi_Scheduler")
    implicit val timeout = Timeout(60 minutes)
    val future = scheduler ? Messages.Start
    try {
      val result = Await.result(future, 60 minutes).toString
      logger.info(result)
    } catch {
      case e: Exception =>
        logger.error("", e)
    } finally {
      system.terminate()
    }
  }

}
