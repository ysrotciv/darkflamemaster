package space.yangshuai.darkflamemaster.crawler

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger

/**
  * Created by rotciv on 2017/1/26.
  */
package object youdaili {
  implicit val headerHost = "www.youdaili.net/"
  implicit val timeout = 20000
  val logger = Logger("YouDaiLi")
  val system =  ActorSystem("YouDaiLi")
}
