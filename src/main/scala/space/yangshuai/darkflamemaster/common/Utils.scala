package space.yangshuai.darkflamemaster.common

import java.text.SimpleDateFormat
import java.util.Date

/**
  * Created by rotciv on 2017/1/25.
  */
object Utils {

  def futureTime(seconds: Int): String = {
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.format(new Date(new Date().getTime + 1000l * seconds))
  }

}
