package space.yangshuai.darkflamemaster.crawler.youdaili

import akka.actor.ActorSystem
import akka.event.Logging.LogLevel
import akka.pattern.ask
import akka.util.Timeout
import space.yangshuai.darkflamemaster.crawler.Crawler

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by rotciv on 2017/1/25.
  */
object YouDaiLiCrawler extends Crawler {

  val system =  ActorSystem("YouDaiLi")
  system.eventStream.setLogLevel(LogLevel(3))

  override def start(): Unit = {
    logger.info("Begin to collect proxies on YouDaiLi...")
    val actor = system.actorOf(SchedulerActor.props, "SchedulerActor")
    implicit val timeout = Timeout(10 minutes)
    val future = actor ? SchedulerActor.Start
    try {
      val result = Await.result(future, 10 minutes).toString
      logger.info(result)
    } catch {
      case e: Exception =>
        logger.error("", e)
    } finally {
      system.terminate()
    }
  }

}
