package space.yangshuai.darkflamemaster.crawler

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger

/**
  * Created by yang
  * Created on 28/01/2017.
  */
package object kuaidaili {
  implicit val headerHost = "www.kuaidaili.com"
  implicit val timeout = 20000
  val logger = Logger("KuaiDaiLi")
  val system = ActorSystem("KuaiDaiLi")
}
