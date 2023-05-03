package audio

import chisel3._
import utility.Constants.{CTRL_WIDTH, DATA_WIDTH}

class VolumeControl(defaultCtrl: Int = 16) extends DSPModule(defaultCtrl) {
  // Create a fixed point (24.4) version of audio signal
  val audioInFix = audioInReg ## 0.S((CTRL_WIDTH/2).W)

  // Multiply by control signal and shift back to non fixed point
  val postGain = ((audioInFix*ctrlReg.zext) >> CTRL_WIDTH)(DATA_WIDTH - 1, 0).asSInt
  io.audioOut := postGain
}
