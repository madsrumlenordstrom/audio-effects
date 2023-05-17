package audio

import chisel3._
import utility.Constants.{CTRL_WIDTH, DATA_WIDTH}

class VolumeControl(controlInit: Int = 16, bypassInit: Boolean = false)
    extends DSPModule(controlInit, bypassInit) {
  // Create a fixed point (24.4) version of audio signal
  val audioInFix = (audioInReg ## 0.S((CTRL_WIDTH / 2).W)).asSInt

  // Multiply by control signal and shift back to non fixed point
  val postGain =
    (audioInFix * ctrlReg.zext)(DATA_WIDTH + CTRL_WIDTH - 1, CTRL_WIDTH).asSInt
  audioOut := postGain
}
