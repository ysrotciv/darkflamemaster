package space.yangshuai.darkflamemaster.crawler.kuaidaili

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import space.yangshuai.darkflamemaster.common.Messages
import space.yangshuai.darkflamemaster.db.ProxyManager

/**
  * Created by yang
  * Created on 28/01/2017.
  */
class SchedulerActor extends Actor {

  private var scheduler: ActorRef = _

  override def receive: Receive = {
    case Messages.Start =>
      scheduler = sender
      val actor = system.actorOf(PageActor.props, "kuaidaili")
      val url = "http://www.kuaidaili.com/free/inha/"
      actor ! Messages.RequestMessage(url, null)
    case Messages.ConnectionError(url, proxy) =>
      sender ! Messages.RequestMessage(url, null)
    case Messages.Success =>
      scheduler ! "Completed"
    case Messages.Failed =>
      scheduler ! "Failed"
  }

}

object SchedulerActor {
  val props: Props = Props[SchedulerActor]
}

