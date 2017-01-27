package space.yangshuai.darkflamemaster.crawler.ip66

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import space.yangshuai.darkflamemaster.db.ProxyManager
import space.yangshuai.darkflamemaster.common.Utils._
import space.yangshuai.darkflamemaster.exception.DFMProxyExhaustedException

/**
  * Created by rotciv on 2017/1/26.
  */
class SchedulerActor extends Actor {
  import SchedulerActor._

  var pageCount = 26
  var successCount: Int = 0
  var failureCount: Int = 0
  var entry: ActorRef = _

  override def receive: Receive = {
    case Start =>
      entry = sender
      for (i <- 1 to 26) {
        val url = s"http://www.66ip.cn/areaindex_$i/1.html"
        val actor = system.actorOf(PageActor.props, s"66IP-$i")
        try {
          actor ! RequestMessage(url, ProxyManager.updateProxy())
        } catch {
          case _: DFMProxyExhaustedException =>
            actor ! RequestMessage(url, null)
        }
      }
    case PageActor.Success =>
      context.stop(sender)
      pageCount -= 1
      successCount += 1
      if (pageCount <= 0) entry ! result("Completed", successCount, failureCount)
    case PageActor.Failed =>
      context.stop(sender)
      pageCount -= 1
      failureCount += 1
      if (pageCount <= 0) entry ! result("Completed", successCount, failureCount)
    case PageActor.ConnectionError(url, proxy) =>
      try {
        sender ! RequestMessage(url, ProxyManager.updateProxy(proxy))
      } catch {
        case _: DFMProxyExhaustedException =>
          sender ! RequestMessage(url, null)
      }
  }

}

object SchedulerActor {
  val props: Props = Props[SchedulerActor]
  object Start
  case class RequestMessage(url: String, proxy: (String, Int))
}