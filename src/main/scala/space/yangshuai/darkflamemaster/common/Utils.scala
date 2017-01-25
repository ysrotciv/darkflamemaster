package space.yangshuai.darkflamemaster.common

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.ConfigFactory

/**
  * Created by rotciv on 2017/1/25.
  */
object Utils {

  private val conf = ConfigFactory.load()
  val userAgent = conf.getString("user_agent")

  def futureTime(seconds: Int): String = {
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.format(new Date(new Date().getTime + 1000l * seconds))
  }

}
