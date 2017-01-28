package space.yangshuai.darkflamemaster.common

/**
  * Created by yang
  * Created on 28/01/2017.
  */
object Messages {
  object Start
  object Success
  object Failed
  case class RequestMessage(url: String, proxy: (String, Int))
  case class ConnectionError(url: String, proxy: (String, Int))
}
