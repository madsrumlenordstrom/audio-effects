package audio

import chisel3._
import scala.math.pow
import utility.Constants.{DATA_WIDTH}

class ClampDistortion(defaultCtrl: Int = 16, clampFrac: Int = 256)
    extends VolumeControl(defaultCtrl) {
  // Calculate clamping value
  // clampFrac determines how large the signal should be before the modules clamps it
  // a value of 4 would mean the signal clamps at a quarter it maximum value
  val clampUpper =
    (pow(2.toFloat, (DATA_WIDTH - 1).toFloat).toInt / clampFrac).S
  val clampLower = -clampUpper

  when(postGain > clampUpper) {
    audioOut := clampUpper
  }.elsewhen(postGain < clampLower) {
    audioOut := clampLower
  }
}
