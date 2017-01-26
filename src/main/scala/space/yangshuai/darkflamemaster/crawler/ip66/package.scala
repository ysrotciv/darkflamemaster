package space.yangshuai.darkflamemaster.crawler

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger

/**
  * Created by rotciv on 2017/1/26.
  */
package object ip66 {
  implicit val headerHost = "www.66ip.cn"
  implicit val timeout = 20000
  val logger = Logger("66IP")
  val system = ActorSystem("66IP")
}
