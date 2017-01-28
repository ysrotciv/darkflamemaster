package space.yangshuai.darkflamemaster.crawler.ip66

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import space.yangshuai.darkflamemaster.crawler.Crawler

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by rotciv on 2017/1/25.
  */
object IP66Crawler extends Crawler{

  val logger = Logger("66IP")

  override def start(): Unit = {
    logger.info("Begin to collect proxies on 66IP...")
    val scheduler = system.actorOf(SchedulerActor.props, "IP66_Scheduler")
    implicit val timeout = Timeout(60 minutes)
    val future = scheduler ? SchedulerActor.Start
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
