package audio

import chisel3._
import scala.math.pow
import utility.Constants.{DATA_WIDTH}

class ClampDistortion(defaultCtrl: Int = 16) extends VolumeControl(defaultCtrl) {
  // Calculate clamping value
  val clampUpper = (pow(2.toFloat, (DATA_WIDTH - 1).toFloat).toInt / 4096).S
  val clampLower = -clampUpper

  when (postGain > clampUpper) {
    io.audioOut := clampUpper
  }. elsewhen(postGain < clampLower) {
    io.audioOut := clampLower
  }. otherwise {
    io.audioOut := postGain
  }
}
